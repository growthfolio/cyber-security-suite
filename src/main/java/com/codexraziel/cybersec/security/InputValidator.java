package com.codexraziel.cybersec.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@Component
@Slf4j
public class InputValidator {
    
    // Security patterns
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    
    private static final Pattern SSID_PATTERN = Pattern.compile("^[\\x20-\\x7E]{1,32}$");
    
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$");
    
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    
    // Dangerous patterns to block
    private static final Pattern[] INJECTION_PATTERNS = {
        Pattern.compile(".*[;&|`$(){}\\[\\]<>].*"),  // Command injection
        Pattern.compile(".*\\.\\..*"),                // Path traversal
        Pattern.compile(".*['\"].*"),                 // SQL injection chars
        Pattern.compile(".*\\\\x[0-9a-fA-F]{2}.*")   // Hex encoding
    };
    
    public ValidationResult validateIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return ValidationResult.invalid("IP address cannot be empty");
        }
        
        String trimmed = ip.trim();
        
        // Check for injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(trimmed).matches()) {
                log.warn("Potential injection attempt in IP: {}", sanitizeForLog(trimmed));
                return ValidationResult.invalid("Invalid characters in IP address");
            }
        }
        
        if (!IP_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("Invalid IP address format");
        }
        
        // Block private/reserved ranges if needed
        if (isPrivateIP(trimmed)) {
            log.info("Private IP address used: {}", trimmed);
        }
        
        return ValidationResult.valid(trimmed);
    }
    
    public ValidationResult validateSSID(String ssid) {
        if (ssid == null || ssid.trim().isEmpty()) {
            return ValidationResult.invalid("SSID cannot be empty");
        }
        
        String trimmed = ssid.trim();
        
        // Check length
        if (trimmed.length() > 32) {
            return ValidationResult.invalid("SSID too long (max 32 characters)");
        }
        
        // Check for injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(trimmed).matches()) {
                log.warn("Potential injection attempt in SSID: {}", sanitizeForLog(trimmed));
                return ValidationResult.invalid("Invalid characters in SSID");
            }
        }
        
        if (!SSID_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("SSID contains invalid characters");
        }
        
        return ValidationResult.valid(trimmed);
    }
    
    public ValidationResult validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return ValidationResult.invalid("File path cannot be empty");
        }
        
        String trimmed = filePath.trim();
        
        // Check for injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(trimmed).matches()) {
                log.warn("Potential injection attempt in file path: {}", sanitizeForLog(trimmed));
                return ValidationResult.invalid("Invalid characters in file path");
            }
        }
        
        try {
            Path path = Paths.get(trimmed);
            
            // Normalize and check for path traversal
            Path normalized = path.normalize();
            if (!normalized.equals(path)) {
                return ValidationResult.invalid("Path traversal detected");
            }
            
            // Check if file exists and is readable
            if (Files.exists(path)) {
                if (!Files.isReadable(path)) {
                    return ValidationResult.invalid("File is not readable");
                }
                if (Files.isDirectory(path)) {
                    return ValidationResult.invalid("Path is a directory, not a file");
                }
            }
            
            return ValidationResult.valid(normalized.toString());
            
        } catch (Exception e) {
            log.warn("Invalid file path: {}", sanitizeForLog(trimmed), e);
            return ValidationResult.invalid("Invalid file path format");
        }
    }
    
    public ValidationResult validateNetworkInterface(String interfaceName) {
        if (interfaceName == null || interfaceName.trim().isEmpty()) {
            return ValidationResult.invalid("Network interface cannot be empty");
        }
        
        String trimmed = interfaceName.trim();
        
        // Check for injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(trimmed).matches()) {
                log.warn("Potential injection attempt in interface: {}", sanitizeForLog(trimmed));
                return ValidationResult.invalid("Invalid characters in interface name");
            }
        }
        
        if (!INTERFACE_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("Invalid interface name format");
        }
        
        // Check length
        if (trimmed.length() > 15) {
            return ValidationResult.invalid("Interface name too long");
        }
        
        return ValidationResult.valid(trimmed);
    }
    
    public ValidationResult validatePort(int port) {
        if (port < 1 || port > 65535) {
            return ValidationResult.invalid("Port must be between 1 and 65535");
        }
        
        // Warn about privileged ports
        if (port < 1024) {
            log.info("Privileged port used: {}", port);
        }
        
        return ValidationResult.valid(String.valueOf(port));
    }
    
    public ValidationResult validateThreadCount(int threads) {
        if (threads < 1) {
            return ValidationResult.invalid("Thread count must be at least 1");
        }
        
        if (threads > 100) {
            return ValidationResult.invalid("Thread count too high (max 100)");
        }
        
        return ValidationResult.valid(String.valueOf(threads));
    }
    
    public String sanitizeInput(String input) {
        if (input == null) return "";
        
        return input.trim()
            .replaceAll("[^a-zA-Z0-9._-]", "")
            .substring(0, Math.min(input.length(), 255));
    }
    
    private boolean isPrivateIP(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 10.0.0.0/8
            if (first == 10) return true;
            
            // 172.16.0.0/12
            if (first == 172 && second >= 16 && second <= 31) return true;
            
            // 192.168.0.0/16
            if (first == 192 && second == 168) return true;
            
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\r\\n\\t]", "_").substring(0, Math.min(input.length(), 100));
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private String value;
        private String errorMessage;
        
        public static ValidationResult valid(String value) {
            return new ValidationResult(true, value, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }
    }
}