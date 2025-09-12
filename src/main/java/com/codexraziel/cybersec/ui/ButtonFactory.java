package com.codexraziel.cybersec.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Utility class for creating standardized buttons with icons
 * Ensures consistent styling and behavior across the application
 */
public class ButtonFactory {
    
    // Standard button styles
    public static final String PRIMARY_STYLE = "-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;";
    public static final String SUCCESS_STYLE = "-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;";
    public static final String DANGER_STYLE = "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;";
    public static final String WARNING_STYLE = "-fx-background-color: #ffc107; -fx-text-fill: #212529; -fx-font-weight: bold;";
    public static final String INFO_STYLE = "-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: bold;";
    public static final String SECONDARY_STYLE = "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold;";
    public static final String DARK_STYLE = "-fx-background-color: #343a40; -fx-text-fill: white; -fx-font-weight: bold;";
    
    // Standard icon size
    public static final int DEFAULT_ICON_SIZE = 14;
    public static final int SMALL_ICON_SIZE = 12;
    public static final int LARGE_ICON_SIZE = 16;
    
    /**
     * Create a button with icon and tooltip
     */
    public static Button createButton(String text, Ikon icon, String tooltip) {
        return createButton(text, icon, tooltip, null, DEFAULT_ICON_SIZE);
    }
    
    /**
     * Create a button with icon, tooltip and style
     */
    public static Button createButton(String text, Ikon icon, String tooltip, String style) {
        return createButton(text, icon, tooltip, style, DEFAULT_ICON_SIZE);
    }
    
    /**
     * Create a button with full customization
     */
    public static Button createButton(String text, Ikon icon, String tooltip, String style, int iconSize) {
        Button button = new Button(text);
        
        if (icon != null) {
            FontIcon fontIcon = FontIcon.of(icon, iconSize);
            button.setGraphic(fontIcon);
        }
        
        if (tooltip != null && !tooltip.isEmpty()) {
            button.setTooltip(new Tooltip(tooltip));
        }
        
        if (style != null && !style.isEmpty()) {
            button.setStyle(style);
        }
        
        // Add hover effects
        addHoverEffects(button);
        
        return button;
    }
    
    /**
     * Create action buttons with standard icons
     */
    public static class ActionButtons {
        
        public static Button start(String text, String tooltip) {
            return createButton(text, FontAwesome.PLAY, tooltip, SUCCESS_STYLE);
        }
        
        public static Button stop(String text, String tooltip) {
            return createButton(text, FontAwesome.STOP, tooltip, DANGER_STYLE);
        }
        
        public static Button pause(String text, String tooltip) {
            return createButton(text, FontAwesome.PAUSE, tooltip, WARNING_STYLE);
        }
        
        public static Button resume(String text, String tooltip) {
            return createButton(text, FontAwesome.PLAY, tooltip, SUCCESS_STYLE);
        }
        
        public static Button refresh(String text, String tooltip) {
            return createButton(text, FontAwesome.REFRESH, tooltip, INFO_STYLE);
        }
        
        public static Button save(String text, String tooltip) {
            return createButton(text, FontAwesome.SAVE, tooltip, PRIMARY_STYLE);
        }
        
        public static Button load(String text, String tooltip) {
            return createButton(text, FontAwesome.FOLDER_OPEN, tooltip, SECONDARY_STYLE);
        }
        
        public static Button export(String text, String tooltip) {
            return createButton(text, FontAwesome.DOWNLOAD, tooltip, INFO_STYLE);
        }
        
        public static Button import_(String text, String tooltip) {
            return createButton(text, FontAwesome.UPLOAD, tooltip, INFO_STYLE);
        }
        
        public static Button delete(String text, String tooltip) {
            return createButton(text, FontAwesome.TRASH, tooltip, DANGER_STYLE);
        }
        
        public static Button edit(String text, String tooltip) {
            return createButton(text, FontAwesome.EDIT, tooltip, PRIMARY_STYLE);
        }
        
        public static Button view(String text, String tooltip) {
            return createButton(text, FontAwesome.EYE, tooltip, INFO_STYLE);
        }
        
        public static Button search(String text, String tooltip) {
            return createButton(text, FontAwesome.SEARCH, tooltip, PRIMARY_STYLE);
        }
        
        public static Button filter(String text, String tooltip) {
            return createButton(text, FontAwesome.FILTER, tooltip, SECONDARY_STYLE);
        }
        
        public static Button clear(String text, String tooltip) {
            return createButton(text, FontAwesome.ERASER, tooltip, WARNING_STYLE);
        }
        
        public static Button copy(String text, String tooltip) {
            return createButton(text, FontAwesome.COPY, tooltip, INFO_STYLE);
        }
        
        public static Button paste(String text, String tooltip) {
            return createButton(text, FontAwesome.PASTE, tooltip, INFO_STYLE);
        }
        
        public static Button cut(String text, String tooltip) {
            return createButton(text, FontAwesome.CUT, tooltip, WARNING_STYLE);
        }
        
        public static Button close(String text, String tooltip) {
            return createButton(text, FontAwesome.TIMES, tooltip, SECONDARY_STYLE);
        }
        
        public static Button ok(String text, String tooltip) {
            return createButton(text, FontAwesome.CHECK, tooltip, SUCCESS_STYLE);
        }
        
        public static Button cancel(String text, String tooltip) {
            return createButton(text, FontAwesome.TIMES, tooltip, SECONDARY_STYLE);
        }
        
        public static Button apply(String text, String tooltip) {
            return createButton(text, FontAwesome.CHECK, tooltip, PRIMARY_STYLE);
        }
        
        public static Button reset(String text, String tooltip) {
            return createButton(text, FontAwesome.REFRESH, tooltip, WARNING_STYLE);
        }
        
        public static Button help(String text, String tooltip) {
            return createButton(text, FontAwesome.QUESTION_CIRCLE, tooltip, INFO_STYLE);
        }
        
        public static Button settings(String text, String tooltip) {
            return createButton(text, FontAwesome.COG, tooltip, SECONDARY_STYLE);
        }
        
        public static Button info(String text, String tooltip) {
            return createButton(text, FontAwesome.INFO_CIRCLE, tooltip, INFO_STYLE);
        }
    }
    
    /**
     * Create WiFi-specific buttons
     */
    public static class WiFiButtons {
        
        public static Button scan(String text, String tooltip) {
            return createButton(text, FontAwesome.WIFI, tooltip, PRIMARY_STYLE);
        }
        
        public static Button monitor(String text, String tooltip) {
            return createButton(text, FontAwesome.EYE, tooltip, INFO_STYLE);
        }
        
        public static Button attack(String text, String tooltip) {
            return createButton(text, FontAwesome.BOLT, tooltip, DANGER_STYLE);
        }
        
        public static Button capture(String text, String tooltip) {
            return createButton(text, FontAwesome.CAMERA, tooltip, WARNING_STYLE);
        }
        
        public static Button analyze(String text, String tooltip) {
            return createButton(text, FontAwesome.LINE_CHART, tooltip, INFO_STYLE);
        }
        
        public static Button crack(String text, String tooltip) {
            return createButton(text, FontAwesome.KEY, tooltip, DANGER_STYLE);
        }
        
        public static Button deauth(String text, String tooltip) {
            return createButton(text, FontAwesome.UNLINK, tooltip, DANGER_STYLE);
        }
        
        public static Button handshake(String text, String tooltip) {
            return createButton(text, FontAwesome.HANDSHAKE_O, tooltip, WARNING_STYLE);
        }
    }
    
    /**
     * Create security-specific buttons
     */
    public static class SecurityButtons {
        
        public static Button bruteforce(String text, String tooltip) {
            return createButton(text, FontAwesome.BOLT, tooltip, DANGER_STYLE);
        }
        
        public static Button keylog(String text, String tooltip) {
            return createButton(text, FontAwesome.KEYBOARD_O, tooltip, WARNING_STYLE);
        }
        
        public static Button scan(String text, String tooltip) {
            return createButton(text, FontAwesome.SEARCH, tooltip, INFO_STYLE);
        }
        
        public static Button exploit(String text, String tooltip) {
            return createButton(text, FontAwesome.BUG, tooltip, DANGER_STYLE);
        }
        
        public static Button report(String text, String tooltip) {
            return createButton(text, FontAwesome.FILE_TEXT, tooltip, SUCCESS_STYLE);
        }
        
        public static Button audit(String text, String tooltip) {
            return createButton(text, FontAwesome.SHIELD, tooltip, PRIMARY_STYLE);
        }
        
        public static Button monitor(String text, String tooltip) {
            return createButton(text, FontAwesome.DESKTOP, tooltip, INFO_STYLE);
        }
        
        public static Button analyze(String text, String tooltip) {
            return createButton(text, FontAwesome.BAR_CHART, tooltip, INFO_STYLE);
        }
    }
    
    /**
     * Add hover effects to buttons
     */
    private static void addHoverEffects(Button button) {
        String originalStyle = button.getStyle();
        
        button.setOnMouseEntered(e -> {
            String hoverStyle = originalStyle.isEmpty() ? "" : originalStyle + "; ";
            hoverStyle += "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.5, 0, 2); " +
                         "-fx-scale-x: 1.05; -fx-scale-y: 1.05;";
            button.setStyle(hoverStyle);
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(originalStyle);
        });
        
        button.setOnMousePressed(e -> {
            String pressedStyle = originalStyle.isEmpty() ? "" : originalStyle + "; ";
            pressedStyle += "-fx-scale-x: 0.95; -fx-scale-y: 0.95;";
            button.setStyle(pressedStyle);
        });
        
        button.setOnMouseReleased(e -> {
            button.setStyle(originalStyle);
        });
    }
}
