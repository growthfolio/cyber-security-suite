package com.codexraziel.cybersec.workflow;

import java.util.List;
import java.util.Map;

public class CorrelatedFindings {
    private final Map<String, List<String>> findingsByTool;
    private final List<AttackPath> attackPaths;
    private final RiskAssessment riskAssessment;
    
    public CorrelatedFindings(Map<String, List<String>> findingsByTool, 
                             List<AttackPath> attackPaths, 
                             RiskAssessment riskAssessment) {
        this.findingsByTool = findingsByTool;
        this.attackPaths = attackPaths;
        this.riskAssessment = riskAssessment;
    }
    
    public Map<String, List<String>> getFindingsByTool() { return findingsByTool; }
    public List<AttackPath> getAttackPaths() { return attackPaths; }
    public RiskAssessment getRiskAssessment() { return riskAssessment; }
}

class AttackPath {
    private final String name;
    private final List<String> steps;
    private final double probability;
    
    public AttackPath(String name, List<String> steps, double probability) {
        this.name = name;
        this.steps = steps;
        this.probability = probability;
    }
    
    public String getName() { return name; }
    public List<String> getSteps() { return steps; }
    public double getProbability() { return probability; }
}

class RiskAssessment {
    private final String overallRisk;
    private final Map<String, String> riskByCategory;
    
    public RiskAssessment(String overallRisk, Map<String, String> riskByCategory) {
        this.overallRisk = overallRisk;
        this.riskByCategory = riskByCategory;
    }
    
    public String getOverallRisk() { return overallRisk; }
    public Map<String, String> getRiskByCategory() { return riskByCategory; }
}