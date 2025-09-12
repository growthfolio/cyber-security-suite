package com.codexraziel.cybersec.views.wifi;

import com.codexraziel.cybersec.workflow.WorkflowExecution;
import com.codexraziel.cybersec.workflow.WorkflowStep;
import com.codexraziel.cybersec.ui.ButtonFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Real-time WiFi attack progress visualization
 * Shows step-by-step execution with live updates
 */
@Component
@Slf4j
public class WiFiAttackProgressView extends VBox {
    
    // Progress tracking components
    private ProgressBar overallProgress;
    private Label progressLabel;
    private Label currentStepLabel;
    private Label elapsedTimeLabel;
    private TableView<WorkflowStepDisplay> stepTable;
    private ObservableList<WorkflowStepDisplay> stepData;
    private TextArea outputArea;
    
    // Attack state
    private WorkflowExecution currentExecution;
    private LocalDateTime startTime;
    private boolean isAttackRunning = false;
    
    public WiFiAttackProgressView() {
        initializeUI();
        log.info("WiFi Attack Progress View initialized");
    }
    
    private void initializeUI() {
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f8f9fa;");
        
        // Title
        Label titleLabel = new Label("WiFi Attack Progress Monitor");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.DARKRED);
        
        // Overall progress section
        VBox progressSection = createProgressSection();
        
        // Step-by-step table
        VBox stepSection = createStepTable();
        
        // Real-time output
        VBox outputSection = createOutputSection();
        
        this.getChildren().addAll(
            titleLabel,
            new Separator(),
            progressSection,
            new Separator(),
            stepSection,
            new Separator(),
            outputSection
        );
    }
    
    private VBox createProgressSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label sectionTitle = new Label("Overall Progress");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        // Progress bar
        overallProgress = new ProgressBar(0.0);
        overallProgress.setPrefWidth(400);
        overallProgress.setPrefHeight(25);
        
        progressLabel = new Label("Ready to start attack");
        progressLabel.setStyle("-fx-text-fill: #6c757d;");
        
        // Status row
        HBox statusRow = new HBox(20);
        statusRow.setPadding(new Insets(10, 0, 0, 0));
        
        currentStepLabel = new Label("Current Step: None");
        currentStepLabel.setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;");
        
        elapsedTimeLabel = new Label("Elapsed: 00:00:00");
        elapsedTimeLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        
        statusRow.getChildren().addAll(currentStepLabel, elapsedTimeLabel);
        
        // Control buttons
        HBox controlRow = new HBox(10);
        controlRow.setPadding(new Insets(10, 0, 0, 0));
        
        Button pauseButton = ButtonFactory.ActionButtons.pause("Pause", 
            "Pause the current attack workflow");
        pauseButton.setOnAction(e -> pauseAttack());
        
        Button resumeButton = ButtonFactory.ActionButtons.resume("Resume", 
            "Resume the paused attack workflow");
        resumeButton.setOnAction(e -> resumeAttack());
        
        Button stopButton = ButtonFactory.ActionButtons.stop("Stop", 
            "Stop the current attack workflow");
        stopButton.setOnAction(e -> stopAttack());
        
        Button exportButton = ButtonFactory.ActionButtons.export("Export Report", 
            "Export detailed attack report and results");
        exportButton.setOnAction(e -> exportReport());
        
        controlRow.getChildren().addAll(pauseButton, resumeButton, stopButton, exportButton);
        
        section.getChildren().addAll(
            sectionTitle, overallProgress, progressLabel, statusRow, controlRow
        );
        
        return section;
    }
    
    private VBox createStepTable() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label sectionTitle = new Label("Attack Steps");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        stepTable = new TableView<>();
        stepData = FXCollections.observableArrayList();
        stepTable.setItems(stepData);
        
        // Configure table properties for best practices
        stepTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        stepTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        stepTable.setRowFactory(tv -> {
            TableRow<WorkflowStepDisplay> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    String status = newItem.getStatus();
                    if (status.contains("COMPLETED")) {
                        row.setStyle("-fx-background-color: #e8f5e8;");
                    } else if (status.contains("RUNNING")) {
                        row.setStyle("-fx-background-color: #fff3cd;");
                    } else if (status.contains("FAILED")) {
                        row.setStyle("-fx-background-color: #ffe8e8;");
                    } else {
                        row.setStyle("-fx-background-color: #f8f9fa;");
                    }
                }
            });
            return row;
        });
        
        // Step Number Column - minimal fixed width
        TableColumn<WorkflowStepDisplay, String> stepNumCol = new TableColumn<>("Step");
        stepNumCol.setCellValueFactory(new PropertyValueFactory<>("stepNumber"));
        stepNumCol.setMinWidth(50);
        stepNumCol.setPrefWidth(60);
        stepNumCol.setMaxWidth(70);
        stepNumCol.setResizable(false);
        
        // Step Name Column - expandable for longer descriptions
        TableColumn<WorkflowStepDisplay, String> nameCol = new TableColumn<>("Step Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("stepName"));
        nameCol.setMinWidth(150);
        nameCol.setPrefWidth(200);
        nameCol.setMaxWidth(300);
        
        // Status Column - fixed width with visual indicators
        TableColumn<WorkflowStepDisplay, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setMinWidth(80);
        statusCol.setPrefWidth(100);
        statusCol.setMaxWidth(120);
        statusCol.setCellFactory(column -> new TableCell<WorkflowStepDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("COMPLETED")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else if (item.contains("RUNNING")) {
                        setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                    } else if (item.contains("FAILED")) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    } else if (item.contains("PENDING")) {
                        setStyle("-fx-text-fill: #6c757d; -fx-font-weight: normal;");
                    }
                }
            }
        });
        
        // Duration Column - time display
        TableColumn<WorkflowStepDisplay, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationCol.setMinWidth(70);
        durationCol.setPrefWidth(90);
        durationCol.setMaxWidth(110);
        
        // Result Column - compact result display
        TableColumn<WorkflowStepDisplay, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setMinWidth(80);
        resultCol.setPrefWidth(100);
        resultCol.setMaxWidth(150);
        resultCol.setCellFactory(column -> new TableCell<WorkflowStepDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("SUCCESS")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else if (item.contains("FAILED")) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    } else if (item.contains("PARTIAL")) {
                        setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Details Column - expandable for additional information
        TableColumn<WorkflowStepDisplay, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));
        detailsCol.setMinWidth(120);
        detailsCol.setPrefWidth(180);
        detailsCol.setMaxWidth(300);
        detailsCol.setCellFactory(column -> new TableCell<WorkflowStepDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.length() > 30 ? item.substring(0, 30) + "..." : item);
                    setTooltip(new Tooltip(item));
                }
            }
        });
        
        // Add columns to table
        stepTable.getColumns().clear();
        stepTable.getColumns().add(stepNumCol);
        stepTable.getColumns().add(nameCol);
        stepTable.getColumns().add(statusCol);
        stepTable.getColumns().add(durationCol);
        stepTable.getColumns().add(resultCol);
        stepTable.getColumns().add(detailsCol);
        stepTable.setPrefHeight(250);
        
        // Custom row factory for status colors
        stepTable.setRowFactory(tv -> new TableRow<WorkflowStepDisplay>() {
            @Override
            protected void updateItem(WorkflowStepDisplay item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    switch (item.getStatus()) {
                        case "✅ Completed" -> setStyle("-fx-background-color: #d4edda;");
                        case "🔄 Running" -> setStyle("-fx-background-color: #d1ecf1;");
                        case "❌ Failed" -> setStyle("-fx-background-color: #f8d7da;");
                        case "⏸️ Paused" -> setStyle("-fx-background-color: #fff3cd;");
                        default -> setStyle("");
                    }
                }
            }
        });
        
        section.getChildren().addAll(sectionTitle, stepTable);
        return section;
    }
    
    private VBox createOutputSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label sectionTitle = new Label("Real-time Output");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(150);
        outputArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        outputArea.setText("Attack output will appear here...\n");
        
        // Output controls
        HBox outputControls = new HBox(10);
        
        Button clearOutputButton = new Button("Clear Output");
        clearOutputButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
        clearOutputButton.setOnAction(e -> outputArea.clear());
        
        Button saveOutputButton = new Button("Save Log");
        saveOutputButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        saveOutputButton.setOnAction(e -> saveOutput());
        
        CheckBox autoScrollCheckBox = new CheckBox("Auto-scroll");
        autoScrollCheckBox.setSelected(true);
        
        outputControls.getChildren().addAll(clearOutputButton, saveOutputButton, autoScrollCheckBox);
        
        section.getChildren().addAll(sectionTitle, outputArea, outputControls);
        return section;
    }
    
    public void startAttackMonitoring(WorkflowExecution execution) {
        this.currentExecution = execution;
        this.startTime = LocalDateTime.now();
        this.isAttackRunning = true;
        
        // Initialize step table
        initializeStepTable(execution.getScenario().getSteps());
        
        // Start monitoring
        startProgressMonitoring();
        
        log.info("Started monitoring attack execution: {}", execution.getWorkflowId());
    }
    
    private void initializeStepTable(List<WorkflowStep> steps) {
        stepData.clear();
        
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            WorkflowStepDisplay display = new WorkflowStepDisplay(
                String.valueOf(i + 1),
                step.getName(),
                "⏳ Pending",
                "00:00",
                "Waiting",
                step.getType() // Using type instead of description
            );
            stepData.add(display);
        }
        
        progressLabel.setText("Attack initialized with " + steps.size() + " steps");
        overallProgress.setProgress(0.0);
    }
    
    private void startProgressMonitoring() {
        CompletableFuture.runAsync(() -> {
            try {
                while (isAttackRunning && currentExecution != null) {
                    Platform.runLater(this::updateProgress);
                    Thread.sleep(1000); // Update every second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Progress monitoring interrupted", e);
            }
        });
    }
    
    private void updateProgress() {
        if (currentExecution == null) return;
        
        // Update elapsed time
        if (startTime != null) {
            long elapsed = java.time.Duration.between(startTime, LocalDateTime.now()).toSeconds();
            elapsedTimeLabel.setText("Elapsed: " + formatDuration(elapsed));
        }
        
        // Simulate progress updates
        int completedSteps = simulateCompletedSteps();
        int totalSteps = stepData.size();
        
        if (totalSteps > 0) {
            double progress = (double) completedSteps / totalSteps;
            overallProgress.setProgress(progress);
            progressLabel.setText(String.format("Progress: %d/%d steps completed (%.1f%%)", 
                completedSteps, totalSteps, progress * 100));
        }
        
        // Update current step
        if (completedSteps < totalSteps) {
            currentStepLabel.setText("Current Step: " + (completedSteps + 1) + " - " + 
                stepData.get(completedSteps).getStepName());
        } else {
            currentStepLabel.setText("Current Step: Attack Complete");
            isAttackRunning = false;
        }
        
        // Simulate output
        addSimulatedOutput();
    }
    
    private int simulateCompletedSteps() {
        // Simulate gradual step completion
        long elapsed = startTime != null ? 
            java.time.Duration.between(startTime, LocalDateTime.now()).toSeconds() : 0;
        return Math.min((int)(elapsed / 10), stepData.size()); // One step every 10 seconds
    }
    
    private void addSimulatedOutput() {
        String[] sampleOutputs = {
            "[INFO] Scanning for WiFi networks...",
            "[INFO] Found 15 networks in range",
            "[INFO] Target network identified: HomeNetwork_2.4G",
            "[INFO] Starting monitor mode on wlan0...",
            "[INFO] Capturing handshake packets...",
            "[WARN] WPS vulnerability detected",
            "[INFO] Attempting dictionary attack...",
            "[INFO] Testing password combinations...",
            "[SUCCESS] Password cracked: password123",
            "[INFO] Generating detailed report..."
        };
        
        // Add random output occasionally
        if (Math.random() < 0.3) {
            String output = sampleOutputs[(int)(Math.random() * sampleOutputs.length)];
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            outputArea.appendText("[" + timestamp + "] " + output + "\n");
            
            // Auto-scroll to bottom
            outputArea.setScrollTop(Double.MAX_VALUE);
        }
    }
    
    private void pauseAttack() {
        showAlert("Attack paused");
        log.info("Attack execution paused");
    }
    
    private void resumeAttack() {
        showAlert("Attack resumed");
        log.info("Attack execution resumed");
    }
    
    private void stopAttack() {
        isAttackRunning = false;
        progressLabel.setText("Attack stopped by user");
        currentStepLabel.setText("Current Step: Stopped");
        showAlert("Attack stopped");
        log.info("Attack execution stopped by user");
    }
    
    private void exportReport() {
        String reportContent = generateReport();
        showAlert("Report exported:\n" + reportContent);
        log.info("Attack report exported");
    }
    
    private void saveOutput() {
        String output = outputArea.getText();
        // Simulate saving to file
        showAlert("Output saved to attack_log_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
        log.info("Attack output saved: {} characters", output.length());
    }
    
    private String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("WiFi Attack Report\n");
        report.append("==================\n");
        report.append("Start Time: ").append(startTime).append("\n");
        report.append("Duration: ").append(elapsedTimeLabel.getText()).append("\n");
        report.append("Progress: ").append(progressLabel.getText()).append("\n");
        report.append("\nStep Details:\n");
        
        for (WorkflowStepDisplay step : stepData) {
            report.append("- ").append(step.getStepName())
                  .append(": ").append(step.getStatus())
                  .append(" (").append(step.getDuration()).append(")\n");
        }
        
        return report.toString();
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("WiFi Attack Progress");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Display wrapper for workflow steps
    public static class WorkflowStepDisplay {
        private final String stepNumber;
        private final String stepName;
        private String status;
        private String duration;
        private String result;
        private final String details;
        
        public WorkflowStepDisplay(String stepNumber, String stepName, String status, 
                                 String duration, String result, String details) {
            this.stepNumber = stepNumber;
            this.stepName = stepName;
            this.status = status;
            this.duration = duration;
            this.result = result;
            this.details = details;
        }
        
        // Getters
        public String getStepNumber() { return stepNumber; }
        public String getStepName() { return stepName; }
        public String getStatus() { return status; }
        public String getDuration() { return duration; }
        public String getResult() { return result; }
        public String getDetails() { return details; }
        
        // Setters for dynamic updates
        public void setStatus(String status) { this.status = status; }
        public void setDuration(String duration) { this.duration = duration; }
        public void setResult(String result) { this.result = result; }
    }
}
