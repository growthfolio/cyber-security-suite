package com.research.cybersec.components.bruteforce;

import com.research.cybersec.components.base.CyberSecComponent;
import com.research.cybersec.components.results.ResultsPanel;
import com.research.cybersec.models.AttackConfig;
import com.research.cybersec.models.AttackResult;
import com.research.cybersec.network.WiFiScanner;
import com.research.cybersec.services.ProcessManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Scope("prototype")
public class BruteForcePanel extends CyberSecComponent {
    
    private final VBox root = new VBox();
    
    @FXML private ComboBox<String> profileCombo;
    @FXML private TextField targetField;
    @FXML private ComboBox<String> protocolCombo;
    @FXML private ComboBox<String> wifiNetworkCombo;
    @FXML private Button scanWifiBtn;
    @FXML private Spinner<Integer> threadsSpinner;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private TextArea outputArea;
    @FXML private ProgressBar attackProgress;
    
    @Autowired private ProcessManager processManager;
    @Autowired private WiFiScanner wifiScanner;
    
    private ResultsPanel resultsPanel;
    
    public BruteForcePanel() {
        // Empty constructor for Spring
    }
    
    @PostConstruct
    private void init() {
        loadFXML(root);
    }
    
    public VBox getRoot() {
        return root;
    }
    
    @Override
    protected void onComponentLoaded() {
        setupControls();
        bindToServices();
    }
    
    private void setupControls() {
        profileCombo.getItems().addAll("stealth", "aggressive", "spray", "custom");
        profileCombo.setValue("stealth");
        
        protocolCombo.getItems().addAll("ssh", "http", "https", "ftp", "rdp", "smb", "wifi");
        protocolCombo.setValue("ssh");
        
        wifiNetworkCombo.setPromptText("Scan for WiFi networks...");
        wifiNetworkCombo.setDisable(true);
        
        threadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));
        
        stopBtn.setDisable(true);
        
        // Enable WiFi controls when WiFi protocol is selected
        protocolCombo.valueProperty().addListener((obs, old, newVal) -> {
            boolean isWifi = "wifi".equals(newVal);
            scanWifiBtn.setDisable(!isWifi);
            wifiNetworkCombo.setDisable(!isWifi);
            if (isWifi) {
                targetField.setPromptText("Select WiFi network or enter BSSID");
            } else {
                targetField.setPromptText("Target IP or hostname");
            }
        });
    }
    
    private void bindToServices() {
        processManager.runningProperty().addListener((obs, old, running) -> {
            Platform.runLater(() -> {
                startBtn.setDisable(running);
                stopBtn.setDisable(!running);
                profileCombo.setDisable(running);
                targetField.setDisable(running);
                protocolCombo.setDisable(running);
                threadsSpinner.setDisable(running);
            });
        });
        
        processManager.progressProperty().addListener((obs, old, progress) ->
            Platform.runLater(() -> attackProgress.setProgress(progress.doubleValue()))
        );
    }
    
    @FXML
    private void startAttack() {
        if (targetField.getText().trim().isEmpty()) {
            showAlert("Error", "Please enter a target");
            return;
        }
        
        AttackConfig config = AttackConfig.builder()
            .profile(profileCombo.getValue())
            .target(targetField.getText().trim())
            .protocol(protocolCombo.getValue())
            .threads(threadsSpinner.getValue())
            .build();
        
        outputArea.clear();
        attackProgress.setProgress(0);
        
        processManager.startBruteForce(config, this::onAttackOutput, this::onAttackResult);
    }
    
    @FXML
    private void scanWifiNetworks() {
        scanWifiBtn.setDisable(true);
        wifiNetworkCombo.getItems().clear();
        
        outputArea.appendText("Scanning for WiFi networks...\n");
        
        wifiScanner.scanNetworks().thenAccept(networks -> {
            Platform.runLater(() -> {
                outputArea.appendText(String.format("Found %d WiFi networks:\n", networks.size()));
                
                for (WiFiScanner.WiFiNetwork network : networks) {
                    String networkInfo = String.format("%s (%s) - %s - %ddBm", 
                        network.getSsid(), 
                        network.getBssid(),
                        network.getSecurityLevel(),
                        network.getSignalStrength());
                    
                    wifiNetworkCombo.getItems().add(networkInfo);
                    
                    outputArea.appendText(String.format("  %s\n", networkInfo));
                    
                    if (network.isVulnerable()) {
                        outputArea.appendText("    ⚠️  Vulnerable target detected!\n");
                    }
                }
                
                // Show vulnerable networks first
                var vulnerableNetworks = wifiScanner.getVulnerableNetworks(networks);
                if (!vulnerableNetworks.isEmpty()) {
                    outputArea.appendText(String.format("\n🎯 %d vulnerable networks found:\n", vulnerableNetworks.size()));
                    for (var network : vulnerableNetworks) {
                        outputArea.appendText(String.format("  %s - %s\n", 
                            network.getSsid(), network.getSecurityLevel()));
                    }
                }
                
                outputArea.appendText("\nWiFi scan complete.\n\n");
                scanWifiBtn.setDisable(false);
                wifiNetworkCombo.setDisable(false);
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                outputArea.appendText("WiFi scan failed: " + throwable.getMessage() + "\n\n");
                scanWifiBtn.setDisable(false);
            });
            return null;
        });
    }
    
    @FXML
    private void selectWifiNetwork() {
        String selected = wifiNetworkCombo.getValue();
        if (selected != null && !selected.isEmpty()) {
            // Extract BSSID from selection
            String bssid = selected.split("\\(")[1].split("\\)")[0];
            targetField.setText(bssid);
            
            outputArea.appendText(String.format("Selected WiFi target: %s\n", selected));
        }
    }
    
    @FXML
    private void stopAttack() {
        processManager.stopAll();
    }
    
    @FXML
    private void clearOutput() {
        outputArea.clear();
        attackProgress.setProgress(0);
    }
    
    private void onAttackOutput(String output) {
        Platform.runLater(() -> {
            outputArea.appendText(output + "\n");
            outputArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    private void onAttackResult(AttackResult result) {
        if (resultsPanel != null) {
            resultsPanel.addResult(result);
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void setResultsPanel(ResultsPanel resultsPanel) {
        this.resultsPanel = resultsPanel;
    }
}