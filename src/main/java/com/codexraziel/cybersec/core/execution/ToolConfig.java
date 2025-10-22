package com.codexraziel.cybersec.core.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration for tool execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolConfig {
    
    private String toolName;
    private String executablePath;
    private List<String> arguments;
    private Map<String, String> environment;
    private Path workingDirectory;
    
    @Builder.Default
    private Duration timeout = Duration.ofMinutes(5);
    
    @Builder.Default
    private boolean captureOutput = true;
    
    @Builder.Default
    private boolean inheritIO = false;
    
    @Builder.Default
    private boolean privilegedExecution = false;
    
    private String sudoPassword;
    
    /**
     * Creates a simple config with just executable and args
     */
    public static ToolConfig simple(String executable, String... args) {
        return ToolConfig.builder()
                .executablePath(executable)
                .arguments(List.of(args))
                .build();
    }
    
    /**
     * Creates a config for sudo execution
     */
    public static ToolConfig withSudo(String executable, String password, String... args) {
        return ToolConfig.builder()
                .executablePath(executable)
                .arguments(List.of(args))
                .privilegedExecution(true)
                .sudoPassword(password)
                .build();
    }
}
