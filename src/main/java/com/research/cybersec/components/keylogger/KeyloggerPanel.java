package com.research.cybersec.components.keylogger;

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
public class KeyloggerPanel extends CyberSecComponent {
    
    private final VBox root = new VBox();
    
    @FXML private ComboBox<String> platformCombo;
    @FXML private TextField outputPathField;
    @FXML private CheckBox stealthModeCheck;
    @FXML private CheckBox encryptionCheck;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private TextArea logArea;
    @FXML private ProgressBar monitorProgress;
    
    @Autowired private ProcessManager processManager;
    
    public KeyloggerPanel() {
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
        platformCombo.getItems().addAll("Windows", "Linux", "macOS");
        platformCombo.setValue("Linux");
        
        outputPathField.setText("/tmp/keylog_output");
        stealthModeCheck.setSelected(true);
        encryptionCheck.setSelected(true);
        
        stopBtn.setDisable(true);
    }
    
    private void bindToServices() {
        processManager.runningProperty().addListener((obs, old, running) -> {
            Platform.runLater(() -> {
                startBtn.setDisable(running);
                stopBtn.setDisable(!running);
                platformCombo.setDisable(running);
                outputPathField.setDisable(running);
            });
        });
    }
    
    @FXML
    private void startKeylogger() {
        if (outputPathField.getText().trim().isEmpty()) {
            showAlert("Error", "Please specify output path");
            return;
        }
        
        logArea.clear();
        monitorProgress.setProgress(-1); // Indeterminate
        
        String platform = platformCombo.getValue().toLowerCase();
        String outputPath = outputPathField.getText().trim();
        boolean stealth = stealthModeCheck.isSelected();
        boolean encrypt = encryptionCheck.isSelected();
        
        logArea.appendText("Starting keylogger for " + platform + "...\n");
        logArea.appendText("Output: " + outputPath + "\n");
        logArea.appendText("Stealth mode: " + (stealth ? "ON" : "OFF") + "\n");
        logArea.appendText("Encryption: " + (encrypt ? "ON" : "OFF") + "\n");
        logArea.appendText("Monitoring keyboard events...\n");
    }
    
    @FXML
    private void stopKeylogger() {
        processManager.stopAll();
        monitorProgress.setProgress(0);
        logArea.appendText("Keylogger stopped.\n");
    }
    
    @FXML
    private void clearLog() {
        logArea.clear();
        monitorProgress.setProgress(0);
    }
    
    @FXML
    private void browseOutputPath() {
        // TODO: Implement file chooser
        showAlert("Info", "File chooser will be implemented soon.");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}