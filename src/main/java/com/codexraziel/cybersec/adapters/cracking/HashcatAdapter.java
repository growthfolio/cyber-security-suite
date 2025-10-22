package com.codexraziel.cybersec.adapters.cracking;

import com.codexraziel.cybersec.core.execution.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Professional adapter for Hashcat - GPU-accelerated password cracking
 * Supports 300+ hash types and multiple attack modes
 */
@Slf4j
@Component
public class HashcatAdapter {
    
    @Autowired
    private ToolExecutor toolExecutor;
    
    private static final String HASHCAT = "hashcat";
    
    // Common hash types
    public static final int MD5 = 0;
    public static final int SHA1 = 100;
    public static final int SHA256 = 1400;
    public static final int SHA512 = 1700;
    public static final int BCRYPT = 3200;
    public static final int WPA_WPA2 = 22000;
    public static final int NTLM = 1000;
    public static final int MYSQL5 = 300;
    
    // Attack modes
    public static final int ATTACK_STRAIGHT = 0;      // Dictionary attack
    public static final int ATTACK_COMBINATION = 1;   // Combinator attack
    public static final int ATTACK_BRUTE_FORCE = 3;   // Brute-force
    public static final int ATTACK_HYBRID_WORDLIST = 6; // Hybrid wordlist + mask
    public static final int ATTACK_HYBRID_MASK = 7;   // Hybrid mask + wordlist
    
    /**
     * Check if Hashcat is available
     */
    public boolean isAvailable() {
        return toolExecutor.isToolAvailable(HASHCAT);
    }
    
    /**
     * Get Hashcat version
     */
    public String getVersion() {
        return toolExecutor.getToolVersion(HASHCAT);
    }
    
    /**
     * Check if GPU is available
     */
    public CompletableFuture<GPUInfo> getGPUInfo() {
        return CompletableFuture.supplyAsync(() -> {
            ToolConfig config = ToolConfig.builder()
                    .toolName(HASHCAT)
                    .executablePath(HASHCAT)
                    .arguments(List.of("-I"))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            ExecutionResult result = toolExecutor.execute(config);
            
            if (result.isSuccess()) {
                return parseGPUInfo(result.getStdout());
            } else {
                return new GPUInfo(false, 0, "No GPU detected", List.of());
            }
        });
    }
    
    /**
     * Run benchmark to test GPU performance
     */
    public CompletableFuture<BenchmarkResult> benchmark(int hashType) {
        return CompletableFuture.supplyAsync(() -> {
            ToolConfig config = ToolConfig.builder()
                    .toolName(HASHCAT)
                    .executablePath(HASHCAT)
                    .arguments(List.of(
                        "-b",
                        "-m", String.valueOf(hashType)
                    ))
                    .timeout(Duration.ofMinutes(2))
                    .build();
            
            ExecutionResult result = toolExecutor.execute(config);
            
            if (result.isSuccess()) {
                long speed = parseBenchmarkSpeed(result.getStdout());
                return new BenchmarkResult(true, hashType, speed, result.getStdout());
            } else {
                return new BenchmarkResult(false, hashType, 0, result.getStderr());
            }
        });
    }
    
    /**
     * Crack hashes using dictionary attack
     */
    public CompletableFuture<CrackResult> crackWithWordlist(
            CrackConfig config,
            Consumer<CrackProgress> progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<String> args = buildHashcatArgs(config);
            
            ToolConfig toolConfig = ToolConfig.builder()
                    .toolName(HASHCAT)
                    .executablePath(HASHCAT)
                    .arguments(args)
                    .timeout(config.getTimeout())
                    .build();
            
            StringBuilder progressData = new StringBuilder();
            
            ExecutionResult result = toolExecutor.executeWithOutput(
                    toolConfig,
                    line -> {
                        progressData.append(line).append("\n");
                        
                        // Parse and send progress
                        CrackProgress progress = parseProgress(line);
                        if (progress != null && progressCallback != null) {
                            progressCallback.accept(progress);
                        }
                    },
                    line -> log.warn("Hashcat stderr: {}", line)
            ).join();
            
            // Parse results
            List<String> crackedPasswords = parseCrackedPasswords(progressData.toString());
            boolean allCracked = !crackedPasswords.isEmpty();
            
            return CrackResult.builder()
                    .success(result.isSuccess())
                    .crackedCount(crackedPasswords.size())
                    .passwords(crackedPasswords)
                    .output(progressData.toString())
                    .duration(result.getDuration())
                    .build();
        });
    }
    
    /**
     * Crack WPA/WPA2 handshake
     */
    public CompletableFuture<CrackResult> crackWPA(
            String handshakeFile,
            String wordlistPath,
            Consumer<CrackProgress> progressCallback) {
        
        CrackConfig config = CrackConfig.builder()
                .hashType(WPA_WPA2)
                .attackMode(ATTACK_STRAIGHT)
                .hashFile(handshakeFile)
                .wordlistPath(wordlistPath)
                .workloadProfile(3) // High performance
                .optimized(true)
                .timeout(Duration.ofHours(2))
                .build();
        
        return crackWithWordlist(config, progressCallback);
    }
    
    /**
     * Crack with brute force (mask attack)
     */
    public CompletableFuture<CrackResult> crackWithMask(
            String hashFile,
            int hashType,
            String mask,
            Consumer<CrackProgress> progressCallback) {
        
        CrackConfig config = CrackConfig.builder()
                .hashType(hashType)
                .attackMode(ATTACK_BRUTE_FORCE)
                .hashFile(hashFile)
                .mask(mask)
                .workloadProfile(3)
                .optimized(true)
                .timeout(Duration.ofHours(4))
                .build();
        
        return crackWithWordlist(config, progressCallback);
    }
    
    /**
     * Resume a previous session
     */
    public CompletableFuture<CrackResult> resumeSession(
            String sessionName,
            Consumer<CrackProgress> progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            ToolConfig config = ToolConfig.builder()
                    .toolName(HASHCAT)
                    .executablePath(HASHCAT)
                    .arguments(List.of("--session", sessionName, "--restore"))
                    .timeout(Duration.ofHours(2))
                    .build();
            
            StringBuilder progressData = new StringBuilder();
            
            ExecutionResult result = toolExecutor.executeWithOutput(
                    config,
                    line -> {
                        progressData.append(line).append("\n");
                        CrackProgress progress = parseProgress(line);
                        if (progress != null && progressCallback != null) {
                            progressCallback.accept(progress);
                        }
                    },
                    line -> log.warn("Hashcat stderr: {}", line)
            ).join();
            
            List<String> crackedPasswords = parseCrackedPasswords(progressData.toString());
            
            return CrackResult.builder()
                    .success(result.isSuccess())
                    .crackedCount(crackedPasswords.size())
                    .passwords(crackedPasswords)
                    .output(progressData.toString())
                    .duration(result.getDuration())
                    .build();
        });
    }
    
    // Helper methods
    
    private List<String> buildHashcatArgs(CrackConfig config) {
        List<String> args = new ArrayList<>();
        
        // Hash type
        args.add("-m");
        args.add(String.valueOf(config.getHashType()));
        
        // Attack mode
        args.add("-a");
        args.add(String.valueOf(config.getAttackMode()));
        
        // Workload profile (1=low, 2=default, 3=high, 4=nightmare)
        if (config.getWorkloadProfile() > 0) {
            args.add("-w");
            args.add(String.valueOf(config.getWorkloadProfile()));
        }
        
        // Optimized kernels
        if (config.isOptimized()) {
            args.add("-O");
        }
        
        // Status updates
        args.add("--status");
        args.add("--status-timer=1");
        
        // Session name for resume
        if (config.getSessionName() != null) {
            args.add("--session");
            args.add(config.getSessionName());
        }
        
        // Output file for cracked hashes
        if (config.getOutputFile() != null) {
            args.add("--outfile");
            args.add(config.getOutputFile());
        }
        
        // Hash file
        args.add(config.getHashFile());
        
        // Wordlist or mask
        if (config.getWordlistPath() != null) {
            args.add(config.getWordlistPath());
        } else if (config.getMask() != null) {
            args.add(config.getMask());
        }
        
        // Rules file
        if (config.getRulesFile() != null) {
            args.add("-r");
            args.add(config.getRulesFile());
        }
        
        return args;
    }
    
    private GPUInfo parseGPUInfo(String output) {
        List<String> devices = new ArrayList<>();
        int deviceCount = 0;
        boolean hasGPU = false;
        
        Pattern devicePattern = Pattern.compile("Device #(\\d+): (.+)");
        Matcher matcher = devicePattern.matcher(output);
        
        while (matcher.find()) {
            deviceCount++;
            hasGPU = true;
            devices.add(matcher.group(2).trim());
        }
        
        String primaryDevice = devices.isEmpty() ? "CPU only" : devices.get(0);
        
        return new GPUInfo(hasGPU, deviceCount, primaryDevice, devices);
    }
    
    private long parseBenchmarkSpeed(String output) {
        // Parse "Speed.#1.........:   123.4 MH/s"
        Pattern speedPattern = Pattern.compile("Speed\\.#\\d+.*?:\\s+([\\d.]+)\\s+(H/s|kH/s|MH/s|GH/s)");
        Matcher matcher = speedPattern.matcher(output);
        
        if (matcher.find()) {
            double speed = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);
            
            // Convert to H/s
            return switch (unit) {
                case "kH/s" -> (long) (speed * 1_000);
                case "MH/s" -> (long) (speed * 1_000_000);
                case "GH/s" -> (long) (speed * 1_000_000_000);
                default -> (long) speed;
            };
        }
        
        return 0;
    }
    
    private CrackProgress parseProgress(String line) {
        // Parse status lines from hashcat
        if (line.contains("Progress")) {
            Pattern progressPattern = Pattern.compile("Progress\\.+:\\s+(\\d+)/(\\d+)");
            Matcher matcher = progressPattern.matcher(line);
            
            if (matcher.find()) {
                long current = Long.parseLong(matcher.group(1));
                long total = Long.parseLong(matcher.group(2));
                double percentage = (current * 100.0) / total;
                
                return CrackProgress.builder()
                        .current(current)
                        .total(total)
                        .percentage(percentage)
                        .build();
            }
        }
        
        if (line.contains("Speed")) {
            Pattern speedPattern = Pattern.compile("Speed\\.#\\d+.*?:\\s+([\\d.]+\\s+[kMG]?H/s)");
            Matcher matcher = speedPattern.matcher(line);
            
            if (matcher.find()) {
                return CrackProgress.builder()
                        .speedString(matcher.group(1))
                        .build();
            }
        }
        
        return null;
    }
    
    private List<String> parseCrackedPasswords(String output) {
        List<String> passwords = new ArrayList<>();
        
        // Hashcat outputs cracked hashes as: hash:password
        Pattern crackedPattern = Pattern.compile("([a-fA-F0-9:]+):(.+)");
        
        for (String line : output.lines().toList()) {
            if (line.contains("Cracked") || line.contains(":")) {
                Matcher matcher = crackedPattern.matcher(line);
                if (matcher.find()) {
                    passwords.add(matcher.group(2).trim());
                }
            }
        }
        
        return passwords;
    }
    
    // Result classes
    
    @Data
    @Builder
    public static class CrackConfig {
        private int hashType;
        private int attackMode;
        private String hashFile;
        private String wordlistPath;
        private String mask;
        private String rulesFile;
        private String outputFile;
        private String sessionName;
        private int workloadProfile;
        private boolean optimized;
        private Duration timeout;
    }
    
    @Data
    @Builder
    public static class CrackResult {
        private boolean success;
        private int crackedCount;
        private List<String> passwords;
        private String output;
        private Duration duration;
    }
    
    @Data
    @Builder
    public static class CrackProgress {
        private long current;
        private long total;
        private double percentage;
        private String speedString;
        private long speed;
        private String timeRemaining;
    }
    
    public record GPUInfo(
            boolean hasGPU,
            int deviceCount,
            String primaryDevice,
            List<String> devices
    ) {}
    
    public record BenchmarkResult(
            boolean success,
            int hashType,
            long speedHashesPerSecond,
            String output
    ) {}
}
