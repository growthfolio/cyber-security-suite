package com.research.cybersec.security;

import com.research.cybersec.components.base.CyberSecComponent;
import com.research.cybersec.security.AdvancedSecurityEngine.DetectionResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
@Scope("prototype")
public class SecurityPanel extends CyberSecComponent {
    
    private final VBox root = new VBox();
    
    @FXML private Label securityStatusLabel;
    @FXML private ProgressBar detectionProgress;
    @FXML private TextArea detectionResults;
    @FXML private CheckBox vmDetectionCheck;
    @FXML private CheckBox sandboxDetectionCheck;
    @FXML private CheckBox debuggerDetectionCheck;
    @FXML private CheckBox polymorphicCheck;
    @FXML private CheckBox cryptoCheck;
    @FXML private Button runDetectionBtn;
    @FXML private Button generateVariantBtn;
    @FXML private Button testCryptoBtn;
    @FXML private Button clearLogBtn;
    @FXML private Slider confidenceSlider;
    @FXML private Label confidenceLabel;
    
    @Autowired private AdvancedSecurityEngine securityEngine;
    
    private AdvancedSecurityEngine.PolymorphicContext polymorphicContext;
    private AdvancedSecurityEngine.CryptoContext cryptoContext;
    
    public SecurityPanel() {
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
        initializeSecurityEngine();
    }
    
    private void setupControls() {
        vmDetectionCheck.setSelected(true);
        sandboxDetectionCheck.setSelected(true);
        debuggerDetectionCheck.setSelected(true);
        polymorphicCheck.setSelected(true);
        cryptoCheck.setSelected(true);
        
        confidenceSlider.setMin(0.0);
        confidenceSlider.setMax(1.0);
        confidenceSlider.setValue(0.0);
        
        confidenceSlider.valueProperty().addListener((obs, old, val) -> 
            confidenceLabel.setText(String.format("Confidence: %.2f", val.doubleValue()))
        );
        
        generateVariantBtn.setDisable(true);
        testCryptoBtn.setDisable(true);
    }
    
    private void initializeSecurityEngine() {
        // Lazy initialization to prevent startup CPU spike
        Platform.runLater(() -> {
            try {
                Map<String, Object> status = securityEngine.getSecurityStatus();
                
                StringBuilder statusText = new StringBuilder("Security Engine Status:\n");
                status.forEach((key, value) -> 
                    statusText.append(String.format("- %s: %s\n", key, value))
                );
                
                detectionResults.setText(statusText.toString());
                securityStatusLabel.setText("Ready");
                
                // Initialize crypto context (lightweight)
                cryptoContext = securityEngine.createCryptoContext();
                testCryptoBtn.setDisable(false);
                
                // Initialize polymorphic context with small dummy code
                byte[] dummyCode = "TEST".getBytes();
                polymorphicContext = securityEngine.createPolymorphicContext(dummyCode);
                generateVariantBtn.setDisable(false);
                
            } catch (Exception e) {
                System.err.println("Security engine initialization failed: " + e.getMessage());
                securityStatusLabel.setText("Initialization Failed");
            }
        });
    }
    
    @FXML
    private void runSecurityDetection() {
        runDetectionBtn.setDisable(true);
        detectionProgress.setProgress(-1); // Indeterminate
        securityStatusLabel.setText("Running Detection...");
        
        detectionResults.appendText("\n=== Starting Security Detection ===\n");
        
        securityEngine.performSecurityDetection().thenAccept(result -> {
            Platform.runLater(() -> {
                displayDetectionResults(result);
                updateDetectionStatus(result);
                
                runDetectionBtn.setDisable(false);
                detectionProgress.setProgress(1.0);
                securityStatusLabel.setText("Detection Complete");
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                detectionResults.appendText("ERROR: " + throwable.getMessage() + "\n");
                runDetectionBtn.setDisable(false);
                detectionProgress.setProgress(0.0);
                securityStatusLabel.setText("Detection Failed");
            });
            return null;
        });
    }
    
    private void displayDetectionResults(DetectionResult result) {
        detectionResults.appendText(String.format("VM Detected: %s\n", result.isVm));
        detectionResults.appendText(String.format("Sandbox Detected: %s\n", result.isSandbox));
        detectionResults.appendText(String.format("Debugger Detected: %s\n", result.isDebugged));
        detectionResults.appendText(String.format("System Monitored: %s\n", result.isMonitored));
        detectionResults.appendText(String.format("Confidence Score: %.2f\n", result.confidenceScore));
        detectionResults.appendText(String.format("Detection Methods: %s\n", result.detectionMethod));
        
        if (result.isMonitored) {
            detectionResults.appendText("⚠️  WARNING: Analysis environment detected!\n");
            securityEngine.performAntiAnalysisDelay();
            detectionResults.appendText("🛡️  Anti-analysis measures activated\n");
        } else {
            detectionResults.appendText("✅ Environment appears clean\n");
        }
        
        detectionResults.appendText("=== Detection Complete ===\n\n");
    }
    
    private void updateDetectionStatus(DetectionResult result) {
        vmDetectionCheck.setSelected(result.isVm);
        sandboxDetectionCheck.setSelected(result.isSandbox);
        debuggerDetectionCheck.setSelected(result.isDebugged);
        
        confidenceSlider.setValue(result.confidenceScore);
        confidenceLabel.setText(String.format("Confidence: %.2f", result.confidenceScore));
    }
    
    @FXML
    private void generatePolymorphicVariant() {
        if (polymorphicContext == null) return;
        
        generateVariantBtn.setDisable(true);
        
        boolean success = securityEngine.generatePolymorphicVariant(polymorphicContext);
        
        if (success) {
            detectionResults.appendText(String.format("Generated polymorphic variant #%d\n", 
                polymorphicContext.getGeneration()));
            detectionResults.appendText(String.format("Variant size: %d bytes\n", 
                polymorphicContext.getCurrentVariant().length));
            
            // Display first 32 bytes as hex
            byte[] variant = polymorphicContext.getCurrentVariant();
            StringBuilder hex = new StringBuilder("Variant preview: ");
            for (int i = 0; i < Math.min(32, variant.length); i++) {
                hex.append(String.format("%02X ", variant[i] & 0xFF));
            }
            if (variant.length > 32) hex.append("...");
            detectionResults.appendText(hex.toString() + "\n\n");
        } else {
            detectionResults.appendText("❌ Failed to generate polymorphic variant\n\n");
        }
        
        generateVariantBtn.setDisable(false);
    }
    
    @FXML
    private void testCryptography() {
        if (cryptoContext == null) return;
        
        testCryptoBtn.setDisable(true);
        
        try {
            String testMessage = "This is a test message for AES-256-GCM encryption";
            byte[] plaintext = testMessage.getBytes();
            byte[] aad = "additional_authenticated_data".getBytes();
            
            detectionResults.appendText("=== Cryptography Test ===\n");
            detectionResults.appendText(String.format("Original: %s\n", testMessage));
            
            byte[] ciphertext = securityEngine.encryptAesGcm(cryptoContext, plaintext, aad);
            
            if (ciphertext != null) {
                StringBuilder hex = new StringBuilder("Encrypted: ");
                for (int i = 0; i < Math.min(32, ciphertext.length); i++) {
                    hex.append(String.format("%02X ", ciphertext[i] & 0xFF));
                }
                if (ciphertext.length > 32) hex.append("...");
                detectionResults.appendText(hex.toString() + "\n");
                
                byte[] tag = cryptoContext.getTag();
                if (tag != null) {
                    StringBuilder tagHex = new StringBuilder("Auth Tag: ");
                    for (byte b : tag) {
                        tagHex.append(String.format("%02X ", b & 0xFF));
                    }
                    detectionResults.appendText(tagHex.toString() + "\n");
                }
                
                detectionResults.appendText("✅ Encryption successful\n");
            } else {
                detectionResults.appendText("❌ Encryption failed\n");
            }
            
            detectionResults.appendText("=== Crypto Test Complete ===\n\n");
            
        } catch (Exception e) {
            detectionResults.appendText("❌ Crypto test error: " + e.getMessage() + "\n\n");
        }
        
        testCryptoBtn.setDisable(false);
    }
    
    @FXML
    private void clearLog() {
        detectionResults.clear();
        detectionProgress.setProgress(0.0);
        securityStatusLabel.setText("Ready");
        
        // Reset checkboxes
        vmDetectionCheck.setSelected(false);
        sandboxDetectionCheck.setSelected(false);
        debuggerDetectionCheck.setSelected(false);
        
        confidenceSlider.setValue(0.0);
        confidenceLabel.setText("Confidence: 0.00");
    }
}