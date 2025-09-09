package com.research.cybersec;

import com.research.cybersec.views.BruteForceView;
import com.research.cybersec.views.KeyloggerView;
import com.research.cybersec.views.WiFiView;
import com.research.cybersec.views.PerformanceView;
import com.research.cybersec.views.SecurityView;
import com.research.cybersec.views.ToolsView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CyberSecurityApplication extends Application {
    
    private ConfigurableApplicationContext context;
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void init() {
        context = SpringApplication.run(CyberSecurityApplication.class);
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Main layout
        VBox root = new VBox();
        
        // Toolbar
        ToolBar toolbar = new ToolBar();
        Label title = new Label("🔬 Cyber Security Research Suite");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Button settingsBtn = new Button("⚙️ Settings");
        Button aboutBtn = new Button("ℹ️ About");
        
        Region spacer = new Region();
        spacer.setMaxWidth(Double.MAX_VALUE);
        toolbar.getItems().addAll(title, spacer, settingsBtn, aboutBtn);
        
        // Status bar
        ToolBar statusBar = new ToolBar();
        Label statusLabel = new Label("✅ Ready");
        ProgressBar globalProgress = new ProgressBar(0);
        globalProgress.setPrefWidth(150);
        Label versionLabel = new Label("v1.0.0");
        
        Region statusSpacer = new Region();
        statusSpacer.setMaxWidth(Double.MAX_VALUE);
        statusBar.getItems().addAll(statusLabel, statusSpacer, globalProgress, versionLabel);
        
        TabPane tabs = new TabPane();
        
        // Create tabs using views
        Tab keyloggerTab = new Tab("🔑 Keylogger");
        KeyloggerView keyloggerView = context.getBean(KeyloggerView.class);
        keyloggerTab.setContent(keyloggerView.createView(statusLabel));
        
        Tab bruteforceTab = new Tab("🔥 BruteForce");
        BruteForceView bruteForceView = context.getBean(BruteForceView.class);
        bruteforceTab.setContent(bruteForceView.createView(statusLabel));
        
        Tab wifiTab = new Tab("📶 WiFi Pentest");
        WiFiView wifiView = context.getBean(WiFiView.class);
        wifiTab.setContent(wifiView.createView(statusLabel));
        
        Tab performanceTab = new Tab("⚡ Performance");
        PerformanceView performanceView = context.getBean(PerformanceView.class);
        performanceTab.setContent(performanceView.createView(statusLabel));
        
        Tab securityTab = new Tab("🔒 Security");
        SecurityView securityView = context.getBean(SecurityView.class);
        securityTab.setContent(securityView.createView(statusLabel));
        
        Tab toolsTab = new Tab("🔧 Tools");
        ToolsView toolsView = context.getBean(ToolsView.class);
        toolsTab.setContent(toolsView.createView(statusLabel));
        
        // Results Tab (simple for now)
        Tab resultsTab = new Tab("📊 Results");
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
        Button exportCsvBtn = new Button("📊 Export CSV");
        Button exportJsonBtn = new Button("📝 Export JSON");
        Button clearResultsBtn = new Button("🗑️ Clear Results");
        
        exportControls.getChildren().addAll(exportCsvBtn, exportJsonBtn, clearResultsBtn);
        
        resultsContent.getChildren().addAll(
            new Label("📊 Summary:"),
            summaryGrid,
            new Separator(),
            new Label("📋 Detailed Results:"),
            resultsTable,
            new Separator(),
            new Label("📤 Export:"),
            exportControls
        );
        resultsTab.setContent(resultsContent);
        
        tabs.getTabs().addAll(keyloggerTab, bruteforceTab, wifiTab, performanceTab, securityTab, toolsTab, resultsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Main content area
        VBox content = new VBox(10);
        content.getChildren().add(tabs);
        content.setStyle("-fx-padding: 10;");
        
        root.getChildren().addAll(toolbar, content, statusBar);
        
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/cybersec.css").toExternalForm());
        
        primaryStage.setTitle("Codex Raziel v1.0.0");
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