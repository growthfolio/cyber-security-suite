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