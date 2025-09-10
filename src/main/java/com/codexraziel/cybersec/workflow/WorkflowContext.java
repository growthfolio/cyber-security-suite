package com.codexraziel.cybersec.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowContext {
    private final Map<String, Object> globalContext;
    private final List<String> discoveredTargets;
    private final List<Credential> harvestedCredentials;
    
    public WorkflowContext() {
        this.globalContext = new HashMap<>();
        this.discoveredTargets = new ArrayList<>();
        this.harvestedCredentials = new ArrayList<>();
    }
    
    public Map<String, Object> getGlobalContext() { return globalContext; }
    public List<String> getDiscoveredTargets() { return discoveredTargets; }
    public List<Credential> getHarvestedCredentials() { return harvestedCredentials; }
    
    public void addDiscoveredTarget(String target) {
        if (!discoveredTargets.contains(target)) {
            discoveredTargets.add(target);
        }
    }
    
    public void addHarvestedCredential(Credential credential) {
        harvestedCredentials.add(credential);
    }
    
    public void setContextValue(String key, Object value) {
        globalContext.put(key, value);
    }
    
    public Object getContextValue(String key) {
        return globalContext.get(key);
    }
}