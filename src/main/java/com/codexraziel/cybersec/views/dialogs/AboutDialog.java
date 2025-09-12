package com.codexraziel.cybersec.views.dialogs;

import com.codexraziel.cybersec.ui.CodexIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class AboutDialog {
    
    private final Stage parentStage;
    private Stage dialogStage;
    
    // Application information
    private static final String APP_NAME = "Codex Raziel CS";
    private static final String APP_VERSION = "1.0.0";
    private static final String APP_BUILD = "20250911-001";
    private static final String APP_DESCRIPTION = "Advanced Cybersecurity Testing Suite";
    private static final String COMPANY_NAME = "Codex Raziel Security";
    private static final String COPYRIGHT = "© 2025 Codex Raziel Security. All rights reserved.";
    private static final String WEBSITE = "https://github.com/felipemacedo1/cyber-security-suite";
    private static final String LICENSE = "Educational Use License";
    
    public AboutDialog(Stage parentStage) {
        this.parentStage = parentStage;
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
        dialogStage.setTitle("About " + APP_NAME);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(parentStage);
        dialogStage.setResizable(false);
        
        // Create main layout
        BorderPane mainPane = new BorderPane();
        mainPane.setPrefSize(500, 600);
        
        // Header with logo and title
        VBox header = createHeader();
        mainPane.setTop(header);
        
        // Content with tabs
        TabPane tabPane = createContentTabs();
        mainPane.setCenter(tabPane);
        
        // Footer with buttons
        HBox footer = createFooter();
        mainPane.setBottom(footer);
        
        Scene scene = new Scene(mainPane);
        dialogStage.setScene(scene);
        
        // Center on parent
        dialogStage.setX(parentStage.getX() + (parentStage.getWidth() - 500) / 2);
        dialogStage.setY(parentStage.getY() + (parentStage.getHeight() - 600) / 2);
    }
    
    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: linear-gradient(to bottom, #007bff, #0056b3); " +
                       "-fx-text-fill: white;");
        
        // Application icon (using FontIcon as placeholder)
        FontIcon appIcon = FontIcon.of(FontAwesome.SHIELD, 48);
        appIcon.setStyle("-fx-fill: white;");
        
        // Application name
        Label appName = new Label(APP_NAME);
        appName.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        appName.setStyle("-fx-text-fill: white;");
        
        // Application description
        Label appDesc = new Label(APP_DESCRIPTION);
        appDesc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        appDesc.setStyle("-fx-text-fill: #e3f2fd;");
        
        // Version info
        Label versionInfo = new Label("Version " + APP_VERSION + " (Build " + APP_BUILD + ")");
        versionInfo.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        versionInfo.setStyle("-fx-text-fill: #e3f2fd;");
        
        header.getChildren().addAll(appIcon, appName, appDesc, versionInfo);
        
        return header;
    }
    
    private TabPane createContentTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPadding(new Insets(10));
        
        // About Tab
        Tab aboutTab = new Tab("About");
        aboutTab.setGraphic(FontIcon.of(FontAwesome.INFO_CIRCLE, 14));
        aboutTab.setContent(createAboutTab());
        
        // Features Tab
        Tab featuresTab = new Tab("Features");
        featuresTab.setGraphic(FontIcon.of(FontAwesome.LIST, 14));
        featuresTab.setContent(createFeaturesTab());
        
        // System Info Tab
        Tab systemTab = new Tab("System");
        systemTab.setGraphic(FontIcon.of(FontAwesome.DESKTOP, 14));
        systemTab.setContent(createSystemTab());
        
        // Credits Tab
        Tab creditsTab = new Tab("Credits");
        creditsTab.setGraphic(FontIcon.of(FontAwesome.USERS, 14));
        creditsTab.setContent(createCreditsTab());
        
        // Legal Tab
        Tab legalTab = new Tab("Legal");
        legalTab.setGraphic(FontIcon.of(FontAwesome.LEGAL, 14));
        legalTab.setContent(createLegalTab());
        
        tabPane.getTabs().addAll(aboutTab, featuresTab, systemTab, creditsTab, legalTab);
        
        return tabPane;
    }
    
    private ScrollPane createAboutTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Company info
        VBox companySection = new VBox(10);
        companySection.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                               "-fx-border-radius: 5; -fx-padding: 15;");
        
        Label companyTitle = new Label("About " + COMPANY_NAME);
        companyTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        TextArea companyDesc = new TextArea();
        companyDesc.setText(
            "Codex Raziel Security is dedicated to advancing cybersecurity education and " +
            "authorized security testing. Our tools are designed for security professionals, " +
            "researchers, and students who need to understand and test network security.\n\n" +
            "This application provides a comprehensive suite of security testing tools " +
            "including WiFi penetration testing, network reconnaissance, and security analysis " +
            "capabilities. All tools are designed with education and authorized testing in mind."
        );
        companyDesc.setWrapText(true);
        companyDesc.setEditable(false);
        companyDesc.setPrefHeight(120);
        companyDesc.setStyle("-fx-control-inner-background: #f8f9fa; -fx-background-color: #f8f9fa;");
        
        companySection.getChildren().addAll(companyTitle, companyDesc);
        
        // Contact info
        VBox contactSection = new VBox(10);
        contactSection.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                               "-fx-border-radius: 5; -fx-padding: 15;");
        
        Label contactTitle = new Label("Contact & Support");
        contactTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        VBox contactList = new VBox(8);
        
        Hyperlink websiteLink = new Hyperlink("🌐 Project Website");
        websiteLink.setOnAction(e -> openWebsite(WEBSITE));
        
        Hyperlink githubLink = new Hyperlink("📁 GitHub Repository");
        githubLink.setOnAction(e -> openWebsite("https://github.com/felipemacedo1"));
        
        Hyperlink issuesLink = new Hyperlink("🐛 Report Issues");
        issuesLink.setOnAction(e -> openWebsite(WEBSITE + "/issues"));
        
        Hyperlink docsLink = new Hyperlink("📖 Documentation");
        docsLink.setOnAction(e -> openWebsite(WEBSITE + "/wiki"));
        
        contactList.getChildren().addAll(websiteLink, githubLink, issuesLink, docsLink);
        contactSection.getChildren().addAll(contactTitle, contactList);
        
        content.getChildren().addAll(companySection, contactSection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private ScrollPane createFeaturesTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label title = new Label("Application Features");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        VBox featuresBox = new VBox(12);
        
        String[] features = {
            "🔐 Advanced WiFi Penetration Testing",
            "🖥️ Cross-Platform Keylogger (Educational)",
            "⚡ Multi-Protocol Brute Force Testing",
            "🌐 Network Reconnaissance & Scanning",
            "📊 Real-time Attack Progress Monitoring",
            "🔍 Vulnerability Assessment Tools",
            "📈 Comprehensive Reporting System",
            "🛡️ Security-First Design Approach",
            "⚙️ Highly Configurable Settings",
            "🎯 Professional Attack Workflows",
            "📝 Detailed Logging & Audit Trails",
            "🔧 Extensible Plugin Architecture"
        };
        
        for (String feature : features) {
            Label featureLabel = new Label(feature);
            featureLabel.setStyle("-fx-font-size: 13px; -fx-padding: 5 0 5 10;");
            featuresBox.getChildren().add(featureLabel);
        }
        
        VBox featuresSection = new VBox(10);
        featuresSection.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                                "-fx-border-radius: 5; -fx-padding: 15;");
        featuresSection.getChildren().addAll(title, featuresBox);
        
        // Disclaimer
        HBox disclaimerBox = new HBox(10);
        disclaimerBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; " +
                              "-fx-border-radius: 5; -fx-padding: 10;");
        
        FontIcon warningIcon = FontIcon.of(FontAwesome.EXCLAMATION_TRIANGLE, 16);
        warningIcon.setStyle("-fx-fill: #856404;");
        
        Label disclaimerText = new Label("All features are intended for educational purposes and " +
                                       "authorized security testing only. Ensure you have proper " +
                                       "authorization before using any testing tools.");
        disclaimerText.setWrapText(true);
        disclaimerText.setStyle("-fx-text-fill: #856404; -fx-font-size: 12px;");
        
        disclaimerBox.getChildren().addAll(warningIcon, disclaimerText);
        
        content.getChildren().addAll(featuresSection, disclaimerBox);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private ScrollPane createSystemTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // System Information
        VBox systemSection = new VBox(10);
        systemSection.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                              "-fx-border-radius: 5; -fx-padding: 15;");
        
        Label systemTitle = new Label("System Information");
        systemTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        GridPane systemGrid = new GridPane();
        systemGrid.setHgap(15);
        systemGrid.setVgap(8);
        systemGrid.setPadding(new Insets(10, 0, 0, 0));
        
        addSystemInfo(systemGrid, 0, "Operating System:", System.getProperty("os.name"));
        addSystemInfo(systemGrid, 1, "OS Version:", System.getProperty("os.version"));
        addSystemInfo(systemGrid, 2, "Architecture:", System.getProperty("os.arch"));
        addSystemInfo(systemGrid, 3, "Java Version:", System.getProperty("java.version"));
        addSystemInfo(systemGrid, 4, "Java Vendor:", System.getProperty("java.vendor"));
        addSystemInfo(systemGrid, 5, "JavaFX Version:", System.getProperty("javafx.version", "Unknown"));
        addSystemInfo(systemGrid, 6, "User Name:", System.getProperty("user.name"));
        addSystemInfo(systemGrid, 7, "User Home:", System.getProperty("user.home"));
        addSystemInfo(systemGrid, 8, "Working Directory:", System.getProperty("user.dir"));
        
        // Runtime Information
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        
        addSystemInfo(systemGrid, 9, "Available Processors:", String.valueOf(runtime.availableProcessors()));
        addSystemInfo(systemGrid, 10, "Max Memory:", maxMemory + " MB");
        addSystemInfo(systemGrid, 11, "Total Memory:", totalMemory + " MB");
        addSystemInfo(systemGrid, 12, "Used Memory:", usedMemory + " MB");
        addSystemInfo(systemGrid, 13, "Free Memory:", freeMemory + " MB");
        
        // Application Information
        addSystemInfo(systemGrid, 14, "Application Version:", APP_VERSION);
        addSystemInfo(systemGrid, 15, "Build Number:", APP_BUILD);
        addSystemInfo(systemGrid, 16, "Build Date:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        systemSection.getChildren().addAll(systemTitle, systemGrid);
        content.getChildren().add(systemSection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private void addSystemInfo(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 150px;");
        
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #495057;");
        
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }
    
    private ScrollPane createCreditsTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Development Team
        VBox teamSection = createCreditSection("Development Team", new String[]{
            "Felipe Macedo - Lead Developer & Security Researcher",
            "Codex Raziel Team - Security Architecture & Design",
            "Open Source Contributors - Various Enhancements"
        });
        
        // Libraries and Frameworks
        VBox libSection = createCreditSection("Libraries & Frameworks", new String[]{
            "JavaFX - Modern UI Framework",
            "Spring Boot - Application Framework",
            "Ikonli - Icon Management",
            "Lombok - Code Generation",
            "SLF4J - Logging Framework",
            "Apache Commons - Utility Libraries",
            "Jackson - JSON Processing"
        });
        
        // Security Tools Integration
        VBox toolsSection = createCreditSection("Security Tools & Research", new String[]{
            "Aircrack-ng Suite - WiFi Security Testing",
            "Wireshark - Network Protocol Analysis",
            "OWASP - Security Guidelines & Best Practices",
            "CVE Database - Vulnerability Information",
            "Security Research Community - Methodologies"
        });
        
        // Special Thanks
        VBox thanksSection = createCreditSection("Special Thanks", new String[]{
            "Security Research Community",
            "Open Source Contributors",
            "Educational Institutions",
            "Cybersecurity Professionals",
            "Beta Testers & Feedback Providers"
        });
        
        content.getChildren().addAll(teamSection, libSection, toolsSection, thanksSection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private VBox createCreditSection(String title, String[] items) {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 5; -fx-padding: 15;");
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        VBox itemsBox = new VBox(5);
        for (String item : items) {
            Label itemLabel = new Label("• " + item);
            itemLabel.setStyle("-fx-font-size: 12px; -fx-padding: 2 0 2 10;");
            itemsBox.getChildren().add(itemLabel);
        }
        
        section.getChildren().addAll(titleLabel, itemsBox);
        
        return section;
    }
    
    private ScrollPane createLegalTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Copyright
        VBox copyrightSection = new VBox(10);
        copyrightSection.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                                 "-fx-border-radius: 5; -fx-padding: 15;");
        
        Label copyrightTitle = new Label("Copyright Information");
        copyrightTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Label copyrightText = new Label(COPYRIGHT);
        copyrightText.setStyle("-fx-font-size: 12px;");
        
        copyrightSection.getChildren().addAll(copyrightTitle, copyrightText);
        
        // License
        VBox licenseSection = new VBox(10);
        licenseSection.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                               "-fx-border-radius: 5; -fx-padding: 15;");
        
        Label licenseTitle = new Label("License Agreement");
        licenseTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        TextArea licenseText = new TextArea();
        licenseText.setText(
            "EDUCATIONAL USE LICENSE\n\n" +
            "This software is provided for educational and authorized security testing purposes only. " +
            "By using this software, you agree to the following terms:\n\n" +
            "1. AUTHORIZED USE ONLY: You may only use this software on networks, systems, and " +
            "devices that you own or have explicit written permission to test.\n\n" +
            "2. EDUCATIONAL PURPOSE: This software is intended for learning about cybersecurity " +
            "concepts and authorized penetration testing methodologies.\n\n" +
            "3. NO MALICIOUS USE: You may not use this software for any illegal, unauthorized, " +
            "or malicious activities.\n\n" +
            "4. LIABILITY: The developers and contributors are not responsible for any damage, " +
            "legal consequences, or misuse of this software.\n\n" +
            "5. COMPLIANCE: You are solely responsible for ensuring your use of this software " +
            "complies with all applicable laws and regulations.\n\n" +
            "6. NO WARRANTY: This software is provided 'as is' without any warranties or guarantees.\n\n" +
            "By using this software, you acknowledge that you have read, understood, and agree " +
            "to be bound by these terms."
        );
        licenseText.setWrapText(true);
        licenseText.setEditable(false);
        licenseText.setPrefHeight(250);
        licenseText.setStyle("-fx-control-inner-background: #f8f9fa; -fx-font-size: 11px;");
        
        licenseSection.getChildren().addAll(licenseTitle, licenseText);
        
        content.getChildren().addAll(copyrightSection, licenseSection);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15));
        footer.setStyle("-fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        
        Button systemInfoBtn = new Button("System Info");
        systemInfoBtn.setGraphic(FontIcon.of(FontAwesome.INFO, 12));
        systemInfoBtn.setOnAction(e -> showSystemInfo());
        
        Button closeBtn = new Button("Close");
        closeBtn.setGraphic(FontIcon.of(FontAwesome.TIMES, 12));
        closeBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> dialogStage.close());
        
        footer.getChildren().addAll(systemInfoBtn, closeBtn);
        
        return footer;
    }
    
    private void openWebsite(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback for systems without Desktop support
                log.warn("Desktop not supported, cannot open URL: {}", url);
            }
        } catch (Exception e) {
            log.error("Failed to open website: {}", url, e);
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Cannot open website");
            alert.setContentText("Failed to open: " + url + "\n\nPlease copy the URL manually.");
            alert.showAndWait();
        }
    }
    
    private void showSystemInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Information");
        alert.setHeaderText("Quick System Overview");
        
        String systemInfo = String.format(
            "Application: %s v%s\n" +
            "Build: %s\n" +
            "Java: %s\n" +
            "OS: %s %s\n" +
            "Memory: %d MB available",
            APP_NAME, APP_VERSION, APP_BUILD,
            System.getProperty("java.version"),
            System.getProperty("os.name"), System.getProperty("os.version"),
            Runtime.getRuntime().maxMemory() / (1024 * 1024)
        );
        
        alert.setContentText(systemInfo);
        alert.showAndWait();
    }
}
