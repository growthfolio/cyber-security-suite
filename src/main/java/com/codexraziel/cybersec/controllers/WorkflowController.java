package com.codexraziel.cybersec.controllers;

import com.codexraziel.cybersec.workflow.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

@Controller
@Slf4j
public class WorkflowController {
    
    private final StringProperty status = new SimpleStringProperty("Ready");
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final StringProperty currentStep = new SimpleStringProperty("");
    private final ObservableList<WorkflowExecution> activeWorkflows = FXCollections.observableArrayList();
    
    @Autowired
    private WorkflowManager workflowManager;
    
    private String currentWorkflowId;
    
    public void startWorkflow(AttackScenario scenario) {
        if (running.get()) return;
        
        running.set(true);
        status.set("Starting workflow: " + scenario.getName());
        progress.set(0.0);
        
        WorkflowConfig config = new WorkflowConfig();
        config.setMaxThreads(10);
        config.setTimeoutSeconds(300);
        
        CompletableFuture<WorkflowResult> future = workflowManager.executeWorkflow(scenario, config);
        
        future.thenAccept(result -> {
            running.set(false);
            if (result.getStatus() == WorkflowStatus.COMPLETED) {
                status.set("Workflow completed successfully");
                progress.set(1.0);
                log.info("Workflow {} completed with {} results", 
                    result.getWorkflowId(), result.getResults().size());
            } else {
                status.set("Workflow failed: " + result.getMessage());
                progress.set(0.0);
            }
        }).exceptionally(throwable -> {
            running.set(false);
            status.set("Workflow error: " + throwable.getMessage());
            progress.set(0.0);
            log.error("Workflow execution error", throwable);
            return null;
        });
        
        // Update active workflows list
        refreshActiveWorkflows();
    }
    
    public void pauseCurrentWorkflow() {
        if (currentWorkflowId != null) {
            workflowManager.pauseWorkflow(currentWorkflowId);
            status.set("Workflow paused");
        }
    }
    
    public void resumeCurrentWorkflow() {
        if (currentWorkflowId != null) {
            workflowManager.resumeWorkflow(currentWorkflowId);
            status.set("Workflow resumed");
        }
    }
    
    public void abortCurrentWorkflow() {
        if (currentWorkflowId != null) {
            workflowManager.abortWorkflow(currentWorkflowId);
            running.set(false);
            status.set("Workflow aborted");
            progress.set(0.0);
        }
    }
    
    private void refreshActiveWorkflows() {
        activeWorkflows.setAll(workflowManager.getActiveWorkflows());
    }
    
    // Properties for UI binding
    public StringProperty statusProperty() { return status; }
    public DoubleProperty progressProperty() { return progress; }
    public BooleanProperty runningProperty() { return running; }
    public StringProperty currentStepProperty() { return currentStep; }
    public ObservableList<WorkflowExecution> getActiveWorkflows() { return activeWorkflows; }
}