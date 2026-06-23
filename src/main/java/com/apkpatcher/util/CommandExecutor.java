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
package com.apkpatcher.util;

import com.reandroid.apk.APKLogger;
import com.reandroid.arsc.ARSCLib;
import com.reandroid.arsc.coder.xml.XmlCoderLogger;

import java.io.File;

public class CommandExecutor implements APKLogger, XmlCoderLogger {

    private boolean mEnableLog = true;
    private String mLogTag;

    public CommandExecutor(String mLogTag) {
        this.mLogTag = mLogTag;
    }

    protected void delete(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        logMessage("Delete: " + file);
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            Utils.deleteDir(file);
        }
    }

    @Override
    public void logMessage(String msg) {
        if (!mEnableLog) {
            return;
        }
        Log.i(mLogTag, msg);
    }
    @Override
    public void logError(String msg, Throwable tr) {
        if (!mEnableLog) {
            return;
        }
        Log.e(mLogTag, msg, tr);
    }
    @Override
    public void logVerbose(String msg) {
        if (!mEnableLog) {
            return;
        }
        Log.d(mLogTag, msg);
    }
    @Override
    public void logMessage(String mLogTag, String msg) {
        if (!mEnableLog) {
            return;
        }
        Log.d(mLogTag, msg);
    }

    @Override
    public void logVerbose(String mLogTag, String msg) {
        if (!mEnableLog) {
            return;
        }
        Log.d(mLogTag, msg);
    }
    public void logWarn(String msg) {
        Log.e(mLogTag, msg);
    }

    public void logVersion() {
        logMessage("Using: " + ARSCLib.getName() + " version " + ARSCLib.getVersion());
    }
}