package com.codexraziel.cybersec.workflow;

import com.codexraziel.cybersec.workflow.SimpleAttackResult;
import com.codexraziel.cybersec.security.AuditLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class WorkflowManager {
    
    private final Map<String, WorkflowExecution> activeWorkflows = new ConcurrentHashMap<>();
    private final ExecutorService workflowExecutor = Executors.newFixedThreadPool(5);
    
    @Autowired
    private ToolIntegrationAdapter toolAdapter;
    
    @Autowired
    private ResultCorrelationEngine correlationEngine;
    
    @Autowired
    private AuditLogger auditLogger;
    
    @Autowired
    private WorkflowContextEnhancer contextEnhancer;
    
    public CompletableFuture<WorkflowResult> executeWorkflow(AttackScenario scenario, WorkflowConfig config) {
        String workflowId = UUID.randomUUID().toString();
        
        WorkflowExecution execution = new WorkflowExecution(workflowId, scenario, config);
        activeWorkflows.put(workflowId, execution);
        
        auditLogger.logSystemEvent("WORKFLOW_START", 
            String.format("Starting workflow: %s [%s]", scenario.getName(), workflowId));
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeWorkflowSteps(execution);
            } catch (Exception e) {
                log.error("Workflow execution failed: {}", e.getMessage(), e);
                execution.setState(WorkflowState.ERROR);
                return new WorkflowResult(workflowId, WorkflowStatus.FAILED, e.getMessage());
            } finally {
                activeWorkflows.remove(workflowId);
            }
        }, workflowExecutor);
    }
    
    private WorkflowResult executeWorkflowSteps(WorkflowExecution execution) {
        execution.setState(WorkflowState.RUNNING);
        List<SimpleAttackResult> allResults = new ArrayList<>();
        
        for (WorkflowStep step : execution.getScenario().getSteps()) {
            execution.setCurrentStep(step);
            execution.setState(WorkflowState.STEP_EXECUTING);
            
            try {
                log.info("Executing step: {} [{}]", step.getName(), execution.getWorkflowId());
                
                CompletableFuture<SimpleAttackResult> stepResult = executeStep(step, execution.getContext());
                SimpleAttackResult result = stepResult.get();
                
                allResults.add(result);
                execution.addResult(result);
                execution.setState(WorkflowState.STEP_COMPLETED);
                
                // Update context with step results using real data
                contextEnhancer.enhanceContext(execution.getContext(), step, result);
                
            } catch (Exception e) {
                log.error("Step failed: {} - {}", step.getName(), e.getMessage());
                execution.setState(WorkflowState.STEP_FAILED);
                
                if (step.isCritical()) {
                    execution.setState(WorkflowState.ERROR);
                    return new WorkflowResult(execution.getWorkflowId(), WorkflowStatus.FAILED, 
                        "Critical step failed: " + step.getName());
                }
            }
        }
        
        execution.setState(WorkflowState.COMPLETED);
        
        // Correlate results from all steps
        CorrelatedFindings findings = correlationEngine.correlateResults(allResults);
        
        auditLogger.logSystemEvent("WORKFLOW_COMPLETE", 
            String.format("Workflow completed: %s with %d results", 
                execution.getWorkflowId(), allResults.size()));
        
        return new WorkflowResult(execution.getWorkflowId(), WorkflowStatus.COMPLETED, 
            allResults, findings);
    }
    
    private CompletableFuture<SimpleAttackResult> executeStep(WorkflowStep step, WorkflowContext context) {
        Map<String, Object> params = buildStepParameters(step, context);
        
        return switch (step.getType()) {
            case "wifi_scan" -> toolAdapter.executeWiFiScan(params);
            case "ssh_bruteforce" -> toolAdapter.executeBruteForce(params);
            case "deploy_keylogger" -> toolAdapter.deployKeylogger(params);
            case "network_analysis" -> toolAdapter.executeNetworkAnalysis(params);
            default -> CompletableFuture.completedFuture(
                new SimpleAttackResult("unknown", "Unknown step type: " + step.getType(), false));
        };
    }
    
    private Map<String, Object> buildStepParameters(WorkflowStep step, WorkflowContext context) {
        // Use enhanced parameter building with real context data
        return contextEnhancer.buildEnhancedParameters(step, context);
    }
    
    // Context enhancement now handled by WorkflowContextEnhancer
    
    public void pauseWorkflow(String workflowId) {
        WorkflowExecution execution = activeWorkflows.get(workflowId);
        if (execution != null) {
            execution.setState(WorkflowState.PAUSED);
            auditLogger.logSystemEvent("WORKFLOW_PAUSE", workflowId);
        }
    }
    
    public void resumeWorkflow(String workflowId) {
        WorkflowExecution execution = activeWorkflows.get(workflowId);
        if (execution != null) {
            execution.setState(WorkflowState.RUNNING);
            auditLogger.logSystemEvent("WORKFLOW_RESUME", workflowId);
        }
    }
    
    public void abortWorkflow(String workflowId) {
        WorkflowExecution execution = activeWorkflows.get(workflowId);
        if (execution != null) {
            execution.setState(WorkflowState.ABORTED);
            activeWorkflows.remove(workflowId);
            auditLogger.logSystemEvent("WORKFLOW_ABORT", workflowId);
        }
    }
    
    public WorkflowStatus getWorkflowStatus(String workflowId) {
        WorkflowExecution execution = activeWorkflows.get(workflowId);
        return execution != null ? WorkflowStatus.fromState(execution.getState()) : WorkflowStatus.NOT_FOUND;
    }
    
    public List<WorkflowStep> getExecutionHistory(String workflowId) {
        WorkflowExecution execution = activeWorkflows.get(workflowId);
        return execution != null ? execution.getCompletedSteps() : Collections.emptyList();
    }
    
    public List<WorkflowExecution> getActiveWorkflows() {
        return new ArrayList<>(activeWorkflows.values());
    }
}