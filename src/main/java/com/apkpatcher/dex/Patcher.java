package com.apkpatcher.dex;

import com.apkpatcher.util.Log;
import com.apkpatcher.util.BackgroundWorker;

import com.reandroid.apk.ApkModule;
import com.reandroid.apk.DexFileInputSource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Patcher {

    private static final AtomicReference<Exception> mFirstError = new AtomicReference<>();
    private static final List<DexPatcher.PatchResult> results = new CopyOnWriteArrayList<>();
    private static final int jobs = Math.min(Runtime.getRuntime().availableProcessors(), 8);
    private static BackgroundWorker worker = null;

    public static void patch(ApkModule module) throws Exception {

        try {
            if (jobs > 1) {
                worker = new BackgroundWorker(jobs - 1);
                Log.i("[INFO]", "Using " + jobs + " threads for patching");
            }

            /* Patch dex */
            for (DexFileInputSource source : module.listDexFiles()) {
                if (source == null) continue;
                byte[] dexBytes;
                try (InputStream inputStream = source.openStream()) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    dexBytes = outputStream.toByteArray();
                }

                processPatch(source.getAlias(), dexBytes);
            }

            if (worker != null) {
                worker.waitForFinish();
                if (mFirstError.get() != null) {
                    throw mFirstError.get();
                }
            }

            /* Build dex */
            mFirstError.set(null);
            for (DexPatcher.PatchResult result : results) {
                processBuild(module, result);
            }

            if (worker != null) {
                worker.waitForFinish();
                if (mFirstError.get() != null) {
                    throw mFirstError.get();
                }
            }
        } finally {
            if (worker != null) {
                worker.shutdownNow();
            }
        }
    }

    private static void processPatch(String name, byte[] dexBytes) throws Exception {
        if (worker != null) {
            worker.submit(() -> {
                if (mFirstError.get() == null) {
                    try {
                        DexPatcher.PatchResult result = DexPatcher.patch(name, dexBytes);
                        if (result != null) {
                            results.add(result);
                        }
                    } catch (Exception ex) {
                        mFirstError.compareAndSet(null, ex);
                    }
                }
            });
        } else {
            DexPatcher.PatchResult result = DexPatcher.patch(name, dexBytes);
            if (result != null) {
                results.add(result);
            }
        }
    }

    private static void processBuild(ApkModule module, DexPatcher.PatchResult result) throws Exception {
        if (worker != null) {
            worker.submit(() -> {
                if (mFirstError.get() == null) {
                    try {
                        DexPatcher.build(module, result);
                    } catch (Exception ex) {
                        mFirstError.compareAndSet(null, ex);
                    }
                }
            });
        } else {
            DexPatcher.build(module, result);
        }
    }
}