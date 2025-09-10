package com.codexraziel.cybersec.workflow;

import java.util.HashMap;
import java.util.Map;

public class WorkflowConfig {
    private final Map<String, Object> parameters;
    private int maxThreads = 10;
    private int timeoutSeconds = 300;
    private boolean stopOnFailure = false;
    
    public WorkflowConfig() {
        this.parameters = new HashMap<>();
    }
    
    public Map<String, Object> getParameters() { return parameters; }
    
    public int getMaxThreads() { return maxThreads; }
    public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }
    
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public boolean isStopOnFailure() { return stopOnFailure; }
    public void setStopOnFailure(boolean stopOnFailure) { this.stopOnFailure = stopOnFailure; }
    
    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return parameters.get(key);
    }
}