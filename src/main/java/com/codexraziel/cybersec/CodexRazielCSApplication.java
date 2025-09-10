package com.codexraziel.cybersec;

import com.codexraziel.cybersec.views.BruteForceView;
import com.codexraziel.cybersec.views.KeyloggerView;
import com.codexraziel.cybersec.views.WiFiView;
import com.codexraziel.cybersec.views.PerformanceView;
import com.codexraziel.cybersec.views.SecurityView;
import com.codexraziel.cybersec.views.ToolsView;
import com.codexraziel.cybersec.views.WorkflowView;
import com.codexraziel.cybersec.ui.CodexIcons;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

@SpringBootApplication
public class CodexRazielCSApplication extends Application {
    
    private ConfigurableApplicationContext context;
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void init() {
        context = SpringApplication.run(CodexRazielCSApplication.class);
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Main layout
        VBox root = new VBox();
        
        // Toolbar
        ToolBar toolbar = new ToolBar();
        Label title = new Label("Codex Raziel CS");
        title.setGraphic(CodexIcons.SECURITY);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Button settingsBtn = new Button("Settings");
        settingsBtn.setGraphic(FontIcon.of(FontAwesome.COG, 14));
        Button aboutBtn = new Button("About");
        aboutBtn.setGraphic(CodexIcons.INFO);
        
        Region spacer = new Region();
        spacer.setMaxWidth(Double.MAX_VALUE);
        toolbar.getItems().addAll(title, spacer, settingsBtn, aboutBtn);
        
        // Status bar
        ToolBar statusBar = new ToolBar();
        Label statusLabel = new Label("Ready");
        statusLabel.setGraphic(CodexIcons.SUCCESS);
        ProgressBar globalProgress = new ProgressBar(0);
        globalProgress.setPrefWidth(150);
        Label versionLabel = new Label("v1.0.0");
        
        Region statusSpacer = new Region();
        statusSpacer.setMaxWidth(Double.MAX_VALUE);
        statusBar.getItems().addAll(statusLabel, statusSpacer, globalProgress, versionLabel);
        
        TabPane tabs = new TabPane();
        
        // Create tabs using views
        Tab keyloggerTab = new Tab("Keylogger");
        keyloggerTab.setGraphic(CodexIcons.KEYLOGGER);
        KeyloggerView keyloggerView = context.getBean(KeyloggerView.class);
        keyloggerTab.setContent(keyloggerView.createView(statusLabel));
        
        Tab bruteforceTab = new Tab("BruteForce");
        bruteforceTab.setGraphic(CodexIcons.BRUTEFORCE);
        BruteForceView bruteForceView = context.getBean(BruteForceView.class);
        bruteforceTab.setContent(bruteForceView.createView(statusLabel));
        
        Tab wifiTab = new Tab("WiFi Pentest");
        wifiTab.setGraphic(CodexIcons.WIFI);
        WiFiView wifiView = context.getBean(WiFiView.class);
        wifiTab.setContent(wifiView.createView(statusLabel));
        
        Tab performanceTab = new Tab("Performance");
        performanceTab.setGraphic(CodexIcons.PERFORMANCE);
        PerformanceView performanceView = context.getBean(PerformanceView.class);
        performanceTab.setContent(performanceView.createView(statusLabel));
        
        Tab securityTab = new Tab("Security");
        securityTab.setGraphic(CodexIcons.SECURITY);
        SecurityView securityView = context.getBean(SecurityView.class);
        securityTab.setContent(securityView.createView(statusLabel));
        
        Tab toolsTab = new Tab("Tools");
        toolsTab.setGraphic(CodexIcons.TOOLS);
        ToolsView toolsView = context.getBean(ToolsView.class);
        toolsTab.setContent(toolsView.createView(statusLabel));
        
        Tab workflowTab = new Tab("Red Team Workflows");
        workflowTab.setGraphic(CodexIcons.createIcon("PLAY"));
        WorkflowView workflowView = context.getBean(WorkflowView.class);
        workflowView.initialize();
        workflowTab.setContent(workflowView);
        
        // Results Tab (simple for now)
        Tab resultsTab = new Tab("Results");
        resultsTab.setGraphic(CodexIcons.RESULTS);
        VBox resultsContent = new VBox(10);
        
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(20);
        summaryGrid.setVgap(10);
        
        Label totalAttacksLabel = new Label("Total Attacks: 0");
        Label successfulLabel = new Label("Successful: 0");
        Label failedLabel = new Label("Failed: 0");
        Label uptimeLabel = new Label("Uptime: 00:00:00");
        
        summaryGrid.add(totalAttacksLabel, 0, 0);
        summaryGrid.add(successfulLabel, 1, 0);
        summaryGrid.add(failedLabel, 2, 0);
        summaryGrid.add(uptimeLabel, 3, 0);
        
        TableView<String> resultsTable = new TableView<>();
        TableColumn<String, String> timeCol = new TableColumn<>("Time");
        TableColumn<String, String> targetCol = new TableColumn<>("Target");
        TableColumn<String, String> protocolCol = new TableColumn<>("Protocol");
        TableColumn<String, String> statusCol = new TableColumn<>("Status");
        TableColumn<String, String> credentialsCol = new TableColumn<>("Credentials");
        
        resultsTable.getColumns().addAll(timeCol, targetCol, protocolCol, statusCol, credentialsCol);
        resultsTable.setPrefHeight(200);
        
        HBox exportControls = new HBox(10);
        Button exportCsvBtn = new Button("Export CSV");
        exportCsvBtn.setGraphic(CodexIcons.DOWNLOAD);
        Button exportJsonBtn = new Button("Export JSON");
        exportJsonBtn.setGraphic(CodexIcons.DOWNLOAD);
        Button clearResultsBtn = new Button("Clear Results");
        clearResultsBtn.setGraphic(FontIcon.of(FontAwesome.TRASH, 14));
        
        exportControls.getChildren().addAll(exportCsvBtn, exportJsonBtn, clearResultsBtn);
        
        Label summaryLabel = new Label("Summary:");
        summaryLabel.setGraphic(CodexIcons.INFO);
        Label detailedLabel = new Label("Detailed Results:");
        detailedLabel.setGraphic(CodexIcons.RESULTS);
        Label exportLabel = new Label("Export:");
        exportLabel.setGraphic(CodexIcons.DOWNLOAD);
        
        resultsContent.getChildren().addAll(
            summaryLabel,
            summaryGrid,
            new Separator(),
            detailedLabel,
            resultsTable,
            new Separator(),
            exportLabel,
            exportControls
        );
        resultsTab.setContent(resultsContent);
        
        tabs.getTabs().addAll(keyloggerTab, bruteforceTab, wifiTab, workflowTab, performanceTab, securityTab, toolsTab, resultsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Main content area
        VBox content = new VBox(10);
        content.getChildren().add(tabs);
        content.setStyle("-fx-padding: 10;");
        
        root.getChildren().addAll(toolbar, content, statusBar);
        
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/codex-raziel.css").toExternalForm());
        
        primaryStage.setTitle("Codex Raziel CS - Cybersecurity Research Suite v1.0.0");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }
    
    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}