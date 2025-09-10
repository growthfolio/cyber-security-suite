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
}