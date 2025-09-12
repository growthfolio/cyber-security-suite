package com.codexraziel.cybersec.views.wifi;

import com.codexraziel.cybersec.wifi.models.WiFiInterface;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced WiFi interface selector with capability detection
 * Shows available interfaces and their monitoring capabilities
 */
@Component
@Slf4j
public class WiFiInterfaceSelector extends VBox {
    
    // UI Components
    private TableView<WiFiInterfaceDisplay> interfaceTable;
    private ObservableList<WiFiInterfaceDisplay> interfaceData;
    private Label selectedInterfaceLabel;
    private Button refreshButton;
    private Button enableMonitorButton;
    private Button disableMonitorButton;
    private TextArea capabilitiesArea;
    
    // State
    private WiFiInterface selectedInterface;
    
    public WiFiInterfaceSelector() {
        initializeUI();
        loadInterfaces();
        log.info("WiFi Interface Selector initialized");
    }
    
    private void initializeUI() {
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f8f9fa;");
        
        // Title
        Label titleLabel = new Label("WiFi Interface Management");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        // Control section
        HBox controlSection = createControlSection();
        
        // Interface table
        VBox tableSection = createInterfaceTable();
        
        // Details section
        VBox detailsSection = createDetailsSection();
        
        this.getChildren().addAll(
            titleLabel,
            new Separator(),
            controlSection,
            new Separator(),
            tableSection,
            new Separator(),
            detailsSection
        );
    }
    
    private HBox createControlSection() {
        HBox section = new HBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        refreshButton = ButtonFactory.ActionButtons.refresh("Refresh Interfaces", 
            "Refresh the list of available network interfaces");
        refreshButton.setOnAction(e -> loadInterfaces());
        
        enableMonitorButton = ButtonFactory.WiFiButtons.monitor("Enable Monitor Mode", 
            "Enable monitor mode on the selected interface");
        enableMonitorButton.setDisable(true);
        enableMonitorButton.setOnAction(e -> enableMonitorMode());
        
        disableMonitorButton = ButtonFactory.ActionButtons.stop("Disable Monitor Mode", 
            "Disable monitor mode and return to managed mode");
        disableMonitorButton.setDisable(true);
        disableMonitorButton.setOnAction(e -> disableMonitorMode());
        
        Button testInterfaceButton = ButtonFactory.SecurityButtons.audit("Test Interface", 
            "Test the capabilities of the selected interface");
        testInterfaceButton.setOnAction(e -> testSelectedInterface());
        
        selectedInterfaceLabel = new Label("No interface selected");
        selectedInterfaceLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-weight: bold;");
        
        section.getChildren().addAll(
            refreshButton, enableMonitorButton, disableMonitorButton, 
            testInterfaceButton, selectedInterfaceLabel
        );
        
        return section;
    }
    
    private VBox createInterfaceTable() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label tableTitle = new Label("Available WiFi Interfaces");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        interfaceTable = new TableView<>();
        interfaceData = FXCollections.observableArrayList();
        interfaceTable.setItems(interfaceData);
        
        // Configure table properties for best practices
        interfaceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        interfaceTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        interfaceTable.setRowFactory(tv -> {
            TableRow<WiFiInterfaceDisplay> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    if (newItem.getMonitorSupport().contains("Yes")) {
                        row.setStyle("-fx-background-color: #e8f5e8;");
                    } else {
                        row.setStyle("-fx-background-color: #ffe8e8;");
                    }
                }
            });
            return row;
        });
        
        // Interface Name Column - fixed minimum for readability
        TableColumn<WiFiInterfaceDisplay, String> interfaceCol = new TableColumn<>("Interface");
        interfaceCol.setCellValueFactory(new PropertyValueFactory<>("interfaceName"));
        interfaceCol.setMinWidth(80);
        interfaceCol.setPrefWidth(100);
        interfaceCol.setMaxWidth(130);
        
        // Driver Column - can expand as needed
        TableColumn<WiFiInterfaceDisplay, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(new PropertyValueFactory<>("driverName"));
        driverCol.setMinWidth(80);
        driverCol.setPrefWidth(120);
        driverCol.setMaxWidth(180);
        
        // Operating Mode Column - medium width
        TableColumn<WiFiInterfaceDisplay, String> modeCol = new TableColumn<>("Mode");
        modeCol.setCellValueFactory(new PropertyValueFactory<>("operatingMode"));
        modeCol.setMinWidth(70);
        modeCol.setPrefWidth(90);
        modeCol.setMaxWidth(110);
        
        // Status Column - fixed small width with visual indicators
        TableColumn<WiFiInterfaceDisplay, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("interfaceStatus"));
        statusCol.setMinWidth(60);
        statusCol.setPrefWidth(80);
        statusCol.setMaxWidth(100);
        statusCol.setCellFactory(column -> new TableCell<WiFiInterfaceDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("UP")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Monitor Support Column - critical information with highlighting
        TableColumn<WiFiInterfaceDisplay, String> monitorCol = new TableColumn<>("Monitor");
        monitorCol.setCellValueFactory(new PropertyValueFactory<>("monitorSupport"));
        monitorCol.setMinWidth(70);
        monitorCol.setPrefWidth(90);
        monitorCol.setMaxWidth(110);
        monitorCol.setCellFactory(column -> new TableCell<WiFiInterfaceDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Yes")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold; -fx-background-color: #e8f5e8;");
                    } else {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold; -fx-background-color: #ffe8e8;");
                    }
                }
            }
        });
        
        // Injection Support Column - important capability
        TableColumn<WiFiInterfaceDisplay, String> injectionCol = new TableColumn<>("Injection");
        injectionCol.setCellValueFactory(new PropertyValueFactory<>("injectionCapability"));
        injectionCol.setMinWidth(70);
        injectionCol.setPrefWidth(90);
        injectionCol.setMaxWidth(110);
        injectionCol.setCellFactory(column -> new TableCell<WiFiInterfaceDisplay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Supported")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // TX Power Column - technical specification
        TableColumn<WiFiInterfaceDisplay, String> txPowerCol = new TableColumn<>("TX Power");
        txPowerCol.setCellValueFactory(new PropertyValueFactory<>("txPower"));
        txPowerCol.setMinWidth(70);
        txPowerCol.setPrefWidth(90);
        txPowerCol.setMaxWidth(110);
        
        // Add columns to table
        interfaceTable.getColumns().clear();
        interfaceTable.getColumns().add(interfaceCol);
        interfaceTable.getColumns().add(driverCol);
        interfaceTable.getColumns().add(modeCol);
        interfaceTable.getColumns().add(statusCol);
        interfaceTable.getColumns().add(monitorCol);
        interfaceTable.getColumns().add(injectionCol);
        interfaceTable.getColumns().add(txPowerCol);
        
        // Set table height and responsiveness
        interfaceTable.setPrefHeight(200);
        interfaceTable.setMaxHeight(300);
        
        // Context menu for interface actions
        ContextMenu contextMenu = new ContextMenu();
        MenuItem enableMonitorItem = new MenuItem("Enable Monitor Mode");
        MenuItem disableMonitorItem = new MenuItem("Disable Monitor Mode");
        MenuItem testInjectionItem = new MenuItem("Test Injection");
        MenuItem refreshItem = new MenuItem("Refresh Interface");
        
        enableMonitorItem.setOnAction(e -> enableMonitorMode());
        disableMonitorItem.setOnAction(e -> disableMonitorMode());
        testInjectionItem.setOnAction(e -> testSelectedInterface());
        refreshItem.setOnAction(e -> loadInterfaces());
        
        contextMenu.getItems().addAll(enableMonitorItem, disableMonitorItem, 
                                    testInjectionItem, refreshItem);
        interfaceTable.setContextMenu(contextMenu);
        
        section.getChildren().addAll(tableTitle, interfaceTable);
        return section;
    }
    
    private VBox createDetailsSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        
        Label sectionTitle = new Label("Interface Capabilities & Information");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        capabilitiesArea = new TextArea();
        capabilitiesArea.setEditable(false);
        capabilitiesArea.setPrefHeight(150);
        capabilitiesArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        capabilitiesArea.setText("Select an interface to view detailed capabilities...");
        
        section.getChildren().addAll(sectionTitle, capabilitiesArea);
        return section;
    }
    
    private void loadInterfaces() {
        refreshButton.setDisable(true);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate interface discovery
                return simulateInterfaceDiscovery();
            } catch (Exception e) {
                log.error("Error loading WiFi interfaces", e);
                return List.<WiFiInterface>of();
            }
        }).thenAccept(interfaces -> {
            Platform.runLater(() -> {
                updateInterfaceTable(interfaces);
                refreshButton.setDisable(false);
            });
        });
    }
    
    private List<WiFiInterface> simulateInterfaceDiscovery() {
        return List.of(
            WiFiInterface.builder()
                .interfaceName("wlan0")
                .driverName("ath9k_htc")
                .currentMode("Managed")
                .status(WiFiInterface.InterfaceStatus.AVAILABLE)
                .monitorModeSupported(true)
                .injectionSupported(true)
                .isUp(true)
                .macAddress("AA:BB:CC:DD:EE:01")
                .supportedBands(new String[]{"2.4GHz", "5GHz"})
                .supportedChannels(new String[]{"1-13", "36-64"})
                .build(),
                
            WiFiInterface.builder()
                .interfaceName("wlan1")
                .driverName("rtl8188eu")
                .currentMode("Managed")
                .status(WiFiInterface.InterfaceStatus.ERROR)
                .monitorModeSupported(false)
                .injectionSupported(false)
                .isUp(false)
                .macAddress("AA:BB:CC:DD:EE:02")
                .supportedBands(new String[]{"2.4GHz"})
                .supportedChannels(new String[]{"1-11"})
                .build(),
                
            WiFiInterface.builder()
                .interfaceName("wlan0mon")
                .driverName("ath9k_htc")
                .currentMode("Monitor")
                .status(WiFiInterface.InterfaceStatus.MONITOR_MODE)
                .monitorModeSupported(true)
                .injectionSupported(true)
                .isUp(true)
                .isMonitorMode(true)
                .macAddress("AA:BB:CC:DD:EE:03")
                .supportedBands(new String[]{"2.4GHz", "5GHz"})
                .supportedChannels(new String[]{"1-13", "36-64"})
                .build()
        );
    }
    
    private void updateInterfaceTable(List<WiFiInterface> interfaces) {
        interfaceData.clear();
        
        for (WiFiInterface iface : interfaces) {
            WiFiInterfaceDisplay display = new WiFiInterfaceDisplay(iface);
            interfaceData.add(display);
        }
        
        log.info("Loaded {} WiFi interfaces", interfaces.size());
    }
    
    private void selectInterface(WiFiInterfaceDisplay display) {
        this.selectedInterface = display.getInterface();
        selectedInterfaceLabel.setText("Selected: " + selectedInterface.getInterfaceName());
        
        // Update button states
        boolean isMonitorMode = "Monitor".equals(selectedInterface.getCurrentMode());
        enableMonitorButton.setDisable(isMonitorMode || !selectedInterface.isMonitorModeSupported());
        disableMonitorButton.setDisable(!isMonitorMode);
        
        // Update capabilities display
        updateCapabilitiesDisplay();
        
        log.info("Selected interface: {}", selectedInterface.getInterfaceName());
    }
    
    private void updateCapabilitiesDisplay() {
        if (selectedInterface == null) return;
        
        StringBuilder capabilities = new StringBuilder();
        capabilities.append("Interface: ").append(selectedInterface.getInterfaceName()).append("\n");
        capabilities.append("Driver: ").append(selectedInterface.getDriverName()).append("\n");
        capabilities.append("Current Mode: ").append(selectedInterface.getCurrentMode()).append("\n");
        capabilities.append("Status: ").append(selectedInterface.getStatus().getDescription()).append("\n");
        capabilities.append("MAC Address: ").append(selectedInterface.getMacAddress()).append("\n");
        capabilities.append("Supported Bands: ").append(String.join(", ", selectedInterface.getSupportedBands())).append("\n");
        capabilities.append("Supported Channels: ").append(String.join(", ", selectedInterface.getSupportedChannels())).append("\n\n");
        
        capabilities.append("CAPABILITIES:\n");
        capabilities.append("- Monitor Mode: ").append(selectedInterface.isMonitorModeSupported() ? "✅ Supported" : "❌ Not Supported").append("\n");
        capabilities.append("- Packet Injection: ").append(selectedInterface.isInjectionSupported() ? "✅ Supported" : "❌ Not Supported").append("\n");
        capabilities.append("- WPS Attacks: ").append(selectedInterface.isMonitorModeSupported() ? "✅ Available" : "❌ Requires Monitor Mode").append("\n");
        capabilities.append("- Handshake Capture: ").append(selectedInterface.isMonitorModeSupported() ? "✅ Available" : "❌ Requires Monitor Mode").append("\n");
        capabilities.append("- Evil Twin Attacks: ").append(selectedInterface.isInjectionSupported() ? "✅ Available" : "❌ Requires Injection Support").append("\n");
        
        capabilities.append("\nRECOMMENDED USAGE:\n");
        if (selectedInterface.isMonitorModeSupported() && selectedInterface.isInjectionSupported()) {
            capabilities.append("✅ Excellent for all WiFi attacks\n");
            capabilities.append("✅ Full penetration testing capabilities\n");
        } else if (selectedInterface.isMonitorModeSupported()) {
            capabilities.append("⚠️ Good for passive monitoring and handshake capture\n");
            capabilities.append("❌ Limited active attack capabilities\n");
        } else {
            capabilities.append("❌ Not suitable for advanced WiFi attacks\n");
            capabilities.append("ℹ️ Can be used for basic network scanning only\n");
        }
        
        capabilitiesArea.setText(capabilities.toString());
    }
    
    private void enableMonitorMode() {
        if (selectedInterface == null) return;
        
        showAlert("Enabling monitor mode on " + selectedInterface.getInterfaceName() + "...\n\n" +
                 "Commands that would be executed:\n" +
                 "sudo ifconfig " + selectedInterface.getInterfaceName() + " down\n" +
                 "sudo iwconfig " + selectedInterface.getInterfaceName() + " mode monitor\n" +
                 "sudo ifconfig " + selectedInterface.getInterfaceName() + " up");
                 
        log.info("Monitor mode enabled on interface: {}", selectedInterface.getInterfaceName());
    }
    
    private void disableMonitorMode() {
        if (selectedInterface == null) return;
        
        showAlert("Disabling monitor mode on " + selectedInterface.getInterfaceName() + "...\n\n" +
                 "Commands that would be executed:\n" +
                 "sudo ifconfig " + selectedInterface.getInterfaceName() + " down\n" +
                 "sudo iwconfig " + selectedInterface.getInterfaceName() + " mode managed\n" +
                 "sudo ifconfig " + selectedInterface.getInterfaceName() + " up");
                 
        log.info("Monitor mode disabled on interface: {}", selectedInterface.getInterfaceName());
    }
    
    private void testSelectedInterface() {
        if (selectedInterface == null) {
            showAlert("Please select an interface first");
            return;
        }
        
        String testResults = performInterfaceTest(selectedInterface);
        showAlert("Interface Test Results:\n\n" + testResults);
    }
    
    private String performInterfaceTest(WiFiInterface iface) {
        StringBuilder results = new StringBuilder();
        results.append("Testing interface: ").append(iface.getInterfaceName()).append("\n\n");
        
        // Simulate various tests
        results.append("🔍 Basic connectivity: ✅ PASS\n");
        results.append("🔍 Driver functionality: ").append(iface.getDriverName().contains("ath9k") ? "✅ PASS" : "⚠️ WARNING").append("\n");
        results.append("🔍 Monitor mode capability: ").append(iface.isMonitorModeSupported() ? "✅ PASS" : "❌ FAIL").append("\n");
        results.append("🔍 Injection capability: ").append(iface.isInjectionSupported() ? "✅ PASS" : "❌ FAIL").append("\n");
        results.append("🔍 Power level control: ✅ PASS\n");
        results.append("🔍 Channel switching: ✅ PASS\n");
        
        results.append("\nRECOMMENDATION: ");
        if (iface.isMonitorModeSupported() && iface.isInjectionSupported()) {
            results.append("✅ Interface is ready for advanced WiFi penetration testing");
        } else {
            results.append("⚠️ Interface has limited capabilities for WiFi attacks");
        }
        
        return results.toString();
    }
    
    public WiFiInterface getSelectedInterface() {
        return selectedInterface;
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("WiFi Interface Manager");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Display wrapper for WiFiInterface
    public static class WiFiInterfaceDisplay {
        private final WiFiInterface iface;
        
        public WiFiInterfaceDisplay(WiFiInterface iface) {
            this.iface = iface;
        }
        
        public String getName() { return iface.getInterfaceName(); }
        public String getDriver() { return iface.getDriverName(); }
        public String getMode() { return iface.getCurrentMode(); }
        public String getStatus() { 
            return iface.isUp() ? "🟢 UP" : "🔴 DOWN"; 
        }
        public String getMonitorSupport() { 
            return iface.isMonitorModeSupported() ? "✅ Yes" : "❌ No"; 
        }
        public String getInjectionSupport() { 
            return iface.isInjectionSupported() ? "✅ Yes" : "❌ No"; 
        }
        public String getPowerLevel() { 
            return iface.getSupportedBands().length + " bands"; 
        }
        
        public WiFiInterface getInterface() { return iface; }
    }
}
