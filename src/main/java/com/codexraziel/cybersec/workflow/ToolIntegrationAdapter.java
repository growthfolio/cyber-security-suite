package com.codexraziel.cybersec.workflow;

import com.codexraziel.cybersec.workflow.SimpleAttackResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class ToolIntegrationAdapter {
    
    @Autowired
    private RealToolExecutor realToolExecutor;
    
    public CompletableFuture<SimpleAttackResult> executeWiFiScan(Map<String, Object> params) {
        log.info("Executing real WiFi scan with parameters: {}", params);
        String interfaceName = (String) params.get("interface");
        return realToolExecutor.executeWiFiScan(interfaceName);
    }
    
    public CompletableFuture<SimpleAttackResult> executeBruteForce(Map<String, Object> params) {
        log.info("Executing real bruteforce attack with parameters: {}", params);
        String target = params.getOrDefault("target", "192.168.1.1").toString();
        String protocol = params.getOrDefault("protocol", "SSH").toString();
        return realToolExecutor.executeBruteForce(target, protocol);
    }
    
    public CompletableFuture<SimpleAttackResult> deployKeylogger(Map<String, Object> params) {
        log.info("Deploying real keylogger with parameters: {}", params);
        return realToolExecutor.executeKeylogger();
    }
    
    public CompletableFuture<SimpleAttackResult> executeNetworkAnalysis(Map<String, Object> params) {
        log.info("Executing real network analysis with parameters: {}", params);
        String target = params.getOrDefault("target", "192.168.1.1").toString();
        return realToolExecutor.executeNetworkAnalysis(target);
    }
    
    // ===== WiFi Attack Methods =====
    
    public CompletableFuture<SimpleAttackResult> setupWiFiMonitorMode(Map<String, Object> params) {
        log.info("Setting up WiFi monitor mode with parameters: {}", params);
        String interfaceName = (String) params.get("interface");
        return realToolExecutor.setupWiFiMonitorMode(interfaceName);
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiNetworkScan(Map<String, Object> params) {
        log.info("Executing WiFi network scan with parameters: {}", params);
        String interfaceName = (String) params.get("interface");
        Integer duration = (Integer) params.getOrDefault("duration", 60);
        return realToolExecutor.executeWiFiNetworkScan(interfaceName, duration);
    }
    
    public CompletableFuture<SimpleAttackResult> captureWiFiHandshake(Map<String, Object> params) {
        log.info("Capturing WiFi handshake with parameters: {}", params);
        String interfaceName = (String) params.get("interface");
        String targetSSID = (String) params.get("target_ssid");
        String targetBSSID = (String) params.get("target_bssid");
        Integer timeout = (Integer) params.getOrDefault("timeout", 300);
        return realToolExecutor.captureWiFiHandshake(interfaceName, targetSSID, targetBSSID, timeout);
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiDictionaryAttack(Map<String, Object> params) {
        log.info("Executing WiFi dictionary attack with parameters: {}", params);
        String handshakeFile = (String) params.get("handshake_file");
        String wordlistPath = (String) params.get("wordlist_path");
        String targetBSSID = (String) params.get("target_bssid");
        return realToolExecutor.executeWiFiDictionaryAttack(handshakeFile, wordlistPath, targetBSSID);
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiVulnerabilityAssessment(Map<String, Object> params) {
        log.info("Executing WiFi vulnerability assessment with parameters: {}", params);
        String interfaceName = (String) params.get("interface");
        return realToolExecutor.executeWiFiVulnerabilityAssessment(interfaceName);
    }
    
    public CompletableFuture<SimpleAttackResult> executeWiFiMultiVectorAttack(Map<String, Object> params) {
        log.info("Executing WiFi multi-vector attack with parameters: {}", params);
        String interfaceName = (String) params.get("interface");
        String targetSSID = (String) params.get("target_ssid");
        String attackMethods = (String) params.getOrDefault("attack_methods", "dictionary,wps");
        return realToolExecutor.executeWiFiMultiVectorAttack(interfaceName, targetSSID, attackMethods);
    }
    
    public CompletableFuture<SimpleAttackResult> setupEvilTwinAP(Map<String, Object> params) {
        log.info("Setting up Evil Twin AP with parameters: {}", params);
        String interfaceName = (String) params.get("interface");
        String targetSSID = (String) params.get("target_ssid");
        String channel = (String) params.get("channel");
        return realToolExecutor.setupEvilTwinAP(interfaceName, targetSSID, channel);
    }
    
    public CompletableFuture<SimpleAttackResult> harvestWiFiCredentials(Map<String, Object> params) {
        log.info("Harvesting WiFi credentials with parameters: {}", params);
        String method = (String) params.getOrDefault("method", "captive_portal");
        return realToolExecutor.harvestWiFiCredentials(method);
    }
}