/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apkpatcher.merge;

import com.apkpatcher.util.*;

import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.apk.AndroidManifestBlockSplitSanitizer;
import com.reandroid.archive.ArchiveEntry;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.utils.HexUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class Merger extends CommandExecutor {

    public Merger() {
        super("[MERGE]");
    }

    public File generateOutputFromInput(File input) {
        return FileUtils.generateOutputFromInput(input, "_merged.apk");
    }

    public void mergeApkFile(File inputFile) throws IOException {

        File originalFile = inputFile;

        // Force overwrite old merged apk
        File outFile = generateOutputFromInput(originalFile);
        delete(outFile);

        boolean extracted = false;

        if (inputFile.isFile()) {
            inputFile = extractFile(inputFile);
            extracted = true;
        }

        logMessage("Searching apk files ...");
                
        ApkBundle bundle = new ApkBundle();
        bundle.setAPKLogger(this);
        bundle.loadApkDirectory(inputFile, extracted);
        logMessage("Found modules: " + bundle.getApkModuleList().size());
        for(ApkModule apkModule:bundle.getApkModuleList()) {
            String protect = Utils.isProtected(apkModule);
            if (protect != null) {
                logMessage(inputFile.getAbsolutePath());
                logMessage(protect);
                bundle.close();
                if (extracted) {
                    Utils.deleteDir(inputFile);
                    inputFile.deleteOnExit();
                }
                return;
            }
        }
        ApkModule mergedModule = bundle.mergeModules();
        sanitizeManifest(mergedModule);
        logMessage("Writing APK...");
        mergedModule.writeApk(outFile);
        mergedModule.close();
        bundle.close();
        if (extracted) {
            Utils.deleteDir(inputFile);
            inputFile.deleteOnExit();
        }
        logMessage("Saved to: " + outFile);
    }

    private File extractFile(File file) throws IOException {
        File tmp = toTmpDir(file);
        logMessage("Extracting to: " + tmp);
        if (tmp.exists()) {
            logMessage("Delete: " + tmp);
            Utils.deleteDir(tmp);
        }
        tmp.deleteOnExit();
        ArchiveFile archive = new ArchiveFile(file);
        fixFilePermissions(archive);
        Predicate <ArchiveEntry> filter = archiveEntry -> archiveEntry.getName().endsWith(".apk");
        int count = archive.extractAll(tmp, filter, this);
        //int count = archive.extractAll(tmp, filter, null);
        archive.close();
        if (count == 0) {
            throw new IOException("No *.apk files found on: " + file);
        }
        return tmp;
    }

    private void fixFilePermissions(ArchiveFile archive) {
        int rw_all = 438; // equivalent to chmod 666
        Iterator<ArchiveEntry> iterator = archive.iterator();
        while (iterator.hasNext()) {
            ArchiveEntry entry = iterator.next();
            entry.getCentralEntryHeader()
                    .getFilePermissions().permissions(rw_all);
        }
    }

    private File toTmpDir(File file) {
        String name = file.getName();
        name = HexUtil.toHex8("tmp_", name.hashCode());
        File dir = file.getParentFile();
        File tmp;
        if (dir == null) {
            tmp = new File(name);
        }else {
            tmp = new File(dir, name);
        }
        tmp = Utils.ensureUniqueFile(tmp);
        return tmp;
    }

    public void sanitizeManifest(ApkModule apkModule) {

        logMessage("Sanitizing manifest ...");

        AndroidManifestBlockSplitSanitizer sanitizer = new AndroidManifestBlockSplitSanitizer();
        sanitizer.sanitize(apkModule);

        logMessage("Patching AndroidManifest.xml");

        AndroidManifestBlock manifest = apkModule.getAndroidManifest();

        if (manifest == null) {
            return;
        }

        Boolean extractNativeLibs = manifest.isExtractNativeLibs();
        if (extractNativeLibs != null && !extractNativeLibs) {
            manifest.setExtractNativeLibs(true);
        }

        List<String> activitiesToRemove = Collections.singletonList(
                "com.apkpatcher.licensecheck.LicenseActivity"
        );
        List<String> providersToRemove = Collections.singletonList(
                "com.apkpatcher.licensecheck.LicenseContentProvider"
        );
        List<String> permissionsToRemove = Collections.singletonList(
                "com.android.vending.CHECK_LICENSE"
        );

        removeElementsByName(manifest, "activity", activitiesToRemove);
        removeElementsByName(manifest, "provider", providersToRemove);
        removeElementsByName(manifest, "uses-permission", permissionsToRemove);

        manifest.refresh();
        apkModule.refreshManifest();
        apkModule.refreshTable();
    }

    private void removeElementsByName(AndroidManifestBlock manifest, String tag, List<String> namesToRemove) {

        Iterator<ResXmlElement> iterator;

        if ("uses-permission".equals(tag)) {
            iterator = manifest.getManifestElement().getElements(tag);
        } else {
            iterator = manifest.listApplicationElementsByTag(tag).iterator();
        }
        List<ResXmlElement> elementsToRemove = new ArrayList<>();

        while (iterator.hasNext()) {
            ResXmlElement element = iterator.next();
            String name = AndroidManifestBlock.getAndroidNameValue(element);
            if (namesToRemove.contains(name)) {
                elementsToRemove.add(element);
            }
        }
        for (ResXmlElement element : elementsToRemove) {
            element.removeSelf();
        }
    }
}