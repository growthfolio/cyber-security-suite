package com.research.cybersec.views;

import com.research.cybersec.controllers.BruteForceController;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BruteForceView {
    
    @Autowired
    private BruteForceController controller;
    
    public VBox createView(Label statusLabel) {
        VBox bruteforceContent = new VBox(10);
        
        // Target configuration
        GridPane targetGrid = new GridPane();
        targetGrid.setHgap(10);
        targetGrid.setVgap(5);
        
        TextField targetField = new TextField("192.168.1.100");
        TextField portField = new TextField("22");
        ComboBox<String> protocolCombo = new ComboBox<>();
        protocolCombo.getItems().addAll("SSH", "HTTP", "HTTPS", "RDP", "SMB", "WiFi", "Generic");
        protocolCombo.setValue("SSH");
        
        TextField usernameField = new TextField("admin");
        
        targetGrid.add(new Label("Target:"), 0, 0);
        targetGrid.add(targetField, 1, 0);
        targetGrid.add(new Label("Port:"), 2, 0);
        targetGrid.add(portField, 3, 0);
        targetGrid.add(new Label("Protocol:"), 0, 1);
        targetGrid.add(protocolCombo, 1, 1);
        targetGrid.add(new Label("Username:"), 2, 1);
        targetGrid.add(usernameField, 3, 1);
        
        // Password Generation Configuration
        GridPane passwordConfig = new GridPane();
        passwordConfig.setHgap(10);
        passwordConfig.setVgap(5);
        
        Spinner<Integer> minLengthSpinner = new Spinner<>(1, 20, 1);
        Spinner<Integer> maxLengthSpinner = new Spinner<>(1, 20, 8);
        
        CheckBox useNumbers = new CheckBox("Numbers (0-9)");
        CheckBox useLowercase = new CheckBox("Lowercase (a-z)");
        CheckBox useUppercase = new CheckBox("Uppercase (A-Z)");
        CheckBox useSpecialChars = new CheckBox("Special (!@#$%^&*)");
        
        useNumbers.setSelected(true);
        useLowercase.setSelected(true);
        
        TextField customCharset = new TextField();
        customCharset.setPromptText("Custom charset (overrides above)");
        
        Spinner<Integer> threadsSpinner = new Spinner<>(1, 16, 4);
        Spinner<Integer> delaySpinner = new Spinner<>(0, 1000, 0);
        TextField maxAttemptsField = new TextField("1000000");
        
        passwordConfig.add(new Label("Min Length:"), 0, 0);
        passwordConfig.add(minLengthSpinner, 1, 0);
        passwordConfig.add(new Label("Max Length:"), 2, 0);
        passwordConfig.add(maxLengthSpinner, 3, 0);
        
        passwordConfig.add(useNumbers, 0, 1);
        passwordConfig.add(useLowercase, 1, 1);
        passwordConfig.add(useUppercase, 2, 1);
        passwordConfig.add(useSpecialChars, 3, 1);
        
        passwordConfig.add(new Label("Custom Charset:"), 0, 2);
        passwordConfig.add(customCharset, 1, 2, 3, 1);
        
        passwordConfig.add(new Label("Threads:"), 0, 3);
        passwordConfig.add(threadsSpinner, 1, 3);
        passwordConfig.add(new Label("Delay (ms):"), 2, 3);
        passwordConfig.add(delaySpinner, 3, 3);
        
        passwordConfig.add(new Label("Max Attempts:"), 0, 4);
        passwordConfig.add(maxAttemptsField, 1, 4);
        
        // Attack controls
        HBox attackControls = new HBox(10);
        Button startBfBtn = new Button("⚔️ Start Brute Force");
        Button stopBfBtn = new Button("⛔ Stop Attack");
        Button previewBtn = new Button("🔍 Preview Combinations");
        Button calculateBtn = new Button("📊 Calculate Total");
        ProgressBar attackProgress = new ProgressBar(0);
        attackProgress.setPrefWidth(200);
        
        attackControls.getChildren().addAll(startBfBtn, stopBfBtn, previewBtn, calculateBtn, attackProgress);
        
        // Real-time Statistics panel
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(5);
        
        Label attemptsLabel = new Label("Attempts: 0 / 0");
        Label rateLabel = new Label("Rate: 0/sec");
        Label timeLabel = new Label("Elapsed: 00:00:00");
        Label etaLabel = new Label("ETA: --:--:--");
        Label currentPasswordLabel = new Label("Current: --");
        Label progressLabel = new Label("Progress: 0.00%");
        
        statsGrid.add(attemptsLabel, 0, 0);
        statsGrid.add(rateLabel, 1, 0);
        statsGrid.add(timeLabel, 2, 0);
        statsGrid.add(etaLabel, 0, 1);
        statsGrid.add(currentPasswordLabel, 1, 1);
        statsGrid.add(progressLabel, 2, 1);
        
        // Log area
        TextArea bfLogArea = new TextArea();
        bfLogArea.setPrefRowCount(12);
        bfLogArea.setPromptText("Attack results will appear here...");
        
        // Bind components to controller
        controller.bindComponents(
            targetField, portField, protocolCombo, usernameField,
            minLengthSpinner, maxLengthSpinner,
            useNumbers, useLowercase, useUppercase, useSpecialChars, customCharset,
            threadsSpinner, delaySpinner, maxAttemptsField,
            startBfBtn, stopBfBtn, attackProgress, bfLogArea,
            attemptsLabel, rateLabel, timeLabel, currentPasswordLabel, progressLabel, statusLabel
        );
        
        // Setup additional event handlers
        calculateBtn.setOnAction(e -> controller.calculateCombinations());
        
        stopBfBtn.setDisable(true);
        
        bruteforceContent.getChildren().addAll(
            new Label("🎯 Target Configuration:"),
            targetGrid,
            new Separator(),
            new Label("🔐 Password Generation:"),
            passwordConfig,
            new Separator(),
            new Label("⚔️ Attack Controls:"),
            attackControls,
            new Separator(),
            new Label("📊 Real-time Statistics:"),
            statsGrid,
            new Separator(),
            new Label("📝 Attack Log:"),
            bfLogArea
        );
        
        return bruteforceContent;
    }
}