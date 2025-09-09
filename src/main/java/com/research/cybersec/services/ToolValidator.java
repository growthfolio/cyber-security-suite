package com.research.cybersec.services;

import com.research.cybersec.security.AuditLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ToolValidator {
    
    @Autowired
    private AuditLogger auditLogger;
    
    private static final Map<String, ToolInfo> REQUIRED_TOOLS = new HashMap<>();
    
    static {
        REQUIRED_TOOLS.put("nmcli", new ToolInfo("nmcli", "Network Manager CLI", "0.9.0", "network-manager", true));
        REQUIRED_TOOLS.put("iwlist", new ToolInfo("iwlist", "Wireless Tools", "30", "wireless-tools", true));
        REQUIRED_TOOLS.put("aircrack-ng", new ToolInfo("aircrack-ng", "WiFi Security Auditing", "1.6", "aircrack-ng", false));
        REQUIRED_TOOLS.put("hydra", new ToolInfo("hydra", "Network Login Cracker", "9.0", "hydra", false));
        REQUIRED_TOOLS.put("nmap", new ToolInfo("nmap", "Network Discovery", "7.0", "nmap", false));
        REQUIRED_TOOLS.put("john", new ToolInfo("john", "Password Cracker", "1.9", "john", false));
        REQUIRED_TOOLS.put("hashcat", new ToolInfo("hashcat", "Advanced Password Recovery", "6.0", "hashcat", false));
        REQUIRED_TOOLS.put("adb", new ToolInfo("adb", "Android Debug Bridge", "1.0", "android-tools-adb", false));
    }
    
    public ValidationReport checkDependencies() {
        auditLogger.logSystemEvent("DEPENDENCY_CHECK_START", "Checking all tool dependencies");
        
        ValidationReport report = new ValidationReport();
        
        for (ToolInfo toolInfo : REQUIRED_TOOLS.values()) {
            ToolStatus status = checkTool(toolInfo);
            report.addToolStatus(status);
            
            if (toolInfo.isRequired() && !status.isAvailable()) {
                report.addCriticalIssue("Required tool missing: " + toolInfo.getName());
            }
        }
        
        // Check system requirements
        checkSystemRequirements(report);
        
        auditLogger.logSystemEvent("DEPENDENCY_CHECK_COMPLETE", 
            String.format("Available: %d/%d, Critical issues: %d", 
                report.getAvailableCount(), report.getTotalCount(), report.getCriticalIssues().size()));
        
        return report;
    }
    
    public List<String> getMissingTools() {
        List<String> missing = new ArrayList<>();
        
        for (ToolInfo toolInfo : REQUIRED_TOOLS.values()) {
            if (!isToolAvailable(toolInfo.getName())) {
                missing.add(toolInfo.getName());
            }
        }
        
        return missing;
    }
    
    public boolean validateToolVersion(String toolName, String minVersion) {
        ToolInfo toolInfo = REQUIRED_TOOLS.get(toolName);
        if (toolInfo == null) {
            return false;
        }
        
        try {
            String actualVersion = getToolVersion(toolName);
            if (actualVersion == null) {
                return false;
            }
            
            return compareVersions(actualVersion, minVersion) >= 0;
            
        } catch (Exception e) {
            log.debug("Version check failed for {}: {}", toolName, e.getMessage());
            return false;
        }
    }
    
    private ToolStatus checkTool(ToolInfo toolInfo) {
        boolean available = isToolAvailable(toolInfo.getName());
        String version = available ? getToolVersion(toolInfo.getName()) : null;
        boolean versionOk = version != null && compareVersions(version, toolInfo.getMinVersion()) >= 0;
        
        return ToolStatus.builder()
            .toolInfo(toolInfo)
            .available(available)
            .version(version)
            .versionOk(versionOk)
            .installCommand(getInstallCommand(toolInfo))
            .build();
    }
    
    private boolean isToolAvailable(String toolName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", toolName);
            Process process = pb.start();
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getToolVersion(String toolName) {
        try {
            String[] versionCommands = getVersionCommand(toolName);
            if (versionCommands == null) return "unknown";
            
            ProcessBuilder pb = new ProcessBuilder(versionCommands);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\\n");
                }
            }
            
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "timeout";
            }
            
            return extractVersion(toolName, output.toString());
            
        } catch (Exception e) {
            log.debug("Failed to get version for {}: {}", toolName, e.getMessage());
            return "error";
        }
    }
    
    private String[] getVersionCommand(String toolName) {
        switch (toolName) {
            case "nmcli": return new String[]{"nmcli", "--version"};
            case "iwlist": return new String[]{"iwlist", "--version"};
            case "aircrack-ng": return new String[]{"aircrack-ng", "--help"};
            case "hydra": return new String[]{"hydra", "-h"};
            case "nmap": return new String[]{"nmap", "--version"};
            case "john": return new String[]{"john", "--version"};
            case "hashcat": return new String[]{"hashcat", "--version"};
            case "adb": return new String[]{"adb", "version"};
            default: return null;
        }
    }
    
    private String extractVersion(String toolName, String output) {
        // Simple version extraction patterns
        String[] patterns = {
            "version ([0-9]+\\.[0-9]+\\.[0-9]+)",
            "version ([0-9]+\\.[0-9]+)",
            "v([0-9]+\\.[0-9]+\\.[0-9]+)",
            "v([0-9]+\\.[0-9]+)",
            "([0-9]+\\.[0-9]+\\.[0-9]+)",
            "([0-9]+\\.[0-9]+)"
        };
        
        String lowerOutput = output.toLowerCase();
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(lowerOutput);
            if (m.find()) {
                return m.group(1);
            }
        }
        
        return "unknown";
    }
    
    private int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;
            
            if (v1Part != v2Part) {
                return Integer.compare(v1Part, v2Part);
            }
        }
        
        return 0;
    }
    
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private String getInstallCommand(ToolInfo toolInfo) {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("ubuntu") || os.contains("debian")) {
            return "sudo apt install " + toolInfo.getPackageName();
        } else if (os.contains("fedora") || os.contains("rhel") || os.contains("centos")) {
            return "sudo dnf install " + toolInfo.getPackageName();
        } else if (os.contains("arch")) {
            return "sudo pacman -S " + toolInfo.getPackageName();
        } else if (os.contains("mac")) {
            return "brew install " + toolInfo.getPackageName();
        } else {
            return "Install " + toolInfo.getPackageName() + " using your package manager";
        }
    }
    
    private void checkSystemRequirements(ValidationReport report) {
        // Check Java version
        String javaVersion = System.getProperty("java.version");
        if (compareVersions(javaVersion, "11.0") < 0) {
            report.addCriticalIssue("Java 11+ required, found: " + javaVersion);
        }
        
        // Check available memory
        long maxMemory = Runtime.getRuntime().maxMemory();
        long maxMemoryMB = maxMemory / (1024 * 1024);
        if (maxMemoryMB < 512) {
            report.addWarning("Low memory available: " + maxMemoryMB + "MB (recommended: 1GB+)");
        }
        
        // Check disk space
        long freeSpace = new java.io.File(".").getFreeSpace();
        long freeSpaceMB = freeSpace / (1024 * 1024);
        if (freeSpaceMB < 100) {
            report.addWarning("Low disk space: " + freeSpaceMB + "MB");
        }
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ToolInfo {
        private String name;
        private String description;
        private String minVersion;
        private String packageName;
        private boolean required;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ToolStatus {
        private ToolInfo toolInfo;
        private boolean available;
        private String version;
        private boolean versionOk;
        private String installCommand;
        
        public String getStatusIcon() {
            if (!available) return "❌";
            if (!versionOk) return "⚠️";
            return "✅";
        }
        
        public String getStatusText() {
            if (!available) return "Not Available";
            if (!versionOk) return "Version Too Old";
            return "Available";
        }
    }
    
    @lombok.Data
    public static class ValidationReport {
        private List<ToolStatus> toolStatuses = new ArrayList<>();
        private List<String> criticalIssues = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public void addToolStatus(ToolStatus status) {
            toolStatuses.add(status);
        }
        
        public void addCriticalIssue(String issue) {
            criticalIssues.add(issue);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public int getAvailableCount() {
            return (int) toolStatuses.stream().filter(ToolStatus::isAvailable).count();
        }
        
        public int getTotalCount() {
            return toolStatuses.size();
        }
        
        public boolean isFullyOperational() {
            return criticalIssues.isEmpty() && 
                   toolStatuses.stream().filter(s -> s.getToolInfo().isRequired()).allMatch(ToolStatus::isAvailable);
        }
        
        public String getSummary() {
            if (isFullyOperational()) {
                return "✅ All dependencies satisfied";
            } else if (criticalIssues.isEmpty()) {
                return "⚠️ Some optional tools missing";
            } else {
                return "❌ Critical dependencies missing";
            }
        }
    }
}