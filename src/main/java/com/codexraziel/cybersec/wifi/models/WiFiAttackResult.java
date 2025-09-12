package com.codexraziel.cybersec.wifi.models;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents the result of a WiFi attack operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WiFiAttackResult {
    
    private String attackId;
    private WiFiNetwork targetNetwork;
    private AttackType attackType;
    private AttackStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String discoveredPassword;
    private String wordlistUsed;
    private long passwordsAttempted;
    private long passwordsPerSecond;
    private String crackingMethod;
    private String errorMessage;
    private List<String> detailedLog;
    private boolean isSuccessful;
    private double confidenceScore;
    
    /**
     * Attack type enumeration
     */
    public enum AttackType {
        DICTIONARY("Dictionary Attack"),
        BRUTE_FORCE("Brute Force"),
        WPS_ATTACK("WPS Attack"),
        PMKID_ATTACK("PMKID Attack"),
        EVIL_TWIN("Evil Twin"),
        DEAUTH_ATTACK("Deauth Attack"),
        HANDSHAKE_CAPTURE("Handshake Capture"),
        GPU_CRACKING("GPU Accelerated Cracking");
        
        private final String displayName;
        
        AttackType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Attack status enumeration
     */
    public enum AttackStatus {
        INITIALIZING("Initializing attack"),
        PREPARING("Preparing attack vectors"),
        ATTACKING("Attack in progress"),
        SUCCESS("Attack successful"),
        FAILED("Attack failed"),
        TIMEOUT("Attack timeout"),
        CANCELLED("Attack cancelled"),
        ERROR("Attack error");
        
        private final String description;
        
        AttackStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isCompleted() {
            return this == SUCCESS || this == FAILED || 
                   this == TIMEOUT || this == CANCELLED || this == ERROR;
        }
        
        public boolean isSuccessful() {
            return this == SUCCESS;
        }
    }
    
    /**
     * Initialize detailed log if null
     */
    public List<String> getDetailedLog() {
        if (detailedLog == null) {
            detailedLog = new ArrayList<>();
        }
        return detailedLog;
    }
    
    /**
     * Add entry to detailed log
     */
    public void addLogEntry(String entry) {
        getDetailedLog().add(String.format("[%s] %s", 
                           LocalDateTime.now().toString(), entry));
    }
    
    /**
     * Get attack duration in seconds
     */
    public long getDurationSeconds() {
        if (startTime == null) return 0;
        
        LocalDateTime endTimeOrNow = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, endTimeOrNow).getSeconds();
    }
    
    /**
     * Get formatted duration string
     */
    public String getFormattedDuration() {
        long seconds = getDurationSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
        } else {
            return String.format("%02d:%02d", minutes, remainingSeconds);
        }
    }
    
    /**
     * Calculate attack efficiency score (0-100)
     */
    public int getEfficiencyScore() {
        if (!isSuccessful || passwordsAttempted == 0) return 0;
        
        long duration = getDurationSeconds();
        if (duration == 0) return 100;
        
        // Base score for success
        int score = 50;
        
        // Bonus for speed (less time = higher score)
        if (duration < 60) score += 30;           // Under 1 minute
        else if (duration < 300) score += 20;    // Under 5 minutes
        else if (duration < 900) score += 10;    // Under 15 minutes
        
        // Bonus for fewer attempts needed
        if (passwordsAttempted < 100) score += 20;
        else if (passwordsAttempted < 1000) score += 10;
        else if (passwordsAttempted < 10000) score += 5;
        
        return Math.min(score, 100);
    }
    
    /**
     * Get attack difficulty assessment based on result
     */
    public String getDifficultyAssessment() {
        if (!isSuccessful) return "Failed";
        
        long duration = getDurationSeconds();
        
        if (duration < 60 && passwordsAttempted < 100) {
            return "Very Easy";
        } else if (duration < 300 && passwordsAttempted < 1000) {
            return "Easy";
        } else if (duration < 900 && passwordsAttempted < 10000) {
            return "Moderate";
        } else if (duration < 3600) {
            return "Hard";
        } else {
            return "Very Hard";
        }
    }
    
    /**
     * Get status color for UI display
     */
    public String getStatusColor() {
        if (status == null) return "#808080";
        
        switch (status) {
            case SUCCESS: return "#28a745";      // Green
            case ATTACKING: return "#007bff";    // Blue
            case FAILED: return "#dc3545";       // Red
            case TIMEOUT: return "#fd7e14";      // Orange
            case CANCELLED: return "#6c757d";    // Gray
            case ERROR: return "#dc3545";        // Red
            default: return "#ffc107";           // Yellow
        }
    }
    
    /**
     * Get recommendations based on attack result
     */
    public String getRecommendations() {
        if (isSuccessful) {
            return String.format("Password cracked successfully! Consider using stronger passwords. " +
                               "Attack took %s with %d attempts.", 
                               getFormattedDuration(), passwordsAttempted);
        }
        
        if (status == AttackStatus.TIMEOUT) {
            return "Attack timed out. Try with a larger wordlist or different attack method.";
        }
        
        if (status == AttackStatus.FAILED) {
            return "Attack failed. The password might not be in the wordlist or network uses strong security.";
        }
        
        return "Attack incomplete. Check logs for details.";
    }
    
    @Override
    public String toString() {
        return String.format("WiFiAttackResult{target='%s', type=%s, status=%s, success=%s, duration=%s}", 
                           targetNetwork != null ? targetNetwork.getSsid() : "Unknown",
                           attackType, status, isSuccessful, getFormattedDuration());
    }
}
