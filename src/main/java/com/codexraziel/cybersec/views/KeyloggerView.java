package com.codexraziel.cybersec.views;

import com.codexraziel.cybersec.controllers.KeyloggerController;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.codexraziel.cybersec.ui.CodexIcons;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

@Component
public class KeyloggerView {
    
    @Autowired
    private KeyloggerController controller;
    
    public VBox createView(Label statusLabel) {
        VBox keyloggerContent = new VBox(10);
        
        // Configuration panel
        GridPane configGrid = new GridPane();
        configGrid.setHgap(10);
        configGrid.setVgap(5);
        
        ComboBox<String> platformCombo = new ComboBox<>();
        platformCombo.getItems().addAll("Linux", "Windows", "macOS");
        platformCombo.setValue("Linux");
        
        TextField outputPath = new TextField("/tmp/keylogger.log");
        CheckBox stealthMode = new CheckBox("Stealth Mode");
        CheckBox encryption = new CheckBox("Encrypt Output");
        stealthMode.setSelected(true);
        encryption.setSelected(true);
        
        configGrid.add(new Label("Platform:"), 0, 0);
        configGrid.add(platformCombo, 1, 0);
        configGrid.add(new Label("Output:"), 0, 1);
        configGrid.add(outputPath, 1, 1);
        configGrid.add(stealthMode, 0, 2);
        configGrid.add(encryption, 1, 2);
        
        // Control buttons
        HBox controls = new HBox(10);
        Button startBtn = new Button("Start Keylogger");
        startBtn.setGraphic(CodexIcons.START);
        Button stopBtn = new Button("Stop");
        stopBtn.setGraphic(CodexIcons.STOP);
        Button clearBtn = new Button("Clear");
        clearBtn.setGraphic(FontIcon.of(FontAwesome.TRASH, 14));
        ProgressBar keyloggerProgress = new ProgressBar(0);
        keyloggerProgress.setPrefWidth(150);
        
        controls.getChildren().addAll(startBtn, stopBtn, clearBtn, keyloggerProgress);
        
        // Log area
        TextArea logArea = new TextArea();
        logArea.setPrefRowCount(15);
        logArea.setPromptText("Keylogger output will appear here...");
        
        // Bind components to controller
        controller.bindComponents(platformCombo, outputPath, stealthMode, encryption,
                                startBtn, stopBtn, keyloggerProgress, logArea, statusLabel);
        
        // Additional event handlers
        clearBtn.setOnAction(e -> controller.clearLog());
        stopBtn.setDisable(true);
        
        Label configLabel = new Label("Configuration:");
        configLabel.setGraphic(CodexIcons.TOOLS);
        Label controlsLabel = new Label("Controls:");
        controlsLabel.setGraphic(CodexIcons.KEYLOGGER);
        Label outputLabel = new Label("Output:");
        outputLabel.setGraphic(CodexIcons.INFO);
        
        keyloggerContent.getChildren().addAll(
            configLabel,
            configGrid,
            new Separator(),
            controlsLabel,
            controls,
            new Separator(),
            outputLabel,
            logArea
        );
        
        return keyloggerContent;
    }
}