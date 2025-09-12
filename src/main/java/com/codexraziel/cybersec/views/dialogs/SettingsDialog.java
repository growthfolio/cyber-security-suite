package com.codexraziel.cybersec.views.dialogs;

import com.codexraziel.cybersec.ui.CodexIcons;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.prefs.Preferences;

@Slf4j
public class SettingsDialog {
    
    private final Stage parentStage;
    private Stage dialogStage;
    
    // Settings properties
    private final BooleanProperty darkModeEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty autoSaveEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty soundEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty autoUpdateEnabled = new SimpleBooleanProperty(true);
    private final StringProperty outputDirectory = new SimpleStringProperty(System.getProperty("user.home") + "/codex-output");
    private final StringProperty logLevel = new SimpleStringProperty("INFO");
    private final StringProperty networkInterface = new SimpleStringProperty("auto");
    private final StringProperty proxyHost = new SimpleStringProperty("");
    private final StringProperty proxyPort = new SimpleStringProperty("8080");
    private final BooleanProperty proxyEnabled = new SimpleBooleanProperty(false);
    
    private final Preferences preferences = Preferences.userNodeForPackage(SettingsDialog.class);
    
    public SettingsDialog(Stage parentStage) {
        this.parentStage = parentStage;
        loadSettings();
    }
    
    public void showDialog() {
        if (dialogStage != null) {
            dialogStage.toFront();
            return;
        }
        
        createDialog();
        dialogStage.showAndWait();
    }
    
    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.setTitle("Application Settings");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(parentStage);
        dialogStage.setResizable(true);
        dialogStage.setMinWidth(600);
        dialogStage.setMinHeight(500);
        
        // Create main layout
        BorderPane mainPane = new BorderPane();
        mainPane.setPadding(new Insets(10));
        
        // Header
        HBox header = createHeader();
        mainPane.setTop(header);
        
        // Content tabs
        TabPane tabPane = createContentTabs();
        mainPane.setCenter(tabPane);
        
        // Footer buttons
        HBox footer = createFooter();
        mainPane.setBottom(footer);
        
        Scene scene = new Scene(mainPane, 700, 600);
        dialogStage.setScene(scene);
        
        // Center on parent
        dialogStage.setX(parentStage.getX() + (parentStage.getWidth() - 700) / 2);
        dialogStage.setY(parentStage.getY() + (parentStage.getHeight() - 600) / 2);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        header.setStyle("-fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");
        
        FontIcon settingsIcon = FontIcon.of(FontAwesome.COG, 32);
        settingsIcon.setStyle("-fx-fill: #007bff;");
        
        VBox titleBox = new VBox(5);
        Label title = new Label("Application Settings");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        Label subtitle = new Label("Configure application preferences and behavior");
        subtitle.setStyle("-fx-text-fill: #6c757d;");
        
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(settingsIcon, titleBox);
        
        return header;
    }
    
    private TabPane createContentTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // General Tab
        Tab generalTab = new Tab("General");
        generalTab.setGraphic(FontIcon.of(FontAwesome.HOME, 14));
        generalTab.setContent(createGeneralTab());
        
        // Appearance Tab
        Tab appearanceTab = new Tab("Appearance");
        appearanceTab.setGraphic(FontIcon.of(FontAwesome.PAINT_BRUSH, 14));
        appearanceTab.setContent(createAppearanceTab());
        
        // Network Tab
        Tab networkTab = new Tab("Network");
        networkTab.setGraphic(FontIcon.of(FontAwesome.GLOBE, 14));
        networkTab.setContent(createNetworkTab());
        
        // Security Tab
        Tab securityTab = new Tab("Security");
        securityTab.setGraphic(CodexIcons.SECURITY);
        securityTab.setContent(createSecurityTab());
        
        // Advanced Tab
        Tab advancedTab = new Tab("Advanced");
        advancedTab.setGraphic(FontIcon.of(FontAwesome.COG, 14));
        advancedTab.setContent(createAdvancedTab());
        
        tabPane.getTabs().addAll(generalTab, appearanceTab, networkTab, securityTab, advancedTab);
        
        return tabPane;
    }
    
    private ScrollPane createGeneralTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // Output Directory Section
        VBox outputSection = createSection("Output Directory", 
            "Configure where attack results and logs are saved");
        
        HBox outputBox = new HBox(10);
        outputBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField outputField = new TextField();
        outputField.textProperty().bindBidirectional(outputDirectory);
        outputField.setPrefWidth(350);
        
        Button browseBtn = new Button("Browse");
        browseBtn.setGraphic(FontIcon.of(FontAwesome.FOLDER_OPEN, 12));
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Output Directory");
            chooser.setInitialDirectory(new File(outputDirectory.get()));
            File selected = chooser.showDialog(dialogStage);
            if (selected != null) {
                outputDirectory.set(selected.getAbsolutePath());
            }
        });
        
        outputBox.getChildren().addAll(outputField, browseBtn);
        outputSection.getChildren().add(outputBox);
        
        // Auto-save Section
        VBox autoSaveSection = createSection("Auto-save", 
            "Automatically save results and configurations");
        
        CheckBox autoSaveCheck = new CheckBox("Enable auto-save");
        autoSaveCheck.selectedProperty().bindBidirectional(autoSaveEnabled);
        autoSaveSection.getChildren().add(autoSaveCheck);
        
        // Sound Section
        VBox soundSection = createSection("Audio Feedback", 
            "Enable sound notifications and alerts");
        
        CheckBox soundCheck = new CheckBox("Enable sound effects");
        soundCheck.selectedProperty().bindBidirectional(soundEnabled);
        soundSection.getChildren().add(soundCheck);
        
        // Auto-update Section
        VBox updateSection = createSection("Updates", 
            "Automatically check for application updates");
        
        CheckBox updateCheck = new CheckBox("Check for updates automatically");
        updateCheck.selectedProperty().bindBidirectional(autoUpdateEnabled);
        updateSection.getChildren().add(updateCheck);
        
        content.getChildren().addAll(outputSection, autoSaveSection, soundSection, updateSection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private ScrollPane createAppearanceTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // Theme Section
        VBox themeSection = createSection("Theme", 
            "Choose application color scheme and appearance");
        
        CheckBox darkModeCheck = new CheckBox("Enable dark mode");
        darkModeCheck.selectedProperty().bindBidirectional(darkModeEnabled);
        themeSection.getChildren().add(darkModeCheck);
        
        Label themeNote = new Label("Note: Theme changes require application restart");
        themeNote.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
        themeSection.getChildren().add(themeNote);
        
        content.getChildren().add(themeSection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private ScrollPane createNetworkTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // Network Interface Section
        VBox interfaceSection = createSection("Network Interface", 
            "Select default network interface for operations");
        
        ComboBox<String> interfaceCombo = new ComboBox<>();
        interfaceCombo.getItems().addAll("auto", "wlan0", "wlan1", "eth0", "eth1");
        interfaceCombo.valueProperty().bindBidirectional(networkInterface);
        interfaceCombo.setPrefWidth(200);
        interfaceSection.getChildren().add(interfaceCombo);
        
        // Proxy Settings Section
        VBox proxySection = createSection("Proxy Settings", 
            "Configure proxy server for network operations");
        
        CheckBox proxyCheck = new CheckBox("Enable proxy");
        proxyCheck.selectedProperty().bindBidirectional(proxyEnabled);
        
        GridPane proxyGrid = new GridPane();
        proxyGrid.setHgap(10);
        proxyGrid.setVgap(10);
        proxyGrid.setPadding(new Insets(10, 0, 0, 20));
        
        Label hostLabel = new Label("Host:");
        TextField hostField = new TextField();
        hostField.textProperty().bindBidirectional(proxyHost);
        hostField.disableProperty().bind(proxyEnabled.not());
        
        Label portLabel = new Label("Port:");
        TextField portField = new TextField();
        portField.textProperty().bindBidirectional(proxyPort);
        portField.disableProperty().bind(proxyEnabled.not());
        portField.setPrefWidth(100);
        
        proxyGrid.add(hostLabel, 0, 0);
        proxyGrid.add(hostField, 1, 0);
        proxyGrid.add(portLabel, 2, 0);
        proxyGrid.add(portField, 3, 0);
        
        proxySection.getChildren().addAll(proxyCheck, proxyGrid);
        
        content.getChildren().addAll(interfaceSection, proxySection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private ScrollPane createSecurityTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // Security Notice
        HBox warningBox = new HBox(10);
        warningBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; " +
                           "-fx-border-radius: 5; -fx-padding: 10;");
        
        FontIcon warningIcon = FontIcon.of(FontAwesome.EXCLAMATION_TRIANGLE, 16);
        warningIcon.setStyle("-fx-fill: #856404;");
        
        Label warningText = new Label("This application contains security testing tools. " +
                                    "Use only on networks you own or have explicit permission to test.");
        warningText.setWrapText(true);
        warningText.setStyle("-fx-text-fill: #856404;");
        
        warningBox.getChildren().addAll(warningIcon, warningText);
        
        // Legal Disclaimer
        VBox disclaimerSection = createSection("Legal Disclaimer", 
            "Important information about legal usage");
        
        TextArea disclaimerText = new TextArea();
        disclaimerText.setText(
            "IMPORTANT: This software is intended for educational purposes and authorized " +
            "security testing only. The user is solely responsible for complying with all " +
            "applicable laws and regulations. Unauthorized access to computer networks is " +
            "illegal in most jurisdictions.\n\n" +
            "By using this software, you acknowledge that:\n" +
            "• You will only use it on networks you own or have explicit written permission to test\n" +
            "• You understand the legal implications of network security testing\n" +
            "• You will not use this software for malicious purposes\n" +
            "• The developers are not responsible for any misuse of this software"
        );
        disclaimerText.setWrapText(true);
        disclaimerText.setEditable(false);
        disclaimerText.setPrefHeight(200);
        disclaimerText.setStyle("-fx-control-inner-background: #f8f9fa;");
        
        disclaimerSection.getChildren().add(disclaimerText);
        
        content.getChildren().addAll(warningBox, disclaimerSection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private ScrollPane createAdvancedTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // Logging Section
        VBox loggingSection = createSection("Logging", 
            "Configure application logging level and behavior");
        
        HBox logLevelBox = new HBox(10);
        logLevelBox.setAlignment(Pos.CENTER_LEFT);
        
        Label logLevelLabel = new Label("Log Level:");
        ComboBox<String> logLevelCombo = new ComboBox<>();
        logLevelCombo.getItems().addAll("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
        logLevelCombo.valueProperty().bindBidirectional(logLevel);
        logLevelCombo.setPrefWidth(150);
        
        logLevelBox.getChildren().addAll(logLevelLabel, logLevelCombo);
        loggingSection.getChildren().add(logLevelBox);
        
        // Reset Section
        VBox resetSection = createSection("Reset", 
            "Reset application settings to defaults");
        
        Button resetBtn = new Button("Reset to Defaults");
        resetBtn.setGraphic(FontIcon.of(FontAwesome.REFRESH, 12));
        resetBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        resetBtn.setOnAction(e -> resetToDefaults());
        
        resetSection.getChildren().add(resetBtn);
        
        content.getChildren().addAll(loggingSection, resetSection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private VBox createSection(String title, String description) {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 5; -fx-padding: 15;");
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
        descLabel.setWrapText(true);
        
        section.getChildren().addAll(titleLabel, descLabel);
        
        return section;
    }
    
    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(20, 0, 0, 0));
        footer.setStyle("-fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        
        Button resetBtn = new Button("Reset");
        resetBtn.setGraphic(FontIcon.of(FontAwesome.REFRESH, 12));
        resetBtn.setOnAction(e -> resetToDefaults());
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setGraphic(FontIcon.of(FontAwesome.TIMES, 12));
        cancelBtn.setOnAction(e -> dialogStage.close());
        
        Button applyBtn = new Button("Apply");
        applyBtn.setGraphic(FontIcon.of(FontAwesome.CHECK, 12));
        applyBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        applyBtn.setOnAction(e -> applySettings());
        
        Button okBtn = new Button("OK");
        okBtn.setGraphic(FontIcon.of(FontAwesome.CHECK, 12));
        okBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        okBtn.setOnAction(e -> {
            applySettings();
            dialogStage.close();
        });
        
        footer.getChildren().addAll(resetBtn, cancelBtn, applyBtn, okBtn);
        
        return footer;
    }
    
    private void loadSettings() {
        darkModeEnabled.set(preferences.getBoolean("darkMode", false));
        autoSaveEnabled.set(preferences.getBoolean("autoSave", true));
        soundEnabled.set(preferences.getBoolean("sound", true));
        autoUpdateEnabled.set(preferences.getBoolean("autoUpdate", true));
        outputDirectory.set(preferences.get("outputDirectory", System.getProperty("user.home") + "/codex-output"));
        logLevel.set(preferences.get("logLevel", "INFO"));
        networkInterface.set(preferences.get("networkInterface", "auto"));
        proxyHost.set(preferences.get("proxyHost", ""));
        proxyPort.set(preferences.get("proxyPort", "8080"));
        proxyEnabled.set(preferences.getBoolean("proxyEnabled", false));
    }
    
    private void applySettings() {
        try {
            preferences.putBoolean("darkMode", darkModeEnabled.get());
            preferences.putBoolean("autoSave", autoSaveEnabled.get());
            preferences.putBoolean("sound", soundEnabled.get());
            preferences.putBoolean("autoUpdate", autoUpdateEnabled.get());
            preferences.put("outputDirectory", outputDirectory.get());
            preferences.put("logLevel", logLevel.get());
            preferences.put("networkInterface", networkInterface.get());
            preferences.put("proxyHost", proxyHost.get());
            preferences.put("proxyPort", proxyPort.get());
            preferences.putBoolean("proxyEnabled", proxyEnabled.get());
            
            preferences.flush();
            
            log.info("Settings applied successfully");
            
            // Show confirmation
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Settings Applied");
            alert.setHeaderText(null);
            alert.setContentText("Settings have been saved successfully.\nSome changes may require application restart.");
            alert.showAndWait();
            
        } catch (Exception e) {
            log.error("Failed to save settings", e);
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save settings");
            alert.setContentText("An error occurred while saving settings: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void resetToDefaults() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Reset Settings");
        confirmation.setHeaderText("Reset to Default Settings");
        confirmation.setContentText("Are you sure you want to reset all settings to their default values?");
        
        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                preferences.clear();
                loadSettings();
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Settings Reset");
                success.setHeaderText(null);
                success.setContentText("All settings have been reset to default values.");
                success.showAndWait();
                
            } catch (Exception e) {
                log.error("Failed to reset settings", e);
                
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to reset settings");
                alert.setContentText("An error occurred while resetting settings: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
