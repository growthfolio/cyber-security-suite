package com.codexraziel.cybersec.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigManager {
    
    @Value("${cybersec.tools.bruteforce.path:./tools/redteam-bruteforce/bin/redteam-bf}")
    private String bruteForcePath;
    
    @Value("${cybersec.tools.keylogger.path:./tools/stealth-keylogger/bin/system-monitor}")
    private String keyloggerPath;
    
    @Value("${cybersec.tools.android.adb-path:/usr/bin/adb}")
    private String adbPath;
    
    @Value("${cybersec.tools.android.apk-path:./tools/android-monitor/app-debug.apk}")
    private String apkPath;
    
    public String getBruteForcePath() {
        return bruteForcePath;
    }
    
    public String getKeyloggerPath() {
        return keyloggerPath;
    }
    
    public String getAdbPath() {
        return adbPath;
    }
    
    public String getApkPath() {
        return apkPath;
    }
}