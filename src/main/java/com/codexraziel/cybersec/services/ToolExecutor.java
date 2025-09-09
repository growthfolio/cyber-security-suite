package com.codexraziel.cybersec.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@Slf4j
public class ToolExecutor {
    
    public CompletableFuture<Void> executeKeylogger(String platform, Consumer<String> outputCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                String toolPath = getKeyloggerPath(platform);
                if (toolPath != null) {
                    executeRealTool(toolPath, outputCallback);
                } else {
                    simulateKeylogger(platform, outputCallback);
                }
            } catch (Exception e) {
                log.error("Error executing keylogger", e);
                outputCallback.accept("[ERROR] " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Void> executeBruteForce(String target, String protocol, int threads, Consumer<String> outputCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                String toolPath = getBruteForceToolPath(protocol);
                if (toolPath != null) {
                    executeRealBruteForce(toolPath, target, protocol, threads, outputCallback);
                } else {
                    simulateBruteForce(target, protocol, threads, outputCallback);
                }
            } catch (Exception e) {
                log.error("Error executing brute force", e);
                outputCallback.accept("[ERROR] " + e.getMessage());
            }
        });
    }
    
    private String getKeyloggerPath(String platform) {
        String[] paths = {
            "./tools/learning-c-so/linux/optimized_keylogger",
            "./tools/learning-c-so/linux/system-monitor",
            "/usr/local/bin/keylogger"
        };
        
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                return path;
            }
        }
        return null;
    }
    
    private String getBruteForceToolPath(String protocol) {
        String[] paths = {
            "./tools/redteam-bruteforce/bin/redteam-bf",
            "/usr/bin/hydra",
            "/usr/bin/medusa",
            "/usr/bin/ncrack"
        };
        
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                return path;
            }
        }
        return null;
    }
    
    private void executeRealTool(String toolPath, Consumer<String> outputCallback) {
        try {
            ProcessBuilder pb = new ProcessBuilder(toolPath);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                outputCallback.accept(line);
            }
        } catch (Exception e) {
            outputCallback.accept("[ERROR] Failed to execute real tool: " + e.getMessage());
        }
    }
    
    private void executeRealBruteForce(String toolPath, String target, String protocol, int threads, Consumer<String> outputCallback) {
        try {
            ProcessBuilder pb = new ProcessBuilder(toolPath, protocol.toLowerCase(), "--target", target, "--threads", String.valueOf(threads));
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                outputCallback.accept(line);
            }
        } catch (Exception e) {
            outputCallback.accept("[ERROR] Failed to execute brute force tool: " + e.getMessage());
        }
    }
    
    private void simulateKeylogger(String platform, Consumer<String> outputCallback) {
        outputCallback.accept("[WARN] Real keylogger not found in expected paths");
        outputCallback.accept("[IDEA] Build the keylogger first: cd tools/learning-c-so/linux && make");
        outputCallback.accept("[INFO] Platform: " + platform);
        outputCallback.accept("[ERROR] Keylogger simulation disabled - use real tools only");
    }
    
    private void simulateBruteForce(String target, String protocol, int threads, Consumer<String> outputCallback) {
        outputCallback.accept("[WARN] Real brute force tools not found");
        outputCallback.accept("[IDEA] Install hydra, medusa, or ncrack for real attacks");
        outputCallback.accept("[INFO] Target: " + target + " (" + protocol + ")");
        outputCallback.accept("[ERROR] Brute force simulation disabled - use real tools only");
    }
}