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
import com.github.unidbg.linux.android.dvm.BaseVM;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.VaList;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import lombok.SneakyThrows;

public class AobiShield extends AbstractJni implements IOResolver<AndroidFileIO> {
    private static final String APK_PATH = "data/aobi/base.apk";
    private static final String APK_ROOT_PATH = "/data/app/com.taomee.moleleiting-yQDSCp3KshKyHlSwvkRYBA==/base.apk";
    private static final String APP_PACKAGE_NAME = "com.leiting.aobi";
    private static final String SO_PATH = "data/aobi/lib/arm64-v8a/libNetHTProtect.so";
    private final File File_APK;
    private final DvmClass JNIFactory;
    private final AndroidEmulator emulator;
    private boolean hasinit = false;
    private final VM vm;

    @SneakyThrows
    AobiShield(UnidbgProperties unidbgProperties) {
        EmulatorBuilder<AndroidEmulator> builder = AndroidEmulatorBuilder.for64Bit().setProcessName(APP_PACKAGE_NAME);
        if (unidbgProperties != null && unidbgProperties.isDynarmic()) {
            builder.addBackendFactory(new DynarmicFactory(true));
        }
        this.emulator = builder.build();
        this.emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = this.emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        this.File_APK = ResFileUtils.open(APK_PATH);
        this.vm = this.emulator.createDalvikVM(this.File_APK);
        this.vm.setVerbose(unidbgProperties != null && unidbgProperties.isVerbose());
        File soLibFile = ResFileUtils.open(SO_PATH);
        DalvikModule dm = this.vm.loadLibrary(soLibFile, true);
        System.out.println("- - - - - - - - - -aObi call JNI_OnLoad - - - - - - - - - -");
        this.vm.setJni(this);
        dm.callJNI_OnLoad(this.emulator);
        this.JNIFactory = this.vm.resolveClass("com/netease/htprotect/factory/JNIFactory");
    }

    public static void main(String[] args) {
        UnidbgProperties unidbgProperties = new UnidbgProperties();
        AobiShield test = new AobiShield(unidbgProperties);
        //test.htp_safe_comm("CAE=", 0, false);
    }

    public void destroy() throws IOException {
        this.emulator.close();
    }

    public byte[] r25d273c7ad4065c3(byte[] bytes, int val, int val2, boolean bool) {
        ByteArray array = this.JNIFactory.callStaticJniMethodObject(this.emulator, "r25d273c7ad4065c3([BIIZ)[B", new ByteArray(this.vm, bytes), val, val2, bool);
        return (byte[]) array.getValue();
    }

    public String safeCommToServer(String str) {
        byte[] bytes = r25d273c7ad4065c3(str.getBytes(), 0, 0, false);
        String s = new String(Arrays.copyOfRange(bytes, 4, bytes.length));
        return new BigInteger(1, Base64.decodeBase64(s)).toString(16).toUpperCase();
    }

    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (!"Ljava/lang/reflect/Field->setAccessible(Z)V".equals(signature)) {
            super.callVoidMethodV(vm, dvmObject, signature, vaList);
        }
    }

    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/Class->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;":
                return vm.resolveClass("Ljava/lang/reflect/Field").newObject(null);
            case "java/lang/Class->getClass()Ljava/lang/Class;":
                return vm.resolveClass("java/lang/Class");
            case "java/lang/Class->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;":
            case "Ljava/lang/reflect/Field->get(Ljava/lang/Object;)Ljava/lang/Object;":
                return vm.resolveClass("Ljava/lang/Object;");
            case "java/lang/Class->getPackageResourcePath()Ljava/lang/String;":
                return new StringObject(vm, APK_ROOT_PATH);
            case "java/lang/Class->getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;":
                return vm.resolveClass("Ljava/lang/reflect/Method;");
            default:
                return super.callObjectMethodV(vm, dvmObject, signature, vaList);
        }
    }

    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        return "java/lang/Class->forName(Ljava/lang/String;)Ljava/lang/Class;".equals(signature) ? vm.resolveClass(vaList.getObjectArg(0).toString()) : super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    public FileResult<AndroidFileIO> resolve(Emulator emulator, String pathname, int oflags) {
        if (APK_ROOT_PATH.equals(pathname)) {
            return FileResult.success(new SimpleFileIO(oflags, this.File_APK, pathname));
        }
        return null;
    }
}
