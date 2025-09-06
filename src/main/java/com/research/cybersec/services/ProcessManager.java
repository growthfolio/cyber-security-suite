package com.research.cybersec.services;

import com.research.cybersec.models.AttackConfig;
import com.research.cybersec.models.AttackResult;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
@Slf4j
public class ProcessManager {
    private final StringProperty status = new SimpleStringProperty("Ready");
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    
    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    @Autowired
    private ConfigManager configManager;
    
    public void startBruteForce(AttackConfig config, Consumer<String> outputHandler, Consumer<AttackResult> resultHandler) {
        if (running.get()) return;
        
        running.set(true);
        status.set("Starting bruteforce attack...");
        progress.set(0.0);
        
        executor.submit(() -> {
            try {
                String toolPath = configManager.getBruteForcePath();
                ProcessBuilder pb = new ProcessBuilder(
                    toolPath,
                    config.getProtocol(),
                    "--target", config.getTarget(),
                    "--profile", config.getProfile(),
                    "--threads", String.valueOf(config.getThreads())
                );
                
                Process process = pb.start();
                processes.put("bruteforce", process);
                
                monitorProcess(process, outputHandler, resultHandler);
                
            } catch (Exception e) {
                log.error("BruteForce process failed", e);
                status.set("Attack failed: " + e.getMessage());
            } finally {
                running.set(false);
                processes.remove("bruteforce");
                status.set("Attack completed");
            }
        });
    }
    
    public void startKeylogger(String platform, Consumer<String> outputHandler) {
        if (running.get()) return;
        
        running.set(true);
        status.set("Starting keylogger...");
        
        executor.submit(() -> {
            try {
                String toolPath = configManager.getKeyloggerPath();
                ProcessBuilder pb = new ProcessBuilder(toolPath);
                
                Process process = pb.start();
                processes.put("keylogger", process);
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                        outputHandler.accept(line);
                    }
                }
                
            } catch (Exception e) {
                log.error("Keylogger process failed", e);
                status.set("Keylogger failed: " + e.getMessage());
            } finally {
                running.set(false);
                processes.remove("keylogger");
                status.set("Keylogger stopped");
            }
        });
    }
    
    private void monitorProcess(Process process, Consumer<String> outputHandler, Consumer<AttackResult> resultHandler) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                outputHandler.accept(line);
                
                if (line.contains("SUCCESS:") || line.contains("[+]")) {
                    AttackResult result = parseAttackResult(line);
                    if (result != null) {
                        resultHandler.accept(result);
                    }
                }
                
                updateProgress(line);
            }
        } catch (IOException e) {
            log.error("Error monitoring process output", e);
        }
    }
    
    private AttackResult parseAttackResult(String line) {
        try {
            // Parse: "SUCCESS: admin:password@192.168.1.100:22"
            if (line.contains("SUCCESS:")) {
                String[] parts = line.split(":");
                String credentials = parts[1].trim();
                String[] credParts = credentials.split("@");
                String[] userPass = credParts[0].split(":");
                
                return AttackResult.builder()
                    .target(credParts[1])
                    .username(userPass[0])
                    .password(userPass[1])
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
        } catch (Exception e) {
            log.warn("Failed to parse result: {}", line, e);
        }
        return null;
    }
    
    private void updateProgress(String line) {
        // Throttle progress updates to reduce CPU usage
        if (line.contains("Trying") || line.contains("[*]")) {
            double currentProgress = progress.get();
            if (currentProgress < 0.95) {
                // Update progress less frequently
                if (Math.random() < 0.1) { // Only 10% of the time
                    progress.set(currentProgress + 0.05);
                }
            }
        }
    }
    
    public void stopAll() {
        processes.values().forEach(process -> {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        });
        processes.clear();
        running.set(false);
        status.set("All processes stopped");
    }
    
    // Property getters for UI binding
    public StringProperty statusProperty() { return status; }
    public DoubleProperty progressProperty() { return progress; }
    public BooleanProperty runningProperty() { return running; }
}