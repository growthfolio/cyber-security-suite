package com.codexraziel.cybersec.views.wifi;

import com.codexraziel.cybersec.workflow.AttackScenario;
import com.codexraziel.cybersec.workflow.WorkflowManager;
import com.codexraziel.cybersec.workflow.WorkflowConfig;
import com.codexraziel.cybersec.wifi.examples.WiFiWorkflowExamples;
import com.codexraziel.cybersec.ui.ButtonFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Main WiFi workflow management panel for the UI
 * Provides interface for executing and monitoring WiFi attack workflows
 */
@Component
@Slf4j
public class WiFiWorkflowPanel extends VBox {
    
    @Autowired
    private WorkflowManager workflowManager;
    
    @Autowired
    private WiFiWorkflowExamples wifiWorkflowExamples;
    
    // UI Components
    private ComboBox<String> interfaceSelector;
    private TextField targetSSIDField;
    private TextField targetBSSIDField;
    private ComboBox<AttackScenario> workflowSelector;
    private TextArea logArea;
    private TableView<WorkflowExecutionInfo> executionTable;
    private ObservableList<WorkflowExecutionInfo> executionData;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button startButton;
    private Button stopButton;
    
    public WiFiWorkflowPanel() {
        initializeUI();
        setupEventHandlers();
        log.info("WiFi Workflow Panel initialized");
    }
    
    private void initializeUI() {
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f8f9fa;");
        
        // Title
        Label titleLabel = new Label("WiFi Security Testing Workflows");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        // Configuration Section
        VBox configSection = createConfigurationSection();
        
        // Control Section
        HBox controlSection = createControlSection();
        
        // Progress Section
        VBox progressSection = createProgressSection();
        
        // Execution History Table
        VBox tableSection = createTableSection();
        
        // Log Section
        VBox logSection = createLogSection();
        
        this.getChildren().addAll(
            titleLabel,
            new Separator(),
            configSection,
            new Separator(),
            controlSection,
            progressSection,
            new Separator(),
            tableSection,
            new Separator(),
            logSection
        );
    }
    
    private VBox createConfigurationSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label sectionTitle = new Label("Attack Configuration");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        // Interface selection
        HBox interfaceBox = new HBox(10);
        interfaceBox.setAlignment(Pos.CENTER_LEFT);
        Label interfaceLabel = new Label("Interface:");
        interfaceLabel.setMinWidth(100);
        interfaceSelector = new ComboBox<>();
        interfaceSelector.getItems().addAll("wlan0", "wlan1", "wlan0mon", "wlan1mon");
        interfaceSelector.setValue("wlan0");
        interfaceSelector.setPrefWidth(150);
        
        Button refreshInterfacesBtn = ButtonFactory.ActionButtons.refresh("Refresh", 
            "Refresh available network interfaces");
        refreshInterfacesBtn.setOnAction(e -> refreshWiFiInterfaces());
        
        interfaceBox.getChildren().addAll(interfaceLabel, interfaceSelector, refreshInterfacesBtn);
        
        // Target configuration
        HBox targetBox = new HBox(10);
        targetBox.setAlignment(Pos.CENTER_LEFT);
        Label ssidLabel = new Label("Target SSID:");
        ssidLabel.setMinWidth(100);
        targetSSIDField = new TextField();
        targetSSIDField.setPromptText("e.g., TargetNetwork");
        targetSSIDField.setPrefWidth(150);
        
        Label bssidLabel = new Label("BSSID:");
        bssidLabel.setMinWidth(60);
        targetBSSIDField = new TextField();
        targetBSSIDField.setPromptText("AA:BB:CC:DD:EE:FF");
        targetBSSIDField.setPrefWidth(150);
        
        targetBox.getChildren().addAll(ssidLabel, targetSSIDField, bssidLabel, targetBSSIDField);
        
        // Workflow selection
        HBox workflowBox = new HBox(10);
        workflowBox.setAlignment(Pos.CENTER_LEFT);
        Label workflowLabel = new Label("Workflow:");
        workflowLabel.setMinWidth(100);
        workflowSelector = new ComboBox<>();
        workflowSelector.getItems().addAll(
            AttackScenario.WIFI_PASSWORD_ATTACK,
            AttackScenario.ADVANCED_WIFI_PENETRATION,
            AttackScenario.WIFI_RED_TEAM
        );
        workflowSelector.setValue(AttackScenario.WIFI_PASSWORD_ATTACK);
        workflowSelector.setPrefWidth(200);
        
        workflowBox.getChildren().addAll(workflowLabel, workflowSelector);
        
        section.getChildren().addAll(sectionTitle, interfaceBox, targetBox, workflowBox);
        return section;
    }
    
    private HBox createControlSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setPadding(new Insets(10));
        
        startButton = ButtonFactory.WiFiButtons.attack("Start Attack", 
            "Begin the selected WiFi penetration testing workflow");
        startButton.setPrefWidth(120);
        
        stopButton = ButtonFactory.ActionButtons.stop("Stop Attack", 
            "Stop the currently running attack workflow");
        stopButton.setPrefWidth(120);
        stopButton.setDisable(true);
        
        Button clearLogsBtn = ButtonFactory.ActionButtons.clear("Clear Logs", 
            "Clear the attack log display");
        clearLogsBtn.setOnAction(e -> logArea.clear());
        
        Button exportResultsBtn = ButtonFactory.ActionButtons.export("Export Results", 
            "Export attack results and logs to file");
        exportResultsBtn.setOnAction(e -> exportResults());
        
        section.getChildren().addAll(startButton, stopButton, clearLogsBtn, exportResultsBtn);
        return section;
    }
    
    private VBox createProgressSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label progressTitle = new Label("Attack Progress");
        progressTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(20);
        
        statusLabel = new Label("Ready to start attack");
        statusLabel.setStyle("-fx-text-fill: #6c757d;");
        
        section.getChildren().addAll(progressTitle, progressBar, statusLabel);
        return section;
    }
    
    private VBox createTableSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label tableTitle = new Label("Execution History");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        executionTable = new TableView<>();
        executionData = FXCollections.observableArrayList();
        executionTable.setItems(executionData);
        
        // Configure table properties for best practices
        executionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        executionTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        executionTable.setRowFactory(tv -> {
            TableRow<WorkflowExecutionInfo> row = new TableRow<>();
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
        
        // Time Column - fixed width for consistent time display
        TableColumn<WorkflowExecutionInfo, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        timeCol.setMinWidth(100);
        timeCol.setPrefWidth(120);
        timeCol.setMaxWidth(140);
        
        // Workflow Column - expandable for workflow names
        TableColumn<WorkflowExecutionInfo, String> workflowCol = new TableColumn<>("Workflow");
        workflowCol.setCellValueFactory(new PropertyValueFactory<>("workflowName"));
        workflowCol.setMinWidth(120);
        workflowCol.setPrefWidth(180);
        workflowCol.setMaxWidth(250);
        
        // Target Column - medium width for SSID/BSSID
        TableColumn<WorkflowExecutionInfo, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(new PropertyValueFactory<>("target"));
        targetCol.setMinWidth(100);
        targetCol.setPrefWidth(150);
        targetCol.setMaxWidth(200);
        
        // Status Column - compact with visual indicators
        TableColumn<WorkflowExecutionInfo, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setMinWidth(80);
        statusCol.setPrefWidth(100);
        statusCol.setMaxWidth(120);
        statusCol.setCellFactory(column -> new TableCell<WorkflowExecutionInfo, String>() {
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
        
        // Result Column - expandable for detailed results
        TableColumn<WorkflowExecutionInfo, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setMinWidth(120);
        resultCol.setPrefWidth(200);
        resultCol.setMaxWidth(300);
        resultCol.setCellFactory(column -> new TableCell<WorkflowExecutionInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    // Truncate long text and add tooltip
                    setText(item.length() > 40 ? item.substring(0, 40) + "..." : item);
                    setTooltip(new Tooltip(item));
                    
                    // Color coding based on result type
                    if (item.contains("SUCCESS") || item.contains("PASSWORD FOUND")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else if (item.contains("FAILED") || item.contains("NO PASSWORD")) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    } else if (item.contains("PARTIAL")) {
                        setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Add columns to table
        executionTable.getColumns().clear();
        executionTable.getColumns().add(timeCol);
        executionTable.getColumns().add(workflowCol);
        executionTable.getColumns().add(targetCol);
        executionTable.getColumns().add(statusCol);
        executionTable.getColumns().add(resultCol);
        
        executionTable.setPrefHeight(150);
        executionTable.setMaxHeight(250);
        
        // Context menu for history actions
        ContextMenu contextMenu = new ContextMenu();
        MenuItem viewDetailsItem = new MenuItem("View Details");
        MenuItem retryItem = new MenuItem("Retry Workflow");
        MenuItem exportItem = new MenuItem("Export Results");
        MenuItem deleteItem = new MenuItem("Delete Entry");
        
        viewDetailsItem.setOnAction(e -> {
            WorkflowExecutionInfo selected = executionTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showExecutionDetails(selected);
            }
        });
        retryItem.setOnAction(e -> {
            WorkflowExecutionInfo selected = executionTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                retryExecution(selected);
            }
        });
        exportItem.setOnAction(e -> exportResults());
        deleteItem.setOnAction(e -> {
            WorkflowExecutionInfo selected = executionTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteExecution(selected);
            }
        });
        
        contextMenu.getItems().addAll(viewDetailsItem, retryItem, exportItem, deleteItem);
        executionTable.setContextMenu(contextMenu);
        
        section.getChildren().addAll(tableTitle, executionTable);
        return section;
    }
    
    private VBox createLogSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label logTitle = new Label("Attack Logs");
        logTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");
        
        section.getChildren().addAll(logTitle, logArea);
        return section;
    }
    
    private void setupEventHandlers() {
        startButton.setOnAction(e -> startAttack());
        stopButton.setOnAction(e -> stopAttack());
        
        // Auto-fill BSSID when SSID changes (simulation)
        targetSSIDField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && targetBSSIDField.getText().isEmpty()) {
                // Simulate BSSID lookup
                targetBSSIDField.setText("AA:BB:CC:DD:EE:FF");
            }
        });
    }
    
    private void startAttack() {
        if (!validateConfiguration()) {
            return;
        }
        
        String interfaceName = interfaceSelector.getValue();
        String targetSSID = targetSSIDField.getText().trim();
        String targetBSSID = targetBSSIDField.getText().trim();
        AttackScenario selectedWorkflow = workflowSelector.getValue();
        
        // Update UI state
        startButton.setDisable(true);
        stopButton.setDisable(false);
        progressBar.setProgress(0);
        updateStatus("Initializing attack...", Color.BLUE);
        
        // Log attack start
        appendLog(String.format("[%s] Starting %s against %s (%s)", 
            getCurrentTime(), selectedWorkflow.getName(), targetSSID, targetBSSID));
        
        // Add to execution table
        WorkflowExecutionInfo execution = new WorkflowExecutionInfo(
            getCurrentTime(), selectedWorkflow.getName(), targetSSID, "Running", "In progress..."
        );
        executionData.add(execution);
        
        // Execute workflow asynchronously
        CompletableFuture<String> attackFuture;
        
        switch (selectedWorkflow) {
            case WIFI_PASSWORD_ATTACK:
                attackFuture = wifiWorkflowExamples.executeBasicWiFiAttack(interfaceName, targetSSID, targetBSSID);
                break;
            case ADVANCED_WIFI_PENETRATION:
                attackFuture = wifiWorkflowExamples.executeAdvancedWiFiPentest(interfaceName);
                break;
            case WIFI_RED_TEAM:
                attackFuture = wifiWorkflowExamples.executeWiFiRedTeamEngagement(interfaceName, targetSSID);
                break;
            default:
                attackFuture = CompletableFuture.completedFuture("Unknown workflow");
        }
        
        // Simulate progress updates
        simulateProgress();
        
        // Handle completion
        attackFuture.whenComplete((result, throwable) -> {
            Platform.runLater(() -> {
                if (throwable != null) {
                    handleAttackFailure(execution, throwable);
                } else {
                    handleAttackSuccess(execution, result);
                }
                resetUIState();
            });
        });
    }
    
    private void stopAttack() {
        updateStatus("Stopping attack...", Color.ORANGE);
        appendLog("[" + getCurrentTime() + "] Attack stopped by user");
        
        // In a real implementation, this would cancel the running workflow
        resetUIState();
        updateStatus("Attack stopped", Color.RED);
    }
    
    private boolean validateConfiguration() {
        if (interfaceSelector.getValue() == null || interfaceSelector.getValue().isEmpty()) {
            showAlert("Please select a network interface");
            return false;
        }
        
        if (targetSSIDField.getText().trim().isEmpty()) {
            showAlert("Please enter a target SSID");
            return false;
        }
        
        if (workflowSelector.getValue() == null) {
            showAlert("Please select a workflow");
            return false;
        }
        
        return true;
    }
    
    private void simulateProgress() {
        // Simulate attack progress for demonstration
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    Thread.sleep(2000); // 2 seconds per step
                    final int progress = i;
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress / 10.0);
                        updateStatus(getProgressMessage(progress), Color.BLUE);
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private String getProgressMessage(int step) {
        return switch (step) {
            case 1 -> "Setting up monitor mode...";
            case 2 -> "Scanning for networks...";
            case 3 -> "Analyzing targets...";
            case 4 -> "Capturing handshake...";
            case 5 -> "Preparing attack vectors...";
            case 6 -> "Loading wordlists...";
            case 7 -> "Executing dictionary attack...";
            case 8 -> "Testing password combinations...";
            case 9 -> "Analyzing results...";
            case 10 -> "Finalizing attack...";
            default -> "Processing...";
        };
    }
    
    private void handleAttackSuccess(WorkflowExecutionInfo execution, String result) {
        execution.setStatus("Completed");
        execution.setResult(result);
        updateStatus("Attack completed successfully!", Color.GREEN);
        appendLog("[" + getCurrentTime() + "] Attack completed: " + result);
        progressBar.setProgress(1.0);
    }
    
    private void handleAttackFailure(WorkflowExecutionInfo execution, Throwable error) {
        execution.setStatus("Failed");
        execution.setResult("Error: " + error.getMessage());
        updateStatus("Attack failed: " + error.getMessage(), Color.RED);
        appendLog("[" + getCurrentTime() + "] Attack failed: " + error.getMessage());
        progressBar.setProgress(0);
    }
    
    private void resetUIState() {
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }
    
    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }
    
    private void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    private void refreshWiFiInterfaces() {
        // Simulate interface detection
        appendLog("[" + getCurrentTime() + "] Refreshing network interfaces...");
        
        // In a real implementation, this would call WiFiInterfaceDetector
        interfaceSelector.getItems().clear();
        interfaceSelector.getItems().addAll("wlan0", "wlan1", "wlan0mon", "wlan1mon", "wlp2s0");
        
        appendLog("[" + getCurrentTime() + "] Found " + interfaceSelector.getItems().size() + " interfaces");
    }
    
    private void exportResults() {
        // Simulate result export
        appendLog("[" + getCurrentTime() + "] Exporting results to file...");
        
        // In a real implementation, this would generate reports
        showAlert("Results exported to /tmp/wifi_attack_results.html");
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("WiFi Attack Tool");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showExecutionDetails(WorkflowExecutionInfo execution) {
        log.info("Showing details for execution: {}", execution.getWorkflowName());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Execution Details");
        alert.setHeaderText("Workflow: " + execution.getWorkflowName());
        alert.setContentText(
            "Start Time: " + execution.getStartTime() + "\n" +
            "Target: " + execution.getTarget() + "\n" +
            "Status: " + execution.getStatus() + "\n" +
            "Result: " + execution.getResult()
        );
        alert.showAndWait();
    }
    
    private void retryExecution(WorkflowExecutionInfo execution) {
        log.info("Retrying execution: {}", execution.getWorkflowName());
        updateStatus("Retrying workflow execution...", Color.BLUE);
        // TODO: Implement retry logic
    }
    
    private void deleteExecution(WorkflowExecutionInfo execution) {
        log.info("Deleting execution: {}", execution.getWorkflowName());
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Execution");
        confirmation.setHeaderText("Delete Execution History");
        confirmation.setContentText("Are you sure you want to delete this execution record?");
        
        if (confirmation.showAndWait().get() == ButtonType.OK) {
            executionData.remove(execution);
            updateStatus("Execution record deleted", Color.GREEN);
        }
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    // Inner class for table data
    public static class WorkflowExecutionInfo {
        private String startTime;
        private String workflowName;
        private String target;
        private String status;
        private String result;
        
        public WorkflowExecutionInfo(String startTime, String workflowName, String target, String status, String result) {
            this.startTime = startTime;
            this.workflowName = workflowName;
            this.target = target;
            this.status = status;
            this.result = result;
        }
        
        // Getters and setters
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        
        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
        
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}
