/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.apkpatcher.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.*;
import java.util.Set;

public final class Log {

    private static final ConcurrentMap<String, Logger> sCache = new ConcurrentHashMap<>();
    private static final Set<String> logged = ConcurrentHashMap.newKeySet();
    private static final long START_TIME = System.currentTimeMillis();

    private Log() {
        // Private constructor for utility class.
    }

    static {
        setupLogging();
    }

    private static void setupLogging() {
        LogManager.getLogManager().reset();
        Logger logger = Logger.getLogger("");

        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }
                try {
                    String message = getFormatter().format(record);
                    int level = record.getLevel().intValue();
                    if (level >= Level.WARNING.intValue()) {
                        System.err.println(message);
                    } else {
                        System.out.println(message);
                    }
                } catch (Exception ex) {
                    reportError(null, ex, ErrorManager.FORMAT_FAILURE);
                }
            }

            @Override
            public void flush() {
                System.out.flush();
                System.err.flush();
            }

            @Override
            public void close() throws SecurityException {
                flush();
            }
        };
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String prefix;
                int level = record.getLevel().intValue();
                if (level >= Level.SEVERE.intValue()) {
                    prefix = "E";
                } else if (level >= Level.WARNING.intValue()) {
                    prefix = "W";
                } else if (level >= Level.INFO.intValue()) {
                    prefix = "I";
                } else {
                    prefix = "D";
                }
                return prefix + ": " + record.getMessage();
            }
        });
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
    }

    private static void log(Level level, String tag, String message) {
        Logger logger = sCache.computeIfAbsent(tag, Logger::getLogger);
        if (logger.isLoggable(level)) {
            logger.log(level, tag + " " + message);
        }
    }

    private static void log(Level level, String tag, String message, Object... args) {
        Logger logger = sCache.computeIfAbsent(tag, Logger::getLogger);
        if (logger.isLoggable(level)) {
            logger.log(level, tag + " " + String.format(message, args));
        }
    }

    public static void d(String tag, String message) {
        log(Level.FINE, tag, message);
    }

    public static void d(String tag, String message, Object... args) {
        log(Level.FINE, tag, message, args);
    }

    public static void i(String tag, String message) {
        log(Level.INFO, tag, message);
    }

    public static void i(String tag, String message, Object... args) {
        log(Level.INFO, tag, message, args);
    }

    public static void w(String tag, String message) {
        log(Level.WARNING, tag, message);
    }

    public static void w(String tag, String message, Object... args) {
        log(Level.WARNING, tag, message, args);
    }

    public static void e(String tag, String message) {
        log(Level.SEVERE, tag, message);
    }

    public static void e(String tag, String message, Object... args) {
        log(Level.SEVERE, tag, message, args);
    }
    
    public static void o(String tag, String message) {
        if (logged.add(message)) {
            log(Level.INFO, tag, message);
        }
    }

    public static String elapsedTime() {

        long millis = System.currentTimeMillis() - START_TIME;

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(minutes);

        if (minutes == 0) {
            return seconds + " sec";
        }

        return minutes + " min " + seconds + " sec";
    }
}