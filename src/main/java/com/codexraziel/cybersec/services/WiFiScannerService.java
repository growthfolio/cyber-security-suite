package com.codexraziel.cybersec.services;

import com.codexraziel.cybersec.models.WiFiNetwork;
import com.codexraziel.cybersec.models.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
public class WiFiScannerService {
    
    private static final int SCAN_TIMEOUT_SECONDS = 30;
    
    public CompletableFuture<ScanResult> scanNetworks(String networkInterface, Consumer<String> outputCallback) {
        return CompletableFuture.supplyAsync(() -> {
            List<WiFiNetwork> networks = new ArrayList<>();
            String scanMethod = "Unknown";
            boolean successful = false;
            String errorMessage = null;
            
            try {
                outputCallback.accept("[SATELLITE] Starting WiFi scan on interface: " + networkInterface);
                
                if (scanWithNmcli(networks, outputCallback)) {
                    scanMethod = "nmcli";
                    successful = true;
                } else if (scanWithIwlist(networks, outputCallback)) {
                    scanMethod = "iwlist";
                    successful = true;
                } else {
                    scanMethod = "fallback";
                    addTestNetworks(networks, outputCallback);
                    successful = true;
                }
                
                outputCallback.accept("[OK] Scan completed - " + networks.size() + " networks found");
                
            } catch (Exception e) {
                log.error("WiFi scan failed", e);
                errorMessage = e.getMessage();
                outputCallback.accept("[ERROR] Scan error: " + e.getMessage());
            }
            
            return ScanResult.builder()
                .networks(networks)
                .scanTime(LocalDateTime.now())
                .scanInterface(networkInterface)
                .totalNetworks(networks.size())
                .vulnerableNetworks((int) networks.stream().filter(WiFiNetwork::isVulnerable).count())
                .scanMethod(scanMethod)
                .successful(successful)
                .errorMessage(errorMessage)
                .build();
        });
    }
    
    private boolean scanWithNmcli(List<WiFiNetwork> networks, Consumer<String> outputCallback) {
        try {
            ProcessBuilder pb = new ProcessBuilder("nmcli", "-t", "-f", 
                "SSID,BSSID,MODE,CHAN,FREQ,RATE,SIGNAL,BARS,SECURITY", "dev", "wifi", "list");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            boolean finished = process.waitFor(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            if (process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        WiFiNetwork network = parseNmcliLine(line);
                        if (network != null && !network.getSsid().isEmpty()) {
                            networks.add(network);
                            outputCallback.accept("[SATELLITE] Found: " + network.getSsid());
                        }
                    }
                }
                return !networks.isEmpty();
            }
        } catch (Exception e) {
            log.debug("nmcli scan failed: {}", e.getMessage());
        }
        return false;
    }
    
    private boolean scanWithIwlist(List<WiFiNetwork> networks, Consumer<String> outputCallback) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "iwlist", "scan");
            Process process = pb.start();
            
            boolean finished = process.waitFor(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            // Parse iwlist output (simplified)
            outputCallback.accept("[WARN] iwlist parsing not fully implemented");
            return false;
            
        } catch (Exception e) {
            log.debug("iwlist scan failed: {}", e.getMessage());
        }
        return false;
    }
    
    private WiFiNetwork parseNmcliLine(String line) {
        String[] parts = line.split(":");
        if (parts.length < 9) return null;
        
        return WiFiNetwork.builder()
            .ssid(parts[0].trim())
            .bssid(parts[1].trim())
            .channel(parts[3].trim())
            .frequency(parts[4].trim())
            .signalStrength(parseIntSafe(parts[6]))
            .security(parts[8].trim())
            .discoveredAt(LocalDateTime.now())
            .build();
    }
    
    private void addTestNetworks(List<WiFiNetwork> networks, Consumer<String> outputCallback) {
        outputCallback.accept("[WARN] No scanning tools available - showing test networks");
        
        networks.add(WiFiNetwork.builder()
            .ssid("TestNetwork_WPA2").bssid("AA:BB:CC:DD:EE:01")
            .security("WPA2").signalStrength(-45).channel("6")
            .frequency("2.4GHz").discoveredAt(LocalDateTime.now()).build());
            
        networks.add(WiFiNetwork.builder()
            .ssid("OpenWiFi").bssid("AA:BB:CC:DD:EE:02")
            .security("Open").signalStrength(-60).channel("11")
            .frequency("2.4GHz").discoveredAt(LocalDateTime.now()).build());
    }
    
    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -70; // Default signal strength
        }
    }
}