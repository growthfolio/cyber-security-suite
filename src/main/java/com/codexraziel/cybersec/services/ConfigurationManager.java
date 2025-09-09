package com.codexraziel.cybersec.services;

import com.codexraziel.cybersec.models.PentestConfig;
import com.codexraziel.cybersec.security.AuditLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class ConfigurationManager {
    
    @Autowired
    private AuditLogger auditLogger;
    
    private static final Path PROFILES_DIR = Paths.get("profiles");
    private static final Path CONFIG_DIR = Paths.get("config");
    
    public ConfigurationManager() {
        initializeDirectories();
    }
    
    private void initializeDirectories() {
        try {
            Files.createDirectories(PROFILES_DIR);
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            log.error("Failed to create configuration directories", e);
        }
    }
    
    public ConfigResult saveProfile(PentestProfile profile) {
        try {
            if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                return ConfigResult.failure("Profile name cannot be empty");
            }
            
            String safeName = sanitizeProfileName(profile.getName());
            Path profilePath = PROFILES_DIR.resolve(safeName + ".properties");
            
            Properties props = new Properties();
            props.setProperty("name", profile.getName());
            props.setProperty("description", profile.getDescription() != null ? profile.getDescription() : "");
            props.setProperty("targetSSID", profile.getTargetSSID() != null ? profile.getTargetSSID() : "");
            props.setProperty("attackType", profile.getAttackType() != null ? profile.getAttackType() : "Dictionary Attack");
            props.setProperty("wordlistPath", profile.getWordlistPath() != null ? profile.getWordlistPath() : "");
            props.setProperty("networkInterface", profile.getNetworkInterface() != null ? profile.getNetworkInterface() : "wlan0");
            props.setProperty("threadCount", String.valueOf(profile.getThreadCount()));
            props.setProperty("timeoutSeconds", String.valueOf(profile.getTimeoutSeconds()));
            props.setProperty("enableEvasion", String.valueOf(profile.isEnableEvasion()));
            props.setProperty("createdAt", profile.getCreatedAt().toString());
            props.setProperty("lastModified", LocalDateTime.now().toString());
            
            try (var writer = Files.newBufferedWriter(profilePath)) {
                props.store(writer, "Pentest Profile: " + profile.getName());
            }
            
            auditLogger.logUserAction("SAVE_PROFILE", profile.getName(), "Success");
            return ConfigResult.success("Profile saved: " + profilePath.toString());
            
        } catch (IOException e) {
            log.error("Failed to save profile: {}", profile.getName(), e);
            auditLogger.logUserAction("SAVE_PROFILE", profile.getName(), "Failed: " + e.getMessage());
            return ConfigResult.failure("Failed to save profile: " + e.getMessage());
        }
    }
    
    public ConfigResult<PentestProfile> loadProfile(String profileName) {
        try {
            String safeName = sanitizeProfileName(profileName);
            Path profilePath = PROFILES_DIR.resolve(safeName + ".properties");
            
            if (!Files.exists(profilePath)) {
                return ConfigResult.failure("Profile not found: " + profileName);
            }
            
            Properties props = new Properties();
            try (var reader = Files.newBufferedReader(profilePath)) {
                props.load(reader);
            }
            
            PentestProfile profile = PentestProfile.builder()
                .name(props.getProperty("name", profileName))
                .description(props.getProperty("description", ""))
                .targetSSID(props.getProperty("targetSSID", ""))
                .attackType(props.getProperty("attackType", "Dictionary Attack"))
                .wordlistPath(props.getProperty("wordlistPath", ""))
                .networkInterface(props.getProperty("networkInterface", "wlan0"))
                .threadCount(Integer.parseInt(props.getProperty("threadCount", "4")))
                .timeoutSeconds(Integer.parseInt(props.getProperty("timeoutSeconds", "300")))
                .enableEvasion(Boolean.parseBoolean(props.getProperty("enableEvasion", "false")))
                .createdAt(LocalDateTime.parse(props.getProperty("createdAt", LocalDateTime.now().toString())))
                .build();
            
            auditLogger.logUserAction("LOAD_PROFILE", profileName, "Success");
            return ConfigResult.success("Profile loaded", profile);
            
        } catch (Exception e) {
            log.error("Failed to load profile: {}", profileName, e);
            auditLogger.logUserAction("LOAD_PROFILE", profileName, "Failed: " + e.getMessage());
            return ConfigResult.failure("Failed to load profile: " + e.getMessage());
        }
    }
    
    public ConfigResult<List<String>> getAvailableProfiles() {
        try {
            List<String> profiles = new ArrayList<>();
            
            if (Files.exists(PROFILES_DIR)) {
                Files.list(PROFILES_DIR)
                    .filter(path -> path.toString().endsWith(".properties"))
                    .forEach(path -> {
                        String filename = path.getFileName().toString();
                        String profileName = filename.substring(0, filename.lastIndexOf('.'));
                        profiles.add(profileName);
                    });
            }
            
            return ConfigResult.success("Found " + profiles.size() + " profiles", profiles);
            
        } catch (IOException e) {
            log.error("Failed to list profiles", e);
            return ConfigResult.failure("Failed to list profiles: " + e.getMessage());
        }
    }
    
    public ConfigResult deleteProfile(String profileName) {
        try {
            String safeName = sanitizeProfileName(profileName);
            Path profilePath = PROFILES_DIR.resolve(safeName + ".properties");
            
            if (!Files.exists(profilePath)) {
                return ConfigResult.failure("Profile not found: " + profileName);
            }
            
            Files.delete(profilePath);
            
            auditLogger.logUserAction("DELETE_PROFILE", profileName, "Success");
            return ConfigResult.success("Profile deleted: " + profileName);
            
        } catch (IOException e) {
            log.error("Failed to delete profile: {}", profileName, e);
            auditLogger.logUserAction("DELETE_PROFILE", profileName, "Failed: " + e.getMessage());
            return ConfigResult.failure("Failed to delete profile: " + e.getMessage());
        }
    }
    
    public ConfigResult saveApplicationConfig(ApplicationConfig config) {
        try {
            Path configPath = CONFIG_DIR.resolve("application.properties");
            
            Properties props = new Properties();
            props.setProperty("defaultInterface", config.getDefaultInterface());
            props.setProperty("defaultWordlist", config.getDefaultWordlist());
            props.setProperty("maxThreads", String.valueOf(config.getMaxThreads()));
            props.setProperty("defaultTimeout", String.valueOf(config.getDefaultTimeout()));
            props.setProperty("autoSave", String.valueOf(config.isAutoSave()));
            props.setProperty("darkTheme", String.valueOf(config.isDarkTheme()));
            props.setProperty("enableAuditLog", String.valueOf(config.isEnableAuditLog()));
            props.setProperty("lastModified", LocalDateTime.now().toString());
            
            try (var writer = Files.newBufferedWriter(configPath)) {
                props.store(writer, "Application Configuration");
            }
            
            auditLogger.logUserAction("SAVE_APP_CONFIG", "Application settings", "Success");
            return ConfigResult.success("Application configuration saved");
            
        } catch (IOException e) {
            log.error("Failed to save application config", e);
            return ConfigResult.failure("Failed to save configuration: " + e.getMessage());
        }
    }
    
    public ConfigResult<ApplicationConfig> loadApplicationConfig() {
        try {
            Path configPath = CONFIG_DIR.resolve("application.properties");
            
            ApplicationConfig defaultConfig = ApplicationConfig.builder()
                .defaultInterface("wlan0")
                .defaultWordlist("./wordlists/common_passwords.txt")
                .maxThreads(10)
                .defaultTimeout(300)
                .autoSave(true)
                .darkTheme(true)
                .enableAuditLog(true)
                .build();
            
            if (!Files.exists(configPath)) {
                return ConfigResult.success("Using default configuration", defaultConfig);
            }
            
            Properties props = new Properties();
            try (var reader = Files.newBufferedReader(configPath)) {
                props.load(reader);
            }
            
            ApplicationConfig config = ApplicationConfig.builder()
                .defaultInterface(props.getProperty("defaultInterface", "wlan0"))
                .defaultWordlist(props.getProperty("defaultWordlist", "./wordlists/common_passwords.txt"))
                .maxThreads(Integer.parseInt(props.getProperty("maxThreads", "10")))
                .defaultTimeout(Integer.parseInt(props.getProperty("defaultTimeout", "300")))
                .autoSave(Boolean.parseBoolean(props.getProperty("autoSave", "true")))
                .darkTheme(Boolean.parseBoolean(props.getProperty("darkTheme", "true")))
                .enableAuditLog(Boolean.parseBoolean(props.getProperty("enableAuditLog", "true")))
                .build();
            
            return ConfigResult.success("Configuration loaded", config);
            
        } catch (Exception e) {
            log.error("Failed to load application config", e);
            return ConfigResult.failure("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private String sanitizeProfileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PentestProfile {
        private String name;
        private String description;
        private String targetSSID;
        private String attackType;
        private String wordlistPath;
        private String networkInterface;
        private int threadCount;
        private int timeoutSeconds;
        private boolean enableEvasion;
        private LocalDateTime createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ApplicationConfig {
        private String defaultInterface;
        private String defaultWordlist;
        private int maxThreads;
        private int defaultTimeout;
        private boolean autoSave;
        private boolean darkTheme;
        private boolean enableAuditLog;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ConfigResult<T> {
        private boolean success;
        private String message;
        private T data;
        
        public static <T> ConfigResult<T> success(String message, T data) {
            return new ConfigResult<>(true, message, data);
        }
        
        public static ConfigResult<Void> success(String message) {
            return new ConfigResult<>(true, message, null);
        }
        
        public static <T> ConfigResult<T> failure(String message) {
            return new ConfigResult<>(false, message, null);
        }
    }
}