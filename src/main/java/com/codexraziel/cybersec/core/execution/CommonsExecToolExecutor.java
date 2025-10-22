package com.codexraziel.cybersec.core.execution;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Professional implementation of ToolExecutor using Apache Commons Exec
 */
@Slf4j
@Service
public class CommonsExecToolExecutor implements ToolExecutor {
    
    private final Map<String, ExecuteWatchdog> runningProcesses = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Override
    public ExecutionResult execute(ToolConfig config) {
        try {
            return executeAsync(config).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Execution interrupted: {}", config.getToolName(), e);
            return ExecutionResult.error(config.getToolName(), e);
        }
    }
    
    @Override
    public CompletableFuture<ExecutionResult> executeAsync(ToolConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            return executeInternal(config, null, null);
        }, executorService);
    }
    
    @Override
    public CompletableFuture<ExecutionResult> executeWithOutput(
            ToolConfig config,
            Consumer<String> stdoutCallback,
            Consumer<String> stderrCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            return executeInternal(config, stdoutCallback, stderrCallback);
        }, executorService);
    }
    
    private ExecutionResult executeInternal(
            ToolConfig config,
            Consumer<String> stdoutCallback,
            Consumer<String> stderrCallback) {
        
        Instant startTime = Instant.now();
        
        try {
            // Build command line
            CommandLine commandLine = new CommandLine(config.getExecutablePath());
            if (config.getArguments() != null) {
                config.getArguments().forEach(commandLine::addArgument);
            }
            
            // Setup executor
            DefaultExecutor executor = new DefaultExecutor();
            executor.setExitValues(null); // Accept all exit codes
            
            if (config.getWorkingDirectory() != null) {
                executor.setWorkingDirectory(config.getWorkingDirectory().toFile());
            }
            
            // Setup watchdog for timeout
            ExecuteWatchdog watchdog = new ExecuteWatchdog(config.getTimeout().toMillis());
            executor.setWatchdog(watchdog);
            
            String processId = generateProcessId(config);
            runningProcesses.put(processId, watchdog);
            
            // Capture output
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            
            PumpStreamHandler streamHandler;
            if (stdoutCallback != null || stderrCallback != null) {
                streamHandler = new PumpStreamHandler(
                    new CallbackOutputStream(stdout, stdoutCallback),
                    new CallbackOutputStream(stderr, stderrCallback)
                );
            } else {
                streamHandler = new PumpStreamHandler(stdout, stderr);
            }
            
            executor.setStreamHandler(streamHandler);
            
            // Execute
            log.info("Executing: {}", commandLine);
            int exitCode;
            
            try {
                exitCode = executor.execute(commandLine, config.getEnvironment());
            } catch (ExecuteException e) {
                exitCode = e.getExitValue();
            } finally {
                runningProcesses.remove(processId);
            }
            
            Instant endTime = Instant.now();
            
            // Build result
            return ExecutionResult.builder()
                    .toolName(config.getToolName())
                    .exitCode(exitCode)
                    .stdout(stdout.toString())
                    .stderr(stderr.toString())
                    .startTime(startTime)
                    .endTime(endTime)
                    .successful(exitCode == 0)
                    .build();
                    
        } catch (IOException e) {
            log.error("Execution failed: {}", config.getToolName(), e);
            return ExecutionResult.error(config.getToolName(), e);
        }
    }
    
    @Override
    public void stop(String processId) {
        ExecuteWatchdog watchdog = runningProcesses.get(processId);
        if (watchdog != null) {
            watchdog.destroyProcess();
            runningProcesses.remove(processId);
            log.info("Stopped process: {}", processId);
        }
    }
    
    @Override
    public boolean isToolAvailable(String toolName) {
        try {
            ToolConfig config = ToolConfig.simple(toolName, "--version");
            config.setTimeout(java.time.Duration.ofSeconds(5));
            ExecutionResult result = execute(config);
            return result.isSuccess() || result.getExitCode() == 1; // Some tools return 1 for --version
        } catch (Exception e) {
            log.debug("Tool not available: {}", toolName);
            return false;
        }
    }
    
    @Override
    public String getToolVersion(String toolName) {
        try {
            ToolConfig config = ToolConfig.simple(toolName, "--version");
            config.setTimeout(java.time.Duration.ofSeconds(5));
            ExecutionResult result = execute(config);
            
            String output = result.getStdout() != null ? result.getStdout() : result.getStderr();
            if (output != null) {
                // Extract first line which usually contains version
                return output.lines().findFirst().orElse("unknown");
            }
        } catch (Exception e) {
            log.debug("Could not get version for: {}", toolName);
        }
        return "unknown";
    }
    
    private String generateProcessId(ToolConfig config) {
        return config.getToolName() + "-" + System.currentTimeMillis();
    }
    
    /**
     * OutputStream that calls a callback for each line
     */
    private static class CallbackOutputStream extends OutputStream {
        private final ByteArrayOutputStream buffer;
        private final Consumer<String> callback;
        private final StringBuilder lineBuffer = new StringBuilder();
        
        public CallbackOutputStream(ByteArrayOutputStream buffer, Consumer<String> callback) {
            this.buffer = buffer;
            this.callback = callback;
        }
        
        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
            
            if (callback != null) {
                char c = (char) b;
                if (c == '\n') {
                    callback.accept(lineBuffer.toString());
                    lineBuffer.setLength(0);
                } else {
                    lineBuffer.append(c);
                }
            }
        }
    }
}
