package com.codexraziel.cybersec.wifi.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Checks and validates privileges required for WiFi operations
 */
@Component
@Slf4j
public class WiFiPrivilegeChecker {
    
    private static final String[] REQUIRED_TOOLS = {
        "aircrack-ng", "airodump-ng", "aireplay-ng", "airmon-ng"
    };
    
    private static final String[] REQUIRED_PATHS = {
        "/usr/bin", "/usr/local/bin", "/bin"
    };
    
    /**
     * Check if running with sufficient privileges for WiFi operations
     */
    public boolean hasRequiredPrivileges() {
        try {
            // Check if we can access wireless interfaces
            Path sysNetWireless = Paths.get("/sys/class/net");
            if (!Files.exists(sysNetWireless) || !Files.isReadable(sysNetWireless)) {
                log.warn("Cannot access /sys/class/net - insufficient privileges");
                return false;
            }
            
            // Check if we're running as root or with appropriate capabilities
            String user = System.getProperty("user.name");
            if ("root".equals(user)) {
                log.debug("Running as root - privileges OK");
                return true;
            }
            
            // Check for sudo/pkexec availability
            if (canExecutePrivilegedCommand()) {
                log.debug("Can execute privileged commands - privileges OK");
                return true;
            }
            
            log.warn("Insufficient privileges - not root and cannot execute privileged commands");
            return false;
            
        } catch (Exception e) {
            log.error("Error checking privileges", e);
            return false;
        }
    }
    
    /**
     * Check if required aircrack-ng tools are available
     */
    public boolean hasRequiredTools() {
        for (String tool : REQUIRED_TOOLS) {
            if (!isToolAvailable(tool)) {
                log.warn("Required tool not found: {}", tool);
                return false;
            }
        }
        
        log.debug("All required tools are available");
        return true;
    }
    
    /**
     * Check if a specific tool is available in the system PATH
     */
    public boolean isToolAvailable(String toolName) {
        try {
            // Check common paths
            for (String path : REQUIRED_PATHS) {
                File toolFile = new File(path, toolName);
                if (toolFile.exists() && toolFile.canExecute()) {
                    log.debug("Found tool {} at {}", toolName, toolFile.getAbsolutePath());
                    return true;
                }
            }
            
            // Try using 'which' command
            ProcessBuilder pb = new ProcessBuilder("which", toolName);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.debug("Tool {} found via 'which' command", toolName);
                return true;
            }
            
            log.debug("Tool {} not found", toolName);
            return false;
            
        } catch (Exception e) {
            log.error("Error checking tool availability: {}", toolName, e);
            return false;
        }
    }
    
    /**
     * Check if we can execute privileged commands via sudo/pkexec
     */
    private boolean canExecutePrivilegedCommand() {
        try {
            // Try sudo with non-interactive check
            ProcessBuilder pb = new ProcessBuilder("sudo", "-n", "echo", "test");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.debug("Can execute sudo commands");
                return true;
            }
            
            // Try pkexec
            pb = new ProcessBuilder("pkexec", "--version");
            process = pb.start();
            exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.debug("pkexec is available");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.debug("Cannot execute privileged commands", e);
            return false;
        }
    }
    
    /**
     * Check if a wireless interface exists and is accessible
     */
    public boolean canAccessWirelessInterface(String interfaceName) {
        try {
            Path interfacePath = Paths.get("/sys/class/net", interfaceName);
            
            if (!Files.exists(interfacePath)) {
                log.warn("Wireless interface {} does not exist", interfaceName);
                return false;
            }
            
            // Check if it's a wireless interface
            Path wirelessPath = interfacePath.resolve("wireless");
            if (!Files.exists(wirelessPath)) {
                log.warn("Interface {} is not a wireless interface", interfaceName);
                return false;
            }
            
            // Check if we can read interface properties
            if (!Files.isReadable(wirelessPath)) {
                log.warn("Cannot read wireless interface {} properties", interfaceName);
                return false;
            }
            
            log.debug("Can access wireless interface: {}", interfaceName);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking wireless interface access: {}", interfaceName, e);
            return false;
        }
    }
    
    /**
     * Check monitor mode capabilities for an interface
     */
    public boolean canSetMonitorMode(String interfaceName) {
        if (!canAccessWirelessInterface(interfaceName)) {
            return false;
        }
        
        try {
            // Try to check interface capabilities (this might require privileges)
            ProcessBuilder pb = new ProcessBuilder("iw", interfaceName, "info");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.warn("Cannot get interface info for {}", interfaceName);
                return false;
            }
            
            log.debug("Can check monitor mode capabilities for {}", interfaceName);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking monitor mode capabilities: {}", interfaceName, e);
            return false;
        }
    }
    
    /**
     * Get comprehensive privilege status report
     */
    public PrivilegeStatus getPrivilegeStatus() {
        return PrivilegeStatus.builder()
            .hasRootPrivileges(hasRequiredPrivileges())
            .hasRequiredTools(hasRequiredTools())
            .canExecuteSudo(canExecutePrivilegedCommand())
            .userName(System.getProperty("user.name"))
            .build();
    }
    
    /**
     * Privilege status data class
     */
    @lombok.Data
    @lombok.Builder
    public static class PrivilegeStatus {
        private boolean hasRootPrivileges;
        private boolean hasRequiredTools;
        private boolean canExecuteSudo;
        private String userName;
        
        public boolean isFullyCapable() {
            return hasRootPrivileges && hasRequiredTools;
        }
        
        public String getStatusMessage() {
            if (isFullyCapable()) {
                return "All privileges and tools available";
            }
            
            StringBuilder sb = new StringBuilder("Missing: ");
            if (!hasRootPrivileges) sb.append("root privileges ");
            if (!hasRequiredTools) sb.append("required tools ");
            
            return sb.toString().trim();
        }
    }
    
    /**
     * Validate that we have everything needed for WiFi operations
     */
    public void validateEnvironment() throws SecurityException {
        if (!hasRequiredPrivileges()) {
            throw new SecurityException("Insufficient privileges for WiFi operations. " +
                                      "Run as root or with appropriate sudo privileges.");
        }
        
        if (!hasRequiredTools()) {
            throw new SecurityException("Required tools not found. " +
                                      "Please install aircrack-ng suite.");
        }
        
        log.info("WiFi environment validation passed");
    }
    
    /**
     * Get recommendations for fixing privilege issues
     */
    public String getPrivilegeRecommendations() {
        PrivilegeStatus status = getPrivilegeStatus();
        
        if (status.isFullyCapable()) {
            return "Environment is properly configured for WiFi operations.";
        }
        
        StringBuilder recommendations = new StringBuilder();
        
        if (!status.hasRootPrivileges) {
            recommendations.append("• Run the application as root or configure sudo access\n");
            recommendations.append("• Add user to appropriate groups (netdev, wireshark)\n");
        }
        
        if (!status.hasRequiredTools) {
            recommendations.append("• Install aircrack-ng suite: sudo apt install aircrack-ng\n");
            recommendations.append("• Ensure tools are in system PATH\n");
        }
        
        return recommendations.toString();
    }
}
