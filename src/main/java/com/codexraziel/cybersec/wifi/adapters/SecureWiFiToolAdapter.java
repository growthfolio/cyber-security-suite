package com.codexraziel.cybersec.wifi.adapters;

import com.codexraziel.cybersec.wifi.models.WiFiNetwork;
import com.codexraziel.cybersec.wifi.models.HandshakeCapture;
import com.codexraziel.cybersec.wifi.models.WiFiAttackResult;
import com.codexraziel.cybersec.wifi.security.WiFiInputValidator;
import com.codexraziel.cybersec.wifi.security.WiFiPrivilegeChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Secure adapter for aircrack-ng suite tools
 * Provides controlled access to WiFi attack tools with comprehensive security validation
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecureWiFiToolAdapter {
    
    private final WiFiInputValidator inputValidator;
    private final WiFiPrivilegeChecker privilegeChecker;
    
    private static final int DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes
    private static final int SCAN_TIMEOUT_SECONDS = 60;     // 1 minute for scanning
    private static final String[] REQUIRED_TOOLS = {"airodump-ng", "aircrack-ng", "aireplay-ng", "airmon-ng"};
    
    /**
     * Enable monitor mode on a wireless interface
     */
    public boolean enableMonitorMode(String interfaceName) {
        try {
            inputValidator.validateInterfaceName(interfaceName);
            privilegeChecker.validateEnvironment();
            
            log.info("Enabling monitor mode on interface: {}", interfaceName);
            
            // Check if interface exists and is wireless
            if (!isWirelessInterface(interfaceName)) {
                throw new IllegalArgumentException("Interface is not wireless: " + interfaceName);
            }
            
            // Build secure command
            List<String> command = Arrays.asList(
                "sudo", "airmon-ng", "start", interfaceName
            );
            
            ProcessResult result = executeSecureCommand(command, DEFAULT_TIMEOUT_SECONDS);
            
            if (result.exitCode == 0) {
                log.info("Successfully enabled monitor mode on {}", interfaceName);
                return true;
            } else {
                log.error("Failed to enable monitor mode on {}: {}", interfaceName, result.stderr);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error enabling monitor mode on interface: {}", interfaceName, e);
            return false;
        }
    }
    
    /**
     * Disable monitor mode on a wireless interface
     */
    public boolean disableMonitorMode(String interfaceName) {
        try {
            inputValidator.validateInterfaceName(interfaceName);
            privilegeChecker.validateEnvironment();
            
            log.info("Disabling monitor mode on interface: {}", interfaceName);
            
            // Build secure command
            List<String> command = Arrays.asList(
                "sudo", "airmon-ng", "stop", interfaceName
            );
            
            ProcessResult result = executeSecureCommand(command, DEFAULT_TIMEOUT_SECONDS);
            
            if (result.exitCode == 0) {
                log.info("Successfully disabled monitor mode on {}", interfaceName);
                return true;
            } else {
                log.error("Failed to disable monitor mode on {}: {}", interfaceName, result.stderr);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error disabling monitor mode on interface: {}", interfaceName, e);
            return false;
        }
    }
    
    /**
     * Scan for WiFi networks using airodump-ng
     */
    public List<WiFiNetwork> scanNetworks(String interfaceName, int durationSeconds) {
        try {
            inputValidator.validateInterfaceName(interfaceName);
            if (durationSeconds < 5 || durationSeconds > 300) {
                throw new IllegalArgumentException("Duration must be between 5 and 300 seconds");
            }
            privilegeChecker.validateEnvironment();
            
            log.info("Scanning for networks on interface {} for {} seconds", interfaceName, durationSeconds);
            
            // Create temporary file for scan results
            Path tempFile = Files.createTempFile("wifi_scan_", ".csv");
            String outputPrefix = tempFile.toString().replace(".csv", "");
            
            try {
                // Build secure command
                List<String> command = Arrays.asList(
                    "sudo", "timeout", String.valueOf(durationSeconds),
                    "airodump-ng", "--write", outputPrefix, "--output-format", "csv",
                    "--write-interval", "1", interfaceName
                );
                
                ProcessResult result = executeSecureCommand(command, durationSeconds + 10);
                
                // Parse scan results
                Path csvFile = Paths.get(outputPrefix + "-01.csv");
                if (Files.exists(csvFile)) {
                    return parseAirodumpCsv(csvFile);
                } else {
                    log.warn("No scan results file found: {}", csvFile);
                    return new ArrayList<>();
                }
                
            } finally {
                // Clean up temporary files
                cleanupTempFiles(outputPrefix);
            }
            
        } catch (Exception e) {
            log.error("Error scanning networks on interface: {}", interfaceName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Capture handshake for a specific network
     */
    public HandshakeCapture captureHandshake(String interfaceName, WiFiNetwork target, 
                                           Path outputDirectory, int timeoutSeconds) {
        try {
            inputValidator.validateInterfaceName(interfaceName);
            inputValidator.validateSSID(target.getSsid());
            inputValidator.validateBSSID(target.getBssid());
            inputValidator.validateAndSanitizeFilePath(outputDirectory.toString(), "/tmp");
            if (timeoutSeconds < 30 || timeoutSeconds > 1800) {
                throw new IllegalArgumentException("Timeout must be between 30 and 1800 seconds");
            }
            privilegeChecker.validateEnvironment();
            
            log.info("Capturing handshake for network {} ({})", target.getSsid(), target.getBssid());
            
            // Create output file path
            String filename = String.format("handshake_%s_%s", 
                target.getSsid().replaceAll("[^a-zA-Z0-9]", "_"),
                LocalDateTime.now().toString().replaceAll("[^a-zA-Z0-9]", "_"));
            Path outputFile = outputDirectory.resolve(filename);
            
            // Build secure command for capturing
            List<String> command = Arrays.asList(
                "sudo", "timeout", String.valueOf(timeoutSeconds),
                "airodump-ng", "--write", outputFile.toString(),
                "--output-format", "cap", "--channel", String.valueOf(target.getChannel()),
                "--bssid", target.getBssid(), interfaceName
            );
            
            ProcessResult result = executeSecureCommand(command, timeoutSeconds + 10);
            
            // Check if handshake was captured
            Path capFile = Paths.get(outputFile.toString() + "-01.cap");
            boolean hasHandshake = false;
            
            if (Files.exists(capFile)) {
                hasHandshake = verifyHandshakeCapture(capFile, target.getBssid());
            }
            
            return HandshakeCapture.builder()
                .targetNetwork(target)
                .captureFilePath(capFile)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .handshakeDetected(hasHandshake)
                .interfaceName(interfaceName)
                .status(hasHandshake ? HandshakeCapture.CaptureStatus.HANDSHAKE_CAPTURED : HandshakeCapture.CaptureStatus.ERROR)
                .errorMessage(hasHandshake ? null : "No handshake captured")
                .build();
            
        } catch (Exception e) {
            log.error("Error capturing handshake for network: {}", target.getSsid(), e);
            return HandshakeCapture.builder()
                .targetNetwork(target)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .handshakeDetected(false)
                .status(HandshakeCapture.CaptureStatus.ERROR)
                .errorMessage("Capture failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Crack WiFi password using aircrack-ng
     */
    public WiFiAttackResult crackPassword(HandshakeCapture handshake, Path wordlistPath) {
        try {
            inputValidator.validateWordlistPath(wordlistPath.getFileName().toString());
            privilegeChecker.validateEnvironment();
            
            if (!handshake.isHandshakeDetected()) {
                throw new IllegalArgumentException("Invalid handshake provided");
            }
            
            if (!Files.exists(wordlistPath)) {
                throw new IllegalArgumentException("Wordlist not found: " + wordlistPath);
            }
            
            log.info("Cracking password for network {} using wordlist {}", 
                handshake.getTargetNetwork().getSsid(), wordlistPath.getFileName());
            
            LocalDateTime startTime = LocalDateTime.now();
            
            // Build secure command
            List<String> command = Arrays.asList(
                "aircrack-ng", "-w", wordlistPath.toString(),
                "-b", handshake.getTargetNetwork().getBssid(),
                handshake.getCaptureFilePath().toString()
            );
            
            ProcessResult result = executeSecureCommand(command, 3600); // 1 hour timeout
            
            // Parse results
            String password = parseAircrackOutput(result.stdout);
            boolean success = password != null && !password.isEmpty();
            
            return WiFiAttackResult.builder()
                .targetNetwork(handshake.getTargetNetwork())
                .attackType(WiFiAttackResult.AttackType.DICTIONARY)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .isSuccessful(success)
                .discoveredPassword(password)
                .wordlistUsed(wordlistPath.toString())
                .passwordsAttempted(countKeyAttempts(result.stdout))
                .errorMessage(success ? null : "Password not found in wordlist")
                .build();
            
        } catch (Exception e) {
            log.error("Error cracking password for network: {}", handshake.getTargetNetwork().getSsid(), e);
            return WiFiAttackResult.builder()
                .targetNetwork(handshake.getTargetNetwork())
                .attackType(WiFiAttackResult.AttackType.DICTIONARY)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .isSuccessful(false)
                .errorMessage("Crack failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Execute a command securely with timeout and validation
     */
    private ProcessResult executeSecureCommand(List<String> command, int timeoutSeconds) throws IOException, InterruptedException {
        // Validate command
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }
        
        // Log command (without sensitive parameters)
        log.debug("Executing secure command: {}", sanitizeCommandForLogging(command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        
        Process process = pb.start();
        
        // Capture output
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        
        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            } catch (IOException e) {
                log.debug("Error reading stdout", e);
            }
        });
        
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            } catch (IOException e) {
                log.debug("Error reading stderr", e);
            }
        });
        
        stdoutReader.start();
        stderrReader.start();
        
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new InterruptedException("Command timed out after " + timeoutSeconds + " seconds");
        }
        
        stdoutReader.join(1000);
        stderrReader.join(1000);
        
        int exitCode = process.exitValue();
        
        return new ProcessResult(exitCode, stdout.toString(), stderr.toString());
    }
    
    /**
     * Parse airodump CSV output to extract WiFi networks
     */
    private List<WiFiNetwork> parseAirodumpCsv(Path csvFile) {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        try (var lines = Files.lines(csvFile)) {
            final boolean[] inNetworkSection = {false};
            
            lines.forEach(line -> {
                if (line.trim().isEmpty()) return;
                
                if (line.contains("BSSID")) {
                    inNetworkSection[0] = true;
                    return;
                }
                
                if (line.contains("Station MAC")) {
                    inNetworkSection[0] = false;
                    return;
                }
                
                if (inNetworkSection[0] && !line.startsWith("BSSID")) {
                    try {
                        WiFiNetwork network = parseNetworkLine(line);
                        if (network != null) {
                            networks.add(network);
                        }
                    } catch (Exception e) {
                        log.debug("Error parsing network line: {}", line, e);
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("Error parsing airodump CSV file: {}", csvFile, e);
        }
        
        return networks;
    }
    
    /**
     * Parse a single network line from airodump CSV
     */
    private WiFiNetwork parseNetworkLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 14) return null;
        
        try {
            String bssid = parts[0].trim();
            String ssid = parts[13].trim();
            int channel = Integer.parseInt(parts[3].trim());
            String encryption = parts[5].trim();
            int power = Integer.parseInt(parts[8].trim());
            
            // Validate parsed data
            inputValidator.validateBSSID(bssid);
            if (!ssid.isEmpty()) {
                inputValidator.validateSSID(ssid);
            }
            inputValidator.validateChannel(String.valueOf(channel));
            
            return WiFiNetwork.builder()
                .ssid(ssid.isEmpty() ? "<hidden>" : ssid)
                .bssid(bssid)
                .channel(String.valueOf(channel))
                .encryptionType(encryption)
                .signalStrength(power)
                .isHidden(ssid.isEmpty())
                .discoveredAt(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.debug("Error parsing network data from line: {}", line, e);
            return null;
        }
    }
    
    /**
     * Verify if handshake was captured in the cap file
     */
    private boolean verifyHandshakeCapture(Path capFile, String bssid) {
        try {
            // Use aircrack-ng to check if handshake exists
            List<String> command = Arrays.asList(
                "aircrack-ng", "-b", bssid, capFile.toString()
            );
            
            ProcessResult result = executeSecureCommand(command, 30);
            
            // Check if output contains handshake confirmation
            return result.stdout.contains("1 handshake") || result.stdout.contains("handshake");
            
        } catch (Exception e) {
            log.debug("Error verifying handshake: {}", capFile, e);
            return false;
        }
    }
    
    /**
     * Parse aircrack-ng output to extract password
     */
    private String parseAircrackOutput(String output) {
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("KEY FOUND!")) {
                // Extract password from line like "KEY FOUND! [ password ]"
                int start = line.indexOf('[');
                int end = line.indexOf(']');
                if (start != -1 && end != -1 && end > start) {
                    return line.substring(start + 1, end).trim();
                }
            }
        }
        return null;
    }
    
    /**
     * Count key attempts from aircrack output
     */
    private int countKeyAttempts(String output) {
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("Tested") && line.contains("keys")) {
                try {
                    String[] parts = line.split("\\s+");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if ("Tested".equals(parts[i])) {
                            return Integer.parseInt(parts[i + 1]);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.debug("Error parsing key count from: {}", line);
                }
            }
        }
        return 0;
    }
    
    /**
     * Check if interface is wireless
     */
    private boolean isWirelessInterface(String interfaceName) {
        try {
            Path wirelessPath = Paths.get("/sys/class/net", interfaceName, "wireless");
            return Files.exists(wirelessPath);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Clean up temporary files
     */
    private void cleanupTempFiles(String prefix) {
        try {
            // Common airodump output files
            String[] extensions = {"-01.csv", "-01.cap", "-01.kismet.csv", "-01.kismet.netxml"};
            
            for (String ext : extensions) {
                Path file = Paths.get(prefix + ext);
                if (Files.exists(file)) {
                    Files.delete(file);
                    log.debug("Cleaned up temp file: {}", file);
                }
            }
        } catch (Exception e) {
            log.debug("Error cleaning up temp files with prefix: {}", prefix, e);
        }
    }
    
    /**
     * Sanitize command for logging (remove sensitive parameters)
     */
    private List<String> sanitizeCommandForLogging(List<String> command) {
        List<String> sanitized = new ArrayList<>();
        boolean skipNext = false;
        
        for (String arg : command) {
            if (skipNext) {
                sanitized.add("***");
                skipNext = false;
            } else if ("-w".equals(arg) || "--write".equals(arg)) {
                sanitized.add(arg);
                skipNext = true;
            } else {
                sanitized.add(arg);
            }
        }
        
        return sanitized;
    }
    
    /**
     * Process execution result
     */
    private static class ProcessResult {
        final int exitCode;
        final String stdout;
        final String stderr;
        
        ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
