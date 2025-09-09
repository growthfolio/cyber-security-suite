package com.research.cybersec.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackResult {
    private String target;
    private String protocol;
    private String username;
    private String password;
    private String status;
    private LocalDateTime timestamp;
    private Long responseTime;
    private String banner;
    private String error;
    
    // Compatibility methods
    public String getErrorMessage() { return error; }
    public void setErrorMessage(String error) { this.error = error; }
    public int getResponseTime() { return responseTime != null ? responseTime.intValue() : 0; }
    public void setResponseTime(int responseTime) { this.responseTime = (long) responseTime; }
}