package com.codexraziel.cybersec.workflow;

import com.codexraziel.cybersec.workflow.SimpleAttackResult;
import java.util.List;

public class WorkflowResult {
    private final String workflowId;
    private final WorkflowStatus status;
    private final String message;
    private final List<SimpleAttackResult> results;
    private final CorrelatedFindings correlatedFindings;
    
    public WorkflowResult(String workflowId, WorkflowStatus status, String message) {
        this.workflowId = workflowId;
        this.status = status;
        this.message = message;
        this.results = null;
        this.correlatedFindings = null;
    }
    
    public WorkflowResult(String workflowId, WorkflowStatus status, 
                         List<SimpleAttackResult> results, CorrelatedFindings correlatedFindings) {
        this.workflowId = workflowId;
        this.status = status;
        this.message = "Workflow completed successfully";
        this.results = results;
        this.correlatedFindings = correlatedFindings;
    }
    
    public String getWorkflowId() { return workflowId; }
    public WorkflowStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public List<SimpleAttackResult> getResults() { return results; }
    public CorrelatedFindings getCorrelatedFindings() { return correlatedFindings; }
}