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
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoleShield extends AbstractJni implements IOResolver<AndroidFileIO> {
    private static final String APK_PATH = "data/mole/base.apk";
    private static final String APK_ROOT_PATH = "/data/app/com.taomee.moleleiting-yQDSCp3KshKyHlSwvkRYBA==/base.apk";
    private static final String APP_PACKAGE_NAME = "com.taomee.moleleiting";
    private static final String CmdLine_PATH = "data/mole/root/proc/self/cmdline";
    private static final String MAPS_PATH = "data/mole/root/proc/self/maps";
    private static final String SO_PATH = "data/mole/lib/arm64-v8a/libNetHTProtect.so";
    private File File_APK;
    private File File_cmdline;
    private File File_maps;
    private DvmClass JNIFactory;
    private AndroidEmulator emulator;
    private boolean hasinit = false;
    private VM vm;

    @SneakyThrows
    MoleShield(UnidbgProperties unidbgProperties) {
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
        vm = emulator.createDalvikVM(File_APK);
        vm.setVerbose(unidbgProperties != null && unidbgProperties.isVerbose());
        File soLibFile = ResFileUtils.open(SO_PATH);
        DalvikModule dm = vm.loadLibrary(soLibFile, true);
        vm.setJni(this);
        dm.callJNI_OnLoad(emulator);
        JNIFactory = vm.resolveClass("com/netease/htprotect/factory/JNIFactory");
    }

    public void destroy() throws IOException{
        emulator.close();
    }

    public void htp_init(String appid, String game_key, int serverType) {
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        JNIFactory.callStaticJniMethodObject(emulator, "hccd63688a790ca65(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;I)V", context, appid, game_key, serverType);
        hasinit = true;
    }

    public byte[] htp_safe_comm(String buf, int algType, boolean dec) {
        if (!hasinit) {
            htp_init("A005824535", "59972d6a42b4f5a9824dc4e10b469827", 1);
        }
        ByteArray array = JNIFactory.callStaticJniMethodObject(emulator, "r25d273c7ad4065c3(Ljava/lang/String;IZ)[B", buf, algType, dec);
        return (byte[]) array.getValue();
    }

    public static void main(String[] args) {
        UnidbgProperties unidbgProperties = new UnidbgProperties();
        MoleShield test = new MoleShield(unidbgProperties);
        //test.htp_safe_comm("CAE=", 0, false);
    }

    public String safeComm(String p0) {
        byte[] bytes = htp_safe_comm(p0, 0, false);
        return new BigInteger(1, bytes).toString(16).toUpperCase();
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
