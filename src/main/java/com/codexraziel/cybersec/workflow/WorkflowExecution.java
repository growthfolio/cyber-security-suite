package com.codexraziel.cybersec.workflow;

import com.codexraziel.cybersec.workflow.SimpleAttackResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkflowExecution {
    private final String workflowId;
    private final AttackScenario scenario;
    private final WorkflowConfig config;
    private final WorkflowContext context;
    private final LocalDateTime startTime;
    
    private WorkflowState currentState;
    private WorkflowStep currentStep;
    private LocalDateTime endTime;
    private final List<SimpleAttackResult> results;
    private final List<WorkflowStep> completedSteps;
    
    public WorkflowExecution(String workflowId, AttackScenario scenario, WorkflowConfig config) {
        this.workflowId = workflowId;
        this.scenario = scenario;
        this.config = config;
        this.context = new WorkflowContext();
        this.startTime = LocalDateTime.now();
        this.currentState = WorkflowState.INITIALIZED;
        this.results = new ArrayList<>();
        this.completedSteps = new ArrayList<>();
    }
    
    public String getWorkflowId() { return workflowId; }
    public AttackScenario getScenario() { return scenario; }
    public WorkflowConfig getConfig() { return config; }
    public WorkflowContext getContext() { return context; }
    public LocalDateTime getStartTime() { return startTime; }
    
    public WorkflowState getState() { return currentState; }
    public void setState(WorkflowState state) { this.currentState = state; }
    
    public WorkflowStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(WorkflowStep step) { this.currentStep = step; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public List<SimpleAttackResult> getResults() { return results; }
    public void addResult(SimpleAttackResult result) { 
        results.add(result);
        if (currentStep != null) {
            completedSteps.add(currentStep);
        }
    }
    
    public List<WorkflowStep> getCompletedSteps() { return completedSteps; }
}