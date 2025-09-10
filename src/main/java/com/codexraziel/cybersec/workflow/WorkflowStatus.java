package com.codexraziel.cybersec.workflow;

public enum WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    PAUSED,
    ABORTED,
    NOT_FOUND;
    
    public static WorkflowStatus fromState(WorkflowState state) {
        return switch (state) {
            case INITIALIZED -> PENDING;
            case RUNNING, STEP_EXECUTING, STEP_COMPLETED -> RUNNING;
            case COMPLETED -> COMPLETED;
            case ERROR, STEP_FAILED -> FAILED;
            case PAUSED -> PAUSED;
            case ABORTED -> ABORTED;
        };
    }
}