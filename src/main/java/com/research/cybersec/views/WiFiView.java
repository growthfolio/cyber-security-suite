package com.research.cybersec.views;

import com.research.cybersec.controllers.WiFiController;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WiFiView {
    
    @Autowired
    private WiFiController controller;
    
    public VBox createView(Label statusLabel) {
        VBox wifiContent = new VBox(10);
        
        // Scan controls with loading state
        HBox scanControls = new HBox(10);
        Button scanBtn = new Button("🔍 Scan Networks");
        Button refreshBtn = new Button("🔄 Refresh");
        Button cancelBtn = new Button("⛔ Cancel");
        ComboBox<String> interfaceCombo = new ComboBox<>();
        interfaceCombo.getItems().addAll("wlan0", "wlan1", "wlp2s0");
        interfaceCombo.setValue("wlan0");
        
        // Loading indicator
        ProgressIndicator scanProgress = new ProgressIndicator();
        scanProgress.setPrefSize(20, 20);
        scanProgress.setVisible(false);
        
        Label scanStatus = new Label("✅ Ready to scan");
        scanStatus.setStyle("-fx-font-weight: bold;");
        
        cancelBtn.setVisible(false);
        
        scanControls.getChildren().addAll(
            new Label("Interface:"), interfaceCombo,
            scanBtn, refreshBtn, cancelBtn,
            scanProgress, scanStatus
        );
        
        // Networks table with status column
        TableView<String[]> networksTable = new TableView<>();
        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        TableColumn<String[], String> ssidCol = new TableColumn<>("SSID");
        TableColumn<String[], String> bssidCol = new TableColumn<>("BSSID");
        TableColumn<String[], String> securityCol = new TableColumn<>("Security");
        TableColumn<String[], String> signalCol = new TableColumn<>("Signal");
        TableColumn<String[], String> channelCol = new TableColumn<>("Channel");
        
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        ssidCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        bssidCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        securityCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));
        signalCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[4]));
        channelCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[5]));
        
        // Style status column with colors
        statusCol.setCellFactory(column -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("🔴")) setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    else if (item.contains("🟡")) setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    else if (item.contains("🟢")) setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    else if (item.contains("📡")) setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                }
            }
        });
        
        // Auto-resize columns with status
        statusCol.prefWidthProperty().bind(networksTable.widthProperty().multiply(0.15));
        ssidCol.prefWidthProperty().bind(networksTable.widthProperty().multiply(0.25));
        bssidCol.prefWidthProperty().bind(networksTable.widthProperty().multiply(0.2));
        securityCol.prefWidthProperty().bind(networksTable.widthProperty().multiply(0.15));
        signalCol.prefWidthProperty().bind(networksTable.widthProperty().multiply(0.15));
        channelCol.prefWidthProperty().bind(networksTable.widthProperty().multiply(0.1));
        
        networksTable.getColumns().addAll(statusCol, ssidCol, bssidCol, securityCol, signalCol, channelCol);
        networksTable.setPrefHeight(150);
        networksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Attack configuration
        GridPane attackConfig = new GridPane();
        attackConfig.setHgap(10);
        attackConfig.setVgap(5);
        
        TextField targetSSID = new TextField();
        targetSSID.setPromptText("Select network from table above");
        ComboBox<String> attackType = new ComboBox<>();
        attackType.getItems().addAll("Dictionary Attack", "Brute Force", "WPS PIN");
        attackType.setValue("Dictionary Attack");
        
        TextField wordlistPath = new TextField("./wordlists/common_passwords.txt");
        Button browseWordlist = new Button("📁 Browse");
        
        attackConfig.add(new Label("Target SSID:"), 0, 0);
        attackConfig.add(targetSSID, 1, 0);
        attackConfig.add(new Label("Attack Type:"), 0, 1);
        attackConfig.add(attackType, 1, 1);
        attackConfig.add(new Label("Wordlist:"), 0, 2);
        attackConfig.add(wordlistPath, 1, 2);
        attackConfig.add(browseWordlist, 2, 2);
        
        // Attack controls
        HBox wifiAttackControls = new HBox(10);
        Button startWifiAttack = new Button("⚔️ Start Attack");
        Button stopWifiAttack = new Button("⛔ Stop Attack");
        Button captureHandshake = new Button("🤝 Capture Handshake");
        ProgressBar wifiProgress = new ProgressBar(0);
        wifiProgress.setPrefWidth(150);
        
        wifiAttackControls.getChildren().addAll(startWifiAttack, stopWifiAttack, captureHandshake, wifiProgress);
        
        // Profile controls
        HBox profileControls = new HBox(10);
        Button saveProfileBtn = new Button("💾 Save Profile");
        Button loadProfileBtn = new Button("📁 Load Profile");
        Button exportBtn = new Button("📤 Export Results");
        
        profileControls.getChildren().addAll(saveProfileBtn, loadProfileBtn, exportBtn);
        
        // WiFi attack log
        TextArea wifiLogArea = new TextArea();
        wifiLogArea.setPrefRowCount(10);
        wifiLogArea.setPromptText("WiFi pentest results will appear here...");
        
        // Bind components to controller with loading states
        controller.bindComponents(interfaceCombo, networksTable, targetSSID, attackType, wordlistPath,
                                startWifiAttack, stopWifiAttack, wifiProgress, wifiLogArea, statusLabel,
                                scanProgress, scanStatus, cancelBtn);
        
        // Additional event handlers
        scanBtn.setOnAction(e -> controller.scanNetworks());
        refreshBtn.setOnAction(e -> controller.scanNetworks());
        
        // Table selection handler (adjusted for status column)
        networksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                targetSSID.setText(newSelection[1]); // SSID is now at index 1
            }
        });
        
        // Keyboard shortcuts
        wifiContent.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case R -> controller.scanNetworks();
                    case S -> controller.saveResults();
                    case E -> controller.exportResults();
                    case T -> controller.analyzeTarget();
                    case P -> controller.saveProfile();
                    case L -> controller.loadProfile();
                }
            }
        });
        wifiContent.setFocusTraversable(true);
        captureHandshake.setOnAction(e -> controller.captureHandshake());
        
        // Profile event handlers
        saveProfileBtn.setOnAction(e -> controller.saveProfile());
        loadProfileBtn.setOnAction(e -> controller.loadProfile());
        exportBtn.setOnAction(e -> controller.exportResults());
        
        stopWifiAttack.setDisable(true);
        
        wifiContent.getChildren().addAll(
            new Label("🔍 Network Discovery:"),
            scanControls,
            new Label("📶 Available Networks:"),
            networksTable,
            new Separator(),
            new Label("⚙️ Attack Configuration:"),
            attackConfig,
            new Separator(),
            new Label("⚔️ Attack Controls:"),
            wifiAttackControls,
            new Separator(),
            new Label("💾 Profile & Export:"),
            profileControls,
            new Separator(),
            new Label("📝 Attack Log:"),
            wifiLogArea
        );
        
        return wifiContent;
    }
}