package com.research.cybersec.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class NetworkUtils {
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    
    private static final Pattern SSID_PATTERN = Pattern.compile("^[\\x20-\\x7E]{1,32}$");
    
    public boolean isValidIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) return false;
        return IP_PATTERN.matcher(ip.trim()).matches();
    }
    
    public boolean isValidSSID(String ssid) {
        if (ssid == null || ssid.trim().isEmpty()) return false;
        String trimmed = ssid.trim();
        return trimmed.length() <= 32 && SSID_PATTERN.matcher(trimmed).matches();
    }
    
    public List<String> getAvailableInterfaces() {
        List<String> interfaces = new ArrayList<>();
        
        try {
            String[] commonInterfaces = {"wlan0", "wlan1", "wlp2s0", "wlp3s0", "wifi0"};
            
            for (String iface : commonInterfaces) {
                if (interfaceExists(iface)) {
                    interfaces.add(iface);
                }
            }
            
            if (interfaces.isEmpty()) {
                interfaces.add("wlan0");
                interfaces.add("wlan1");
            }
            
        } catch (Exception e) {
            log.error("Failed to get network interfaces", e);
            interfaces.add("wlan0");
        }
        
        return interfaces;
    }
    
    private boolean interfaceExists(String interfaceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ip", "link", "show", interfaceName);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String sanitizeInput(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("[^a-zA-Z0-9._-]", "");
    }
}