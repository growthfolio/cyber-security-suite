package com.codexraziel.cybersec.core.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Result of a tool execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    
    private String toolName;
    private int exitCode;
    private String stdout;
    private String stderr;
    private Instant startTime;
    private Instant endTime;
    private Duration duration;
    private boolean successful;
    private Map<String, Object> metadata;
    private Throwable error;
    
    public static ExecutionResult success(String toolName, String stdout) {
        return ExecutionResult.builder()
                .toolName(toolName)
                .exitCode(0)
                .stdout(stdout)
                .successful(true)
                .build();
    }
    
    public static ExecutionResult failure(String toolName, int exitCode, String stderr) {
        return ExecutionResult.builder()
                .toolName(toolName)
                .exitCode(exitCode)
                .stderr(stderr)
                .successful(false)
                .build();
    }
    
    public static ExecutionResult error(String toolName, Throwable error) {
        return ExecutionResult.builder()
                .toolName(toolName)
                .exitCode(-1)
                .error(error)
                .stderr(error.getMessage())
                .successful(false)
                .build();
    }
    
    public boolean isSuccess() {
        return successful && exitCode == 0;
    }
    
    public Duration getDuration() {
        if (duration == null && startTime != null && endTime != null) {
            duration = Duration.between(startTime, endTime);
        }
        return duration;
    }
}
