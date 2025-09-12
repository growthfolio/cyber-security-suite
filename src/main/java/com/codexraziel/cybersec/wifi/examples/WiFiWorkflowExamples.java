package com.codexraziel.cybersec.wifi.examples;

import com.codexraziel.cybersec.workflow.AttackScenario;
import com.codexraziel.cybersec.workflow.WorkflowManager;
import com.codexraziel.cybersec.workflow.WorkflowContext;
import com.codexraziel.cybersec.workflow.WorkflowConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrations of WiFi attack workflows
 * Shows how to execute the new WiFi workflows programmatically
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WiFiWorkflowExamples {
    
    private final WorkflowManager workflowManager;
    
    /**
     * Example: Basic WiFi Password Attack
     */
    public CompletableFuture<String> executeBasicWiFiAttack(String interfaceName, String targetSSID, String targetBSSID) {
        log.info("Starting basic WiFi password attack on {} ({})", targetSSID, targetBSSID);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Prepare workflow configuration
                WorkflowConfig config = new WorkflowConfig();
                config.setParameter("interface", interfaceName);
                config.setParameter("target_ssid", targetSSID);
                config.setParameter("target_bssid", targetBSSID);
                config.setParameter("duration", 60);
                config.setParameter("timeout", 300);
                config.setParameter("wordlist_path", 
                    "/home/felipe-macedo/cyber-projects/cyber-security-suite/wordlists/passwords/rockyou.txt");
                
                // Execute WiFi Password Attack workflow
                var workflowResult = workflowManager.executeWorkflow(AttackScenario.WIFI_PASSWORD_ATTACK, config);
                
                log.info("WiFi password attack workflow completed");
                return "WiFi attack completed - check results";
                
            } catch (Exception e) {
                log.error("Failed to execute WiFi password attack workflow", e);
                throw new RuntimeException("WiFi attack workflow failed", e);
            }
        });
    }
    
    /**
     * Example: Advanced WiFi Penetration Testing
     */
    public CompletableFuture<String> executeAdvancedWiFiPentest(String interfaceName) {
        log.info("Starting advanced WiFi penetration testing on interface {}", interfaceName);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                WorkflowConfig config = new WorkflowConfig();
                config.setParameter("interface", interfaceName);
                config.setParameter("scan_duration", 120);
                config.setParameter("attack_methods", "dictionary,wps,evil_twin");
                config.setParameter("wordlist_path", 
                    "/home/felipe-macedo/cyber-projects/cyber-security-suite/wordlists/passwords/rockyou.txt");
                
                var workflowResult = workflowManager.executeWorkflow(AttackScenario.ADVANCED_WIFI_PENETRATION, config);
                
                log.info("Advanced WiFi penetration workflow completed");
                return "Advanced WiFi pentest completed - check results";
                
            } catch (Exception e) {
                log.error("Failed to execute advanced WiFi penetration workflow", e);
                throw new RuntimeException("Advanced WiFi workflow failed", e);
            }
        });
    }
    
    /**
     * Example: WiFi Red Team Engagement
     */
    public CompletableFuture<String> executeWiFiRedTeamEngagement(String interfaceName, String targetOrganization) {
        log.info("Starting WiFi red team engagement against {} using interface {}", 
                targetOrganization, interfaceName);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                WorkflowConfig config = new WorkflowConfig();
                config.setParameter("interface", interfaceName);
                config.setParameter("target_organization", targetOrganization);
                config.setParameter("engagement_type", "full_spectrum");
                config.setParameter("stealth_mode", true);
                config.setParameter("cleanup_required", true);
                
                var workflowResult = workflowManager.executeWorkflow(AttackScenario.WIFI_RED_TEAM, config);
                
                log.info("WiFi red team engagement workflow completed");
                return "WiFi red team engagement completed - check results";
                
            } catch (Exception e) {
                log.error("Failed to execute WiFi red team workflow", e);
                throw new RuntimeException("WiFi red team workflow failed", e);
            }
        });
    }
    
    /**
     * Example: Demonstrate all three WiFi workflows in sequence
     */
    public void demonstrateAllWiFiWorkflows(String interfaceName) {
        log.info("=== WiFi Workflow Demonstration ===");
        
        // Note: In real usage, these would be run based on specific targets and requirements
        log.info("Available WiFi Attack Workflows:");
        log.info("1. WIFI_PASSWORD_ATTACK - Basic dictionary attack against specific network");
        log.info("2. ADVANCED_WIFI_PENETRATION - Comprehensive testing with multiple attack vectors");
        log.info("3. WIFI_RED_TEAM - Full red team engagement simulation");
        
        log.info("To execute:");
        log.info("- Basic Attack: executeBasicWiFiAttack('wlan0', 'TargetSSID', 'AA:BB:CC:DD:EE:FF')");
        log.info("- Advanced Pentest: executeAdvancedWiFiPentest('wlan0')");
        log.info("- Red Team: executeWiFiRedTeamEngagement('wlan0', 'TargetCorp')");
        
        log.info("=== End Demonstration ===");
    }
}
