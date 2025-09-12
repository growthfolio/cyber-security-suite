package com.codexraziel.cybersec.wifi.models;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Represents a wireless network interface and its capabilities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WiFiInterface {
    
    private String interfaceName;
    private String driverName;
    private String chipsetInfo;
    private boolean monitorModeSupported;
    private boolean injectionSupported;
    private boolean isUp;
    private boolean isMonitorMode;
    private String currentMode;
    private String macAddress;
    private String[] supportedBands;
    private String[] supportedChannels;
    private InterfaceStatus status;
    private String errorMessage;
    
    /**
     * Interface status enumeration
     */
    public enum InterfaceStatus {
        AVAILABLE("Available for use"),
        IN_USE("Currently in use"),
        MONITOR_MODE("In monitor mode"),
        ERROR("Interface error"),
        NOT_FOUND("Interface not found"),
        NO_DRIVER("Driver not loaded"),
        PERMISSION_DENIED("Permission denied");
        
        private final String description;
        
        InterfaceStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isUsable() {
            return this == AVAILABLE || this == MONITOR_MODE;
        }
    }
    
    /**
     * Check if interface is suitable for WiFi attacks
     */
    public boolean isSuitableForAttacks() {
        return monitorModeSupported && 
               injectionSupported && 
               status != null && 
               status.isUsable();
    }
    
    /**
     * Get capability score (0-100) based on features
     */
    public int getCapabilityScore() {
        int score = 0;
        
        if (monitorModeSupported) score += 40;
        if (injectionSupported) score += 40;
        if (isUp) score += 10;
        if (supportedBands != null && supportedBands.length > 1) score += 10;
        
        return score;
    }
    
    /**
     * Get interface quality assessment
     */
    public String getQualityAssessment() {
        int score = getCapabilityScore();
        
        if (score >= 90) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 50) return "Fair";
        if (score >= 30) return "Limited";
        return "Poor";
    }
    
    /**
     * Get supported bands as string
     */
    public String getSupportedBandsString() {
        if (supportedBands == null || supportedBands.length == 0) {
            return "Unknown";
        }
        return String.join(", ", supportedBands);
    }
    
    /**
     * Get supported channels count
     */
    public int getSupportedChannelCount() {
        return supportedChannels != null ? supportedChannels.length : 0;
    }
    
    /**
     * Check if interface supports 5GHz band
     */
    public boolean supports5GHz() {
        if (supportedBands == null) return false;
        
        for (String band : supportedBands) {
            if (band.contains("5GHz") || band.contains("5.") || band.contains("a")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if interface supports 2.4GHz band
     */
    public boolean supports2_4GHz() {
        if (supportedBands == null) return false;
        
        for (String band : supportedBands) {
            if (band.contains("2.4GHz") || band.contains("2.4") || 
                band.contains("b") || band.contains("g")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get status color for UI display
     */
    public String getStatusColor() {
        if (status == null) return "#808080";
        
        switch (status) {
            case AVAILABLE:
            case MONITOR_MODE: return "#28a745";     // Green
            case IN_USE: return "#ffc107";           // Yellow
            case ERROR:
            case PERMISSION_DENIED: return "#dc3545"; // Red
            case NOT_FOUND:
            case NO_DRIVER: return "#6c757d";        // Gray
            default: return "#808080";               // Default gray
        }
    }
    
    /**
     * Get recommendations for this interface
     */
    public String getRecommendations() {
        if (!monitorModeSupported) {
            return "Interface doesn't support monitor mode. Consider using a different adapter.";
        }
        
        if (!injectionSupported) {
            return "Interface doesn't support packet injection. Some attacks may not work.";
        }
        
        if (status == InterfaceStatus.PERMISSION_DENIED) {
            return "Permission denied. Run application with appropriate privileges.";
        }
        
        if (status == InterfaceStatus.NO_DRIVER) {
            return "Driver not loaded. Install appropriate drivers for this interface.";
        }
        
        if (isSuitableForAttacks()) {
            return "Interface is suitable for WiFi security testing.";
        }
        
        return "Interface has limitations. Check compatibility and drivers.";
    }
    
    @Override
    public String toString() {
        return String.format("WiFiInterface{name='%s', driver='%s', monitor=%s, injection=%s, status=%s}", 
                           interfaceName, driverName, monitorModeSupported, 
                           injectionSupported, status);
    }
}
