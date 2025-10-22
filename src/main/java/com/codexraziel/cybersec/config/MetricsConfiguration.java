package com.codexraziel.cybersec.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for monitoring and observability
 * Exposes Prometheus metrics at /actuator/prometheus
 */
@Slf4j
@Configuration
public class MetricsConfiguration {
    
    /**
     * WiFi scans counter
     */
    @Bean
    public Counter wifiScansCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.wifi.scans.total")
                .description("Total number of WiFi scans performed")
                .tag("module", "wifi")
                .register(registry);
    }
    
    /**
     * Networks detected counter
     */
    @Bean
    public Counter networksDetectedCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.wifi.networks.detected")
                .description("Total number of WiFi networks detected")
                .tag("module", "wifi")
                .register(registry);
    }
    
    /**
     * Handshakes captured counter
     */
    @Bean
    public Counter handshakesCapturedCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.wifi.handshakes.captured")
                .description("Total number of WPA handshakes captured")
                .tag("module", "wifi")
                .register(registry);
    }
    
    /**
     * Bruteforce attacks counter
     */
    @Bean
    public Counter bruteforceAttacksCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.bruteforce.attacks.total")
                .description("Total number of bruteforce attacks executed")
                .tag("module", "bruteforce")
                .register(registry);
    }
    
    /**
     * Successful credentials counter
     */
    @Bean
    public Counter credentialsFoundCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.bruteforce.credentials.found")
                .description("Total number of successful credentials found")
                .tag("module", "bruteforce")
                .register(registry);
    }
    
    /**
     * Hashes cracked counter
     */
    @Bean
    public Counter hashesCrackedCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.hashcat.hashes.cracked")
                .description("Total number of hashes successfully cracked")
                .tag("module", "hashcat")
                .register(registry);
    }
    
    /**
     * Keylogger events counter
     */
    @Bean
    public Counter keyloggerEventsCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.keylogger.events.total")
                .description("Total number of keylogger events captured")
                .tag("module", "keylogger")
                .register(registry);
    }
    
    /**
     * Packets captured counter
     */
    @Bean
    public Counter packetsCapturedCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.packets.captured.total")
                .description("Total number of packets captured")
                .tag("module", "packet_capture")
                .register(registry);
    }
    
    /**
     * Reports generated counter
     */
    @Bean
    public Counter reportsGeneratedCounter(MeterRegistry registry) {
        return Counter.builder("cybersec.reports.generated.total")
                .description("Total number of reports generated")
                .tag("module", "reporting")
                .register(registry);
    }
    
    /**
     * Tool execution timer
     */
    @Bean
    public Timer toolExecutionTimer(MeterRegistry registry) {
        return Timer.builder("cybersec.tools.execution.duration")
                .description("Duration of external tool executions")
                .tag("module", "execution")
                .register(registry);
    }
    
    /**
     * Scan duration timer
     */
    @Bean
    public Timer scanDurationTimer(MeterRegistry registry) {
        return Timer.builder("cybersec.wifi.scan.duration")
                .description("Duration of WiFi scans")
                .tag("module", "wifi")
                .register(registry);
    }
    
    /**
     * Attack duration timer
     */
    @Bean
    public Timer attackDurationTimer(MeterRegistry registry) {
        return Timer.builder("cybersec.bruteforce.attack.duration")
                .description("Duration of bruteforce attacks")
                .tag("module", "bruteforce")
                .register(registry);
    }
}
