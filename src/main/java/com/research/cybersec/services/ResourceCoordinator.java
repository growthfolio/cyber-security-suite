package com.research.cybersec.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ResourceCoordinator {
    
    // Shared thread pool for all modules
    private final ThreadPoolExecutor sharedExecutor;
    private final ScheduledExecutorService scheduler;
    
    // Resource locks
    private final Map<String, Semaphore> networkInterfaces = new ConcurrentHashMap<>();
    private final Set<String> activeOperations = ConcurrentHashMap.newKeySet();
    
    // Memory monitoring
    private final AtomicLong memoryUsage = new AtomicLong(0);
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    
    // Resource limits
    private static final int MAX_THREADS = 20;
    private static final long MAX_MEMORY_MB = 512;
    private static final int NETWORK_INTERFACE_PERMITS = 1;
    
    public ResourceCoordinator() {
        // Shared thread pool with monitoring
        this.sharedExecutor = new ThreadPoolExecutor(
            4, MAX_THREADS, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r, "CyberSec-Worker-" + activeThreads.incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            (r, executor) -> {
                log.warn("Task rejected - thread pool saturated");
                throw new RejectedExecutionException("Resource limit exceeded");
            }
        );
        
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Initialize network interface locks
        initializeNetworkLocks();
        
        // Start monitoring
        startResourceMonitoring();
    }
    
    private void initializeNetworkLocks() {
        String[] interfaces = {"wlan0", "wlan1", "wlp2s0", "eth0", "enp0s3"};
        for (String iface : interfaces) {
            networkInterfaces.put(iface, new Semaphore(NETWORK_INTERFACE_PERMITS));
        }
    }
    
    public CompletableFuture<Void> executeWithResourceControl(String operationId, String networkInterface, Runnable task) {
        if (activeOperations.contains(operationId)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Operation already running: " + operationId));
        }
        
        return CompletableFuture.runAsync(() -> {
            Semaphore interfaceLock = null;
            try {
                // Check memory limit
                if (getMemoryUsageMB() > MAX_MEMORY_MB) {
                    throw new RuntimeException("Memory limit exceeded: " + getMemoryUsageMB() + "MB");
                }
                
                // Acquire network interface lock
                if (networkInterface != null) {
                    interfaceLock = networkInterfaces.get(networkInterface);
                    if (interfaceLock != null) {
                        if (!interfaceLock.tryAcquire(5, TimeUnit.SECONDS)) {
                            throw new RuntimeException("Network interface busy: " + networkInterface);
                        }
                    }
                }
                
                activeOperations.add(operationId);
                log.debug("Started operation: {} on interface: {}", operationId, networkInterface);
                
                task.run();
                
            } catch (Exception e) {
                log.error("Operation failed: {}", operationId, e);
                throw new RuntimeException(e);
            } finally {
                activeOperations.remove(operationId);
                if (interfaceLock != null) {
                    interfaceLock.release();
                }
                log.debug("Completed operation: {}", operationId);
            }
        }, sharedExecutor);
    }
    
    public boolean isResourceAvailable(String networkInterface) {
        if (getMemoryUsageMB() > MAX_MEMORY_MB * 0.9) return false;
        if (activeThreads.get() >= MAX_THREADS * 0.9) return false;
        
        if (networkInterface != null) {
            Semaphore lock = networkInterfaces.get(networkInterface);
            return lock != null && lock.availablePermits() > 0;
        }
        
        return true;
    }
    
    public void cancelOperation(String operationId) {
        activeOperations.remove(operationId);
        log.info("Cancelled operation: {}", operationId);
    }
    
    public ResourceStatus getResourceStatus() {
        return ResourceStatus.builder()
            .activeThreads(activeThreads.get())
            .maxThreads(MAX_THREADS)
            .memoryUsageMB(getMemoryUsageMB())
            .maxMemoryMB(MAX_MEMORY_MB)
            .activeOperations(activeOperations.size())
            .availableInterfaces(getAvailableInterfaces())
            .build();
    }
    
    private void startResourceMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long memoryMB = getMemoryUsageMB();
                int threads = activeThreads.get();
                
                if (memoryMB > MAX_MEMORY_MB * 0.8) {
                    log.warn("High memory usage: {}MB ({}%)", memoryMB, (memoryMB * 100 / MAX_MEMORY_MB));
                    System.gc(); // Suggest garbage collection
                }
                
                if (threads > MAX_THREADS * 0.8) {
                    log.warn("High thread usage: {} threads ({}%)", threads, (threads * 100 / MAX_THREADS));
                }
                
            } catch (Exception e) {
                log.error("Resource monitoring error", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    private long getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
    
    private int getAvailableInterfaces() {
        return (int) networkInterfaces.values().stream()
            .mapToInt(Semaphore::availablePermits)
            .sum();
    }
    
    public void shutdown() {
        sharedExecutor.shutdown();
        scheduler.shutdown();
        try {
            if (!sharedExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                sharedExecutor.shutdownNow();
            }
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ResourceStatus {
        private int activeThreads;
        private int maxThreads;
        private long memoryUsageMB;
        private long maxMemoryMB;
        private int activeOperations;
        private int availableInterfaces;
        
        public double getThreadUsagePercent() {
            return maxThreads > 0 ? (double) activeThreads / maxThreads * 100 : 0;
        }
        
        public double getMemoryUsagePercent() {
            return maxMemoryMB > 0 ? (double) memoryUsageMB / maxMemoryMB * 100 : 0;
        }
    }
}