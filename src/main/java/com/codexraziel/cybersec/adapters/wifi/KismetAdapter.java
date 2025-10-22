package com.codexraziel.cybersec.adapters.wifi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Professional adapter for Kismet REST API
 * Integrates with Kismet wireless monitoring system
 */
@Slf4j
@Component
public class KismetAdapter {
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private String kismetUrl = "http://localhost:2501";
    private String apiKey;
    
    public KismetAdapter() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Configure Kismet connection
     */
    public void configure(String url, String apiKey) {
        this.kismetUrl = url;
        this.apiKey = apiKey;
        log.info("Kismet configured: {}", url);
    }
    
    /**
     * Check if Kismet is available
     */
    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SystemStatus status = getSystemStatus().join();
                return status != null && status.isRunning();
            } catch (Exception e) {
                log.debug("Kismet not available", e);
                return false;
            }
        });
    }
    
    /**
     * Get system status
     */
    public CompletableFuture<SystemStatus> getSystemStatus() {
        return executeRequest("/system/status.json", SystemStatus.class);
    }
    
    /**
     * Get all detected devices
     */
    public CompletableFuture<List<WiFiDevice>> getDevices() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = executeGetRequest("/devices/views/all/devices.json").join();
                JsonNode root = objectMapper.readTree(json);
                
                List<WiFiDevice> devices = new ArrayList<>();
                
                if (root.isArray()) {
                    for (JsonNode deviceNode : root) {
                        WiFiDevice device = parseDevice(deviceNode);
                        if (device != null) {
                            devices.add(device);
                        }
                    }
                }
                
                return devices;
            } catch (Exception e) {
                log.error("Failed to get devices", e);
                return List.of();
            }
        });
    }
    
    /**
     * Get devices by type (WiFi AP, WiFi Client, Bluetooth, etc)
     */
    public CompletableFuture<List<WiFiDevice>> getDevicesByType(String deviceType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = String.format("/devices/views/phy-%s/devices.json", deviceType);
                String json = executeGetRequest(endpoint).join();
                JsonNode root = objectMapper.readTree(json);
                
                List<WiFiDevice> devices = new ArrayList<>();
                
                if (root.isArray()) {
                    for (JsonNode deviceNode : root) {
                        WiFiDevice device = parseDevice(deviceNode);
                        if (device != null) {
                            devices.add(device);
                        }
                    }
                }
                
                return devices;
            } catch (Exception e) {
                log.error("Failed to get devices by type", e);
                return List.of();
            }
        });
    }
    
    /**
     * Get WiFi access points
     */
    public CompletableFuture<List<WiFiDevice>> getAccessPoints() {
        return getDevicesByType("IEEE802.11");
    }
    
    /**
     * Get alerts
     */
    public CompletableFuture<List<Alert>> getAlerts() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = executeGetRequest("/alerts/all_alerts.json").join();
                JsonNode root = objectMapper.readTree(json);
                
                List<Alert> alerts = new ArrayList<>();
                
                if (root.isArray()) {
                    for (JsonNode alertNode : root) {
                        Alert alert = parseAlert(alertNode);
                        if (alert != null) {
                            alerts.add(alert);
                        }
                    }
                }
                
                return alerts;
            } catch (Exception e) {
                log.error("Failed to get alerts", e);
                return List.of();
            }
        });
    }
    
    /**
     * Get device by MAC address
     */
    public CompletableFuture<WiFiDevice> getDeviceByMac(String mac) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = mac.replace(":", "").toUpperCase();
                String endpoint = String.format("/devices/by-mac/%s/device.json", key);
                String json = executeGetRequest(endpoint).join();
                JsonNode deviceNode = objectMapper.readTree(json);
                
                return parseDevice(deviceNode);
            } catch (Exception e) {
                log.error("Failed to get device by MAC", e);
                return null;
            }
        });
    }
    
    /**
     * Get GPS location data
     */
    public CompletableFuture<GpsData> getGpsData() {
        return executeRequest("/gps/location.json", GpsData.class);
    }
    
    // HTTP request helpers
    
    private CompletableFuture<String> executeGetRequest(String endpoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request.Builder requestBuilder = new Request.Builder()
                        .url(kismetUrl + endpoint)
                        .get();
                
                // Add API key if configured
                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.addHeader("KISMET", apiKey);
                }
                
                Request request = requestBuilder.build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response: " + response);
                    }
                    
                    return response.body().string();
                }
            } catch (IOException e) {
                log.error("HTTP request failed: {}", endpoint, e);
                throw new RuntimeException(e);
            }
        });
    }
    
    private <T> CompletableFuture<T> executeRequest(String endpoint, Class<T> responseClass) {
        return executeGetRequest(endpoint).thenApply(json -> {
            try {
                return objectMapper.readValue(json, responseClass);
            } catch (Exception e) {
                log.error("Failed to parse response", e);
                return null;
            }
        });
    }
    
    // Parsing helpers
    
    private WiFiDevice parseDevice(JsonNode node) {
        try {
            String macAddr = node.path("kismet.device.base.macaddr").asText("");
            String name = node.path("kismet.device.base.name").asText("");
            String type = node.path("kismet.device.base.type").asText("");
            
            // WiFi specific fields
            String ssid = node.path("dot11.device").path("dot11.device.last_beaconed_ssid").asText("");
            int channel = node.path("dot11.device").path("dot11.device.channel").asInt(0);
            
            // Signal strength
            int signalDbm = node.path("kismet.device.base.signal").path("kismet.common.signal.last_signal").asInt(-100);
            
            // Packets
            long packetsTotal = node.path("kismet.device.base.packets.total").asLong(0);
            
            // First and last seen
            long firstTime = node.path("kismet.device.base.first_time").asLong(0);
            long lastTime = node.path("kismet.device.base.last_time").asLong(0);
            
            return WiFiDevice.builder()
                    .macAddress(macAddr)
                    .name(name)
                    .deviceType(type)
                    .ssid(ssid)
                    .channel(channel)
                    .signalStrength(signalDbm)
                    .totalPackets(packetsTotal)
                    .firstSeen(Instant.ofEpochSecond(firstTime))
                    .lastSeen(Instant.ofEpochSecond(lastTime))
                    .build();
                    
        } catch (Exception e) {
            log.warn("Failed to parse device", e);
            return null;
        }
    }
    
    private Alert parseAlert(JsonNode node) {
        try {
            String alertType = node.path("kismet.alert.class").asText("");
            String text = node.path("kismet.alert.text").asText("");
            long timestamp = node.path("kismet.alert.timestamp").asLong(0);
            
            return Alert.builder()
                    .alertType(alertType)
                    .text(text)
                    .timestamp(Instant.ofEpochSecond(timestamp))
                    .build();
                    
        } catch (Exception e) {
            log.warn("Failed to parse alert", e);
            return null;
        }
    }
    
    // Data models
    
    @Data
    @Builder
    public static class WiFiDevice {
        private String macAddress;
        private String name;
        private String deviceType;
        private String ssid;
        private int channel;
        private int signalStrength;
        private long totalPackets;
        private Instant firstSeen;
        private Instant lastSeen;
        
        public boolean isAccessPoint() {
            return deviceType != null && deviceType.contains("AP");
        }
        
        public boolean isClient() {
            return deviceType != null && deviceType.contains("Client");
        }
    }
    
    @Data
    @Builder
    public static class Alert {
        private String alertType;
        private String text;
        private Instant timestamp;
    }
    
    @Data
    public static class SystemStatus {
        private String version;
        private String serverName;
        private long uptime;
        private int memoryUsage;
        private int deviceCount;
        private int packetRate;
        
        public boolean isRunning() {
            return version != null && !version.isEmpty();
        }
    }
    
    @Data
    @Builder
    public static class GpsData {
        private double latitude;
        private double longitude;
        private double altitude;
        private double speed;
        private String fix;
        private Instant timestamp;
    }
}
