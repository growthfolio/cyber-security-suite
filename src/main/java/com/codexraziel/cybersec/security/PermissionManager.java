package com.codexraziel.cybersec.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PermissionManager {
    
    private static final int PERMISSION_CHECK_TIMEOUT = 5;
    
    public PermissionCheckResult checkNetworkPermissions() {
        try {
            // Check if we can access network interfaces
            if (canExecuteCommand("ip", "link", "show")) {
                return PermissionCheckResult.success("Network permissions available");
            }
            
            if (canExecuteCommand("ifconfig")) {
                return PermissionCheckResult.success("Network permissions available (ifconfig)");
            }
            
            return PermissionCheckResult.failure("No network interface access",
                "Install net-tools or iproute2 package");
                
        } catch (Exception e) {
            log.error("Network permission check failed", e);
            return PermissionCheckResult.failure("Permission check error: " + e.getMessage(),
                "Check system configuration");
        }
    }
    
    public PermissionCheckResult checkWiFiPermissions() {
        try {
            // Check for WiFi scanning tools
            if (canExecuteCommand("nmcli", "--version")) {
                return PermissionCheckResult.success("WiFi scanning available (nmcli)");
            }
            
            if (canExecuteCommand("iwlist", "--version")) {
                return PermissionCheckResult.success("WiFi scanning available (iwlist)");
            }
            
            return PermissionCheckResult.failure("No WiFi scanning tools available",
                "Install network-manager or wireless-tools");
                
        } catch (Exception e) {
            log.error("WiFi permission check failed", e);
            return PermissionCheckResult.failure("WiFi permission check error: " + e.getMessage(),
                "Install WiFi management tools");
        }
    }
    
    public PermissionCheckResult checkSudoPermissions() {
        try {
            // Check if user has sudo access
            ProcessBuilder pb = new ProcessBuilder("sudo", "-n", "true");
            Process process = pb.start();
            
            boolean finished = process.waitFor(PERMISSION_CHECK_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return PermissionCheckResult.failure("Sudo check timeout", "Check sudo configuration");
            }
            
            if (process.exitValue() == 0) {
                return PermissionCheckResult.success("Sudo access available");
            } else {
                return PermissionCheckResult.warning("No sudo access", 
                    "Some features may be limited without sudo");
            }
            
        } catch (Exception e) {
            log.debug("Sudo check failed: {}", e.getMessage());
            return PermissionCheckResult.warning("Sudo not available",
                "Some advanced features require sudo access");
        }
    }
    
    public PermissionCheckResult checkFilePermissions(String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return PermissionCheckResult.failure("File does not exist: " + filePath,
                    "Check file path and ensure file exists");
            }
            
            if (!Files.isReadable(path)) {
                return PermissionCheckResult.failure("File is not readable: " + filePath,
                    "Check file permissions (chmod +r)");
            }
            
            if (Files.isDirectory(path)) {
                return PermissionCheckResult.failure("Path is a directory: " + filePath,
                    "Specify a file, not a directory");
            }
            
            return PermissionCheckResult.success("File accessible: " + filePath);
            
        } catch (Exception e) {
            log.error("File permission check failed for: {}", filePath, e);
            return PermissionCheckResult.failure("File access error: " + e.getMessage(),
                "Check file path and permissions");
        }
    }
    
    public PermissionCheckResult checkToolAvailability(String toolName) {
        try {
            if (canExecuteCommand("which", toolName)) {
                return PermissionCheckResult.success(toolName + " is available");
            }
            
            return PermissionCheckResult.failure(toolName + " not found",
                getInstallSuggestion(toolName));
                
        } catch (Exception e) {
            log.error("Tool availability check failed for: {}", toolName, e);
            return PermissionCheckResult.failure("Tool check error: " + e.getMessage(),
                "Check system PATH and tool installation");
        }
    }
    
    public PermissionCheckResult checkWritePermissions(String directoryPath) {
        try {
            Path dir = Paths.get(directoryPath);
            
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            
            if (!Files.isWritable(dir)) {
                return PermissionCheckResult.failure("Directory not writable: " + directoryPath,
                    "Check directory permissions (chmod +w)");
            }
            
            // Test write by creating a temporary file
            Path testFile = dir.resolve(".permission_test");
            try {
                Files.createFile(testFile);
                Files.delete(testFile);
                return PermissionCheckResult.success("Write permissions available");
            } catch (Exception e) {
                return PermissionCheckResult.failure("Cannot write to directory: " + directoryPath,
                    "Check disk space and permissions");
            }
            
        } catch (Exception e) {
            log.error("Write permission check failed for: {}", directoryPath, e);
            return PermissionCheckResult.failure("Write permission error: " + e.getMessage(),
                "Check directory path and permissions");
        }
    }
    
    private boolean canExecuteCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean finished = process.waitFor(PERMISSION_CHECK_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getInstallSuggestion(String toolName) {
        switch (toolName.toLowerCase()) {
            case "nmcli":
                return "Install network-manager: sudo apt install network-manager";
            case "iwlist":
            case "iwconfig":
                return "Install wireless-tools: sudo apt install wireless-tools";
            case "aircrack-ng":
                return "Install aircrack-ng: sudo apt install aircrack-ng";
            case "hydra":
                return "Install hydra: sudo apt install hydra";
            case "nmap":
                return "Install nmap: sudo apt install nmap";
            default:
                return "Install " + toolName + " using your package manager";
        }
    }
    
    public SystemPermissionStatus getSystemPermissionStatus() {
        return SystemPermissionStatus.builder()
            .networkAccess(checkNetworkPermissions().isSuccess())
            .wifiAccess(checkWiFiPermissions().isSuccess())
            .sudoAccess(checkSudoPermissions().isSuccess())
            .toolsAvailable(checkEssentialTools())
            .build();
    }
    
    private int checkEssentialTools() {
        String[] tools = {"nmcli", "iwlist", "ping", "nmap"};
        int available = 0;
        
        for (String tool : tools) {
            if (checkToolAvailability(tool).isSuccess()) {
                available++;
            }
        }
        
        return available;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PermissionCheckResult {
        private boolean success;
        private boolean warning;
        private String message;
        private String suggestion;
        
        public static PermissionCheckResult success(String message) {
            return new PermissionCheckResult(true, false, message, null);
        }
        
        public static PermissionCheckResult failure(String message, String suggestion) {
            return new PermissionCheckResult(false, false, message, suggestion);
        }
        
        public static PermissionCheckResult warning(String message, String suggestion) {
            return new PermissionCheckResult(false, true, message, suggestion);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SystemPermissionStatus {
        private boolean networkAccess;
        private boolean wifiAccess;
        private boolean sudoAccess;
        private int toolsAvailable;
        
        public boolean isFullyOperational() {
            return networkAccess && wifiAccess && toolsAvailable >= 2;
        }
        
        public String getStatusSummary() {
            if (isFullyOperational()) {
                return "OK All permissions available";
            } else if (networkAccess || wifiAccess) {
                return "WARN Limited permissions - some features unavailable";
            } else {
                return "ERROR Insufficient permissions - most features unavailable";
            }
        }
    }
}