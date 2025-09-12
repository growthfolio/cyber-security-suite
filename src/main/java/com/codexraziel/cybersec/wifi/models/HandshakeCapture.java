package com.codexraziel.cybersec.wifi.models;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.nio.file.Path;

/**
 * Represents a WiFi handshake capture session and its status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandshakeCapture {
    
    private String captureId;
    private WiFiNetwork targetNetwork;
    private String interfaceName;
    private Path captureFilePath;
    private CaptureStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int packetsCapturado;
    private int deauthAttempts;
    private boolean handshakeDetected;
    private String errorMessage;
    private CaptureMethod method;
    private int duration; // in seconds
    
    /**
     * Capture status enumeration
     */
    public enum CaptureStatus {
        INITIALIZING("Initializing capture"),
        SCANNING("Scanning for target"),
        WAITING_FOR_CLIENT("Waiting for client connection"),
        DEAUTH_ATTACK("Performing deauth attack"),
        CAPTURING("Capturing handshake"),
        HANDSHAKE_CAPTURED("Handshake captured successfully"),
        TIMEOUT("Capture timeout"),
        ERROR("Capture error"),
        CANCELLED("Capture cancelled"),
        ANALYZING("Analyzing capture file");
        
        private final String description;
        
        CaptureStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isCompleted() {
            return this == HANDSHAKE_CAPTURED || this == TIMEOUT || 
                   this == ERROR || this == CANCELLED;
        }
        
        public boolean isSuccessful() {
            return this == HANDSHAKE_CAPTURED;
        }
    }
    
    /**
     * Capture method enumeration
     */
    public enum CaptureMethod {
        PASSIVE("Passive monitoring"),
        DEAUTH_ATTACK("Deauth attack"),
        EVIL_TWIN("Evil twin attack"),
        PMKID("PMKID attack");
        
        private final String description;
        
        CaptureMethod(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Get capture progress as percentage (0-100)
     */
    public int getProgress() {
        if (status == null) return 0;
        
        switch (status) {
            case INITIALIZING: return 10;
            case SCANNING: return 20;
            case WAITING_FOR_CLIENT: return 30;
            case DEAUTH_ATTACK: return 50;
            case CAPTURING: return 70;
            case ANALYZING: return 90;
            case HANDSHAKE_CAPTURED: return 100;
            case ERROR:
            case TIMEOUT:
            case CANCELLED: return 0;
            default: return 0;
        }
    }
    
    /**
     * Get elapsed time in seconds
     */
    public long getElapsedSeconds() {
        if (startTime == null) return 0;
        
        LocalDateTime endTimeOrNow = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, endTimeOrNow).getSeconds();
    }
    
    /**
     * Check if capture file exists and is valid
     */
    public boolean hasValidCaptureFile() {
        return captureFilePath != null && 
               captureFilePath.toFile().exists() && 
               captureFilePath.toFile().length() > 0;
    }
    
    /**
     * Get capture file size in bytes
     */
    public long getCaptureFileSize() {
        if (!hasValidCaptureFile()) return 0;
        return captureFilePath.toFile().length();
    }
    
    /**
     * Get human-readable file size
     */
    public String getFormattedFileSize() {
        long bytes = getCaptureFileSize();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
    
    /**
     * Get capture quality score based on packets and time
     */
    public int getQualityScore() {
        if (!handshakeDetected) return 0;
        
        // Base score from handshake detection
        int score = 60;
        
        // Bonus for more packets
        if (packetsCapturado > 1000) score += 20;
        else if (packetsCapturado > 500) score += 10;
        
        // Bonus for reasonable capture time
        long elapsed = getElapsedSeconds();
        if (elapsed < 60) score += 20; // Fast capture
        else if (elapsed < 300) score += 10; // Reasonable time
        
        return Math.min(score, 100);
    }
    
    /**
     * Get status color for UI display
     */
    public String getStatusColor() {
        if (status == null) return "#808080";
        
        switch (status) {
            case HANDSHAKE_CAPTURED: return "#28a745"; // Green
            case CAPTURING:
            case DEAUTH_ATTACK: return "#007bff"; // Blue
            case ERROR: return "#dc3545"; // Red
            case TIMEOUT: return "#fd7e14"; // Orange
            case CANCELLED: return "#6c757d"; // Gray
            default: return "#ffc107"; // Yellow
        }
    }
    
    @Override
    public String toString() {
        return String.format("HandshakeCapture{target='%s', status=%s, packets=%d, handshake=%s}", 
                           targetNetwork != null ? targetNetwork.getSsid() : "Unknown",
                           status, packetsCapturado, handshakeDetected);
    }
}
