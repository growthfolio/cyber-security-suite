package com.research.cybersec.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class MemoryManager {
    
    private final ScheduledExecutorService cleanupScheduler;
    private final AtomicLong logFileSize = new AtomicLong(0);
    
    // Cleanup thresholds
    private static final long MAX_LOG_SIZE_MB = 100;
    private static final int LOG_RETENTION_DAYS = 7;
    private static final int CLEANUP_INTERVAL_MINUTES = 30;
    
    public MemoryManager() {
        this.cleanupScheduler = Executors.newScheduledThreadPool(1);
        startAutoCleanup();
        calculateCurrentLogSize();
    }
    
    private void startAutoCleanup() {
        cleanupScheduler.scheduleAtFixedRate(() -> {
            try {
                performCleanup();
            } catch (Exception e) {
                log.error("Auto-cleanup failed", e);
            }
        }, CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
        
        log.info("Auto-cleanup started - interval: {} minutes", CLEANUP_INTERVAL_MINUTES);
    }
    
    public void performCleanup() {
        log.info("Starting memory cleanup...");
        
        long beforeMemory = getUsedMemoryMB();
        long beforeLogSize = logFileSize.get();
        
        // Clean old log files
        cleanupLogFiles();
        
        // Clean temporary files
        cleanupTempFiles();
        
        // Suggest garbage collection
        System.gc();
        
        long afterMemory = getUsedMemoryMB();
        long afterLogSize = logFileSize.get();
        
        log.info("Cleanup completed - Memory: {}MB -> {}MB, Logs: {}MB -> {}MB", 
                beforeMemory, afterMemory, beforeLogSize / (1024*1024), afterLogSize / (1024*1024));
    }
    
    private void cleanupLogFiles() {
        try {
            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir)) return;
            
            LocalDateTime cutoff = LocalDateTime.now().minus(LOG_RETENTION_DAYS, ChronoUnit.DAYS);
            
            Files.walk(logsDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".log"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toInstant()
                            .isBefore(cutoff.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        long size = Files.size(path);
                        Files.delete(path);
                        logFileSize.addAndGet(-size);
                        log.debug("Deleted old log file: {}", path);
                    } catch (IOException e) {
                        log.warn("Failed to delete log file: {}", path, e);
                    }
                });
                
        } catch (IOException e) {
            log.error("Log cleanup failed", e);
        }
    }
    
    private void cleanupTempFiles() {
        try {
            // Clean system temp directory
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            
            Files.walk(tempDir, 1)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith("cybersec_"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toInstant()
                            .isBefore(LocalDateTime.now().minus(1, ChronoUnit.HOURS)
                                .atZone(java.time.ZoneId.systemDefault()).toInstant());
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.debug("Deleted temp file: {}", path);
                    } catch (IOException e) {
                        log.debug("Could not delete temp file: {}", path);
                    }
                });
                
        } catch (IOException e) {
            log.error("Temp file cleanup failed", e);
        }
    }
    
    private void calculateCurrentLogSize() {
        try {
            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir)) {
                logFileSize.set(0);
                return;
            }
            
            long totalSize = Files.walk(logsDir)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
                
            logFileSize.set(totalSize);
            log.info("Current log size: {}MB", totalSize / (1024 * 1024));
            
        } catch (IOException e) {
            log.error("Failed to calculate log size", e);
        }
    }
    
    public void rotateLogIfNeeded(String logFileName) {
        try {
            Path logFile = Paths.get("logs", logFileName);
            if (!Files.exists(logFile)) return;
            
            long size = Files.size(logFile);
            if (size > MAX_LOG_SIZE_MB * 1024 * 1024) {
                String timestamp = LocalDateTime.now().toString().replace(":", "-");
                Path rotatedFile = Paths.get("logs", logFileName + "." + timestamp);
                
                Files.move(logFile, rotatedFile);
                Files.createFile(logFile);
                
                log.info("Rotated log file: {} -> {}", logFile, rotatedFile);
            }
            
        } catch (IOException e) {
            log.error("Log rotation failed for: {}", logFileName, e);
        }
    }
    
    public MemoryStatus getMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return MemoryStatus.builder()
            .usedMemoryMB(usedMemory / (1024 * 1024))
            .totalMemoryMB(totalMemory / (1024 * 1024))
            .maxMemoryMB(maxMemory / (1024 * 1024))
            .freeMemoryMB(freeMemory / (1024 * 1024))
            .logSizeMB(logFileSize.get() / (1024 * 1024))
            .build();
    }
    
    private long getUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
    
    public void forceCleanup() {
        log.info("Force cleanup requested");
        performCleanup();
    }
    
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MemoryStatus {
        private long usedMemoryMB;
        private long totalMemoryMB;
        private long maxMemoryMB;
        private long freeMemoryMB;
        private long logSizeMB;
        
        public double getUsagePercent() {
            return maxMemoryMB > 0 ? (double) usedMemoryMB / maxMemoryMB * 100 : 0;
        }
        
        public boolean isHighUsage() {
            return getUsagePercent() > 80;
        }
    }
}