package com.codexraziel.cybersec.ui;

import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.paint.Color;

public class CodexIcons {
    
    // Security Icons
    public static final FontIcon SECURITY = createIcon(FontAwesome.SHIELD, 16, "#00FF00");
    public static final FontIcon VULNERABILITY = createIcon(FontAwesome.UNLOCK, 16, "#FF4444");
    public static final FontIcon SUSPICIOUS = createIcon(FontAwesome.EXCLAMATION_TRIANGLE, 16, "#FFA500");
    public static final FontIcon SCANNING = createIcon(FontAwesome.WIFI, 16, "#00BFFF");
    
    // Tool Icons
    public static final FontIcon WIFI = createIcon(FontAwesome.WIFI, 16, "#FFFFFF");
    public static final FontIcon KEYLOGGER = createIcon(FontAwesome.TERMINAL, 16, "#FFFFFF");
    public static final FontIcon BRUTEFORCE = createIcon(FontAwesome.UNLOCK, 16, "#FFFFFF");
    public static final FontIcon PERFORMANCE = createIcon(FontAwesome.BAR_CHART, 16, "#FFFFFF");
    public static final FontIcon TOOLS = createIcon(FontAwesome.WRENCH, 16, "#FFFFFF");
    public static final FontIcon RESULTS = createIcon(FontAwesome.LINE_CHART, 16, "#FFFFFF");
    
    // Action Icons
    public static final FontIcon START = createIcon(FontAwesome.PLAY, 14, "#00FF00");
    public static final FontIcon PAUSE = createIcon(FontAwesome.PAUSE, 14, "#FFA500");
    public static final FontIcon STOP = createIcon(FontAwesome.STOP, 14, "#FF4444");
    public static final FontIcon REFRESH = createIcon(FontAwesome.REFRESH, 14, "#00BFFF");
    public static final FontIcon SEARCH = createIcon(FontAwesome.SEARCH, 14, "#FFFFFF");
    public static final FontIcon DOWNLOAD = createIcon(FontAwesome.DOWNLOAD, 14, "#FFFFFF");
    public static final FontIcon FOLDER = createIcon(FontAwesome.FOLDER, 14, "#FFFFFF");
    
    // Icon constants for workflow
    public static final String PLAY_ICON = "PLAY";
    public static final String PAUSE_ICON = "PAUSE";
    public static final String STOP_ICON = "STOP";
    
    // Status Icons
    public static final FontIcon SUCCESS = createIcon(FontAwesome.CHECK_CIRCLE, 12, "#00FF00");
    public static final FontIcon WARNING = createIcon(FontAwesome.EXCLAMATION_TRIANGLE, 12, "#FFA500");
    public static final FontIcon ERROR = createIcon(FontAwesome.TIMES_CIRCLE, 12, "#FF4444");
    public static final FontIcon INFO = createIcon(FontAwesome.INFO_CIRCLE, 12, "#00BFFF");
    
    private static FontIcon createIcon(FontAwesome iconCode, int size, String color) {
        return FontIcon.of(iconCode, size, Color.web(color));
    }
    
    public static FontIcon createSecurityIcon(int size, Color color) {
        return FontIcon.of(FontAwesome.SHIELD, size, color);
    }
    
    public static FontIcon createIcon(String iconName) {
        return switch (iconName) {
            case "PLAY" -> createIcon(FontAwesome.PLAY, 14, "#00FF00");
            case "PAUSE" -> createIcon(FontAwesome.PAUSE, 14, "#FFA500");
            case "STOP" -> createIcon(FontAwesome.STOP, 14, "#FF4444");
            default -> createIcon(FontAwesome.CIRCLE, 14, "#FFFFFF");
        };
    }
    
    public static FontIcon createNetworkStatusIcon(String status, int size) {
        FontAwesome iconCode;
        Color color;
        
        switch (status.toUpperCase()) {
            case "SECURE":
                iconCode = FontAwesome.CHECK_CIRCLE;
                color = Color.web("#00FF00");
                break;
            case "SUSPICIOUS":
                iconCode = FontAwesome.EXCLAMATION_TRIANGLE;
                color = Color.web("#FFA500");
                break;
            case "VULNERABLE":
                iconCode = FontAwesome.TIMES_CIRCLE;
                color = Color.web("#FF4444");
                break;
            case "SCANNING":
                iconCode = FontAwesome.WIFI;
                color = Color.web("#00BFFF");
                break;
            default:
                iconCode = FontAwesome.CIRCLE;
                color = Color.web("#CCCCCC");
        }
        
        return FontIcon.of(iconCode, size, color);
    }
}