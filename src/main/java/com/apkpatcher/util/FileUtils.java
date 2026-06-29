package com.apkpatcher.util;

import java.io.File;

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
}