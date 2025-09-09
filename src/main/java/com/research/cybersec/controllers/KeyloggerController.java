package com.research.cybersec.controllers;

import com.research.cybersec.services.ToolExecutor;
import javafx.application.Platform;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

@Controller
@Slf4j
public class KeyloggerController {
    
    @Autowired
    private ToolExecutor toolExecutor;
    
    private ComboBox<String> platformCombo;
    private TextField outputPath;
    private CheckBox stealthMode;
    private CheckBox encryption;
    private Button startBtn;
    private Button stopBtn;
    private ProgressBar keyloggerProgress;
    private TextArea logArea;
    private Label statusLabel;
    
    private CompletableFuture<Void> currentTask;
    
    public void bindComponents(ComboBox<String> platformCombo, TextField outputPath,
                              CheckBox stealthMode, CheckBox encryption,
                              Button startBtn, Button stopBtn,
                              ProgressBar keyloggerProgress, TextArea logArea,
                              Label statusLabel) {
        
        this.platformCombo = platformCombo;
        this.outputPath = outputPath;
        this.stealthMode = stealthMode;
        this.encryption = encryption;
        this.startBtn = startBtn;
        this.stopBtn = stopBtn;
        this.keyloggerProgress = keyloggerProgress;
        this.logArea = logArea;
        this.statusLabel = statusLabel;
        
        setupEventHandlers();
    }
    
    private void setupEventHandlers() {
        startBtn.setOnAction(e -> startKeylogger());
        stopBtn.setOnAction(e -> stopKeylogger());
    }
    
    private void startKeylogger() {
        logArea.appendText("[🚀] Starting optimized keylogger for " + platformCombo.getValue() + "...\n");
        logArea.appendText("[⚙️] Stealth mode: " + (stealthMode.isSelected() ? "ENABLED" : "DISABLED") + "\n");
        logArea.appendText("[🔒] Encryption: " + (encryption.isSelected() ? "ENABLED" : "DISABLED") + "\n");
        logArea.appendText("[📁] Output file: " + outputPath.getText() + "\n");
        
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        keyloggerProgress.setProgress(0.3);
        statusLabel.setText("🔑 Keylogger Starting...");
        
        currentTask = toolExecutor.executeKeylogger(platformCombo.getValue(), output -> {
            Platform.runLater(() -> {
                logArea.appendText(output + "\n");
                keyloggerProgress.setProgress(0.7);
                statusLabel.setText("🔑 Keylogger Running");
            });
        });
    }
    
    private void stopKeylogger() {
        logArea.appendText("[⏹️] Stopping keylogger...\n");
        
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        
        logArea.appendText("[✅] Keylogger stopped successfully\n\n");
        
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        keyloggerProgress.setProgress(0);
        statusLabel.setText("✅ Ready");
    }
    
    public void clearLog() {
        logArea.clear();
        keyloggerProgress.setProgress(0);
    }
}