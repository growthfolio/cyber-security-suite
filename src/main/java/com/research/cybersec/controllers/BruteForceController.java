package com.research.cybersec.controllers;

import com.research.cybersec.services.BruteForceGenerator;
import com.research.cybersec.services.BruteForceGenerator.BruteForceConfig;
import com.research.cybersec.services.BruteForceGenerator.BruteForceStats;
import javafx.application.Platform;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

@Controller
@Slf4j
public class BruteForceController {
    
    @Autowired
    private BruteForceGenerator bruteForceGenerator;
    
    // UI Components
    private TextField targetField;
    private TextField portField;
    private ComboBox<String> protocolCombo;
    private TextField usernameField;
    private Spinner<Integer> minLengthSpinner;
    private Spinner<Integer> maxLengthSpinner;
    private CheckBox useNumbers;
    private CheckBox useLowercase;
    private CheckBox useUppercase;
    private CheckBox useSpecialChars;
    private TextField customCharset;
    private Spinner<Integer> threadsSpinner;
    private Spinner<Integer> delaySpinner;
    private TextField maxAttemptsField;
    private Button startBtn;
    private Button stopBtn;
    private ProgressBar attackProgress;
    private TextArea logArea;
    private Label attemptsLabel;
    private Label rateLabel;
    private Label timeLabel;
    private Label currentPasswordLabel;
    private Label progressLabel;
    private Label statusLabel;
    
    public void bindComponents(TextField targetField, TextField portField, 
                              ComboBox<String> protocolCombo, TextField usernameField,
                              Spinner<Integer> minLengthSpinner, Spinner<Integer> maxLengthSpinner,
                              CheckBox useNumbers, CheckBox useLowercase, CheckBox useUppercase, 
                              CheckBox useSpecialChars, TextField customCharset,
                              Spinner<Integer> threadsSpinner, Spinner<Integer> delaySpinner,
                              TextField maxAttemptsField, Button startBtn, Button stopBtn,
                              ProgressBar attackProgress, TextArea logArea,
                              Label attemptsLabel, Label rateLabel, Label timeLabel,
                              Label currentPasswordLabel, Label progressLabel, Label statusLabel) {
        
        this.targetField = targetField;
        this.portField = portField;
        this.protocolCombo = protocolCombo;
        this.usernameField = usernameField;
        this.minLengthSpinner = minLengthSpinner;
        this.maxLengthSpinner = maxLengthSpinner;
        this.useNumbers = useNumbers;
        this.useLowercase = useLowercase;
        this.useUppercase = useUppercase;
        this.useSpecialChars = useSpecialChars;
        this.customCharset = customCharset;
        this.threadsSpinner = threadsSpinner;
        this.delaySpinner = delaySpinner;
        this.maxAttemptsField = maxAttemptsField;
        this.startBtn = startBtn;
        this.stopBtn = stopBtn;
        this.attackProgress = attackProgress;
        this.logArea = logArea;
        this.attemptsLabel = attemptsLabel;
        this.rateLabel = rateLabel;
        this.timeLabel = timeLabel;
        this.currentPasswordLabel = currentPasswordLabel;
        this.progressLabel = progressLabel;
        this.statusLabel = statusLabel;
        
        setupEventHandlers();
    }
    
    private void setupEventHandlers() {
        startBtn.setOnAction(e -> startBruteForce());
        stopBtn.setOnAction(e -> stopBruteForce());
    }
    
    public void calculateCombinations() {
        try {
            BruteForceConfig config = buildConfig();
            
            StringBuilder charset = new StringBuilder();
            if (config.isUseNumbers()) charset.append("0123456789");
            if (config.isUseLowercase()) charset.append("abcdefghijklmnopqrstuvwxyz");
            if (config.isUseUppercase()) charset.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            if (config.isUseSpecialChars()) charset.append("!@#$%^&*()_+-=[]{}|;:,.<>?");
            
            if (!config.getCustomCharset().isEmpty()) {
                charset = new StringBuilder(config.getCustomCharset());
            }
            
            long total = 0;
            for (int len = config.getMinLength(); len <= config.getMaxLength(); len++) {
                total += Math.pow(charset.length(), len);
            }
            
            logArea.appendText("[📊] CALCULATION RESULTS:\n");
            logArea.appendText("    • Charset size: " + charset.length() + " characters\n");
            logArea.appendText("    • Length range: " + config.getMinLength() + "-" + config.getMaxLength() + "\n");
            logArea.appendText("    • Total combinations: " + String.format("%,d", total) + "\n");
            logArea.appendText("    • Estimated time (1000/sec): " + formatTime(total / 1000) + "\n\n");
            
        } catch (Exception ex) {
            logArea.appendText("[❌] Calculation error: " + ex.getMessage() + "\n");
        }
    }
    
    private void startBruteForce() {
        BruteForceConfig config = buildConfig();
        String target = targetField.getText() + ":" + portField.getText();
        
        logArea.appendText("[🚀] STARTING BRUTE FORCE ATTACK\n");
        logArea.appendText("[🎯] Target: " + target + "\n");
        logArea.appendText("[🔧] Protocol: " + protocolCombo.getValue() + "\n");
        logArea.appendText("[👤] Username: " + usernameField.getText() + "\n");
        
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        attackProgress.setProgress(0);
        statusLabel.setText("⚔️ Brute Force Running");
        
        // Use the real service
        CompletableFuture<BruteForceStats> future = bruteForceGenerator.startBruteForce(
            config, 
            target,
            this::onLogMessage,
            this::onStatsUpdate
        );
        
        future.thenAccept(this::onAttackComplete)
               .exceptionally(this::onAttackError);
    }
    
    private void stopBruteForce() {
        bruteForceGenerator.stopBruteForce();
        
        logArea.appendText("[⛔] STOPPING BRUTE FORCE ATTACK...\n");
        logArea.appendText("[✅] Attack stopped by user\n\n");
        
        resetUI();
    }
    
    private void onLogMessage(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }
    
    private void onStatsUpdate(BruteForceStats stats) {
        Platform.runLater(() -> {
            attemptsLabel.setText("Attempts: " + String.format("%,d", stats.getCurrentAttempt()) + 
                                " / " + String.format("%,d", stats.getTotalCombinations()));
            rateLabel.setText("Rate: " + stats.getAttemptsPerSecond() + "/sec");
            timeLabel.setText("Elapsed: " + formatTime(stats.getElapsedTimeMs() / 1000));
            currentPasswordLabel.setText("Current: " + stats.getCurrentPassword());
            progressLabel.setText("Progress: " + String.format("%.2f", stats.getProgressPercent()) + "%");
            attackProgress.setProgress(stats.getProgressPercent() / 100.0);
        });
    }
    
    private void onAttackComplete(BruteForceStats stats) {
        Platform.runLater(() -> {
            if (stats != null && stats.isFound()) {
                logArea.appendText("\n[🎉] SUCCESS! PASSWORD FOUND: " + stats.getFoundPassword() + "\n");
            } else {
                logArea.appendText("\n[❌] Password not found in search space\n");
            }
            
            if (stats != null) {
                logArea.appendText("[📊] Total attempts: " + String.format("%,d", stats.getCurrentAttempt()) + "\n");
            }
            
            resetUI();
        });
    }
    
    private Void onAttackError(Throwable throwable) {
        Platform.runLater(() -> {
            logArea.appendText("[❌] Error: " + throwable.getMessage() + "\n");
            resetUI();
        });
        return null;
    }
    
    private void resetUI() {
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        attackProgress.setProgress(0);
        statusLabel.setText("✅ Ready");
        
        attemptsLabel.setText("Attempts: 0 / 0");
        rateLabel.setText("Rate: 0/sec");
        timeLabel.setText("Elapsed: 00:00:00");
        currentPasswordLabel.setText("Current: --");
        progressLabel.setText("Progress: 0.00%");
    }
    
    private BruteForceConfig buildConfig() {
        BruteForceConfig config = new BruteForceConfig();
        config.setMinLength(minLengthSpinner.getValue());
        config.setMaxLength(maxLengthSpinner.getValue());
        config.setUseNumbers(useNumbers.isSelected());
        config.setUseLowercase(useLowercase.isSelected());
        config.setUseUppercase(useUppercase.isSelected());
        config.setUseSpecialChars(useSpecialChars.isSelected());
        config.setCustomCharset(customCharset.getText());
        config.setThreadsCount(threadsSpinner.getValue());
        config.setDelayMs(delaySpinner.getValue());
        config.setMaxAttempts(Long.parseLong(maxAttemptsField.getText()));
        return config;
    }
    
    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}