package com.codexraziel.cybersec.workflow.integrators;

import com.codexraziel.cybersec.wifi.adapters.SecureWiFiToolAdapter;
import com.codexraziel.cybersec.wifi.models.WiFiNetwork;
import com.codexraziel.cybersec.wifi.models.HandshakeCapture;
import com.codexraziel.cybersec.wifi.models.WiFiAttackResult;
import com.codexraziel.cybersec.workflow.SimpleAttackResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Integrates WiFi attack capabilities with the workflow system
 * Bridges SecureWiFiToolAdapter with workflow execution framework
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WiFiWorkflowIntegrator {
    
    private final SecureWiFiToolAdapter wifiAdapter;
    
    private static final String DEFAULT_WORDLIST = "/home/felipe-macedo/cyber-projects/cyber-security-suite/wordlists/passwords/rockyou.txt";
    private static final String CAPTURE_OUTPUT_DIR = "/tmp/wifi_captures";
    
    /**
     * Enable monitor mode as workflow step
     */
    public CompletableFuture<SimpleAttackResult> enableMonitorMode(Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String interfaceName = getRequiredParam(params, "interface");
                
                boolean success = wifiAdapter.enableMonitorMode(interfaceName);
                
                return new SimpleAttackResult(
                    "wifi_monitor_mode", 
                    success ? "Monitor mode enabled on " + interfaceName : "Failed to enable monitor mode",
                    success
                );
                
            } catch (Exception e) {
                log.error("Monitor mode workflow step failed", e);
                return new SimpleAttackResult("wifi_monitor_mode", 
                    "Monitor mode failed: " + e.getMessage(), false);
            }
        });
    }
    
    /**
     * Scan for networks as workflow step
     */
    public CompletableFuture<SimpleAttackResult> scanNetworks(Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String interfaceName = getRequiredParam(params, "interface");
                int duration = (Integer) params.getOrDefault("duration", 60);
                
                List<WiFiNetwork> networks = wifiAdapter.scanNetworks(interfaceName, duration);
                
                String message = String.format("Found %d networks in %d seconds", 
                                              networks.size(), duration);
                
                // Store networks in workflow context for next steps
                storeNetworksInContext(params, networks);
                
                return new SimpleAttackResult("wifi_network_scan", message, !networks.isEmpty());
                
            } catch (Exception e) {
                log.error("Network scan workflow step failed", e);
                return new SimpleAttackResult("wifi_network_scan", 
                    "Network scan failed: " + e.getMessage(), false);
            }
        });
    }
    
    /**
     * Capture handshake as workflow step
     */
    public CompletableFuture<SimpleAttackResult> captureHandshake(Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String interfaceName = getRequiredParam(params, "interface");
                String targetSSID = getRequiredParam(params, "target_ssid");
                String targetBSSID = getRequiredParam(params, "target_bssid");
                String channel = (String) params.get("channel");
                int timeout = (Integer) params.getOrDefault("timeout", 300);
                
                // Build target network
                WiFiNetwork target = WiFiNetwork.builder()
                    .ssid(targetSSID)
                    .bssid(targetBSSID)
                    .channel(channel != null ? channel : "1")
                    .build();
                
                Path outputDir = Paths.get(CAPTURE_OUTPUT_DIR);
                HandshakeCapture capture = wifiAdapter.captureHandshake(
                    interfaceName, target, outputDir, timeout);
                
                String message = capture.isHandshakeDetected() ? 
                    "Handshake captured successfully" : 
                    "No handshake captured: " + capture.getErrorMessage();
                
                // Store capture info for next steps
                storeCaptureInContext(params, capture);
                
                return new SimpleAttackResult("wifi_handshake_capture", 
                    message, capture.isHandshakeDetected());
                
            } catch (Exception e) {
                log.error("Handshake capture workflow step failed", e);
                return new SimpleAttackResult("wifi_handshake_capture", 
                    "Handshake capture failed: " + e.getMessage(), false);
            }
        });
    }
    
    /**
     * Execute dictionary attack as workflow step
     */
    public CompletableFuture<SimpleAttackResult> executeDictionaryAttack(Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get handshake from previous step or parameters
                HandshakeCapture handshake = getCaptureFromContext(params);
                if (handshake == null || !handshake.isHandshakeDetected()) {
                    return new SimpleAttackResult("wifi_dictionary_attack", 
                        "No valid handshake available for attack", false);
                }
                
                String wordlistPath = (String) params.getOrDefault("wordlist_path", DEFAULT_WORDLIST);
                Path wordlist = Paths.get(wordlistPath);
                
                WiFiAttackResult result = wifiAdapter.crackPassword(handshake, wordlist);
                
                String message = result.isSuccessful() ? 
                    "Password cracked: " + result.getDiscoveredPassword() :
                    "Password not found in wordlist";
                
                // Store attack result
                storeAttackResultInContext(params, result);
                
                return new SimpleAttackResult("wifi_dictionary_attack", 
                    message, result.isSuccessful());
                
            } catch (Exception e) {
                log.error("Dictionary attack workflow step failed", e);
                return new SimpleAttackResult("wifi_dictionary_attack", 
                    "Dictionary attack failed: " + e.getMessage(), false);
            }
        });
    }
    
    /**
     * Disable monitor mode as cleanup step
     */
    public CompletableFuture<SimpleAttackResult> disableMonitorMode(Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String interfaceName = getRequiredParam(params, "interface");
                
                boolean success = wifiAdapter.disableMonitorMode(interfaceName);
                
                return new SimpleAttackResult("wifi_cleanup", 
                    success ? "Monitor mode disabled" : "Failed to disable monitor mode",
                    success);
                
            } catch (Exception e) {
                log.error("Monitor mode cleanup failed", e);
                return new SimpleAttackResult("wifi_cleanup", 
                    "Cleanup failed: " + e.getMessage(), false);
            }
        });
    }
    
    // === Helper Methods ===
    
    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter missing: " + key);
        }
        return value.toString();
    }
    
    private void storeNetworksInContext(Map<String, Object> params, List<WiFiNetwork> networks) {
        // Store for use by subsequent workflow steps
        params.put("discovered_networks", networks);
        
        // Find vulnerable networks for target selection
        List<WiFiNetwork> vulnerableNetworks = networks.stream()
            .filter(WiFiNetwork::isVulnerable)
            .toList();
        params.put("vulnerable_networks", vulnerableNetworks);
        
        log.info("Stored {} networks in workflow context ({} vulnerable)", 
                networks.size(), vulnerableNetworks.size());
    }
    
    private void storeCaptureInContext(Map<String, Object> params, HandshakeCapture capture) {
        params.put("handshake_capture", capture);
        if (capture.isHandshakeDetected()) {
            params.put("handshake_file", capture.getCaptureFilePath().toString());
        }
        log.info("Stored handshake capture in workflow context: {}", capture.getStatus());
    }
    
    private HandshakeCapture getCaptureFromContext(Map<String, Object> params) {
        return (HandshakeCapture) params.get("handshake_capture");
    }
    
    private void storeAttackResultInContext(Map<String, Object> params, WiFiAttackResult result) {
        params.put("attack_result", result);
        if (result.isSuccessful()) {
            params.put("cracked_password", result.getDiscoveredPassword());
        }
        log.info("Stored attack result in workflow context: success={}", result.isSuccessful());
    }
}
