package com.codexraziel.cybersec.workflow;

import com.codexraziel.cybersec.workflow.SimpleAttackResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ResultCorrelationEngine {
    
    @Autowired
    private RealResultCorrelator realCorrelator;
    
    public CorrelatedFindings correlateResults(List<SimpleAttackResult> results) {
        log.info("Correlating {} attack results using real analysis", results.size());
        return realCorrelator.correlateRealResults(results);
    }
    
    private List<AttackPath> identifyAttackPaths(List<SimpleAttackResult> results) {
        List<AttackPath> paths = new ArrayList<>();
        
        boolean hasWifiScan = results.stream().anyMatch(r -> r.getTarget().contains("wifi"));
        boolean hasBruteforce = results.stream().anyMatch(r -> r.getTarget().contains("bruteforce"));
        boolean hasKeylogger = results.stream().anyMatch(r -> r.getTarget().contains("keylogger"));
        
        if (hasWifiScan && hasBruteforce) {
            paths.add(new AttackPath("Network Compromise", 
                Arrays.asList("WiFi Discovery", "Credential Attack", "System Access"), 0.8));
        }
        
        if (hasBruteforce && hasKeylogger) {
            paths.add(new AttackPath("Credential Harvesting", 
                Arrays.asList("Initial Access", "Keylogger Deployment", "Credential Collection"), 0.9));
        }
        
        return paths;
    }
    
    private RiskAssessment generateRiskAssessment(List<SimpleAttackResult> results) {
        Map<String, String> riskByCategory = new HashMap<>();
        
        long successfulAttacks = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        double successRate = (double) successfulAttacks / results.size();
        
        String overallRisk;
        if (successRate > 0.7) {
            overallRisk = "HIGH";
            riskByCategory.put("Network Security", "HIGH");
            riskByCategory.put("Access Control", "HIGH");
        } else if (successRate > 0.4) {
            overallRisk = "MEDIUM";
            riskByCategory.put("Network Security", "MEDIUM");
            riskByCategory.put("Access Control", "MEDIUM");
        } else {
            overallRisk = "LOW";
            riskByCategory.put("Network Security", "LOW");
            riskByCategory.put("Access Control", "LOW");
        }
        
        return new RiskAssessment(overallRisk, riskByCategory);
    }
}