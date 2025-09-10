package com.codexraziel.cybersec.views;

import com.codexraziel.cybersec.controllers.WorkflowController;
import com.codexraziel.cybersec.ui.CodexIcons;
import com.codexraziel.cybersec.workflow.AttackScenario;
import com.codexraziel.cybersec.workflow.WorkflowExecution;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkflowView extends VBox {
    
    @Autowired
    private WorkflowController workflowController;
    
    private ComboBox<AttackScenario> scenarioSelector;
    private ProgressBar workflowProgress;
    private Label statusLabel;
    private Label currentStepLabel;
    private TextArea logOutput;
    private Button startButton;
    private Button pauseButton;
    private Button stopButton;
    private TableView<WorkflowExecution> workflowTable;
    
    public void initialize() {
        setupUI();
        bindProperties();
        setupEventHandlers();
    }
    
    private void setupUI() {
        setSpacing(10);
        setPadding(new Insets(10));
        
        // Header
        Label titleLabel = new Label("Real Red Team Workflow Orchestration");
        titleLabel.getStyleClass().add("section-title");
        
        Label warningLabel = new Label("⚠️ REAL TOOLS - Use only in authorized environments");
        warningLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
        
        // Scenario selection
        HBox scenarioBox = new HBox(10);
        scenarioBox.getChildren().addAll(
            new Label("Attack Scenario:"),
            scenarioSelector = new ComboBox<>(),
            startButton = new Button("Start Workflow"),
            pauseButton = new Button("Pause"),
            stopButton = new Button("Stop")
        );
        
        scenarioSelector.getItems().addAll(AttackScenario.values());
        scenarioSelector.setValue(AttackScenario.NETWORK_RECONNAISSANCE);
        
        startButton.setGraphic(CodexIcons.createIcon("PLAY"));
        pauseButton.setGraphic(CodexIcons.createIcon("PAUSE"));
        stopButton.setGraphic(CodexIcons.createIcon("STOP"));
        
        // Progress section
        VBox progressBox = new VBox(5);
        progressBox.getChildren().addAll(
            statusLabel = new Label("Ready"),
            workflowProgress = new ProgressBar(),
            currentStepLabel = new Label("")
        );
        
        workflowProgress.setPrefWidth(Double.MAX_VALUE);
        statusLabel.getStyleClass().add("status-label");
        
        // Active workflows table
        workflowTable = new TableView<>();
        setupWorkflowTable();
        
        // Log output
        logOutput = new TextArea();
        logOutput.setEditable(false);
        logOutput.setPrefRowCount(10);
        logOutput.getStyleClass().add("console-output");
        
        // Layout
        getChildren().addAll(
            titleLabel,
            warningLabel,
            new Separator(),
            scenarioBox,
            progressBox,
            new Label("Active Real Workflows:"),
            workflowTable,
            new Label("Real Tool Execution Log:"),
            logOutput
        );
    }
    
    private void setupWorkflowTable() {
        TableColumn<WorkflowExecution, String> idColumn = new TableColumn<>("Workflow ID");
        idColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getWorkflowId()));
        
        TableColumn<WorkflowExecution, String> scenarioColumn = new TableColumn<>("Scenario");
        scenarioColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getScenario().getName()));
        
        TableColumn<WorkflowExecution, String> stateColumn = new TableColumn<>("State");
        stateColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getState().toString()));
        
        TableColumn<WorkflowExecution, String> startTimeColumn = new TableColumn<>("Start Time");
        startTimeColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getStartTime().toString()));
        
        workflowTable.getColumns().addAll(idColumn, scenarioColumn, stateColumn, startTimeColumn);
        workflowTable.setItems(workflowController.getActiveWorkflows());
    }
    
    private void bindProperties() {
        statusLabel.textProperty().bind(workflowController.statusProperty());
        workflowProgress.progressProperty().bind(workflowController.progressProperty());
        currentStepLabel.textProperty().bind(workflowController.currentStepProperty());
        
        startButton.disableProperty().bind(workflowController.runningProperty());
        pauseButton.disableProperty().bind(workflowController.runningProperty().not());
        stopButton.disableProperty().bind(workflowController.runningProperty().not());
    }
    
    private void setupEventHandlers() {
        startButton.setOnAction(e -> {
            AttackScenario selectedScenario = scenarioSelector.getValue();
            if (selectedScenario != null) {
                workflowController.startWorkflow(selectedScenario);
                logOutput.appendText("Starting workflow: " + selectedScenario.getName() + "\n");
            }
        });
        
        pauseButton.setOnAction(e -> {
            workflowController.pauseCurrentWorkflow();
            logOutput.appendText("Workflow paused\n");
        });
        
        stopButton.setOnAction(e -> {
            workflowController.abortCurrentWorkflow();
            logOutput.appendText("Workflow stopped\n");
        });
    }
}