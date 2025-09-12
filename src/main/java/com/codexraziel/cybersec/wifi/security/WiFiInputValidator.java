package com.codexraziel.cybersec.wifi.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Validates and sanitizes all WiFi-related inputs to prevent security vulnerabilities
 */
@Component
@Slf4j
public class WiFiInputValidator {
    
    // Security patterns
    private static final Pattern SAFE_INTERFACE_NAME = Pattern.compile("^[a-zA-Z0-9._-]{1,15}$");
    private static final Pattern SAFE_SSID = Pattern.compile("^[\\w\\s._-]{1,32}$");
    private static final Pattern SAFE_BSSID = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    private static final Pattern SAFE_CHANNEL = Pattern.compile("^([1-9]|1[0-4])$");
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]{1,255}$");
    private static final Pattern SAFE_WORDLIST_NAME = Pattern.compile("^[a-zA-Z0-9._/-]{1,100}$");
    
    // Allowed wordlist base directory
    private static final String WORDLIST_BASE_DIR = "/home/felipe-macedo/cyber-projects/cyber-security-suite/wordlists";
    
    // Dangerous command patterns to block
    private static final List<String> DANGEROUS_PATTERNS = Arrays.asList(
        ";", "&&", "||", "|", ">", "<", "&", "$", "`", 
        "$(", "rm ", "del ", "format", "sudo ", "su ",
        "../", "etc/passwd", "/bin/", "/usr/bin/"
    );
    
    /**
     * Validate network interface name
     */
    public void validateInterfaceName(String interfaceName) {
        if (interfaceName == null || interfaceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Interface name cannot be null or empty");
        }
        
        if (!SAFE_INTERFACE_NAME.matcher(interfaceName).matches()) {
            throw new IllegalArgumentException("Invalid interface name format: " + interfaceName);
        }
        
        checkForDangerousPatterns(interfaceName, "interface name");
        
        log.debug("Validated interface name: {}", interfaceName);
    }
    
    /**
     * Validate SSID
     */
    public void validateSSID(String ssid) {
        if (ssid == null) {
            throw new IllegalArgumentException("SSID cannot be null");
        }
        
        if (ssid.length() > 32) {
            throw new IllegalArgumentException("SSID too long (max 32 characters): " + ssid.length());
        }
        
        if (!SAFE_SSID.matcher(ssid).matches()) {
            throw new IllegalArgumentException("Invalid SSID format: " + ssid);
        }
        
        checkForDangerousPatterns(ssid, "SSID");
        
        log.debug("Validated SSID: {}", ssid);
    }
    
    /**
     * Validate BSSID (MAC address)
     */
    public void validateBSSID(String bssid) {
        if (bssid == null || bssid.trim().isEmpty()) {
            throw new IllegalArgumentException("BSSID cannot be null or empty");
        }
        
        if (!SAFE_BSSID.matcher(bssid).matches()) {
            throw new IllegalArgumentException("Invalid BSSID format: " + bssid);
        }
        
        log.debug("Validated BSSID: {}", bssid);
    }
    
    /**
     * Validate WiFi channel
     */
    public void validateChannel(String channel) {
        if (channel == null || channel.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel cannot be null or empty");
        }
        
        if (!SAFE_CHANNEL.matcher(channel).matches()) {
            throw new IllegalArgumentException("Invalid channel (must be 1-14): " + channel);
        }
        
        log.debug("Validated channel: {}", channel);
    }
    
    /**
     * Validate and sanitize file path
     */
    public Path validateAndSanitizeFilePath(String filePath, String baseDirectory) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        checkForDangerousPatterns(filePath, "file path");
        
        try {
            Path basePath = Paths.get(baseDirectory).toAbsolutePath().normalize();
            Path requestedPath = Paths.get(filePath).normalize();
            Path resolvedPath = basePath.resolve(requestedPath).normalize();
            
            // Ensure the resolved path is within the base directory (prevent path traversal)
            if (!resolvedPath.startsWith(basePath)) {
                throw new SecurityException("Path traversal detected: " + filePath);
            }
            
            log.debug("Validated file path: {}", resolvedPath);
            return resolvedPath;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid file path: " + filePath, e);
        }
    }
    
    /**
     * Validate wordlist path
     */
    public Path validateWordlistPath(String wordlistName) {
        if (wordlistName == null || wordlistName.trim().isEmpty()) {
            throw new IllegalArgumentException("Wordlist name cannot be null or empty");
        }
        
        if (!SAFE_WORDLIST_NAME.matcher(wordlistName).matches()) {
            throw new IllegalArgumentException("Invalid wordlist name format: " + wordlistName);
        }
        
        return validateAndSanitizeFilePath(wordlistName, WORDLIST_BASE_DIR);
    }
    
    /**
     * Validate capture filename
     */
    public void validateCaptureFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Capture filename cannot be null or empty");
        }
        
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            throw new IllegalArgumentException("Invalid filename format: " + filename);
        }
        
        if (!filename.toLowerCase().endsWith(".cap") && 
            !filename.toLowerCase().endsWith(".pcap")) {
            throw new IllegalArgumentException("Invalid capture file extension (must be .cap or .pcap): " + filename);
        }
        
        checkForDangerousPatterns(filename, "filename");
        
        log.debug("Validated capture filename: {}", filename);
    }
    
    /**
     * Validate timeout value
     */
    public void validateTimeout(int timeoutSeconds) {
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("Timeout must be at least 1 second");
        }
        
        if (timeoutSeconds > 3600) { // Max 1 hour
            throw new IllegalArgumentException("Timeout too large (max 3600 seconds): " + timeoutSeconds);
        }
        
        log.debug("Validated timeout: {} seconds", timeoutSeconds);
    }
    
    /**
     * Validate attack parameters
     */
    public void validateAttackParameters(String interfaceName, String ssid, String wordlistPath) {
        validateInterfaceName(interfaceName);
        validateSSID(ssid);
        validateWordlistPath(wordlistPath);
    }
    
    /**
     * Check for dangerous command injection patterns
     */
    private void checkForDangerousPatterns(String input, String fieldName) {
        String lowerInput = input.toLowerCase();
        
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lowerInput.contains(pattern.toLowerCase())) {
                throw new SecurityException(String.format(
                    "Dangerous pattern '%s' detected in %s: %s", pattern, fieldName, input));
            }
        }
    }
    
    /**
     * Sanitize string for safe usage in commands
     */
    public String sanitizeForCommand(String input) {
        if (input == null) return "";
        
        // Remove any potentially dangerous characters
        return input.replaceAll("[;&|><$`()]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    /**
     * Validate process timeout and thread count
     */
    public void validateProcessParameters(int timeoutSeconds, int threadCount) {
        validateTimeout(timeoutSeconds);
        
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be at least 1");
        }
        
        if (threadCount > 50) { // Reasonable limit
            throw new IllegalArgumentException("Thread count too high (max 50): " + threadCount);
        }
        
        log.debug("Validated process parameters: timeout={}s, threads={}", timeoutSeconds, threadCount);
    }
}
