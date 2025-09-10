package com.codexraziel.cybersec.workflow;

import java.util.HashMap;
import java.util.Map;

public class WorkflowStep {
    private final String type;
    private final String name;
    private final boolean critical;
    private final Map<String, Object> parameters;
    
    public WorkflowStep(String type, String name, boolean critical) {
        this.type = type;
        this.name = name;
        this.critical = critical;
        this.parameters = new HashMap<>();
    }
    
    public String getType() { return type; }
    public String getName() { return name; }
    public boolean isCritical() { return critical; }
    public Map<String, Object> getParameters() { return parameters; }
    
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }
}