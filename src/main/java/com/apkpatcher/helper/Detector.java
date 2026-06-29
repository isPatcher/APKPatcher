package com.apkpatcher.helper;

import com.apkpatcher.lib.FlutterSSL;
import com.apkpatcher.util.Log;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;

import java.io.IOException;

public class Detector {

    public static void isFlutter(ApkModule module) throws IOException {

        for (InputSource inputSource : module.listNativeLibraryFiles()) {

            String name = inputSource.getAlias();

            if (name != null && name.startsWith("lib/") && name.endsWith("/libflutter.so")) {

                String arch = name.substring(4, name.indexOf('/', 4));

                Log.o("[INFO]", "Patching Flutter SSL...");
                Log.o("[INFO]", "Flutter : libflutter.so ✓");
                Log.i("[INFO]", "Arch : " + arch);
                Log.i("[INFO]", "Patching : " + name);

                byte[] originalBytes = inputSource.openStream().readAllBytes();

                if (FlutterSSL.patchLibrary(originalBytes, arch)) {

                    synchronized (module) {
                        module.removeInputSource(name);
                        module.add(
                            new ByteInputSource(
                                originalBytes,
                                name
                            )
                        );
                    }
                }
            }
        }
    }
}