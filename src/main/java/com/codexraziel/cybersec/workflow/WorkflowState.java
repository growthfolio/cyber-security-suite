package com.codexraziel.cybersec.workflow;

public enum WorkflowState {
    INITIALIZED,
    RUNNING,
    STEP_EXECUTING,
    STEP_COMPLETED,
    STEP_FAILED,
    PAUSED,
    COMPLETED,
    ABORTED,
    ERROR
}