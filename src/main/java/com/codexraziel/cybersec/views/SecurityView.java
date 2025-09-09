package com.codexraziel.cybersec.views;

import com.codexraziel.cybersec.security.PermissionManager;
import com.codexraziel.cybersec.security.AuditLogger;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class SecurityView {
    
    @Autowired
    private PermissionManager permissionManager;
    
    @Autowired
    private AuditLogger auditLogger;
    
    private ScheduledExecutorService updateScheduler;
    
    public VBox createView(Label statusLabel) {
        VBox securityContent = new VBox(10);
        
        // Security status grid
        GridPane statusGrid = new GridPane();
        statusGrid.setHgap(20);
        statusGrid.setVgap(10);
        
        // Status labels
        Label networkStatusLabel = new Label("Network Access: Checking...");
        Label wifiStatusLabel = new Label("WiFi Access: Checking...");
        Label sudoStatusLabel = new Label("Sudo Access: Checking...");
        Label toolsStatusLabel = new Label("Tools Available: Checking...");
        
        statusGrid.add(new Label("Security Status:"), 0, 0, 2, 1);
        statusGrid.add(networkStatusLabel, 0, 1);
        statusGrid.add(wifiStatusLabel, 1, 1);
        statusGrid.add(sudoStatusLabel, 0, 2);
        statusGrid.add(toolsStatusLabel, 1, 2);
        
        // Control buttons
        HBox securityControls = new HBox(10);
        Button checkPermissionsBtn = new Button("SCAN Check Permissions");
        Button viewAuditLogBtn = new Button("LIST View Audit Log");
        Button clearAuditBtn = new Button("TRASH Clear Audit");
        
        securityControls.getChildren().addAll(checkPermissionsBtn, viewAuditLogBtn, clearAuditBtn);
        
        // Permission details
        TextArea permissionDetails = new TextArea();
        permissionDetails.setPrefRowCount(6);
        permissionDetails.setPromptText("Permission check details will appear here...");
        permissionDetails.setEditable(false);
        
        // Audit log viewer
        TextArea auditLogViewer = new TextArea();
        auditLogViewer.setPrefRowCount(12);
        auditLogViewer.setPromptText("Security audit events will appear here...");
        auditLogViewer.setEditable(false);
        
        // Event handlers
        checkPermissionsBtn.setOnAction(e -> {
            auditLogger.logUserAction("PERMISSION_CHECK", "Manual check", "Requested");
            checkAllPermissions(networkStatusLabel, wifiStatusLabel, sudoStatusLabel, 
                              toolsStatusLabel, permissionDetails, statusLabel);
        });
        
        viewAuditLogBtn.setOnAction(e -> {
            auditLogger.logUserAction("VIEW_AUDIT_LOG", "Security audit", "Accessed");
            loadAuditLog(auditLogViewer);
        });
        
        clearAuditBtn.setOnAction(e -> {
            auditLogger.logUserAction("CLEAR_AUDIT_REQUEST", "Security audit", "Clear requested");
            showClearAuditConfirmation(auditLogViewer);
        });
        
        securityContent.getChildren().addAll(
            new Label("Security Dashboard"),
            statusGrid,
            new Separator(),
            new Label("PANEL Security Controls:"),
            securityControls,
            new Separator(),
            new Label("LIST Permission Details:"),
            permissionDetails,
            new Separator(),
            new Label("LOG Security Audit Log:"),
            auditLogViewer
        );
        
        // Start automatic security monitoring
        startSecurityMonitoring(networkStatusLabel, wifiStatusLabel, sudoStatusLabel, 
                               toolsStatusLabel, auditLogViewer);
        
        // Initial permission check
        checkAllPermissions(networkStatusLabel, wifiStatusLabel, sudoStatusLabel, 
                          toolsStatusLabel, permissionDetails, statusLabel);
        
        return securityContent;
    }
    
    private void startSecurityMonitoring(Label networkStatusLabel, Label wifiStatusLabel,
                                       Label sudoStatusLabel, Label toolsStatusLabel,
                                       TextArea auditLogViewer) {
        
        updateScheduler = Executors.newScheduledThreadPool(1);
        
        // Update security status every 30 seconds
        updateScheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                updateSecurityStatus(networkStatusLabel, wifiStatusLabel, sudoStatusLabel, toolsStatusLabel);
                loadRecentAuditEvents(auditLogViewer);
            });
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private void checkAllPermissions(Label networkStatusLabel, Label wifiStatusLabel,
                                   Label sudoStatusLabel, Label toolsStatusLabel,
                                   TextArea permissionDetails, Label statusLabel) {
        
        permissionDetails.clear();
        permissionDetails.appendText("SCAN Checking system permissions...\\n\\n");
        
        // Check network permissions
        PermissionManager.PermissionCheckResult networkResult = permissionManager.checkNetworkPermissions();
        updateStatusLabel(networkStatusLabel, "Network Access", networkResult);
        permissionDetails.appendText("Network Access: " + networkResult.getMessage() + "\\n");
        if (networkResult.getSuggestion() != null) {
            permissionDetails.appendText("  → " + networkResult.getSuggestion() + "\\n");
        }
        
        // Check WiFi permissions
        PermissionManager.PermissionCheckResult wifiResult = permissionManager.checkWiFiPermissions();
        updateStatusLabel(wifiStatusLabel, "WiFi Access", wifiResult);
        permissionDetails.appendText("WiFi Access: " + wifiResult.getMessage() + "\\n");
        if (wifiResult.getSuggestion() != null) {
            permissionDetails.appendText("  → " + wifiResult.getSuggestion() + "\\n");
        }
        
        // Check sudo permissions
        PermissionManager.PermissionCheckResult sudoResult = permissionManager.checkSudoPermissions();
        updateStatusLabel(sudoStatusLabel, "Sudo Access", sudoResult);
        permissionDetails.appendText("Sudo Access: " + sudoResult.getMessage() + "\\n");
        if (sudoResult.getSuggestion() != null) {
            permissionDetails.appendText("  → " + sudoResult.getSuggestion() + "\\n");
        }
        
        // Check tool availability
        String[] tools = {"nmcli", "iwlist", "aircrack-ng", "hydra", "nmap"};
        int availableTools = 0;
        permissionDetails.appendText("\\nTool Availability:\\n");
        
        for (String tool : tools) {
            PermissionManager.PermissionCheckResult toolResult = permissionManager.checkToolAvailability(tool);
            if (toolResult.isSuccess()) {
                availableTools++;
                permissionDetails.appendText("  OK " + tool + " - Available\\n");
            } else {
                permissionDetails.appendText("  ERROR " + tool + " - " + toolResult.getMessage() + "\\n");
                if (toolResult.getSuggestion() != null) {
                    permissionDetails.appendText("     → " + toolResult.getSuggestion() + "\\n");
                }
            }
        }
        
        toolsStatusLabel.setText(String.format("Tools Available: %d/%d", availableTools, tools.length));
        if (availableTools >= tools.length * 0.7) {
            toolsStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else if (availableTools >= tools.length * 0.4) {
            toolsStatusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            toolsStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
        
        // Update overall status
        PermissionManager.SystemPermissionStatus systemStatus = permissionManager.getSystemPermissionStatus();
        statusLabel.setText(systemStatus.getStatusSummary());
        
        permissionDetails.appendText("\\n" + systemStatus.getStatusSummary());
    }
    
    private void updateStatusLabel(Label label, String prefix, PermissionManager.PermissionCheckResult result) {
        label.setText(prefix + ": " + (result.isSuccess() ? "OK Available" : 
                     result.isWarning() ? "WARN Limited" : "ERROR Unavailable"));
        
        if (result.isSuccess()) {
            label.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else if (result.isWarning()) {
            label.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            label.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }
    
    private void updateSecurityStatus(Label networkStatusLabel, Label wifiStatusLabel,
                                    Label sudoStatusLabel, Label toolsStatusLabel) {
        // Quick status update without detailed output
        PermissionManager.SystemPermissionStatus status = permissionManager.getSystemPermissionStatus();
        
        networkStatusLabel.setText("Network Access: " + (status.isNetworkAccess() ? "OK Available" : "ERROR Unavailable"));
        wifiStatusLabel.setText("WiFi Access: " + (status.isWifiAccess() ? "OK Available" : "ERROR Unavailable"));
        sudoStatusLabel.setText("Sudo Access: " + (status.isSudoAccess() ? "OK Available" : "WARN Limited"));
        toolsStatusLabel.setText(String.format("Tools Available: %d", status.getToolsAvailable()));
    }
    
    private void loadAuditLog(TextArea auditLogViewer) {
        auditLogViewer.clear();
        auditLogViewer.appendText("LIST Security Audit Log\\n");
        auditLogViewer.appendText("===================\\n\\n");
        
        try {
            java.nio.file.Path auditPath = java.nio.file.Paths.get("logs", "security_audit.log");
            if (java.nio.file.Files.exists(auditPath)) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(auditPath);
                
                // Show last 50 lines
                int startIndex = Math.max(0, lines.size() - 50);
                for (int i = startIndex; i < lines.size(); i++) {
                    auditLogViewer.appendText(lines.get(i) + "\\n");
                }
                
                if (lines.size() > 50) {
                    auditLogViewer.appendText("\\n... (showing last 50 entries)\\n");
                }
            } else {
                auditLogViewer.appendText("No audit log file found.\\n");
            }
        } catch (Exception e) {
            auditLogViewer.appendText("Error loading audit log: " + e.getMessage() + "\\n");
        }
    }
    
    private void loadRecentAuditEvents(TextArea auditLogViewer) {
        // Load only recent events for real-time monitoring
        // Implementation would tail the audit log file
    }
    
    private void showClearAuditConfirmation(TextArea auditLogViewer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Audit Log");
        alert.setHeaderText("Clear Security Audit Log");
        alert.setContentText("Are you sure you want to clear the security audit log?\\n\\n" +
                           "This action cannot be undone and will remove all security event history.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    java.nio.file.Path auditPath = java.nio.file.Paths.get("logs", "security_audit.log");
                    if (java.nio.file.Files.exists(auditPath)) {
                        java.nio.file.Files.delete(auditPath);
                        auditLogger.logUserAction("AUDIT_LOG_CLEARED", "Security audit", "Log cleared by user");
                        auditLogViewer.clear();
                        auditLogViewer.appendText("OK Audit log cleared successfully.\\n");
                    }
                } catch (Exception e) {
                    auditLogViewer.appendText("ERROR Error clearing audit log: " + e.getMessage() + "\\n");
                }
            }
        });
    }
    
    public void shutdown() {
        if (updateScheduler != null) {
            updateScheduler.shutdown();
        }
    }
}