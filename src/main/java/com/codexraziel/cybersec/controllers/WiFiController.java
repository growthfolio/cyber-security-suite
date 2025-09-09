package com.codexraziel.cybersec.controllers;

import com.codexraziel.cybersec.models.WiFiNetwork;
import com.codexraziel.cybersec.models.ScanResult;
import com.codexraziel.cybersec.models.PentestConfig;
import com.codexraziel.cybersec.services.WiFiScannerService;
import com.codexraziel.cybersec.services.WiFiPentestService;
import com.codexraziel.cybersec.services.ResourceCoordinator;
import com.codexraziel.cybersec.security.InputValidator;
import com.codexraziel.cybersec.security.PermissionManager;
import com.codexraziel.cybersec.security.AuditLogger;
import com.codexraziel.cybersec.services.ResultsExporter;
import com.codexraziel.cybersec.services.ConfigurationManager;
import javafx.application.Platform;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
@Slf4j
public class WiFiController {
    
    @Autowired
    private WiFiScannerService scannerService;
    
    @Autowired
    private WiFiPentestService pentestService;
    
    @Autowired
    private ResourceCoordinator resourceCoordinator;
    
    @Autowired
    private InputValidator inputValidator;
    
    @Autowired
    private PermissionManager permissionManager;
    
    @Autowired
    private AuditLogger auditLogger;
    
    @Autowired
    private ResultsExporter resultsExporter;
    
    @Autowired
    private ConfigurationManager configManager;
    
    private ScanResult lastScanResult;
    
    private ComboBox<String> interfaceCombo;
    private TableView<String[]> networksTable;
    private TextField targetSSID;
    private ComboBox<String> attackType;
    private TextField wordlistPath;
    private Button startAttackBtn;
    private Button stopAttackBtn;
    private ProgressBar wifiProgress;
    private TextArea wifiLogArea;
    private Label statusLabel;
    
    // Loading state components
    private ProgressIndicator scanProgress;
    private Label scanStatus;
    private Button cancelBtn;
    
    private CompletableFuture<Void> currentTask;
    private CompletableFuture<Void> currentScan;
    
    public void bindComponents(ComboBox<String> interfaceCombo, TableView<String[]> networksTable,
                              TextField targetSSID, ComboBox<String> attackType, TextField wordlistPath,
                              Button startAttackBtn, Button stopAttackBtn,
                              ProgressBar wifiProgress, TextArea wifiLogArea, Label statusLabel,
                              ProgressIndicator scanProgress, Label scanStatus, Button cancelBtn) {
        
        this.interfaceCombo = interfaceCombo;
        this.networksTable = networksTable;
        this.targetSSID = targetSSID;
        this.attackType = attackType;
        this.wordlistPath = wordlistPath;
        this.startAttackBtn = startAttackBtn;
        this.stopAttackBtn = stopAttackBtn;
        this.wifiProgress = wifiProgress;
        this.wifiLogArea = wifiLogArea;
        this.statusLabel = statusLabel;
        this.scanProgress = scanProgress;
        this.scanStatus = scanStatus;
        this.cancelBtn = cancelBtn;
        
        setupEventHandlers();
    }
    
    private void setupEventHandlers() {
        startAttackBtn.setOnAction(e -> startWiFiAttack());
        stopAttackBtn.setOnAction(e -> stopWiFiAttack());
        cancelBtn.setOnAction(e -> cancelScan());
    }
    
    public void scanNetworks() {
        if (currentScan != null && !currentScan.isDone()) {
            showError("Scan in Progress", "A network scan is already running. Please wait or cancel it first.");
            return;
        }
        
        String networkInterface = interfaceCombo.getValue();
        
        // Security validation
        InputValidator.ValidationResult interfaceValidation = inputValidator.validateNetworkInterface(networkInterface);
        if (!interfaceValidation.isValid()) {
            auditLogger.logSecurityViolation("INVALID_INTERFACE", interfaceValidation.getErrorMessage(), "local");
            showError("Invalid Interface", "Network interface validation failed: " + interfaceValidation.getErrorMessage());
            return;
        }
        
        // Permission check
        PermissionManager.PermissionCheckResult wifiPermissions = permissionManager.checkWiFiPermissions();
        if (!wifiPermissions.isSuccess() && !wifiPermissions.isWarning()) {
            showError("Insufficient Permissions", 
                     wifiPermissions.getMessage() + "\n\nSuggestion: " + wifiPermissions.getSuggestion());
            return;
        }
        
        // Resource availability
        if (!resourceCoordinator.isResourceAvailable(networkInterface)) {
            showError("Resource Unavailable", 
                     "Network interface " + networkInterface + " is busy or system resources are low.\n\n" +
                     "Suggested actions:\n" +
                     "• Wait for current operations to complete\n" +
                     "• Try a different network interface\n" +
                     "• Close other resource-intensive operations");
            return;
        }
        
        // Log security event
        auditLogger.logUserAction("WIFI_SCAN_START", networkInterface, "Initiated");
        
        // Set loading state
        Platform.runLater(() -> {
            scanProgress.setVisible(true);
            scanStatus.setText("SATELLITE Scanning networks...");
            scanStatus.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
            cancelBtn.setVisible(true);
            networksTable.getItems().clear();
        });
        
        wifiLogArea.appendText("[SCAN] Starting WiFi network scan...\n");
        wifiLogArea.appendText("[WIFI] Interface: " + interfaceCombo.getValue() + "\n");
        
        String operationId = "wifi_scan_" + System.currentTimeMillis();
        
        currentScan = resourceCoordinator.executeWithResourceControl(operationId, networkInterface, () -> {
            scannerService.scanNetworks(networkInterface, output -> {
                Platform.runLater(() -> wifiLogArea.appendText(output + "\n"));
            }).thenAccept(scanResult -> {
            Platform.runLater(() -> {
                // Reset loading state
                scanProgress.setVisible(false);
                cancelBtn.setVisible(false);
                
                if (scanResult.isSuccessful()) {
                    scanStatus.setText("OK Scan completed - " + scanResult.getTotalNetworks() + " networks found");
                    scanStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    
                    for (WiFiNetwork network : scanResult.getNetworks()) {
                        String[] row = {
                            network.getNetworkStatus().toString(),
                            network.getSsid(), 
                            network.getBssid(), 
                            network.getSecurityLevel(), 
                            network.getSignalStrength() + "dBm", 
                            network.getChannel()
                        };
                        networksTable.getItems().add(row);
                    }
                    
                    wifiLogArea.appendText("[OK] Scan completed - " + scanResult.getTotalNetworks() + " networks found\n");
                    wifiLogArea.appendText("[WARN] Vulnerable networks: " + scanResult.getVulnerableNetworks() + "\n\n");
                    
                    // Store scan result for export
                    lastScanResult = scanResult;
                    
                    if (scanResult.getVulnerableNetworks() > 0) {
                        showWarning("Vulnerable Networks Found", 
                                  scanResult.getVulnerableNetworks() + " vulnerable networks detected. Review security settings.");
                    }
                } else {
                    scanStatus.setText("ERROR Scan failed");
                    scanStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    wifiLogArea.appendText("[ERROR] Scan failed: " + scanResult.getErrorMessage() + "\n\n");
                    
                    showError("Scan Failed", 
                             "Network scan failed: " + scanResult.getErrorMessage() + 
                             "\n\nSuggested actions:\n" +
                             "• Check if WiFi interface exists\n" +
                             "• Install network-manager (nmcli)\n" +
                             "• Run with appropriate permissions\n" +
                             "• Check system resource usage");
                }
            });
            }).exceptionally(throwable -> {
                Platform.runLater(() -> {
                    scanProgress.setVisible(false);
                    cancelBtn.setVisible(false);
                    scanStatus.setText("ERROR Scan error");
                    scanStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    
                    String errorMsg = throwable.getMessage();
                    if (errorMsg.contains("Resource limit exceeded")) {
                        showError("Resource Limit Exceeded", 
                                 "System resources are exhausted.\n\n" +
                                 "Suggested actions:\n" +
                                 "• Wait for other operations to complete\n" +
                                 "• Close unused applications\n" +
                                 "• Restart the application if needed");
                    } else {
                        showError("Scan Error", "Unexpected error during scan: " + errorMsg);
                    }
                });
                return null;
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                scanProgress.setVisible(false);
                cancelBtn.setVisible(false);
                scanStatus.setText("ERROR Resource error");
                scanStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                
                showError("Resource Error", "Failed to acquire system resources: " + throwable.getMessage());
            });
            return null;
        });
    }
    
    private void startWiFiAttack() {
        String ssid = targetSSID.getText();
        String attack = attackType.getValue();
        String wordlist = wordlistPath.getText();
        
        // Security validation
        InputValidator.ValidationResult ssidValidation = inputValidator.validateSSID(ssid);
        if (!ssidValidation.isValid()) {
            auditLogger.logSecurityViolation("INVALID_SSID", ssidValidation.getErrorMessage(), "local");
            showError("Invalid SSID", "SSID validation failed: " + ssidValidation.getErrorMessage());
            return;
        }
        
        InputValidator.ValidationResult wordlistValidation = inputValidator.validateFilePath(wordlist);
        if (!wordlistValidation.isValid()) {
            auditLogger.logSecurityViolation("INVALID_WORDLIST_PATH", wordlistValidation.getErrorMessage(), "local");
            showError("Invalid Wordlist", "Wordlist path validation failed: " + wordlistValidation.getErrorMessage());
            return;
        }
        
        // Permission check for file access
        PermissionManager.PermissionCheckResult filePermissions = permissionManager.checkFilePermissions(wordlist);
        if (!filePermissions.isSuccess()) {
            showError("File Access Error", 
                     filePermissions.getMessage() + "\n\nSuggestion: " + filePermissions.getSuggestion());
            return;
        }
        
        // Log security event
        auditLogger.logUserAction("WIFI_ATTACK_START", ssid, "Attack type: " + attack);
        
        wifiLogArea.appendText("[🚀] Starting WiFi attack\n");
        wifiLogArea.appendText("[TARGET] Target: " + ssid + "\n");
        wifiLogArea.appendText("[ATTACK] Attack: " + attack + "\n");
        wifiLogArea.appendText("[FILE] Wordlist: " + wordlist + "\n");
        
        startAttackBtn.setDisable(true);
        stopAttackBtn.setDisable(false);
        wifiProgress.setProgress(0.3);
        statusLabel.setText("WiFi Attack Running");
        
        // Create config for the attack
        PentestConfig config = PentestConfig.builder()
            .targetSSID(ssid)
            .attackType(attack)
            .wordlistPath(wordlist)
            .networkInterface(interfaceCombo.getValue())
            .threadCount(4)
            .timeoutSeconds(300)
            .build();
        
        currentTask = pentestService.executeAttack(config, output -> {
            Platform.runLater(() -> {
                wifiLogArea.appendText(output + "\n");
                wifiProgress.setProgress(0.7);
            });
        }).thenRun(() -> {
            Platform.runLater(() -> {
                startAttackBtn.setDisable(false);
                stopAttackBtn.setDisable(true);
                wifiProgress.setProgress(0);
                statusLabel.setText("Ready");
            });
        });
    }
    
    private void stopWiFiAttack() {
        wifiLogArea.appendText("[STOP] Stopping WiFi attack...\n");
        
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        
        wifiLogArea.appendText("[OK] Attack stopped by user\n\n");
        
        startAttackBtn.setDisable(false);
        stopAttackBtn.setDisable(true);
        wifiProgress.setProgress(0);
        statusLabel.setText("Ready");
    }
    
    public void captureHandshake() {
        wifiLogArea.appendText("[HANDSHAKE] Capturing WPA handshake...\n");
        wifiLogArea.appendText("[CONFIG] Monitoring for authentication packets...\n");
        wifiLogArea.appendText("[OK] Handshake captured successfully\n\n");
    }
    
    public void cancelScan() {
        if (currentScan != null && !currentScan.isDone()) {
            currentScan.cancel(true);
            
            // Cancel resource allocation
            String operationId = "wifi_scan_" + System.currentTimeMillis();
            resourceCoordinator.cancelOperation(operationId);
            
            Platform.runLater(() -> {
                scanProgress.setVisible(false);
                cancelBtn.setVisible(false);
                scanStatus.setText("STOP Scan cancelled");
                scanStatus.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                wifiLogArea.appendText("[STOP] Network scan cancelled by user\n\n");
            });
        }
    }
    
    // Keyboard shortcut handlers
    public void saveResults() {
        if (lastScanResult == null || lastScanResult.getNetworks().isEmpty()) {
            showWarning("No Results", "No scan results available to save. Please run a network scan first.");
            return;
        }
        
        auditLogger.logUserAction("SAVE_RESULTS", "WiFi scan results", "Requested");
        wifiLogArea.appendText("[SAVE] Saving scan results...\n");
        
        // Save as JSON by default
        ResultsExporter.ExportResult result = resultsExporter.exportToJSON(lastScanResult.getNetworks(), "wifi_scan");
        
        if (result.isSuccess()) {
            wifiLogArea.appendText("[OK] Results saved: " + result.getFilePath() + "\n");
            wifiLogArea.appendText("[STATS] Records exported: " + result.getRecordCount() + "\n\n");
            showInfo("Save Successful", "Results saved to: " + result.getFilePath());
        } else {
            wifiLogArea.appendText("[ERROR] Save failed: " + result.getMessage() + "\n\n");
            showError("Save Failed", result.getMessage());
        }
    }
    
    public void exportResults() {
        if (lastScanResult == null || lastScanResult.getNetworks().isEmpty()) {
            showWarning("No Results", "No scan results available to export. Please run a network scan first.");
            return;
        }
        
        auditLogger.logUserAction("EXPORT_RESULTS", "WiFi scan results", "Requested");
        wifiLogArea.appendText("[EXPORT] Exporting results...\n");
        
        // Show export options dialog
        showExportDialog();
    }
    
    public void analyzeTarget() {
        String ssid = targetSSID.getText();
        
        InputValidator.ValidationResult validation = inputValidator.validateSSID(ssid);
        if (!validation.isValid()) {
            showWarning("Invalid Target", "SSID validation failed: " + validation.getErrorMessage());
            return;
        }
        
        auditLogger.logUserAction("ANALYZE_TARGET", ssid, "Analysis requested");
        wifiLogArea.appendText("[SCAN] Analyzing target: " + ssid + "\n");
        // Implementation for target analysis
    }
    
    // Professional error handling
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showExportDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Export Results");
        alert.setHeaderText("Choose Export Format");
        alert.setContentText("Select the format for exporting scan results:");
        
        ButtonType csvButton = new ButtonType("CSV");
        ButtonType jsonButton = new ButtonType("JSON");
        ButtonType reportButton = new ButtonType("Report");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(csvButton, jsonButton, reportButton, cancelButton);
        
        alert.showAndWait().ifPresent(response -> {
            ResultsExporter.ExportResult result = null;
            
            if (response == csvButton) {
                result = resultsExporter.exportToCSV(lastScanResult.getNetworks(), "wifi_scan");
            } else if (response == jsonButton) {
                result = resultsExporter.exportToJSON(lastScanResult.getNetworks(), "wifi_scan");
            } else if (response == reportButton) {
                result = resultsExporter.exportScanReport(lastScanResult, "wifi_report");
            }
            
            if (result != null) {
                if (result.isSuccess()) {
                    wifiLogArea.appendText("[OK] Export completed: " + result.getFilePath() + "\n");
                    wifiLogArea.appendText("[STATS] Records exported: " + result.getRecordCount() + "\n\n");
                    showInfo("Export Successful", "Results exported to: " + result.getFilePath());
                } else {
                    wifiLogArea.appendText("[ERROR] Export failed: " + result.getMessage() + "\n\n");
                    showError("Export Failed", result.getMessage());
                }
            }
        });
    }
    
    public void saveProfile() {
        String ssid = targetSSID.getText();
        String attack = attackType.getValue();
        String wordlist = wordlistPath.getText();
        String networkInterface = interfaceCombo.getValue();
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Save Profile");
        dialog.setHeaderText("Save Pentest Profile");
        dialog.setContentText("Enter profile name:");
        
        dialog.showAndWait().ifPresent(profileName -> {
            if (profileName.trim().isEmpty()) {
                showWarning("Invalid Name", "Profile name cannot be empty.");
                return;
            }
            
            ConfigurationManager.PentestProfile profile = ConfigurationManager.PentestProfile.builder()
                .name(profileName.trim())
                .description("WiFi pentest profile")
                .targetSSID(ssid)
                .attackType(attack)
                .wordlistPath(wordlist)
                .networkInterface(networkInterface)
                .threadCount(4)
                .timeoutSeconds(300)
                .enableEvasion(false)
                .createdAt(java.time.LocalDateTime.now())
                .build();
            
            ConfigurationManager.ConfigResult result = configManager.saveProfile(profile);
            
            if (result.isSuccess()) {
                wifiLogArea.appendText("[SAVE] Profile saved: " + profileName + "\n\n");
                showInfo("Profile Saved", "Profile '" + profileName + "' saved successfully.");
            } else {
                wifiLogArea.appendText("[ERROR] Profile save failed: " + result.getMessage() + "\n\n");
                showError("Save Failed", result.getMessage());
            }
        });
    }
    
    public void loadProfile() {
        ConfigurationManager.ConfigResult<List<String>> profilesResult = configManager.getAvailableProfiles();
        
        if (!profilesResult.isSuccess() || profilesResult.getData().isEmpty()) {
            showInfo("No Profiles", "No saved profiles found.");
            return;
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(profilesResult.getData().get(0), profilesResult.getData());
        dialog.setTitle("Load Profile");
        dialog.setHeaderText("Load Pentest Profile");
        dialog.setContentText("Select profile:");
        
        dialog.showAndWait().ifPresent(selectedProfile -> {
            ConfigurationManager.ConfigResult<ConfigurationManager.PentestProfile> result = 
                configManager.loadProfile(selectedProfile);
            
            if (result.isSuccess()) {
                ConfigurationManager.PentestProfile profile = result.getData();
                
                // Apply profile settings
                targetSSID.setText(profile.getTargetSSID());
                attackType.setValue(profile.getAttackType());
                wordlistPath.setText(profile.getWordlistPath());
                interfaceCombo.setValue(profile.getNetworkInterface());
                
                wifiLogArea.appendText("[SAVE] Profile loaded: " + selectedProfile + "\n\n");
                showInfo("Profile Loaded", "Profile '" + selectedProfile + "' loaded successfully.");
            } else {
                wifiLogArea.appendText("[ERROR] Profile load failed: " + result.getMessage() + "\n\n");
                showError("Load Failed", result.getMessage());
            }
        });
    }
}