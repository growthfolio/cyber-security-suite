package com.research.cybersec.models;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WiFiNetwork {
    
    public enum NetworkStatus {
        SECURE("🟢", "Secure", "-fx-text-fill: green;"),
        SUSPICIOUS("🟡", "Suspicious", "-fx-text-fill: orange;"),
        VULNERABLE("🔴", "Vulnerable", "-fx-text-fill: red;"),
        SCANNING("📡", "Scanning", "-fx-text-fill: blue;"),
        UNKNOWN("⚪", "Unknown", "-fx-text-fill: gray;");
        
        private final String icon;
        private final String displayName;
        private final String cssStyle;
        
        NetworkStatus(String icon, String displayName, String cssStyle) {
            this.icon = icon;
            this.displayName = displayName;
            this.cssStyle = cssStyle;
        }
        
        public String getIcon() { return icon; }
        public String getDisplayName() { return displayName; }
        public String getCssStyle() { return cssStyle; }
        
        @Override
        public String toString() { return icon + " " + displayName; }
    }
    private String ssid;
    private String bssid;
    private String security;
    private int signalStrength;
    private String channel;
    private String frequency;
    private boolean isConnected;
    private String vendor;
    private LocalDateTime discoveredAt;
    private NetworkStatus status;
    
    public NetworkStatus getNetworkStatus() {
        if (status != null) return status;
        
        if (isVulnerable()) {
            return security.contains("Open") ? NetworkStatus.VULNERABLE : NetworkStatus.SUSPICIOUS;
        }
        return NetworkStatus.SECURE;
    }
    
    public String getSecurityLevel() {
        if (security.contains("WPA3")) return "WPA3";
        if (security.contains("WPA2")) return "WPA2";
        if (security.contains("WPA")) return "WPA";
        if (security.contains("WEP")) return "WEP";
        return "Open";
    }
    
    public boolean isVulnerable() {
        return security.contains("WEP") || 
               security.contains("Open") || 
               signalStrength > -50;
    }
    
    public String getSignalQuality() {
        if (signalStrength > -30) return "Excellent";
        if (signalStrength > -50) return "Good";
        if (signalStrength > -70) return "Fair";
        return "Poor";
    }
    
    @Override
    public String toString() {
        return String.format("%-20s | %-17s | %-10s | %3ddBm | Ch:%s", 
            ssid, bssid, security, signalStrength, channel);
    }
}