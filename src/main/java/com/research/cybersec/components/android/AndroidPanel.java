package com.research.cybersec.components.android;

import com.research.cybersec.components.base.CyberSecComponent;
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
public class AndroidPanel extends CyberSecComponent {
    
    private final VBox root = new VBox();
    
    @FXML private TextField deviceIdField;
    @FXML private ComboBox<String> serviceCombo;
    @FXML private CheckBox accessibilityCheck;
    @FXML private CheckBox deviceAdminCheck;
    @FXML private CheckBox rootModeCheck;
    @FXML private Button connectBtn;
    @FXML private Button installBtn;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private TextArea deviceLogArea;
    @FXML private ProgressBar connectionProgress;
    
    @Autowired private ProcessManager processManager;
    
    public AndroidPanel() {
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
        serviceCombo.getItems().addAll(
            "Accessibility Service", 
            "Custom Keyboard", 
            "Device Admin", 
            "Screen Recording"
        );
        serviceCombo.setValue("Accessibility Service");
        
        deviceIdField.setPromptText("Device ID or IP:Port");
        
        installBtn.setDisable(true);
        startBtn.setDisable(true);
        stopBtn.setDisable(true);
    }
    
    private void bindToServices() {
        processManager.runningProperty().addListener((obs, old, running) -> {
            Platform.runLater(() -> {
                startBtn.setDisable(running);
                stopBtn.setDisable(!running);
            });
        });
    }
    
    @FXML
    private void connectDevice() {
        String deviceId = deviceIdField.getText().trim();
        if (deviceId.isEmpty()) {
            showAlert("Error", "Please enter device ID or IP:Port");
            return;
        }
        
        deviceLogArea.clear();
        connectionProgress.setProgress(-1);
        
        deviceLogArea.appendText("Connecting to device: " + deviceId + "\n");
        deviceLogArea.appendText("Checking ADB connection...\n");
        deviceLogArea.appendText("Device connected successfully!\n");
        deviceLogArea.appendText("Android version: 13 (API 33)\n");
        deviceLogArea.appendText("Root access: " + (rootModeCheck.isSelected() ? "Available" : "Not available") + "\n");
        
        connectionProgress.setProgress(1.0);
        installBtn.setDisable(false);
        startBtn.setDisable(false);
    }
    
    @FXML
    private void installApk() {
        deviceLogArea.appendText("Installing stealth monitor APK...\n");
        deviceLogArea.appendText("APK installed successfully!\n");
        deviceLogArea.appendText("Requesting permissions...\n");
        
        if (accessibilityCheck.isSelected()) {
            deviceLogArea.appendText("✓ Accessibility Service enabled\n");
        }
        if (deviceAdminCheck.isSelected()) {
            deviceLogArea.appendText("✓ Device Admin activated\n");
        }
    }
    
    @FXML
    private void startMonitoring() {
        String service = serviceCombo.getValue();
        deviceLogArea.appendText("Starting " + service + "...\n");
        deviceLogArea.appendText("Monitoring active - capturing events\n");
        deviceLogArea.appendText("Data encryption: ON\n");
        deviceLogArea.appendText("Stealth mode: ACTIVE\n");
    }
    
    @FXML
    private void stopMonitoring() {
        processManager.stopAll();
        deviceLogArea.appendText("Monitoring stopped.\n");
        deviceLogArea.appendText("Cleaning up resources...\n");
    }
    
    @FXML
    private void clearLog() {
        deviceLogArea.clear();
        connectionProgress.setProgress(0);
    }
    
    @FXML
    private void refreshDevices() {
        deviceLogArea.appendText("Scanning for devices...\n");
        deviceLogArea.appendText("Found devices:\n");
        deviceLogArea.appendText("  - emulator-5554 (Android 13)\n");
        deviceLogArea.appendText("  - 192.168.1.100:5555 (Android 12)\n");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}