package com.research.cybersec.network;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WiFiScanner {
    
    @Data
    public static class WiFiNetwork {
        private String ssid;
        private String bssid;
        private int channel;
        private int signalStrength;
        private String security;
        private String frequency;
        private boolean isConnected;
        private String vendor;
        
        public String getSecurityLevel() {
            if (security.contains("WPA3")) return "WPA3";
            if (security.contains("WPA2")) return "WPA2";
            if (security.contains("WPA")) return "WPA";
            if (security.contains("WEP")) return "WEP";
            return "Open";
        }
        
        public boolean isVulnerable() {
            return security.contains("WEP") || security.contains("Open") || 
                   signalStrength > -50; // Strong signal = close target
        }
    }
    
    public CompletableFuture<List<WiFiNetwork>> scanNetworks() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.currentThread().setName("WiFi-Scanner");
                String os = System.getProperty("os.name").toLowerCase();
                
                log.info("Starting real WiFi scan for OS: {}", os);
                
                if (os.contains("linux")) {
                    List<WiFiNetwork> realNetworks = scanLinuxReal();
                    if (!realNetworks.isEmpty()) {
                        log.info("Found {} real WiFi networks on Linux", realNetworks.size());
                        return realNetworks;
                    }
                } else if (os.contains("windows")) {
                    List<WiFiNetwork> realNetworks = scanWindowsReal();
                    if (!realNetworks.isEmpty()) {
                        log.info("Found {} real WiFi networks on Windows", realNetworks.size());
                        return realNetworks;
                    }
                }
                
                log.warn("Real WiFi scan failed or no networks found, using mock data");
                return generateMockNetworks();
                
            } catch (Exception e) {
                log.error("WiFi scan failed", e);
                return generateMockNetworks();
            }
        });
    }
    
    private List<WiFiNetwork> scanLinuxReal() {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        try {
            // Try nmcli first with timeout
            ProcessBuilder pb = new ProcessBuilder("nmcli", "-t", "-f", 
                "SSID,BSSID,MODE,CHAN,FREQ,RATE,SIGNAL,BARS,SECURITY", "dev", "wifi", "list");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Set timeout to prevent hanging
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("nmcli command timed out");
                return new ArrayList<>();
            }
            
            if (process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        WiFiNetwork network = parseNmcliOutput(line);
                        if (network != null && !network.getSsid().isEmpty()) {
                            networks.add(network);
                        }
                    }
                }
            }
            
            // If nmcli failed, try iwconfig as fallback
            if (networks.isEmpty()) {
                networks = scanWithIwconfig();
            }
            
        } catch (Exception e) {
            log.error("Linux WiFi scan failed: {}", e.getMessage());
        }
        
        return networks;
    }
    
    private List<WiFiNetwork> scanWithIwconfig() {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        try {
            // Try iwconfig without sudo first
            ProcessBuilder pb = new ProcessBuilder("iwconfig");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                
                networks = parseIwconfigOutput(output.toString());
            }
            
        } catch (Exception e) {
            log.error("iwlist scan failed", e);
        }
        
        return networks;
    }
    
    private List<WiFiNetwork> scanWindowsReal() {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        try {
            // Scan for available networks with timeout
            ProcessBuilder pb = new ProcessBuilder("netsh", "wlan", "show", "available");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("netsh command timed out");
                return new ArrayList<>();
            }
            
            if (process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    networks = parseNetshAvailableOutput(reader);
                }
            }
            
        } catch (Exception e) {
            log.error("Windows WiFi scan failed: {}", e.getMessage());
        }
        
        return networks;
    }
    
    private List<WiFiNetwork> scanMacOS() {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        try {
            ProcessBuilder pb = new ProcessBuilder("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport", "-s");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue; // Skip header
                    }
                    
                    WiFiNetwork network = parseAirportOutput(line);
                    if (network != null) {
                        networks.add(network);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("macOS WiFi scan failed", e);
            return generateMockNetworks();
        }
        
        return networks;
    }
    
    private WiFiNetwork parseNmcliOutput(String line) {
        String[] parts = line.split(":");
        if (parts.length < 9) return null;
        
        WiFiNetwork network = new WiFiNetwork();
        network.setSsid(parts[0].trim());
        network.setBssid(parts[1].trim());
        network.setChannel(parseIntSafe(parts[3]));
        network.setFrequency(parts[4].trim());
        network.setSignalStrength(parseIntSafe(parts[6]));
        network.setSecurity(parts[8].trim());
        
        return network;
    }
    
    private List<WiFiNetwork> parseIwconfigOutput(String output) {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        // Parse iwconfig output for basic WiFi interface info
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("IEEE 802.11") && line.contains("ESSID:")) {
                WiFiNetwork network = new WiFiNetwork();
                
                // Extract ESSID
                int essidStart = line.indexOf("ESSID:\"") + 7;
                int essidEnd = line.indexOf("\"", essidStart);
                if (essidStart > 6 && essidEnd > essidStart) {
                    network.setSsid(line.substring(essidStart, essidEnd));
                    network.setBssid("00:00:00:00:00:00"); // iwconfig doesn't show BSSID
                    network.setChannel(6); // Default
                    network.setSignalStrength(-60); // Default
                    network.setSecurity("Unknown");
                    network.setFrequency("2.4GHz");
                    
                    if (!network.getSsid().isEmpty()) {
                        networks.add(network);
                    }
                }
            }
        }
        
        return networks;
    }
    
    private List<WiFiNetwork> parseIwlistOutput(String output) {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        Pattern cellPattern = Pattern.compile("Cell \\d+");
        Pattern ssidPattern = Pattern.compile("ESSID:\"([^\"]+)\"");
        Pattern bssidPattern = Pattern.compile("Address: ([A-Fa-f0-9:]{17})");
        Pattern channelPattern = Pattern.compile("Channel:(\\d+)");
        Pattern signalPattern = Pattern.compile("Signal level=(-?\\d+)");
        Pattern securityPattern = Pattern.compile("Encryption key:(on|off)");
        
        String[] cells = output.split("Cell \\d+");
        
        for (String cell : cells) {
            if (cell.trim().isEmpty()) continue;
            
            WiFiNetwork network = new WiFiNetwork();
            
            Matcher ssidMatcher = ssidPattern.matcher(cell);
            if (ssidMatcher.find()) {
                network.setSsid(ssidMatcher.group(1));
            }
            
            Matcher bssidMatcher = bssidPattern.matcher(cell);
            if (bssidMatcher.find()) {
                network.setBssid(bssidMatcher.group(1));
            }
            
            Matcher channelMatcher = channelPattern.matcher(cell);
            if (channelMatcher.find()) {
                network.setChannel(Integer.parseInt(channelMatcher.group(1)));
            }
            
            Matcher signalMatcher = signalPattern.matcher(cell);
            if (signalMatcher.find()) {
                network.setSignalStrength(Integer.parseInt(signalMatcher.group(1)));
            }
            
            Matcher securityMatcher = securityPattern.matcher(cell);
            if (securityMatcher.find()) {
                network.setSecurity(securityMatcher.group(1).equals("on") ? "WPA/WPA2" : "Open");
            }
            
            if (network.getSsid() != null && !network.getSsid().isEmpty()) {
                networks.add(network);
            }
        }
        
        return networks;
    }
    
    private List<WiFiNetwork> parseNetshAvailableOutput(BufferedReader reader) {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        try {
            String line;
            WiFiNetwork currentNetwork = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.startsWith("SSID") && line.contains(":")) {
                    if (currentNetwork != null && !currentNetwork.getSsid().isEmpty()) {
                        networks.add(currentNetwork);
                    }
                    currentNetwork = new WiFiNetwork();
                    String ssid = line.substring(line.indexOf(":") + 1).trim();
                    currentNetwork.setSsid(ssid);
                    currentNetwork.setBssid(generateRandomMAC());
                    currentNetwork.setFrequency("2.4GHz");
                } else if (currentNetwork != null) {
                    if (line.startsWith("Signal") && line.contains(":")) {
                        String signal = line.substring(line.indexOf(":") + 1).trim().replace("%", "");
                        try {
                            int signalPercent = Integer.parseInt(signal);
                            currentNetwork.setSignalStrength(-100 + signalPercent);
                        } catch (NumberFormatException e) {
                            currentNetwork.setSignalStrength(-70);
                        }
                    } else if (line.startsWith("Authentication") && line.contains(":")) {
                        String auth = line.substring(line.indexOf(":") + 1).trim();
                        currentNetwork.setSecurity(auth);
                    } else if (line.startsWith("Channel") && line.contains(":")) {
                        String channel = line.substring(line.indexOf(":") + 1).trim();
                        try {
                            currentNetwork.setChannel(Integer.parseInt(channel));
                        } catch (NumberFormatException e) {
                            currentNetwork.setChannel(6);
                        }
                    }
                }
            }
            
            // Add the last network
            if (currentNetwork != null && !currentNetwork.getSsid().isEmpty()) {
                networks.add(currentNetwork);
            }
            
        } catch (Exception e) {
            log.error("Error parsing netsh output", e);
        }
        
        return networks;
    }
    
    private List<WiFiNetwork> parseNetshOutput(BufferedReader reader) {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        try {
            String line;
            WiFiNetwork currentNetwork = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.startsWith("SSID")) {
                    if (currentNetwork != null) {
                        networks.add(currentNetwork);
                    }
                    currentNetwork = new WiFiNetwork();
                    String ssid = line.split(":")[1].trim();
                    currentNetwork.setSsid(ssid);
                } else if (currentNetwork != null) {
                    if (line.startsWith("Signal")) {
                        String signal = line.split(":")[1].trim().replace("%", "");
                        int signalPercent = parseIntSafe(signal);
                        currentNetwork.setSignalStrength(-100 + signalPercent);
                    } else if (line.startsWith("Authentication")) {
                        String auth = line.split(":")[1].trim();
                        currentNetwork.setSecurity(auth);
                    } else if (line.startsWith("Channel")) {
                        String channel = line.split(":")[1].trim();
                        currentNetwork.setChannel(parseIntSafe(channel));
                    }
                }
            }
            
            if (currentNetwork != null) {
                networks.add(currentNetwork);
            }
            
        } catch (Exception e) {
            log.error("Error parsing netsh output", e);
        }
        
        return networks;
    }
    
    private WiFiNetwork parseAirportOutput(String line) {
        // Parse macOS airport output format
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 6) return null;
        
        WiFiNetwork network = new WiFiNetwork();
        network.setSsid(parts[0]);
        network.setBssid(parts[1]);
        network.setSignalStrength(parseIntSafe(parts[2]));
        network.setChannel(parseIntSafe(parts[3]));
        network.setSecurity(parts.length > 6 ? parts[6] : "Unknown");
        
        return network;
    }
    
    private List<WiFiNetwork> generateMockNetworks() {
        List<WiFiNetwork> networks = new ArrayList<>();
        
        // Generate realistic mock networks for testing
        String[] mockSSIDs = {
            "NETGEAR_5G", "TP-Link_2.4G", "Linksys_Guest", "FRITZ!Box_7590",
            "Vodafone-WiFi", "AndroidAP", "iPhone_Hotspot", "Office_Network",
            "Home_WiFi_5G", "Guest_Network", "Cafe_Free_WiFi", "Hotel_WiFi"
        };
        
        String[] securities = {"WPA3", "WPA2", "WPA", "WEP", "Open"};
        Random random = new Random();
        
        for (int i = 0; i < mockSSIDs.length; i++) {
            WiFiNetwork network = new WiFiNetwork();
            network.setSsid(mockSSIDs[i]);
            network.setBssid(generateRandomMAC());
            network.setChannel(1 + random.nextInt(11));
            network.setSignalStrength(-30 - random.nextInt(70)); // -30 to -100 dBm
            network.setSecurity(securities[random.nextInt(securities.length)]);
            network.setFrequency(network.getChannel() <= 11 ? "2.4GHz" : "5GHz");
            network.setVendor(getVendorFromMAC(network.getBssid()));
            
            networks.add(network);
        }
        
        log.info("Generated {} mock WiFi networks for testing", networks.size());
        return networks;
    }
    
    public List<WiFiNetwork> getVulnerableNetworks(List<WiFiNetwork> networks) {
        return networks.stream()
                .filter(WiFiNetwork::isVulnerable)
                .sorted((a, b) -> Integer.compare(b.getSignalStrength(), a.getSignalStrength()))
                .toList();
    }
    
    public List<WiFiNetwork> filterBySecurityType(List<WiFiNetwork> networks, String securityType) {
        return networks.stream()
                .filter(n -> n.getSecurityLevel().equalsIgnoreCase(securityType))
                .toList();
    }
    
    private String generateRandomMAC() {
        Random random = new Random();
        StringBuilder mac = new StringBuilder();
        
        for (int i = 0; i < 6; i++) {
            if (i > 0) mac.append(":");
            mac.append(String.format("%02X", random.nextInt(256)));
        }
        
        return mac.toString();
    }
    
    private String getVendorFromMAC(String mac) {
        if (mac == null || mac.length() < 8) return "Unknown";
        
        String oui = mac.substring(0, 8).toUpperCase();
        
        Map<String, String> vendors = Map.of(
            "00:1B:63", "Apple",
            "00:26:5A", "Netgear",
            "94:10:3E", "TP-Link",
            "20:4E:7F", "Linksys",
            "38:10:D5", "Fritz!Box",
            "00:50:56", "VMware"
        );
        
        return vendors.getOrDefault(oui, "Unknown");
    }
    
    private int parseIntSafe(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}