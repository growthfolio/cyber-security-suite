package com.research.cybersec.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AuditLogger {
    
    private final Path auditLogPath;
    private final ConcurrentLinkedQueue<AuditEvent> eventQueue;
    private final ScheduledExecutorService logWriter;
    private final DateTimeFormatter formatter;
    
    public AuditLogger() {
        this.auditLogPath = Paths.get("logs", "security_audit.log");
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.logWriter = Executors.newSingleThreadScheduledExecutor();
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        
        initializeAuditLog();
        startLogWriter();
    }
    
    private void initializeAuditLog() {
        try {
            Files.createDirectories(auditLogPath.getParent());
            if (!Files.exists(auditLogPath)) {
                Files.createFile(auditLogPath);
                writeLogEntry("AUDIT_LOG_INITIALIZED", "System", "Audit logging started");
            }
        } catch (IOException e) {
            log.error("Failed to initialize audit log", e);
        }
    }
    
    private void startLogWriter() {
        logWriter.scheduleAtFixedRate(() -> {
            try {
                flushEventQueue();
            } catch (Exception e) {
                log.error("Audit log writer error", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    public void logSecurityEvent(String eventType, String details, String context) {
        AuditEvent event = AuditEvent.builder()
            .timestamp(LocalDateTime.now())
            .eventType(eventType)
            .user(System.getProperty("user.name"))
            .details(sanitizeForLog(details))
            .context(sanitizeForLog(context))
            .severity(determineSeverity(eventType))
            .build();
        
        eventQueue.offer(event);
        
        // Log critical events immediately
        if (event.getSeverity() == AuditSeverity.CRITICAL) {
            flushEventQueue();
        }
    }
    
    public void logUserAction(String action, String target, String result) {
        logSecurityEvent("USER_ACTION", 
            String.format("Action: %s, Target: %s, Result: %s", action, target, result),
            "User interaction");
    }
    
    public void logSystemEvent(String event, String details) {
        logSecurityEvent("SYSTEM_EVENT", event, details);
    }
    
    public void logSecurityViolation(String violation, String details, String remoteAddr) {
        logSecurityEvent("SECURITY_VIOLATION",
            String.format("Violation: %s, Details: %s", violation, details),
            String.format("Remote: %s", remoteAddr != null ? remoteAddr : "local"));
    }
    
    public void logAuthenticationEvent(String event, String username, boolean success) {
        logSecurityEvent("AUTHENTICATION",
            String.format("Event: %s, User: %s, Success: %s", event, username, success),
            "Authentication attempt");
    }
    
    public void logDataAccess(String resource, String operation, String user) {
        logSecurityEvent("DATA_ACCESS",
            String.format("Resource: %s, Operation: %s", resource, operation),
            String.format("User: %s", user));
    }
    
    private void flushEventQueue() {
        if (eventQueue.isEmpty()) return;
        
        StringBuilder batch = new StringBuilder();
        AuditEvent event;
        
        while ((event = eventQueue.poll()) != null) {
            String logLine = formatAuditEvent(event);
            batch.append(logLine).append("\\n");
            
            // Also log to application log for critical events
            if (event.getSeverity() == AuditSeverity.CRITICAL) {
                log.error("SECURITY AUDIT: {}", logLine);
            } else if (event.getSeverity() == AuditSeverity.HIGH) {
                log.warn("SECURITY AUDIT: {}", logLine);
            }
        }
        
        if (batch.length() > 0) {
            writeToAuditLog(batch.toString());
        }
    }
    
    private String formatAuditEvent(AuditEvent event) {
        return String.format("[%s] [%s] [%s] [%s] %s | Context: %s",
            event.getTimestamp().format(formatter),
            event.getSeverity(),
            event.getEventType(),
            event.getUser(),
            event.getDetails(),
            event.getContext());
    }
    
    private void writeToAuditLog(String content) {
        try {
            Files.write(auditLogPath, content.getBytes(), 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write to audit log", e);
        }
    }
    
    private void writeLogEntry(String eventType, String user, String message) {
        AuditEvent event = AuditEvent.builder()
            .timestamp(LocalDateTime.now())
            .eventType(eventType)
            .user(user)
            .details(message)
            .context("System")
            .severity(AuditSeverity.INFO)
            .build();
        
        writeToAuditLog(formatAuditEvent(event) + "\\n");
    }
    
    private AuditSeverity determineSeverity(String eventType) {
        switch (eventType.toUpperCase()) {
            case "SECURITY_VIOLATION":
            case "AUTHENTICATION_FAILURE":
            case "UNAUTHORIZED_ACCESS":
                return AuditSeverity.CRITICAL;
                
            case "COMMAND_VALIDATION_FAILED":
            case "PERMISSION_DENIED":
            case "COMMAND_TIMEOUT":
                return AuditSeverity.HIGH;
                
            case "USER_ACTION":
            case "COMMAND_EXECUTION":
            case "DATA_ACCESS":
                return AuditSeverity.MEDIUM;
                
            default:
                return AuditSeverity.INFO;
        }
    }
    
    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        
        return input
            .replaceAll("[\\r\\n\\t]", " ")
            .replaceAll("\\s+", " ")
            .trim()
            .substring(0, Math.min(input.length(), 500));
    }
    
    public void shutdown() {
        flushEventQueue();
        logSystemEvent("AUDIT_LOG_SHUTDOWN", "Audit logging stopped");
        
        logWriter.shutdown();
        try {
            if (!logWriter.awaitTermination(5, TimeUnit.SECONDS)) {
                logWriter.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AuditEvent {
        private LocalDateTime timestamp;
        private String eventType;
        private String user;
        private String details;
        private String context;
        private AuditSeverity severity;
    }
    
    public enum AuditSeverity {
        INFO, MEDIUM, HIGH, CRITICAL
    }
}