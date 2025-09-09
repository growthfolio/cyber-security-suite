package com.research.cybersec.services;

import com.research.cybersec.models.WiFiNetwork;
import com.research.cybersec.models.ScanResult;
import com.research.cybersec.security.AuditLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class ResultsExporter {
    
    @Autowired
    private AuditLogger auditLogger;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    public ExportResult exportToCSV(List<WiFiNetwork> networks, String filename) {
        try {
            Path exportPath = getExportPath(filename, "csv");
            
            try (FileWriter writer = new FileWriter(exportPath.toFile())) {
                // CSV Header
                writer.write("SSID,BSSID,Security,Signal_Strength,Channel,Frequency,Status,Discovered_At\n");
                
                // Data rows
                for (WiFiNetwork network : networks) {
                    writer.write(String.format("%s,%s,%s,%d,%s,%s,%s,%s\n",
                        escapeCsv(network.getSsid()),
                        network.getBssid(),
                        network.getSecurityLevel(),
                        network.getSignalStrength(),
                        network.getChannel(),
                        network.getFrequency(),
                        network.getNetworkStatus().getDisplayName(),
                        network.getDiscoveredAt() != null ? network.getDiscoveredAt().toString() : ""
                    ));
                }
            }
            
            auditLogger.logUserAction("EXPORT_CSV", exportPath.toString(), "Success");
            return ExportResult.success(exportPath.toString(), networks.size());
            
        } catch (IOException e) {
            log.error("CSV export failed", e);
            auditLogger.logUserAction("EXPORT_CSV", filename, "Failed: " + e.getMessage());
            return ExportResult.failure("CSV export failed: " + e.getMessage());
        }
    }
    
    public ExportResult exportToJSON(List<WiFiNetwork> networks, String filename) {
        try {
            Path exportPath = getExportPath(filename, "json");
            
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"export_info\": {\n");
            json.append("    \"timestamp\": \"").append(LocalDateTime.now()).append("\",\n");
            json.append("    \"total_networks\": ").append(networks.size()).append(",\n");
            json.append("    \"tool\": \"Cyber Security Research Suite\"\n");
            json.append("  },\n");
            json.append("  \"networks\": [\n");
            
            for (int i = 0; i < networks.size(); i++) {
                WiFiNetwork network = networks.get(i);
                json.append("    {\n");
                json.append("      \"ssid\": \"").append(escapeJson(network.getSsid())).append("\",\n");
                json.append("      \"bssid\": \"").append(network.getBssid()).append("\",\n");
                json.append("      \"security\": \"").append(network.getSecurityLevel()).append("\",\n");
                json.append("      \"signal_strength\": ").append(network.getSignalStrength()).append(",\n");
                json.append("      \"channel\": \"").append(network.getChannel()).append("\",\n");
                json.append("      \"frequency\": \"").append(network.getFrequency()).append("\",\n");
                json.append("      \"status\": \"").append(network.getNetworkStatus().getDisplayName()).append("\",\n");
                json.append("      \"vulnerable\": ").append(network.isVulnerable()).append(",\n");
                json.append("      \"discovered_at\": \"").append(network.getDiscoveredAt() != null ? network.getDiscoveredAt() : "").append("\"\n");
                json.append("    }").append(i < networks.size() - 1 ? "," : "").append("\n");
            }
            
            json.append("  ]\n");
            json.append("}\n");
            
            Files.write(exportPath, json.toString().getBytes());
            
            auditLogger.logUserAction("EXPORT_JSON", exportPath.toString(), "Success");
            return ExportResult.success(exportPath.toString(), networks.size());
            
        } catch (IOException e) {
            log.error("JSON export failed", e);
            auditLogger.logUserAction("EXPORT_JSON", filename, "Failed: " + e.getMessage());
            return ExportResult.failure("JSON export failed: " + e.getMessage());
        }
    }
    
    public ExportResult exportScanReport(ScanResult scanResult, String filename) {
        try {
            Path exportPath = getExportPath(filename, "txt");
            
            StringBuilder report = new StringBuilder();
            report.append("=== WIFI SCAN REPORT ===\n\n");
            report.append("Scan Information:\n");
            report.append("- Timestamp: ").append(scanResult.getScanTime()).append("\n");
            report.append("- Interface: ").append(scanResult.getScanInterface()).append("\n");
            report.append("- Method: ").append(scanResult.getScanMethod()).append("\n");
            report.append("- Success: ").append(scanResult.isSuccessful()).append("\n");
            report.append("- Total Networks: ").append(scanResult.getTotalNetworks()).append("\n");
            report.append("- Vulnerable Networks: ").append(scanResult.getVulnerableNetworks()).append("\n");
            report.append("- Vulnerability Rate: ").append(String.format("%.1f%%", scanResult.getVulnerabilityPercentage())).append("\n\n");
            
            if (scanResult.getErrorMessage() != null) {
                report.append("Error: ").append(scanResult.getErrorMessage()).append("\n\n");
            }
            
            report.append("=== NETWORK DETAILS ===\n\n");
            report.append(String.format("%-20s | %-17s | %-8s | %-6s | %-4s | %-10s\n", 
                "SSID", "BSSID", "Security", "Signal", "Ch", "Status"));
            report.append("-".repeat(80)).append("\n");
            
            for (WiFiNetwork network : scanResult.getNetworks()) {
                report.append(String.format("%-20s | %-17s | %-8s | %4ddBm | %-4s | %-10s\n",
                    truncate(network.getSsid(), 20),
                    network.getBssid(),
                    network.getSecurityLevel(),
                    network.getSignalStrength(),
                    network.getChannel(),
                    network.getNetworkStatus().getDisplayName()
                ));
            }
            
            report.append("\n=== SECURITY ANALYSIS ===\n\n");
            
            long openNetworks = scanResult.getNetworks().stream()
                .filter(n -> n.getSecurityLevel().equals("Open"))
                .count();
            long wepNetworks = scanResult.getNetworks().stream()
                .filter(n -> n.getSecurityLevel().equals("WEP"))
                .count();
            long wpaNetworks = scanResult.getNetworks().stream()
                .filter(n -> n.getSecurityLevel().startsWith("WPA"))
                .count();
            
            report.append("Security Distribution:\n");
            report.append("- Open Networks: ").append(openNetworks).append("\n");
            report.append("- WEP Networks: ").append(wepNetworks).append("\n");
            report.append("- WPA/WPA2/WPA3 Networks: ").append(wpaNetworks).append("\n\n");
            
            if (openNetworks > 0 || wepNetworks > 0) {
                report.append("⚠️ SECURITY RECOMMENDATIONS:\n");
                if (openNetworks > 0) {
                    report.append("- ").append(openNetworks).append(" open networks detected - consider enabling encryption\n");
                }
                if (wepNetworks > 0) {
                    report.append("- ").append(wepNetworks).append(" WEP networks detected - upgrade to WPA2/WPA3\n");
                }
            }
            
            Files.write(exportPath, report.toString().getBytes());
            
            auditLogger.logUserAction("EXPORT_REPORT", exportPath.toString(), "Success");
            return ExportResult.success(exportPath.toString(), scanResult.getTotalNetworks());
            
        } catch (IOException e) {
            log.error("Report export failed", e);
            auditLogger.logUserAction("EXPORT_REPORT", filename, "Failed: " + e.getMessage());
            return ExportResult.failure("Report export failed: " + e.getMessage());
        }
    }
    
    private Path getExportPath(String filename, String extension) throws IOException {
        Path exportsDir = Paths.get("exports");
        Files.createDirectories(exportsDir);
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String finalFilename = filename.isEmpty() ? 
            "wifi_scan_" + timestamp + "." + extension :
            filename + "_" + timestamp + "." + extension;
            
        return exportsDir.resolve(finalFilename);
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ExportResult {
        private boolean success;
        private String message;
        private String filePath;
        private int recordCount;
        
        public static ExportResult success(String filePath, int recordCount) {
            return new ExportResult(true, "Export completed successfully", filePath, recordCount);
        }
        
        public static ExportResult failure(String message) {
            return new ExportResult(false, message, null, 0);
        }
    }
}