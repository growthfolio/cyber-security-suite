package com.codexraziel.cybersec.wifi.models;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a discovered WiFi network with all relevant security information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WiFiNetwork {
    
    private String ssid;
    private String bssid;
    private String channel;
    private String frequency;
    private int signalStrength;
    private String encryptionType;
    private String authType;
    private String cipher;
    private boolean isWpsEnabled;
    private boolean isHidden;
    private LocalDateTime discoveredAt;
    private String vendorInfo;
    private int clientCount;
    
    /**
     * Encryption types supported
     */
    public enum EncryptionType {
        OPEN("Open"),
        WEP("WEP"),
        WPA("WPA"),
        WPA2("WPA2"),
        WPA3("WPA3"),
        WPA_WPA2("WPA/WPA2"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        EncryptionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static EncryptionType fromString(String encryption) {
            if (encryption == null) return UNKNOWN;
            
            String enc = encryption.toLowerCase();
            if (enc.contains("wpa3")) return WPA3;
            if (enc.contains("wpa2")) return WPA2;
            if (enc.contains("wpa")) return WPA;
            if (enc.contains("wep")) return WEP;
            if (enc.contains("open") || enc.isEmpty()) return OPEN;
            
            return UNKNOWN;
        }
    }
    
    /**
     * Check if network is vulnerable to common attacks
     */
    public boolean isVulnerable() {
        return encryptionType != null && 
               (encryptionType.contains("WEP") || 
                encryptionType.contains("Open") || 
                isWpsEnabled);
    }
    
    /**
     * Get attack difficulty score (1-10, where 1 is easiest)
     */
    public int getAttackDifficulty() {
        if (encryptionType == null) return 10;
        
        switch (encryptionType.toLowerCase()) {
            case "open": return 1;
            case "wep": return 2;
            case "wpa": return 5;
            case "wpa2": return 7;
            case "wpa3": return 10;
            default: return 8;
        }
    }
    
    /**
     * Get recommended attack methods for this network
     */
    public String getRecommendedAttacks() {
        if (encryptionType == null) return "Unknown encryption";
        
        switch (encryptionType.toLowerCase()) {
            case "open":
                return "Direct connection, Evil Twin";
            case "wep":
                return "WEP cracking, IV attack";
            case "wpa":
            case "wpa2":
                if (isWpsEnabled) {
                    return "WPS attack, Dictionary attack, Handshake capture";
                }
                return "Dictionary attack, Handshake capture";
            case "wpa3":
                return "Dragonfly attack (if vulnerable), Social engineering";
            default:
                return "Analysis required";
        }
    }
    
    /**
     * Get signal quality description
     */
    public String getSignalQuality() {
        if (signalStrength >= -30) return "Excellent";
        if (signalStrength >= -50) return "Good";
        if (signalStrength >= -70) return "Fair";
        if (signalStrength >= -80) return "Weak";
        return "Very Weak";
    }
    
    /**
     * Check if network is suitable for attack (good signal, vulnerable)
     */
    public boolean isSuitableForAttack() {
        return signalStrength >= -75 && // Decent signal
               !encryptionType.toLowerCase().contains("wpa3") && // Not WPA3
               !ssid.toLowerCase().contains("hidden"); // Not hidden
    }
    
    @Override
    public String toString() {
        return String.format("WiFiNetwork{ssid='%s', bssid='%s', encryption='%s', signal=%ddBm, channel='%s'}", 
                           ssid, bssid, encryptionType, signalStrength, channel);
    }
}
