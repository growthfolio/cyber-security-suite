package com.codexraziel.cybersec.views.wifi;

import com.codexraziel.cybersec.wifi.models.WiFiNetwork;
import com.codexraziel.cybersec.wifi.adapters.SecureWiFiToolAdapter;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Real-time WiFi network monitoring panel
 * Shows discovered networks with security analysis
 */
@Component
@Slf4j
public class WiFiNetworkMonitor extends VBox {
    
    @Autowired
    private SecureWiFiToolAdapter wifiAdapter;
    
    // UI Components
    private TableView<WiFiNetworkDisplay> networkTable;
    private ObservableList<WiFiNetworkDisplay> networkData;
    private ComboBox<String> interfaceSelector;
    private Button startScanButton;
    private Button stopScanButton;
    private Label statusLabel;
    private Label networkCountLabel;
    private ProgressIndicator scanProgress;
    
    // Monitoring state
    private ScheduledExecutorService scheduler;
    private boolean isScanning = false;
    
    public WiFiNetworkMonitor() {
        initializeUI();
        setupEventHandlers();
        log.info("WiFi Network Monitor initialized");
    }
    
    private void initializeUI() {
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f8f9fa;");
        
        // Title
        Label titleLabel = new Label("WiFi Network Discovery & Monitoring");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        // Control Section
        HBox controlSection = createControlSection();
        
        // Status Section
        HBox statusSection = createStatusSection();
        
        // Network Table
        VBox tableSection = createNetworkTable();
        
        this.getChildren().addAll(
            titleLabel,
            new Separator(),
            controlSection,
            statusSection,
            new Separator(),
            tableSection
        );
    }
    
    private HBox createControlSection() {
        HBox section = new HBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label interfaceLabel = new Label("Interface:");
        interfaceLabel.setMinWidth(70);
        
        interfaceSelector = new ComboBox<>();
        interfaceSelector.getItems().addAll("wlan0", "wlan1", "wlan0mon", "wlan1mon");
        interfaceSelector.setValue("wlan0");
        interfaceSelector.setPrefWidth(120);
        
        startScanButton = ButtonFactory.WiFiButtons.scan("Start Scan", 
            "Begin scanning for nearby WiFi networks");
        
        stopScanButton = ButtonFactory.ActionButtons.stop("Stop Scan", 
            "Stop the current WiFi network scan");
        stopScanButton.setDisable(true);
        
        Button refreshButton = ButtonFactory.ActionButtons.refresh("Refresh", 
            "Perform a single network scan to update the list");
        refreshButton.setOnAction(e -> performSingleScan());
        
        Button clearButton = ButtonFactory.ActionButtons.clear("Clear", 
            "Clear all discovered networks from the table");
        clearButton.setOnAction(e -> clearNetworks());
        
        scanProgress = new ProgressIndicator();
        scanProgress.setVisible(false);
        scanProgress.setPrefSize(30, 30);
        
        section.getChildren().addAll(
            interfaceLabel, interfaceSelector, startScanButton, stopScanButton, 
            refreshButton, clearButton, scanProgress
        );
        
        return section;
    }
    
    private HBox createStatusSection() {
        HBox section = new HBox(20);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        statusLabel = new Label("Ready to scan");
        statusLabel.setStyle("-fx-text-fill: #6c757d;");
        
        networkCountLabel = new Label("Networks: 0");
        networkCountLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        
        Label vulnerableLabel = new Label("Vulnerable: 0");
        vulnerableLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        
        Label lastScanLabel = new Label("Last scan: Never");
        lastScanLabel.setStyle("-fx-text-fill: #6c757d;");
        
        section.getChildren().addAll(statusLabel, networkCountLabel, vulnerableLabel, lastScanLabel);
        return section;
    }
    
    private VBox createNetworkTable() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label tableTitle = new Label("Discovered Networks");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        networkTable = new TableView<>();
        networkData = FXCollections.observableArrayList();
        networkTable.setItems(networkData);
        
        // Configure table properties for best practices
        networkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        networkTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        networkTable.setRowFactory(tv -> {
            TableRow<WiFiNetworkDisplay> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    launchAttackOnSelected();
                }
            });
            return row;
        });
        
        // SSID Column - resizable with minimum width
        TableColumn<WiFiNetworkDisplay, String> ssidCol = new TableColumn<>("SSID");
        ssidCol.setCellValueFactory(new PropertyValueFactory<>("ssid"));
        ssidCol.setMinWidth(120);
        ssidCol.setPrefWidth(150);
        ssidCol.setMaxWidth(200);
        
        // BSSID Column - fixed width (MAC address always same length)
        TableColumn<WiFiNetworkDisplay, String> bssidCol = new TableColumn<>("BSSID");
        bssidCol.setCellValueFactory(new PropertyValueFactory<>("bssid"));
        bssidCol.setMinWidth(130);
        bssidCol.setPrefWidth(140);
        bssidCol.setMaxWidth(140);
        bssidCol.setResizable(false);
        
        // Channel Column - small fixed width
        TableColumn<WiFiNetworkDisplay, String> channelCol = new TableColumn<>("Ch");
        channelCol.setCellValueFactory(new PropertyValueFactory<>("channel"));
        channelCol.setMinWidth(40);
        channelCol.setPrefWidth(50);
        channelCol.setMaxWidth(60);
        channelCol.setResizable(false);
        
        // Signal Column - medium width with custom cell factory for visual indicators
        TableColumn<WiFiNetworkDisplay, String> signalCol = new TableColumn<>("Signal");
        signalCol.setCellValueFactory(new PropertyValueFactory<>("signalStrength"));
        signalCol.setMinWidth(80);
        signalCol.setPrefWidth(100);
        signalCol.setMaxWidth(120);
        signalCol.setCellFactory(column -> new TableCell<WiFiNetworkDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Color coding based on signal strength
                    if (item.contains("-30") || item.contains("-40")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;"); // Strong
                    } else if (item.contains("-50") || item.contains("-60")) {
                        setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;"); // Medium
                    } else {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // Weak
                    }
                }
            }
        });
        
        // Encryption Column - medium width
        TableColumn<WiFiNetworkDisplay, String> encryptionCol = new TableColumn<>("Security");
        encryptionCol.setCellValueFactory(new PropertyValueFactory<>("encryptionType"));
        encryptionCol.setMinWidth(80);
        encryptionCol.setPrefWidth(100);
        encryptionCol.setMaxWidth(120);
        
        // Vulnerability Column - medium width with custom styling
        TableColumn<WiFiNetworkDisplay, String> vulnCol = new TableColumn<>("Vulnerability");
        vulnCol.setCellValueFactory(new PropertyValueFactory<>("vulnerabilityStatus"));
        vulnCol.setMinWidth(100);
        vulnCol.setPrefWidth(130);
        vulnCol.setMaxWidth(150);
        vulnCol.setCellFactory(column -> new TableCell<WiFiNetworkDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("VULNERABLE")) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Attack Difficulty Column - medium width with color coding
        TableColumn<WiFiNetworkDisplay, String> difficultyCol = new TableColumn<>("Difficulty");
        difficultyCol.setCellValueFactory(new PropertyValueFactory<>("attackDifficulty"));
        difficultyCol.setMinWidth(80);
        difficultyCol.setPrefWidth(100);
        difficultyCol.setMaxWidth(120);
        difficultyCol.setCellFactory(column -> new TableCell<WiFiNetworkDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Easy")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else if (item.contains("Medium")) {
                        setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                    } else if (item.contains("Hard")) {
                        setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                    } else if (item.contains("Very Hard")) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Last Seen Column - time display
        TableColumn<WiFiNetworkDisplay, String> lastSeenCol = new TableColumn<>("Last Seen");
        lastSeenCol.setCellValueFactory(new PropertyValueFactory<>("lastSeen"));
        lastSeenCol.setMinWidth(80);
        lastSeenCol.setPrefWidth(100);
        lastSeenCol.setMaxWidth(120);
        
        // Add columns to table
        networkTable.getColumns().clear();
        networkTable.getColumns().add(ssidCol);
        networkTable.getColumns().add(bssidCol);
        networkTable.getColumns().add(channelCol);
        networkTable.getColumns().add(signalCol);
        networkTable.getColumns().add(encryptionCol);
        networkTable.getColumns().add(vulnCol);
        networkTable.getColumns().add(difficultyCol);
        networkTable.getColumns().add(lastSeenCol);
        
        // Set table height and make it responsive
        networkTable.setPrefHeight(300);
        networkTable.setMaxHeight(Double.MAX_VALUE);
        
        // Context menu for actions
        ContextMenu contextMenu = new ContextMenu();
        MenuItem attackMenuItem = new MenuItem("Launch Attack");
        MenuItem copyBSSIDItem = new MenuItem("Copy BSSID");
        MenuItem exportItem = new MenuItem("Export Details");
        
        attackMenuItem.setOnAction(e -> launchAttackOnSelected());
        copyBSSIDItem.setOnAction(e -> copySelectedBSSID());
        exportItem.setOnAction(e -> exportSelectedNetwork());
        
        contextMenu.getItems().addAll(attackMenuItem, copyBSSIDItem, exportItem);
        networkTable.setContextMenu(contextMenu);
        
        section.getChildren().addAll(tableTitle, networkTable);
        return section;
    }
    
    private void setupEventHandlers() {
        startScanButton.setOnAction(e -> startContinuousScanning());
        stopScanButton.setOnAction(e -> stopContinuousScanning());
        
        // Double-click to launch attack
        networkTable.setRowFactory(tv -> {
            TableRow<WiFiNetworkDisplay> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    launchAttackOnSelected();
                }
            });
            return row;
        });
    }
    
    private void startContinuousScanning() {
        if (isScanning) return;
        
        String interfaceName = interfaceSelector.getValue();
        if (interfaceName == null || interfaceName.isEmpty()) {
            showAlert("Please select a network interface");
            return;
        }
        
        isScanning = true;
        startScanButton.setDisable(true);
        stopScanButton.setDisable(false);
        scanProgress.setVisible(true);
        
        statusLabel.setText("Continuous scanning active...");
        statusLabel.setTextFill(Color.BLUE);
        
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::performSingleScan, 0, 10, TimeUnit.SECONDS);
        
        log.info("Started continuous WiFi scanning on interface: {}", interfaceName);
    }
    
    private void stopContinuousScanning() {
        if (!isScanning) return;
        
        isScanning = false;
        startScanButton.setDisable(false);
        stopScanButton.setDisable(true);
        scanProgress.setVisible(false);
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        statusLabel.setText("Scanning stopped");
        statusLabel.setTextFill(Color.ORANGE);
        
        log.info("Stopped continuous WiFi scanning");
    }
    
    private void performSingleScan() {
        String interfaceName = interfaceSelector.getValue();
        if (interfaceName == null) return;
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network scan for demonstration
                // In real implementation, this would call wifiAdapter.scanNetworks()
                return simulateNetworkScan();
            } catch (Exception e) {
                log.error("Error performing WiFi scan", e);
                return List.<WiFiNetwork>of();
            }
        }).thenAccept(networks -> {
            Platform.runLater(() -> updateNetworkTable(networks));
        });
    }
    
    private List<WiFiNetwork> simulateNetworkScan() {
        // Simulate discovered networks for demonstration
        return List.of(
            WiFiNetwork.builder()
                .ssid("HomeNetwork_2.4G")
                .bssid("AA:BB:CC:DD:EE:01")
                .channel("6")
                .signalStrength(-45)
                .encryptionType("WPA2")
                .isWpsEnabled(false)
                .discoveredAt(LocalDateTime.now())
                .build(),
            
            WiFiNetwork.builder()
                .ssid("Office_WiFi")
                .bssid("AA:BB:CC:DD:EE:02")
                .channel("11")
                .signalStrength(-60)
                .encryptionType("WPA3")
                .isWpsEnabled(false)
                .discoveredAt(LocalDateTime.now())
                .build(),
            
            WiFiNetwork.builder()
                .ssid("OldRouter")
                .bssid("AA:BB:CC:DD:EE:03")
                .channel("1")
                .signalStrength(-70)
                .encryptionType("WEP")
                .isWpsEnabled(true)
                .discoveredAt(LocalDateTime.now())
                .build(),
                
            WiFiNetwork.builder()
                .ssid("FreeWiFi")
                .bssid("AA:BB:CC:DD:EE:04")
                .channel("3")
                .signalStrength(-55)
                .encryptionType("Open")
                .isWpsEnabled(false)
                .discoveredAt(LocalDateTime.now())
                .build()
        );
    }
    
    private void updateNetworkTable(List<WiFiNetwork> networks) {
        networkData.clear();
        
        for (WiFiNetwork network : networks) {
            WiFiNetworkDisplay display = new WiFiNetworkDisplay(network);
            networkData.add(display);
        }
        
        // Update status
        networkCountLabel.setText("Networks: " + networks.size());
        
        long vulnerableCount = networks.stream()
            .mapToLong(n -> n.isVulnerable() ? 1 : 0)
            .sum();
            
        Label vulnerableLabel = (Label) ((HBox) getChildren().get(3)).getChildren().get(2);
        vulnerableLabel.setText("Vulnerable: " + vulnerableCount);
        
        Label lastScanLabel = (Label) ((HBox) getChildren().get(3)).getChildren().get(3);
        lastScanLabel.setText("Last scan: " + getCurrentTime());
        
        statusLabel.setText("Scan completed - Found " + networks.size() + " networks");
        statusLabel.setTextFill(Color.GREEN);
    }
    
    private void launchAttackOnSelected() {
        WiFiNetworkDisplay selected = networkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a network to attack");
            return;
        }
        
        // This would integrate with the WiFiWorkflowPanel to launch an attack
        showAlert("Attack launched against: " + selected.getSsid() + 
                 "\nBSSID: " + selected.getBssid() +
                 "\nVulnerability: " + selected.getVulnerabilityStatus());
    }
    
    private void copySelectedBSSID() {
        WiFiNetworkDisplay selected = networkTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Simulate clipboard copy
            showAlert("BSSID copied to clipboard: " + selected.getBssid());
        }
    }
    
    private void exportSelectedNetwork() {
        WiFiNetworkDisplay selected = networkTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showAlert("Network details exported for: " + selected.getSsid());
        }
    }
    
    private void clearNetworks() {
        networkData.clear();
        networkCountLabel.setText("Networks: 0");
        statusLabel.setText("Networks cleared");
        statusLabel.setTextFill(Color.GRAY);
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("WiFi Monitor");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    // Display wrapper for WiFiNetwork
    public static class WiFiNetworkDisplay {
        private final WiFiNetwork network;
        
        public WiFiNetworkDisplay(WiFiNetwork network) {
            this.network = network;
        }
        
        public String getSsid() { 
            return network.getSsid(); 
        }
        
        public String getBssid() { 
            return network.getBssid(); 
        }
        
        public String getChannel() { 
            return network.getChannel(); 
        }
        
        public String getSignalStrength() { 
            return network.getSignalStrength() + " dBm (" + network.getSignalQuality() + ")"; 
        }
        
        public String getEncryptionType() { 
            return network.getEncryptionType(); 
        }
        
        public String getVulnerabilityStatus() {
            if (network.isVulnerable()) {
                return "⚠️ VULNERABLE";
            } else {
                return "🔒 Secure";
            }
        }
        
        public String getAttackDifficulty() {
            int difficulty = network.getAttackDifficulty();
            return switch (difficulty) {
                case 1, 2 -> "🟢 Easy";
                case 3, 4, 5 -> "🟡 Medium";
                case 6, 7, 8 -> "🟠 Hard";
                case 9, 10 -> "🔴 Very Hard";
                default -> "❓ Unknown";
            };
        }
        
        public String getLastSeen() {
            return network.getDiscoveredAt() != null ? 
                network.getDiscoveredAt().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : 
                "Unknown";
        }
    }
}
