package com.codexraziel.cybersec.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class RealResultCorrelator {
    
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");
    private static final Pattern PORT_PATTERN = Pattern.compile("(\\d+)/(tcp|udp)\\s+open");
    private static final Pattern SSID_PATTERN = Pattern.compile("Network:\\s*([^\\n]+)");
    
    public CorrelatedFindings correlateRealResults(List<SimpleAttackResult> results) {
        log.info("Correlating {} real attack results", results.size());
        
        Map<String, List<String>> findingsByTool = extractFindings(results);
        List<AttackPath> attackPaths = identifyRealAttackPaths(results);
        RiskAssessment riskAssessment = generateRealRiskAssessment(results);
        
        return new CorrelatedFindings(findingsByTool, attackPaths, riskAssessment);
    }
    
    private Map<String, List<String>> extractFindings(List<SimpleAttackResult> results) {
        Map<String, List<String>> findings = new HashMap<>();
        
        for (SimpleAttackResult result : results) {
            String tool = result.getTarget();
            List<String> toolFindings = new ArrayList<>();
            
            switch (tool) {
                case "wifi_scan" -> {
                    if (result.getData() != null) {
                        String data = result.getData().toString();
                        Matcher matcher = SSID_PATTERN.matcher(data);
                        while (matcher.find()) {
                            toolFindings.add("WiFi Network: " + matcher.group(1).trim());
                        }
                    }
                }
                case "network_analysis" -> {
                    if (result.getData() != null) {
                        String data = result.getData().toString();
                        Matcher portMatcher = PORT_PATTERN.matcher(data);
                        while (portMatcher.find()) {
                            toolFindings.add("Open Port: " + portMatcher.group(1) + "/" + portMatcher.group(2));
                        }
                        
                        Matcher ipMatcher = IP_PATTERN.matcher(data);
                        while (ipMatcher.find()) {
                            toolFindings.add("Target IP: " + ipMatcher.group());
                        }
                    }
                }
                case "ssh_bruteforce", "bruteforce" -> {
                    if (result.isSuccess() && result.getMessage().contains("SUCCESS")) {
                        toolFindings.add("Credential Found: " + extractCredentials(result.getMessage()));
                    } else {
                        toolFindings.add("Attack Status: " + result.getMessage());
                    }
                }
                case "keylogger" -> {
                    if (result.isSuccess()) {
                        toolFindings.add("Keylogger Status: Active");
                        toolFindings.add("Data Collection: Enabled");
                    } else {
                        toolFindings.add("Keylogger Status: Failed - " + result.getMessage());
                    }
                }
            }
            
            findings.put(tool, toolFindings);
        }
        
        return findings;
    }
    
    private String extractCredentials(String message) {
        // Extract username:password from success messages
        Pattern credPattern = Pattern.compile("([a-zA-Z0-9_]+):([a-zA-Z0-9_!@#$%^&*]+)");
        Matcher matcher = credPattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1) + ":***";  // Hide password for security
        }
        return "Found valid credentials";
    }
    
    private List<AttackPath> identifyRealAttackPaths(List<SimpleAttackResult> results) {
        List<AttackPath> paths = new ArrayList<>();
        
        boolean hasWifiScan = results.stream().anyMatch(r -> r.getTarget().equals("wifi_scan") && r.isSuccess());
        boolean hasNetworkAnalysis = results.stream().anyMatch(r -> r.getTarget().equals("network_analysis") && r.isSuccess());
        boolean hasBruteforce = results.stream().anyMatch(r -> r.getTarget().contains("bruteforce") && r.isSuccess());
        boolean hasKeylogger = results.stream().anyMatch(r -> r.getTarget().equals("keylogger") && r.isSuccess());
        
        // Network Reconnaissance Path
        if (hasWifiScan && hasNetworkAnalysis) {
            double probability = calculatePathProbability(results, Arrays.asList("wifi_scan", "network_analysis"));
            paths.add(new AttackPath("Network Reconnaissance", 
                Arrays.asList("WiFi Discovery", "Network Scanning", "Target Identification"), probability));
        }
        
        // Credential Attack Path
        if (hasNetworkAnalysis && hasBruteforce) {
            double probability = calculatePathProbability(results, Arrays.asList("network_analysis", "bruteforce"));
            paths.add(new AttackPath("Credential Attack", 
                Arrays.asList("Service Discovery", "Brute Force Attack", "Access Gained"), probability));
        }
        
        // Post-Exploitation Path
        if (hasBruteforce && hasKeylogger) {
            double probability = calculatePathProbability(results, Arrays.asList("bruteforce", "keylogger"));
            paths.add(new AttackPath("Post-Exploitation", 
                Arrays.asList("Initial Access", "Persistence Setup", "Data Collection"), probability));
        }
        
        // Full Attack Chain
        if (hasWifiScan && hasNetworkAnalysis && hasBruteforce && hasKeylogger) {
            paths.add(new AttackPath("Complete Attack Chain", 
                Arrays.asList("Reconnaissance", "Exploitation", "Post-Exploitation", "Persistence"), 0.95));
        }
        
        return paths;
    }
    
    private double calculatePathProbability(List<SimpleAttackResult> results, List<String> pathSteps) {
        long successfulSteps = results.stream()
            .filter(r -> pathSteps.contains(r.getTarget()) && r.isSuccess())
            .count();
        
        return (double) successfulSteps / pathSteps.size();
    }
    
    private RiskAssessment generateRealRiskAssessment(List<SimpleAttackResult> results) {
        Map<String, String> riskByCategory = new HashMap<>();
        
        // Analyze WiFi security
        boolean hasOpenWifi = results.stream()
            .filter(r -> r.getTarget().equals("wifi_scan"))
            .anyMatch(r -> r.getData() != null && r.getData().toString().contains("open"));
        
        riskByCategory.put("WiFi Security", hasOpenWifi ? "HIGH" : "MEDIUM");
        
        // Analyze network security
        boolean hasOpenPorts = results.stream()
            .filter(r -> r.getTarget().equals("network_analysis"))
            .anyMatch(r -> r.getData() != null && r.getData().toString().contains("open"));
        
        riskByCategory.put("Network Security", hasOpenPorts ? "HIGH" : "LOW");
        
        // Analyze access control
        boolean hasWeakCredentials = results.stream()
            .filter(r -> r.getTarget().contains("bruteforce"))
            .anyMatch(r -> r.isSuccess());
        
        riskByCategory.put("Access Control", hasWeakCredentials ? "CRITICAL" : "MEDIUM");
        
        // Analyze monitoring capabilities
        boolean hasKeylogger = results.stream()
            .filter(r -> r.getTarget().equals("keylogger"))
            .anyMatch(r -> r.isSuccess());
        
        riskByCategory.put("Data Protection", hasKeylogger ? "CRITICAL" : "LOW");
        
        // Calculate overall risk
        String overallRisk = calculateOverallRisk(riskByCategory);
        
        return new RiskAssessment(overallRisk, riskByCategory);
    }
    
    private String calculateOverallRisk(Map<String, String> riskByCategory) {
        long criticalCount = riskByCategory.values().stream().mapToLong(risk -> "CRITICAL".equals(risk) ? 1 : 0).sum();
        long highCount = riskByCategory.values().stream().mapToLong(risk -> "HIGH".equals(risk) ? 1 : 0).sum();
        
        if (criticalCount > 0) {
            return "CRITICAL";
        } else if (highCount >= 2) {
            return "HIGH";
        } else if (highCount >= 1) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}