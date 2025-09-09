package com.codexraziel.cybersec.views;

import com.codexraziel.cybersec.services.ResourceCoordinator;
import com.codexraziel.cybersec.services.MemoryManager;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
public class PerformanceView {
    
    @Autowired
    private ResourceCoordinator resourceCoordinator;
    
    @Autowired
    private MemoryManager memoryManager;
    
    private ScheduledExecutorService updateScheduler;
    private XYChart.Series<Number, Number> memorySeries;
    private XYChart.Series<Number, Number> threadSeries;
    private int timeCounter = 0;
    
    public VBox createView(Label statusLabel) {
        VBox performanceContent = new VBox(10);
        
        // Performance metrics grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(10);
        
        // Real-time labels
        Label memoryLabel = new Label("Memory: 0MB (0%)");
        Label threadsLabel = new Label("Threads: 0/20 (0%)");
        Label operationsLabel = new Label("Active Operations: 0");
        Label interfacesLabel = new Label("Available Interfaces: 0");
        
        metricsGrid.add(new Label("STATS System Resources:"), 0, 0, 2, 1);
        metricsGrid.add(memoryLabel, 0, 1);
        metricsGrid.add(threadsLabel, 1, 1);
        metricsGrid.add(operationsLabel, 0, 2);
        metricsGrid.add(interfacesLabel, 1, 2);
        
        // Progress bars
        ProgressBar memoryProgress = new ProgressBar(0);
        ProgressBar threadProgress = new ProgressBar(0);
        memoryProgress.setPrefWidth(200);
        threadProgress.setPrefWidth(200);
        
        metricsGrid.add(memoryProgress, 0, 3);
        metricsGrid.add(threadProgress, 1, 3);
        
        // Control buttons
        HBox controlButtons = new HBox(10);
        Button forceCleanupBtn = new Button("🧹 Force Cleanup");
        Button gcBtn = new Button("Garbage Collect");
        Button refreshBtn = new Button("Refresh");
        
        controlButtons.getChildren().addAll(forceCleanupBtn, gcBtn, refreshBtn);
        
        // Real-time charts
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (seconds)");
        yAxis.setLabel("Usage (%)");
        
        LineChart<Number, Number> performanceChart = new LineChart<>(xAxis, yAxis);
        performanceChart.setTitle("Real-time Performance Monitoring");
        performanceChart.setPrefHeight(300);
        
        memorySeries = new XYChart.Series<>();
        memorySeries.setName("Memory Usage");
        threadSeries = new XYChart.Series<>();
        threadSeries.setName("Thread Usage");
        
        performanceChart.getData().addAll(memorySeries, threadSeries);
        
        // Event handlers
        forceCleanupBtn.setOnAction(e -> {
            memoryManager.forceCleanup();
            statusLabel.setText("🧹 Cleanup completed");
        });
        
        gcBtn.setOnAction(e -> {
            System.gc();
            statusLabel.setText("Garbage collection requested");
        });
        
        refreshBtn.setOnAction(e -> updateMetrics(memoryLabel, threadsLabel, operationsLabel, 
                                                interfacesLabel, memoryProgress, threadProgress));
        
        // Log area for performance events
        TextArea performanceLog = new TextArea();
        performanceLog.setPrefRowCount(8);
        performanceLog.setPromptText("Performance monitoring logs will appear here...");
        performanceLog.setEditable(false);
        
        performanceContent.getChildren().addAll(
            new Label("Performance Monitor"),
            metricsGrid,
            new Separator(),
            new Label("Controls:"),
            controlButtons,
            new Separator(),
            new Label("Real-time Charts:"),
            performanceChart,
            new Separator(),
            new Label("Performance Log:"),
            performanceLog
        );
        
        // Start real-time monitoring
        startRealTimeMonitoring(memoryLabel, threadsLabel, operationsLabel, interfacesLabel,
                              memoryProgress, threadProgress, performanceLog);
        
        return performanceContent;
    }
    
    private void startRealTimeMonitoring(Label memoryLabel, Label threadsLabel, Label operationsLabel,
                                       Label interfacesLabel, ProgressBar memoryProgress, 
                                       ProgressBar threadProgress, TextArea performanceLog) {
        
        updateScheduler = Executors.newScheduledThreadPool(1);
        
        updateScheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                try {
                    updateMetrics(memoryLabel, threadsLabel, operationsLabel, interfacesLabel,
                                memoryProgress, threadProgress);
                    updateCharts();
                    checkPerformanceAlerts(performanceLog);
                } catch (Exception e) {
                    performanceLog.appendText("Error updating metrics: " + e.getMessage() + "\\n");
                }
            });
        }, 0, 2, TimeUnit.SECONDS);
    }
    
    private void updateMetrics(Label memoryLabel, Label threadsLabel, Label operationsLabel,
                             Label interfacesLabel, ProgressBar memoryProgress, ProgressBar threadProgress) {
        
        // Get resource status
        ResourceCoordinator.ResourceStatus resourceStatus = resourceCoordinator.getResourceStatus();
        MemoryManager.MemoryStatus memoryStatus = memoryManager.getMemoryStatus();
        
        // Update labels
        memoryLabel.setText(String.format("Memory: %dMB (%.1f%%)", 
                           memoryStatus.getUsedMemoryMB(), memoryStatus.getUsagePercent()));
        
        threadsLabel.setText(String.format("Threads: %d/%d (%.1f%%)",
                            resourceStatus.getActiveThreads(), resourceStatus.getMaxThreads(),
                            resourceStatus.getThreadUsagePercent()));
        
        operationsLabel.setText("Active Operations: " + resourceStatus.getActiveOperations());
        interfacesLabel.setText("Available Interfaces: " + resourceStatus.getAvailableInterfaces());
        
        // Update progress bars
        memoryProgress.setProgress(memoryStatus.getUsagePercent() / 100.0);
        threadProgress.setProgress(resourceStatus.getThreadUsagePercent() / 100.0);
        
        // Color coding for high usage
        if (memoryStatus.getUsagePercent() > 80) {
            memoryLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            memoryProgress.setStyle("-fx-accent: red;");
        } else if (memoryStatus.getUsagePercent() > 60) {
            memoryLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            memoryProgress.setStyle("-fx-accent: orange;");
        } else {
            memoryLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            memoryProgress.setStyle("-fx-accent: green;");
        }
        
        if (resourceStatus.getThreadUsagePercent() > 80) {
            threadsLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            threadProgress.setStyle("-fx-accent: red;");
        } else if (resourceStatus.getThreadUsagePercent() > 60) {
            threadsLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            threadProgress.setStyle("-fx-accent: orange;");
        } else {
            threadsLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            threadProgress.setStyle("-fx-accent: green;");
        }
    }
    
    private void updateCharts() {
        timeCounter++;
        
        ResourceCoordinator.ResourceStatus resourceStatus = resourceCoordinator.getResourceStatus();
        MemoryManager.MemoryStatus memoryStatus = memoryManager.getMemoryStatus();
        
        // Add new data points
        memorySeries.getData().add(new XYChart.Data<>(timeCounter, memoryStatus.getUsagePercent()));
        threadSeries.getData().add(new XYChart.Data<>(timeCounter, resourceStatus.getThreadUsagePercent()));
        
        // Keep only last 30 data points
        if (memorySeries.getData().size() > 30) {
            memorySeries.getData().remove(0);
        }
        if (threadSeries.getData().size() > 30) {
            threadSeries.getData().remove(0);
        }
    }
    
    private void checkPerformanceAlerts(TextArea performanceLog) {
        ResourceCoordinator.ResourceStatus resourceStatus = resourceCoordinator.getResourceStatus();
        MemoryManager.MemoryStatus memoryStatus = memoryManager.getMemoryStatus();
        
        if (memoryStatus.getUsagePercent() > 90) {
            performanceLog.appendText(String.format("[%s] ALERT CRITICAL: Memory usage at %.1f%%\\n",
                                    java.time.LocalTime.now(), memoryStatus.getUsagePercent()));
        }
        
        if (resourceStatus.getThreadUsagePercent() > 90) {
            performanceLog.appendText(String.format("[%s] WARN WARNING: Thread usage at %.1f%%\\n",
                                    java.time.LocalTime.now(), resourceStatus.getThreadUsagePercent()));
        }
        
        if (resourceStatus.getAvailableInterfaces() == 0) {
            performanceLog.appendText(String.format("[%s] LOCK All network interfaces busy\\n",
                                    java.time.LocalTime.now()));
        }
    }
    
    public void shutdown() {
        if (updateScheduler != null) {
            updateScheduler.shutdown();
        }
    }
}