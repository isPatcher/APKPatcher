package com.apkpatcher;

import com.apkpatcher.dex.Patcher;
import com.apkpatcher.merge.Merger;
import com.apkpatcher.xml.XMLPatcher;
import com.apkpatcher.ui.*;
import com.apkpatcher.util.*;

import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.ARSCLib;
import com.reandroid.archive.WriteProgress;

import java.io.*;
import java.nio.file.Files;
import java.util.Set;

public class Main {

    private static final Set<String> SPLIT_EXTENSIONS = Set.of(".apks", ".apkm", ".xapk");

    private static boolean isSplitPackage(String path) {
        String lower = path.toLowerCase();

        return SPLIT_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            help.help();
            return;
        }

        String inputPath = null;

        for (int i = 0; i < args.length; i++) {
            if ("-i".equals(args[i]) && i + 1 < args.length) {
                inputPath = args[i + 1];
            }
        }

        if (inputPath == null) {
            help.help();
            return;
        }

        File inputApk = new File(inputPath);

        if (!inputApk.exists()) {
            Log.w("[WARN]", "Input file not found: " + inputPath);
            return;
        }

        banner.banner();

        Log.i("[INFO]", "Version : " + Info.getVersion());
        Log.i("[INFO]", "ARSCLib : " + ARSCLib.getVersion());

        try {
            ApkModule module;
            File mergedApkFile;

            Log.i("[INFO]", "Loading APK...");
            if (inputPath.endsWith(".apk")) {
                module = ApkModule.loadApkFile(inputApk);
                mergedApkFile = inputApk;
            } else if (isSplitPackage(inputPath)) {
                Log.i("[INFO]", "Merging APK's...");
                Merger merger = new Merger();
                merger.mergeApkFile(inputApk);
                mergedApkFile = merger.generateOutputFromInput(inputApk);
                if (!mergedApkFile.exists()) {
                    return;
                }
                module = ApkModule.loadApkFile(mergedApkFile);
            } else {
                Log.w("[WARN]", "Supported Extension : '.apk' & " + SPLIT_EXTENSIONS);
                Log.e("[ERROR]", "UnSupported Extension : " + inputPath);
                return;
            }

            Log.i("[INFO]", "Patchng XMLs...");
            XMLPatcher.addNSC(module);

            try {
                Patcher.patch(module);
            } catch (Exception e) {
                Log.e("[ERROR]", "Patching failed: " + e.getMessage());
            }

            File patchedApk = FileUtils.generateOutputFromInput(inputApk, "_Patched.apk");

            Log.i("[BUILD]", "Building APK...");
            FileUtils.write(module, patchedApk);
            Log.i("[BUILD]", "APK built at: " + patchedApk.getAbsolutePath());

            Log.i("[BUILD]", "Process completed");
            Log.i("[TIME]", Log.elapsedTime());

        } catch (Exception e) {
            Log.e("[ERROR]", "Process failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}