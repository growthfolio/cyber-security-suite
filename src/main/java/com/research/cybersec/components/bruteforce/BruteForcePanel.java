package com.research.cybersec.components.bruteforce;

import com.research.cybersec.components.base.CyberSecComponent;
import com.research.cybersec.components.results.ResultsPanel;
import com.research.cybersec.models.AttackConfig;
import com.research.cybersec.models.AttackResult;
import com.research.cybersec.services.ProcessManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class BruteForcePanel extends CyberSecComponent<VBox> {
    
    @FXML private ComboBox<String> profileCombo;
    @FXML private TextField targetField;
    @FXML private ComboBox<String> protocolCombo;
    @FXML private Spinner<Integer> threadsSpinner;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private TextArea outputArea;
    @FXML private ProgressBar attackProgress;
    
    @Autowired private ProcessManager processManager;
    
    private ResultsPanel resultsPanel;
    
    @Override
    protected void onComponentLoaded() {
        setupControls();
        bindToServices();
    }
    
    private void setupControls() {
        profileCombo.getItems().addAll("stealth", "aggressive", "spray", "custom");
        profileCombo.setValue("stealth");
        
        protocolCombo.getItems().addAll("ssh", "http", "https", "ftp", "rdp", "smb");
        protocolCombo.setValue("ssh");
        
        threadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));
        
        stopBtn.setDisable(true);
        outputArea.setStyle("-fx-font-family: 'Courier New', monospace;");
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