package com.codexraziel.cybersec.workflow;

import java.util.Arrays;
import java.util.List;

public enum AttackScenario {
    
    NETWORK_RECONNAISSANCE("Network Discovery & Analysis",
        Arrays.asList(
            new WorkflowStep("wifi_scan", "WiFi Network Discovery", true),
            new WorkflowStep("network_analysis", "Vulnerability Assessment", false),
            new WorkflowStep("target_selection", "Priority Target Identification", false)
        )),
    
    CREDENTIAL_HARVESTING("SSH BruteForce & Keylogging",
        Arrays.asList(
            new WorkflowStep("ssh_bruteforce", "SSH Credential Attack", true),
            new WorkflowStep("deploy_keylogger", "Keylogger Deployment", false),
            new WorkflowStep("credential_extraction", "Credential Harvesting", false)
        )),
    
    LATERAL_MOVEMENT("Post-Exploitation Movement",
        Arrays.asList(
            new WorkflowStep("privilege_escalation", "Escalate Privileges", true),
            new WorkflowStep("persistence_setup", "Maintain Access", false),
            new WorkflowStep("network_pivot", "Lateral Movement", false)
        )),
    
    FULL_RED_TEAM("Complete Red Team Engagement",
        Arrays.asList(
            new WorkflowStep("reconnaissance", "Initial Reconnaissance", true),
            new WorkflowStep("exploitation", "Initial Compromise", true),
            new WorkflowStep("post_exploitation", "Post-Exploitation", false),
            new WorkflowStep("persistence", "Maintain Persistence", false),
            new WorkflowStep("data_exfiltration", "Data Collection", false),
            new WorkflowStep("cleanup", "Evidence Cleanup", false)
        )),
    
    // ===== WiFi Attack Workflows =====
    WIFI_PASSWORD_ATTACK("WiFi Password Cracking", 
        Arrays.asList(
            new WorkflowStep("wifi_interface_setup", "Configure Monitor Mode", true),
            new WorkflowStep("wifi_network_scan", "Discover Networks", true),
            new WorkflowStep("wifi_target_selection", "Select Target", false),
            new WorkflowStep("wifi_handshake_capture", "Capture Handshake", true),
            new WorkflowStep("wifi_dictionary_attack", "Dictionary Attack", true),
            new WorkflowStep("wifi_result_analysis", "Analyze Results", false)
        )),

    ADVANCED_WIFI_PENETRATION("Advanced WiFi Testing",
        Arrays.asList(
            new WorkflowStep("wifi_comprehensive_scan", "Full Network Analysis", true),
            new WorkflowStep("wifi_vulnerability_assessment", "Check WPS/Weak Encryption", true),
            new WorkflowStep("wifi_multi_vector_attack", "Multi-Attack Strategy", true),
            new WorkflowStep("wifi_gpu_acceleration", "GPU Cracking", false),
            new WorkflowStep("wifi_post_compromise", "Post-Attack Analysis", false)
        )),

    WIFI_RED_TEAM("WiFi Red Team Engagement",
        Arrays.asList(
            new WorkflowStep("wifi_target_profiling", "Target Reconnaissance", true),
            new WorkflowStep("wifi_evil_twin_setup", "Evil Twin AP", true),
            new WorkflowStep("wifi_credential_harvest", "Harvest Credentials", true),
            new WorkflowStep("wifi_legitimate_compromise", "Compromise Real Network", true),
            new WorkflowStep("wifi_lateral_movement", "Network Movement", false),
            new WorkflowStep("wifi_cleanup", "Clean Evidence", false)
        ));
    
    private final String name;
    private final List<WorkflowStep> steps;
    
    AttackScenario(String name, List<WorkflowStep> steps) {
        this.name = name;
        this.steps = steps;
    }
    
    public String getName() { return name; }
    public List<WorkflowStep> getSteps() { return steps; }
}