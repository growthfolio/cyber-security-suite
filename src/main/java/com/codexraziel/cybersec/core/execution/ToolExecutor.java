package com.codexraziel.cybersec.core.execution;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Interface for executing external security tools
 */
public interface ToolExecutor {
    
    /**
     * Execute a tool synchronously
     */
    ExecutionResult execute(ToolConfig config);
    
    /**
     * Execute a tool asynchronously
     */
    CompletableFuture<ExecutionResult> executeAsync(ToolConfig config);
    
    /**
     * Execute with real-time output callback
     */
    CompletableFuture<ExecutionResult> executeWithOutput(
            ToolConfig config,
            Consumer<String> stdoutCallback,
            Consumer<String> stderrCallback
    );
    
    /**
     * Stop a running execution
     */
    void stop(String processId);
    
    /**
     * Check if tool is available in system
     */
    boolean isToolAvailable(String toolName);
    
    /**
     * Get tool version
     */
    String getToolVersion(String toolName);
}
