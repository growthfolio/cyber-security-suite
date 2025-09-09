package com.codexraziel.cybersec.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KeyloggerService {
    
    public static class KeyloggerStatus {
        public long eventsCaptured;
        public long eventsProcessed;
        public long bytesExfiltrated;
        public double bufferUsage;
        public String status;
        public int devices;
        public int pid;
    }
    
    public static class KeyloggerPerformance {
        public long eventsPerSecond;
        public long totalEvents;
        public long processingLag;
        public long uptime;
    }
    
    public CompletableFuture<KeyloggerStatus> getStatus() {
        return CompletableFuture.supplyAsync(() -> {
            KeyloggerStatus status = new KeyloggerStatus();
            
            try {
                Map<String, String> data = readStatusFile("/tmp/.keylogger_status");
                
                status.eventsCaptured = parseLong(data.get("events_captured"));
                status.eventsProcessed = parseLong(data.get("events_processed"));
                status.bytesExfiltrated = parseLong(data.get("bytes_exfiltrated"));
                status.bufferUsage = parseDouble(data.get("buffer_usage"));
                status.status = data.getOrDefault("status", "unknown");
                status.devices = parseInt(data.get("devices"));
                status.pid = parseInt(data.get("pid"));
                
            } catch (Exception e) {
                log.debug("Failed to read keylogger status: {}", e.getMessage());
                status.status = "not_running";
            }
            
            return status;
        });
    }
    
    public CompletableFuture<KeyloggerPerformance> getPerformance() {
        return CompletableFuture.supplyAsync(() -> {
            KeyloggerPerformance perf = new KeyloggerPerformance();
            
            try {
                Map<String, String> data = readStatusFile("/tmp/.keylogger_perf");
                
                perf.eventsPerSecond = parseLong(data.get("events_per_second"));
                perf.totalEvents = parseLong(data.get("total_events"));
                perf.processingLag = parseLong(data.get("processing_lag"));
                perf.uptime = parseLong(data.get("uptime"));
                
            } catch (Exception e) {
                log.debug("Failed to read keylogger performance: {}", e.getMessage());
            }
            
            return perf;
        });
    }
    
    private Map<String, String> readStatusFile(String filename) {
        Map<String, String> data = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    data.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            // File might not exist
        }
        
        return data;
    }
    
    private long parseLong(String value) {
        try {
            return value != null ? Long.parseLong(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private int parseInt(String value) {
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}