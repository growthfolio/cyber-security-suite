package com.research.cybersec.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Slf4j
public class SafeProcessExecutor {
    
    @Autowired
    private InputValidator inputValidator;
    
    @Autowired
    private AuditLogger auditLogger;
    
    // Security limits
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MAX_TIMEOUT_SECONDS = 300;
    private static final int MAX_OUTPUT_LINES = 10000;
    
    public CompletableFuture<ProcessResult> executeCommand(ProcessRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate request
            ValidationResult validation = validateRequest(request);
            if (!validation.isValid()) {
                auditLogger.logSecurityEvent("COMMAND_VALIDATION_FAILED", 
                    request.getCommand().toString(), validation.getErrorMessage());
                return ProcessResult.failure("Validation failed: " + validation.getErrorMessage());
            }
            
            // Log command execution
            auditLogger.logSecurityEvent("COMMAND_EXECUTION", 
                request.getCommand().toString(), "User: " + System.getProperty("user.name"));
            
            try {
                return executeSecurely(request);
            } catch (Exception e) {
                log.error("Command execution failed", e);
                auditLogger.logSecurityEvent("COMMAND_EXECUTION_ERROR", 
                    request.getCommand().toString(), e.getMessage());
                return ProcessResult.failure("Execution error: " + e.getMessage());
            }
        });
    }
    
    private ProcessResult executeSecurely(ProcessRequest request) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(request.getCommand());
        pb.redirectErrorStream(true);
        
        // Set working directory if specified
        if (request.getWorkingDirectory() != null) {
            pb.directory(request.getWorkingDirectory());
        }
        
        // Clear environment variables for security
        pb.environment().clear();
        pb.environment().put("PATH", System.getenv("PATH"));
        pb.environment().put("HOME", System.getenv("HOME"));
        
        Process process = pb.start();
        
        // Set up timeout
        int timeoutSeconds = Math.min(request.getTimeoutSeconds(), MAX_TIMEOUT_SECONDS);
        
        StringBuilder output = new StringBuilder();
        int lineCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null && lineCount < MAX_OUTPUT_LINES) {
                output.append(line).append("\\n");
                lineCount++;
                
                // Send real-time output if callback provided
                if (request.getOutputCallback() != null) {
                    request.getOutputCallback().accept(line);
                }
                
                // Check if process is still alive
                if (!process.isAlive()) {
                    break;
                }
            }
            
            if (lineCount >= MAX_OUTPUT_LINES) {
                output.append("... [Output truncated - too many lines]\\n");
                log.warn("Command output truncated due to line limit: {}", request.getCommand());
            }
        }
        
        // Wait for process completion with timeout
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            auditLogger.logSecurityEvent("COMMAND_TIMEOUT", 
                request.getCommand().toString(), "Timeout: " + timeoutSeconds + "s");
            return ProcessResult.timeout("Command timed out after " + timeoutSeconds + " seconds");
        }
        
        int exitCode = process.exitValue();
        String outputStr = output.toString();
        
        // Log completion
        auditLogger.logSecurityEvent("COMMAND_COMPLETED", 
            request.getCommand().toString(), "Exit code: " + exitCode);
        
        if (exitCode == 0) {
            return ProcessResult.success(outputStr);
        } else {
            return ProcessResult.failure("Command failed with exit code: " + exitCode + "\\n" + outputStr);
        }
    }
    
    private ValidationResult validateRequest(ProcessRequest request) {
        if (request == null) {
            return ValidationResult.invalid("Request cannot be null");
        }
        
        if (request.getCommand() == null || request.getCommand().isEmpty()) {
            return ValidationResult.invalid("Command cannot be empty");
        }
        
        // Validate command name
        String commandName = request.getCommand().get(0);
        if (!isAllowedCommand(commandName)) {
            return ValidationResult.invalid("Command not allowed: " + commandName);
        }
        
        // Validate arguments
        for (String arg : request.getCommand()) {
            if (containsDangerousPatterns(arg)) {
                return ValidationResult.invalid("Dangerous pattern detected in argument: " + arg);
            }
        }
        
        // Validate timeout
        if (request.getTimeoutSeconds() < 1 || request.getTimeoutSeconds() > MAX_TIMEOUT_SECONDS) {
            return ValidationResult.invalid("Invalid timeout: " + request.getTimeoutSeconds());
        }
        
        return ValidationResult.valid("Request validated");
    }
    
    private boolean isAllowedCommand(String command) {
        // Whitelist of allowed commands
        String[] allowedCommands = {
            "nmcli", "iwlist", "iwconfig", "ip", "ping", "nmap", "which",
            "aircrack-ng", "hydra", "medusa", "ncrack", "john",
            "adb", "aapt", "zipalign"
        };
        
        for (String allowed : allowedCommands) {
            if (command.equals(allowed) || command.endsWith("/" + allowed)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean containsDangerousPatterns(String arg) {
        // Check for command injection patterns
        String[] dangerousPatterns = {
            ";", "&", "|", "`", "$", "(", ")", "{", "}", "[", "]",
            "<", ">", "\\n", "\\r", "\\t"
        };
        
        for (String pattern : dangerousPatterns) {
            if (arg.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ProcessRequest {
        private List<String> command;
        private java.io.File workingDirectory;
        private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        private Consumer<String> outputCallback;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProcessResult {
        private boolean success;
        private boolean timeout;
        private String output;
        private String errorMessage;
        
        public static ProcessResult success(String output) {
            return new ProcessResult(true, false, output, null);
        }
        
        public static ProcessResult failure(String errorMessage) {
            return new ProcessResult(false, false, null, errorMessage);
        }
        
        public static ProcessResult timeout(String message) {
            return new ProcessResult(false, true, null, message);
        }
    }
    
    private static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid(String message) {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}