package com.codexraziel.cybersec.views;

import com.codexraziel.cybersec.services.ToolValidator;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ToolsView {
    
    @Autowired
    private ToolValidator toolValidator;
    
    private ScheduledExecutorService updateScheduler;
    
    public VBox createView(Label statusLabel) {
        VBox toolsContent = new VBox(10);
        
        // Tools status grid
        GridPane toolsGrid = new GridPane();
        toolsGrid.setHgap(20);
        toolsGrid.setVgap(10);
        
        // Control buttons
        HBox toolsControls = new HBox(10);
        Button checkDepsBtn = new Button("Check Dependencies");
        Button refreshBtn = new Button("Refresh");
        Button installGuideBtn = new Button("Installation Guide");
        
        toolsControls.getChildren().addAll(checkDepsBtn, refreshBtn, installGuideBtn);
        
        // Tools status area
        TextArea toolsStatusArea = new TextArea();
        toolsStatusArea.setPrefRowCount(15);
        toolsStatusArea.setPromptText("Tool validation results will appear here...");
        toolsStatusArea.setEditable(false);
        
        // Summary labels
        Label summaryLabel = new Label("System Status: Checking...");
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Event handlers
        checkDepsBtn.setOnAction(e -> {
            checkAllDependencies(toolsStatusArea, summaryLabel, statusLabel);
        });
        
        refreshBtn.setOnAction(e -> {
            checkAllDependencies(toolsStatusArea, summaryLabel, statusLabel);
        });
        
        installGuideBtn.setOnAction(e -> {
            showInstallationGuide(toolsStatusArea);
        });
        
        toolsContent.getChildren().addAll(
            new Label("Tools & Dependencies"),
            summaryLabel,
            new Separator(),
            new Label("Controls:"),
            toolsControls,
            new Separator(),
            new Label("Dependency Status:"),
            toolsStatusArea
        );
        
        // Initial check
        Platform.runLater(() -> checkAllDependencies(toolsStatusArea, summaryLabel, statusLabel));
        
        return toolsContent;
    }
    
    private void checkAllDependencies(TextArea toolsStatusArea, Label summaryLabel, Label statusLabel) {
        toolsStatusArea.clear();
        toolsStatusArea.appendText("Checking system dependencies...\n\n");
        
        // Run validation in background
        CompletableFuture.supplyAsync(() -> toolValidator.checkDependencies())
            .thenAccept(report -> {
                Platform.runLater(() -> {
                    displayValidationReport(report, toolsStatusArea, summaryLabel, statusLabel);
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    toolsStatusArea.appendText("Dependency check failed: " + throwable.getMessage() + "\n");
                    summaryLabel.setText("Check Failed");
                    summaryLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                });
                return null;
            });
    }
    
    private void displayValidationReport(ToolValidator.ValidationReport report, 
                                       TextArea toolsStatusArea, Label summaryLabel, Label statusLabel) {
        
        toolsStatusArea.clear();
        
        // Summary
        toolsStatusArea.appendText("=== DEPENDENCY VALIDATION REPORT ===\n\n");
        toolsStatusArea.appendText("Summary: " + report.getSummary() + "\n");
        toolsStatusArea.appendText("Available Tools: " + report.getAvailableCount() + "/" + report.getTotalCount() + "\n\n");
        
        // Critical issues
        if (!report.getCriticalIssues().isEmpty()) {
            toolsStatusArea.appendText("🚨 CRITICAL ISSUES:\n");
            for (String issue : report.getCriticalIssues()) {
                toolsStatusArea.appendText("  • " + issue + "\n");
            }
            toolsStatusArea.appendText("\n");
        }
        
        // Warnings
        if (!report.getWarnings().isEmpty()) {
            toolsStatusArea.appendText("⚠️ WARNINGS:\n");
            for (String warning : report.getWarnings()) {
                toolsStatusArea.appendText("  • " + warning + "\n");
            }
            toolsStatusArea.appendText("\n");
        }
        
        // Tool details
        toolsStatusArea.appendText("=== TOOL STATUS ===\n\n");
        toolsStatusArea.appendText(String.format("%-15s | %-8s | %-12s | %-15s | %s\n", 
            "Tool", "Status", "Version", "Required", "Install Command"));
        toolsStatusArea.appendText("-".repeat(80) + "\n");
        
        for (ToolValidator.ToolStatus status : report.getToolStatuses()) {
            toolsStatusArea.appendText(String.format("%-15s | %-8s | %-12s | %-15s | %s\n",
                status.getToolInfo().getName(),
                status.getStatusIcon() + " " + status.getStatusText(),
                status.getVersion() != null ? status.getVersion() : "N/A",
                status.getToolInfo().isRequired() ? "Yes" : "No",
                status.getInstallCommand()
            ));
        }
        
        // Installation suggestions
        if (report.getAvailableCount() < report.getTotalCount()) {
            toolsStatusArea.appendText("\n=== INSTALLATION SUGGESTIONS ===\n\n");
            
            for (ToolValidator.ToolStatus status : report.getToolStatuses()) {
                if (!status.isAvailable()) {
                    toolsStatusArea.appendText("📦 " + status.getToolInfo().getName() + 
                        " (" + status.getToolInfo().getDescription() + "):\n");
                    toolsStatusArea.appendText("   " + status.getInstallCommand() + "\n\n");
                }
            }
        }
        
        // Update summary label
        summaryLabel.setText(report.getSummary());
        if (report.isFullyOperational()) {
            summaryLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            statusLabel.setText("All tools available");
        } else if (report.getCriticalIssues().isEmpty()) {
            summaryLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            statusLabel.setText("Some tools missing");
        } else {
            summaryLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            statusLabel.setText("Critical tools missing");
        }
    }
    
    private void showInstallationGuide(TextArea toolsStatusArea) {
        toolsStatusArea.clear();
        toolsStatusArea.appendText("=== INSTALLATION GUIDE ===\n\n");
        
        toolsStatusArea.appendText("🐧 UBUNTU/DEBIAN:\n");
        toolsStatusArea.appendText("sudo apt update\n");
        toolsStatusArea.appendText("sudo apt install network-manager wireless-tools\n");
        toolsStatusArea.appendText("sudo apt install aircrack-ng hydra nmap john hashcat\n");
        toolsStatusArea.appendText("sudo apt install android-tools-adb\n\n");
        
        toolsStatusArea.appendText("🎩 FEDORA/RHEL/CENTOS:\n");
        toolsStatusArea.appendText("sudo dnf install NetworkManager wireless-tools\n");
        toolsStatusArea.appendText("sudo dnf install aircrack-ng hydra nmap john hashcat\n");
        toolsStatusArea.appendText("sudo dnf install android-tools\n\n");
        
        toolsStatusArea.appendText("🏹 ARCH LINUX:\n");
        toolsStatusArea.appendText("sudo pacman -S networkmanager wireless_tools\n");
        toolsStatusArea.appendText("sudo pacman -S aircrack-ng hydra nmap john hashcat\n");
        toolsStatusArea.appendText("sudo pacman -S android-tools\n\n");
        
        toolsStatusArea.appendText("🍎 MACOS (with Homebrew):\n");
        toolsStatusArea.appendText("brew install aircrack-ng hydra nmap john hashcat\n");
        toolsStatusArea.appendText("brew install android-platform-tools\n\n");
        
        toolsStatusArea.appendText("⚠️ IMPORTANT NOTES:\n");
        toolsStatusArea.appendText("• Some tools may require additional configuration\n");
        toolsStatusArea.appendText("• Root/sudo access may be needed for network operations\n");
        toolsStatusArea.appendText("• Check your distribution's package manager for exact names\n");
        toolsStatusArea.appendText("• Some tools may not be available in default repositories\n\n");
        
        toolsStatusArea.appendText("🔧 TROUBLESHOOTING:\n");
        toolsStatusArea.appendText("• If tools are not found, check your PATH environment\n");
        toolsStatusArea.appendText("• Some distributions package tools with different names\n");
        toolsStatusArea.appendText("• Consider using snap or flatpak for missing packages\n");
        toolsStatusArea.appendText("• Compile from source if packages are unavailable\n");
    }
    
    public void shutdown() {
        if (updateScheduler != null) {
            updateScheduler.shutdown();
        }
    }
}