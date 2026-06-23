package com.apkpatcher.dex;

import com.apkpatcher.util.Log;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DexPatcher {

    public static class PatchResult {
        public final String name;
        public final List<ClassDef> patchedClasses;

        public PatchResult(String name, List<ClassDef> patchedClasses) {
            this.name = name;
            this.patchedClasses = patchedClasses;
        }
    }

    public static PatchResult patch(String name, byte[] dexBytes) throws Exception {

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        File tempDex = File.createTempFile("temp", ".dex", tempDir);

        List<ClassDef> patchedClasses = new ArrayList<>();
        boolean modified = false;

        try {

            Files.write(tempDex.toPath(), dexBytes);

            DexFile dexFile = DexFileFactory.loadDexFile(
                    tempDex,
                    Opcodes.getDefault()
            );
            
            for (ClassDef classDef : dexFile.getClasses()) {

                Log.o("[PATCH]", "Patching dex...");

                boolean classModified = false;

                List<Method> directMethods = new ArrayList<>();
                for (Method method : classDef.getDirectMethods()) {

                    Method patched = PatchMethod.patch(method);

                    if (patched != method) {
                        classModified = true;
                        modified = true;
                    }

                    directMethods.add(patched);
                }

                List<Method> virtualMethods = new ArrayList<>();
                for (Method method : classDef.getVirtualMethods()) {

                    Method patched = PatchMethod.patch(method);

                    if (patched != method) {
                        classModified = true;
                        modified = true;
                    }

                    virtualMethods.add(patched);
                }

                if (classModified) {
                    patchedClasses.add(
                        new ImmutableClassDef(
                            classDef.getType(),
                            classDef.getAccessFlags(),
                            classDef.getSuperclass(),
                            classDef.getInterfaces(),
                            classDef.getSourceFile(),
                            classDef.getAnnotations(),
                            classDef.getStaticFields(),
                            classDef.getInstanceFields(),
                            directMethods,
                            virtualMethods
                        )
                    );
                } else {
                    patchedClasses.add(classDef);
                }
            }
        } finally {
            Files.deleteIfExists(tempDex.toPath());
        }

        return modified ? new PatchResult(name, patchedClasses) : null;
    }

    public static void build(ApkModule module, PatchResult result) throws Exception {

        Log.o("[BUILD]", "Building dex ... ");
        Log.i("[BUILD]", "Building " + result.name);

        MemoryDataStore dataStore = new MemoryDataStore();
        DexPool dexPool = new DexPool(Opcodes.getDefault());

        for (ClassDef classDef : result.patchedClasses) {
            dexPool.internClass(classDef);
        }

        dexPool.writeTo(dataStore);

        byte[] resultBytes = java.util.Arrays.copyOf(
                dataStore.getData(),
                dataStore.getSize()
        );

        synchronized (module) {
            module.add(
                new ByteInputSource(
                    resultBytes,
                    result.name
                )
            );
        }
    }
}