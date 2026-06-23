package com.apkpatcher.util;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.WriteProgress;

import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static File generateOutputFromInput(File file, String suffix) {
        String name = file.getName();
        if (file.isFile()) {
            int idx = name.lastIndexOf('.');
            if (idx > 0) {
                name = name.substring(0, idx);
            }
        }
        name = name + suffix;
        File dir = file.getParentFile();
        if (dir == null) {
            return new File(name);
        }
        return new File(dir, name);
    }

    public static void write(ApkModule module, File file) throws IOException {
        module.writeApk(file, new WriteProgress() {
            @Override
            public void onCompressFile(String path, int method, long length) {
            }
        });
    }

    public static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}