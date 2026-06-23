package com.apkpatcher.util;

import java.io.InputStream;
import java.util.Properties;

public class Info {

    private static Properties sProperties;

    public static String getName() {
        Properties properties=getProperties();
        return properties.getProperty("project", "APKPatcher");
    }
    public static String getVersion() {
        Properties properties=getProperties();
        return properties.getProperty("version", "");
    }
    public static String getRepo() {
        Properties properties=getProperties();
        return properties.getProperty("repo", "https://github.com/isPatcher");
    }
    public static String getDescription() {
        Properties properties=getProperties();
        return properties.getProperty("description", "Failed to load properties");
    }
    
    private static Properties getProperties() {
        if (sProperties==null) {
            sProperties=loadProperties();
        }
        return sProperties;
    }
    private static Properties loadProperties() {
        InputStream inputStream= Info.class.getResourceAsStream("/apkpatcher.properties");
        Properties properties=new Properties();
        try{
            properties.load(inputStream);
        }catch (Exception ignored) {
        }
        return properties;
    }
}