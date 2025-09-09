package com.research.cybersec.models;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ScanResult {
    private List<WiFiNetwork> networks;
    private LocalDateTime scanTime;
    private String scanInterface;
    private int totalNetworks;
    private int vulnerableNetworks;
    private String scanMethod;
    private boolean successful;
    private String errorMessage;
    
    public int getVulnerableCount() {
        return networks != null ? 
            (int) networks.stream().filter(WiFiNetwork::isVulnerable).count() : 0;
    }
    
    public double getVulnerabilityPercentage() {
        return totalNetworks > 0 ? 
            (double) getVulnerableCount() / totalNetworks * 100 : 0;
    }
}