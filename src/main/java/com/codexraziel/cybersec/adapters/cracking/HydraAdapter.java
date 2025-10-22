package com.codexraziel.cybersec.adapters.cracking;

import com.codexraziel.cybersec.core.execution.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Professional adapter for THC-Hydra - Network service bruteforcer
 * Supports 50+ protocols including SSH, FTP, HTTP, RDP, SMB, etc.
 */
@Slf4j
@Component
public class HydraAdapter {
    
    @Autowired
    private ToolExecutor toolExecutor;
    
    private static final String HYDRA = "hydra";
    
    // Supported protocols
    public static final String SSH = "ssh";
    public static final String FTP = "ftp";
    public static final String HTTP_GET = "http-get";
    public static final String HTTP_POST = "http-post-form";
    public static final String HTTPS_GET = "https-get";
    public static final String HTTPS_POST = "https-post-form";
    public static final String MYSQL = "mysql";
    public static final String POSTGRES = "postgres";
    public static final String SMB = "smb";
    public static final String RDP = "rdp";
    public static final String TELNET = "telnet";
    public static final String VNC = "vnc";
    public static final String SMTP = "smtp";
    public static final String POP3 = "pop3";
    public static final String IMAP = "imap";
    
    /**
     * Check if Hydra is available
     */
    public boolean isAvailable() {
        return toolExecutor.isToolAvailable(HYDRA);
    }
    
    /**
     * Get Hydra version
     */
    public String getVersion() {
        return toolExecutor.getToolVersion(HYDRA);
    }
    
    /**
     * Bruteforce a service with username and password lists
     */
    public CompletableFuture<BruteForceResult> bruteforce(
            BruteForceConfig config,
            Consumer<BruteForceProgress> progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<String> args = buildHydraArgs(config);
            
            ToolConfig toolConfig = ToolConfig.builder()
                    .toolName(HYDRA)
                    .executablePath(HYDRA)
                    .arguments(args)
                    .timeout(config.getTimeout())
                    .build();
            
            List<Credential> foundCredentials = new ArrayList<>();
            StringBuilder output = new StringBuilder();
            
            ExecutionResult result = toolExecutor.executeWithOutput(
                    toolConfig,
                    line -> {
                        output.append(line).append("\n");
                        log.debug("Hydra output: {}", line);
                        
                        // Parse found credentials
                        Credential cred = parseCredential(line);
                        if (cred != null) {
                            foundCredentials.add(cred);
                            log.info("Found credential: {}@{}", cred.getUsername(), config.getTarget());
                        }
                        
                        // Parse and send progress
                        BruteForceProgress progress = parseProgress(line);
                        if (progress != null && progressCallback != null) {
                            progressCallback.accept(progress);
                        }
                    },
                    line -> log.warn("Hydra stderr: {}", line)
            ).join();
            
            return BruteForceResult.builder()
                    .success(!foundCredentials.isEmpty())
                    .foundCount(foundCredentials.size())
                    .credentials(foundCredentials)
                    .output(output.toString())
                    .duration(result.getDuration())
                    .exitCode(result.getExitCode())
                    .build();
        });
    }
    
    /**
     * Bruteforce SSH service
     */
    public CompletableFuture<BruteForceResult> bruteforceSSH(
            String target,
            int port,
            String usernameListPath,
            String passwordListPath,
            Consumer<BruteForceProgress> progressCallback) {
        
        BruteForceConfig config = BruteForceConfig.builder()
                .protocol(SSH)
                .target(target)
                .port(port)
                .usernameListPath(usernameListPath)
                .passwordListPath(passwordListPath)
                .threads(16)
                .verbose(true)
                .timeout(Duration.ofMinutes(30))
                .build();
        
        return bruteforce(config, progressCallback);
    }
    
    /**
     * Bruteforce FTP service
     */
    public CompletableFuture<BruteForceResult> bruteforceFTP(
            String target,
            String usernameListPath,
            String passwordListPath,
            Consumer<BruteForceProgress> progressCallback) {
        
        BruteForceConfig config = BruteForceConfig.builder()
                .protocol(FTP)
                .target(target)
                .port(21)
                .usernameListPath(usernameListPath)
                .passwordListPath(passwordListPath)
                .threads(16)
                .verbose(true)
                .timeout(Duration.ofMinutes(30))
                .build();
        
        return bruteforce(config, progressCallback);
    }
    
    /**
     * Bruteforce HTTP form
     */
    public CompletableFuture<BruteForceResult> bruteforceHTTPForm(
            String target,
            String formPath,
            String formParameters,
            String failureString,
            String usernameListPath,
            String passwordListPath,
            Consumer<BruteForceProgress> progressCallback) {
        
        BruteForceConfig config = BruteForceConfig.builder()
                .protocol(HTTP_POST)
                .target(target)
                .port(80)
                .usernameListPath(usernameListPath)
                .passwordListPath(passwordListPath)
                .formPath(formPath)
                .formParameters(formParameters)
                .failureString(failureString)
                .threads(10)
                .verbose(true)
                .timeout(Duration.ofHours(1))
                .build();
        
        return bruteforce(config, progressCallback);
    }
    
    /**
     * Bruteforce with single username
     */
    public CompletableFuture<BruteForceResult> bruteforceWithUsername(
            String protocol,
            String target,
            int port,
            String username,
            String passwordListPath,
            Consumer<BruteForceProgress> progressCallback) {
        
        BruteForceConfig config = BruteForceConfig.builder()
                .protocol(protocol)
                .target(target)
                .port(port)
                .username(username)
                .passwordListPath(passwordListPath)
                .threads(16)
                .verbose(true)
                .timeout(Duration.ofMinutes(30))
                .build();
        
        return bruteforce(config, progressCallback);
    }
    
    // Helper methods
    
    private List<String> buildHydraArgs(BruteForceConfig config) {
        List<String> args = new ArrayList<>();
        
        // Username(s)
        if (config.getUsername() != null) {
            args.add("-l");
            args.add(config.getUsername());
        } else if (config.getUsernameListPath() != null) {
            args.add("-L");
            args.add(config.getUsernameListPath());
        }
        
        // Password(s)
        if (config.getPassword() != null) {
            args.add("-p");
            args.add(config.getPassword());
        } else if (config.getPasswordListPath() != null) {
            args.add("-P");
            args.add(config.getPasswordListPath());
        }
        
        // Threads
        args.add("-t");
        args.add(String.valueOf(config.getThreads()));
        
        // Verbose
        if (config.isVerbose()) {
            args.add("-V");
        }
        
        // Exit on first found
        if (config.isExitOnFirstFound()) {
            args.add("-f");
        }
        
        // Output file
        if (config.getOutputFile() != null) {
            args.add("-o");
            args.add(config.getOutputFile());
        }
        
        // Continue on errors
        if (config.isContinueOnErrors()) {
            args.add("-e");
            args.add("nsr"); // n=null password, s=login as password, r=reverse login
        }
        
        // Target
        args.add(config.getTarget());
        
        // Protocol-specific arguments
        if (config.getProtocol().equals(HTTP_POST) || config.getProtocol().equals(HTTPS_POST)) {
            // HTTP form: protocol://path:form_params:failure_string
            String formSpec = String.format("%s:%s:%s",
                    config.getFormPath() != null ? config.getFormPath() : "/",
                    config.getFormParameters() != null ? config.getFormParameters() : "user=^USER^&pass=^PASS^",
                    config.getFailureString() != null ? config.getFailureString() : "Login failed"
            );
            args.add(config.getProtocol());
            args.add(formSpec);
        } else {
            // Regular protocol
            args.add(config.getProtocol());
            
            // Port if not default
            if (config.getPort() > 0) {
                args.add("-s");
                args.add(String.valueOf(config.getPort()));
            }
        }
        
        return args;
    }
    
    private Credential parseCredential(String line) {
        // Hydra outputs: [port][protocol] host: login password
        // Example: [22][ssh] 192.168.1.1   login: admin   password: password123
        
        Pattern credPattern = Pattern.compile(
                "\\[(\\d+)\\]\\[([^\\]]+)\\]\\s+([^\\s]+)\\s+login:\\s+([^\\s]+)\\s+password:\\s+(.+)"
        );
        
        Matcher matcher = credPattern.matcher(line);
        if (matcher.find()) {
            return Credential.builder()
                    .port(Integer.parseInt(matcher.group(1)))
                    .protocol(matcher.group(2))
                    .host(matcher.group(3))
                    .username(matcher.group(4))
                    .password(matcher.group(5).trim())
                    .build();
        }
        
        return null;
    }
    
    private BruteForceProgress parseProgress(String line) {
        // Parse progress from verbose output
        if (line.contains("valid password found") || line.contains("login:")) {
            return BruteForceProgress.builder()
                    .message(line)
                    .found(true)
                    .build();
        }
        
        // Parse attempt count (not always available in Hydra output)
        Pattern attemptPattern = Pattern.compile("(\\d+) of (\\d+) target");
        Matcher matcher = attemptPattern.matcher(line);
        
        if (matcher.find()) {
            int current = Integer.parseInt(matcher.group(1));
            int total = Integer.parseInt(matcher.group(2));
            
            return BruteForceProgress.builder()
                    .current(current)
                    .total(total)
                    .percentage((current * 100.0) / total)
                    .message(line)
                    .build();
        }
        
        return null;
    }
    
    // Result classes
    
    @Data
    @Builder
    public static class BruteForceConfig {
        private String protocol;
        private String target;
        private int port;
        private String username;
        private String password;
        private String usernameListPath;
        private String passwordListPath;
        private String formPath;
        private String formParameters;
        private String failureString;
        private int threads;
        private boolean verbose;
        private boolean exitOnFirstFound;
        private boolean continueOnErrors;
        private String outputFile;
        private Duration timeout;
    }
    
    @Data
    @Builder
    public static class BruteForceResult {
        private boolean success;
        private int foundCount;
        private List<Credential> credentials;
        private String output;
        private Duration duration;
        private int exitCode;
    }
    
    @Data
    @Builder
    public static class BruteForceProgress {
        private long current;
        private long total;
        private double percentage;
        private String message;
        private boolean found;
    }
    
    @Data
    @Builder
    public static class Credential {
        private int port;
        private String protocol;
        private String host;
        private String username;
        private String password;
    }
}
