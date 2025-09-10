package com.codexraziel.cybersec.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class WorkflowContextEnhancer {
    
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");
    private static final Pattern SSID_PATTERN = Pattern.compile("Network:\\s*([^\\n]+)");
    private static final Pattern CRED_PATTERN = Pattern.compile("([a-zA-Z0-9_]+):([a-zA-Z0-9_!@#$%^&*]+)");
    
    public void enhanceContext(WorkflowContext context, WorkflowStep step, SimpleAttackResult result) {
        log.debug("Enhancing context with result from step: {}", step.getType());
        
        switch (step.getType()) {
            case "wifi_scan" -> enhanceWithWiFiResults(context, result);
            case "network_analysis" -> enhanceWithNetworkResults(context, result);
            case "ssh_bruteforce", "bruteforce" -> enhanceWithBruteForceResults(context, result);
            case "deploy_keylogger" -> enhanceWithKeyloggerResults(context, result);
        }
    }
    
    private void enhanceWithWiFiResults(WorkflowContext context, SimpleAttackResult result) {
        if (!result.isSuccess() || result.getData() == null) return;
        
        String data = result.getData().toString();
        Matcher matcher = SSID_PATTERN.matcher(data);
        
        List<String> networks = new ArrayList<>();
        while (matcher.find()) {
            String ssid = matcher.group(1).trim();
            networks.add(ssid);
            context.addDiscoveredTarget("wifi:" + ssid);
        }
        
        context.setContextValue("discovered_networks", networks);
        context.setContextValue("wifi_scan_data", data);
        
        log.info("Enhanced context with {} WiFi networks", networks.size());
    }
    
    private void enhanceWithNetworkResults(WorkflowContext context, SimpleAttackResult result) {
        if (!result.isSuccess() || result.getData() == null) return;
        
        String data = result.getData().toString();
        Matcher ipMatcher = IP_PATTERN.matcher(data);
        
        List<String> targets = new ArrayList<>();
        while (ipMatcher.find()) {
            String ip = ipMatcher.group();
            targets.add(ip);
            context.addDiscoveredTarget("ip:" + ip);
        }
        
        // Extract open ports
        Pattern portPattern = Pattern.compile("(\\d+)/(tcp|udp)\\s+open");
        Matcher portMatcher = portPattern.matcher(data);
        
        List<String> openPorts = new ArrayList<>();
        while (portMatcher.find()) {
            String port = portMatcher.group(1) + "/" + portMatcher.group(2);
            openPorts.add(port);
        }
        
        context.setContextValue("discovered_targets", targets);
        context.setContextValue("open_ports", openPorts);
        context.setContextValue("network_analysis_data", data);
        
        log.info("Enhanced context with {} targets and {} open ports", targets.size(), openPorts.size());
    }
    
    private void enhanceWithBruteForceResults(WorkflowContext context, SimpleAttackResult result) {
        if (!result.isSuccess()) return;
        
        String message = result.getMessage();
        Matcher credMatcher = CRED_PATTERN.matcher(message);
        
        while (credMatcher.find()) {
            String username = credMatcher.group(1);
            String password = credMatcher.group(2);
            
            Credential credential = new Credential(username, password);
            context.addHarvestedCredential(credential);
        }
        
        context.setContextValue("bruteforce_result", message);
        context.setContextValue("attack_successful", result.isSuccess());
        
        log.info("Enhanced context with {} credentials", context.getHarvestedCredentials().size());
    }
    
    private void enhanceWithKeyloggerResults(WorkflowContext context, SimpleAttackResult result) {
        context.setContextValue("keylogger_deployed", result.isSuccess());
        context.setContextValue("keylogger_status", result.getMessage());
        
        if (result.isSuccess()) {
            context.setContextValue("persistence_established", true);
            log.info("Keylogger successfully deployed - persistence established");
        } else {
            log.warn("Keylogger deployment failed: {}", result.getMessage());
        }
    }
    
    public Map<String, Object> buildEnhancedParameters(WorkflowStep step, WorkflowContext context) {
        Map<String, Object> params = Map.copyOf(step.getParameters());
        
        switch (step.getType()) {
            case "network_analysis" -> {
                // Use discovered WiFi networks as targets
                @SuppressWarnings("unchecked")
                List<String> networks = (List<String>) context.getContextValue("discovered_networks");
                if (networks != null && !networks.isEmpty()) {
                    // Use first network's potential gateway
                    params.put("target", "192.168.1.1"); // Common gateway
                }
            }
            case "ssh_bruteforce", "bruteforce" -> {
                // Use discovered targets from network analysis
                if (!context.getDiscoveredTargets().isEmpty()) {
                    String target = context.getDiscoveredTargets().get(0);
                    if (target.startsWith("ip:")) {
                        params.put("target", target.substring(3));
                    }
                }
                
                // Use discovered open ports to determine protocol
                @SuppressWarnings("unchecked")
                List<String> openPorts = (List<String>) context.getContextValue("open_ports");
                if (openPorts != null) {
                    if (openPorts.stream().anyMatch(p -> p.startsWith("22/"))) {
                        params.put("protocol", "SSH");
                    } else if (openPorts.stream().anyMatch(p -> p.startsWith("3389/"))) {
                        params.put("protocol", "RDP");
                    }
                }
            }
            case "deploy_keylogger" -> {
                // Use harvested credentials for deployment
                if (!context.getHarvestedCredentials().isEmpty()) {
                    Credential cred = context.getHarvestedCredentials().get(0);
                    params.put("username", cred.getUsername());
                    params.put("use_credentials", true);
                }
            }
        }
        
        return params;
    }
}