package com.anjia.unidbgserver.service;

import com.anjia.unidbgserver.config.UnidbgProperties;
import com.anjia.unidbgserver.utils.ResFileUtils;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.EmulatorBuilder;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import lombok.SneakyThrows;

public class CaHxShield extends AbstractJni implements IOResolver<AndroidFileIO> {
    private static final String APP_PACKAGE_NAME = "com.cahx.sy";
    private static final String APK_PATH = "data/cahx/base.apk";
    private static final String CmdLine_PATH = "data/cahx/root/proc/self/cmdline";
    private static final String MAPS_PATH = "data/cahx/root/proc/self/maps";
    private static final String APK_ROOT_PATH = "/data/app/~~qykDtm5XZnDC8ghdkrodIg==/com.cahx.sy-CHLAeR4DnLDPcVfmiEQchw==/base.apk";
    private static final String SO_PATH = "data/cahx/lib/arm64-v8a/libnative-lib.so";
    private File File_APK;
    private File File_cmdline;
    private File File_maps;
    private DvmClass JNIFactory;
    private AndroidEmulator emulator;
    private boolean hasinit = false;
    private VM vm;

    @SneakyThrows
    CaHxShield(UnidbgProperties unidbgProperties) {
        EmulatorBuilder<AndroidEmulator> builder = AndroidEmulatorBuilder.for64Bit().setProcessName(APP_PACKAGE_NAME);
        if (unidbgProperties != null && unidbgProperties.isDynarmic()) {
            builder.addBackendFactory(new DynarmicFactory(true));
        }
        emulator = builder.build();
        emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = this.emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        File_cmdline = ResFileUtils.open(CmdLine_PATH);
        File_maps = ResFileUtils.open(MAPS_PATH);
        File_APK = ResFileUtils.open(APK_PATH);
        vm = emulator.createDalvikVM();
        vm.setVerbose(true);
        File soLibFile = ResFileUtils.open(SO_PATH);
        DalvikModule dm = vm.loadLibrary(soLibFile, false);
       // vm.setJni(this);
       // dm.callJNI_OnLoad(emulator);
       // JNIFactory = vm.resolveClass("com/shiyue/game/utils/NativeUtils");
    }

    public void destroy() throws IOException {
        emulator.close();
    }

    public void signGeneration(String str, String str2) {
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        StringObject ret = JNIFactory.callStaticJniMethodObject(emulator, "signGeneration(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", context, str, str2);
    }

    public static void main(String[] args) {
        UnidbgProperties unidbgProperties = new UnidbgProperties();
        CaHxShield test = new CaHxShield(unidbgProperties);
       // test.signGeneration("CAE=","jmUl7v4IVMcrc6lTF2lSAtlnkpXMeNHWPVIo70bDXc0=");
    }

    public FileResult<AndroidFileIO> resolve(Emulator emulator, String pathname, int oflags) {
        if ("/proc/self/cmdline".equals(pathname)) {
            return FileResult.success(new SimpleFileIO(oflags, File_cmdline, pathname));
        }
        if ("/proc/self/maps".equals(pathname)) {
            return FileResult.success(new SimpleFileIO(oflags, File_maps, pathname));
        }
        if (APK_ROOT_PATH.equals(pathname)) {
            return FileResult.success(new SimpleFileIO(oflags, File_APK, pathname));
        }
        return null;
    }

}

