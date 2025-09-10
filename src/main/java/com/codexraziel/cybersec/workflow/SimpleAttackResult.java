package com.codexraziel.cybersec.workflow;

public class SimpleAttackResult {
    private final String target;
    private final String message;
    private final boolean success;
    private final Object data;
    
    public SimpleAttackResult(String target, String message, boolean success) {
        this.target = target;
        this.message = message;
        this.success = success;
        this.data = null;
    }
    
    public SimpleAttackResult(String target, String message, boolean success, Object data) {
        this.target = target;
        this.message = message;
        this.success = success;
        this.data = data;
    }
    
    public String getTarget() { return target; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
    public Object getData() { return data; }
}