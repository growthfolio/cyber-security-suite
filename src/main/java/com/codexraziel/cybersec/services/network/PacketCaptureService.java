package com.codexraziel.cybersec.services.network;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.util.NifSelector;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Professional packet capture service using pcap4j
 * Supports BPF filters, protocol dissection, and real-time streaming
 */
@Slf4j
@Service
public class PacketCaptureService {
    
    private final AtomicBoolean capturing = new AtomicBoolean(false);
    private final AtomicLong packetCounter = new AtomicLong(0);
    private final AtomicLong bytesCounter = new AtomicLong(0);
    
    private final Sinks.Many<CapturedPacket> packetSink = Sinks.many().multicast().onBackpressureBuffer();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    private PcapHandle handle;
    private PcapNetworkInterface device;
    private Instant captureStartTime;
    
    private static final int SNAPSHOT_LENGTH = 65536; // Max bytes to capture per packet
    private static final int READ_TIMEOUT = 10; // ms
    
    /**
     * List available network interfaces
     */
    public static java.util.List<PcapNetworkInterface> listInterfaces() throws PcapNativeException {
        return Pcaps.findAllDevs();
    }
    
    /**
     * Select interface interactively (for CLI testing)
     */
    public PcapNetworkInterface selectInterface() throws Exception {
        return new NifSelector().selectNetworkInterface();
    }
    
    /**
     * Get interface by name
     */
    public PcapNetworkInterface getInterfaceByName(String name) throws PcapNativeException {
        return Pcaps.getDevByName(name);
    }
    
    /**
     * Start packet capture on specified interface
     */
    public void startCapture(String interfaceName, String bpfFilter) throws Exception {
        if (capturing.compareAndSet(false, true)) {
            device = getInterfaceByName(interfaceName);
            
            if (device == null) {
                throw new IllegalArgumentException("Interface not found: " + interfaceName);
            }
            
            // Open interface in promiscuous mode
            handle = device.openLive(
                    SNAPSHOT_LENGTH,
                    PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                    READ_TIMEOUT
            );
            
            // Set BPF filter if provided
            if (bpfFilter != null && !bpfFilter.isEmpty()) {
                handle.setFilter(bpfFilter, BpfProgram.BpfCompileMode.OPTIMIZE);
                log.info("BPF filter set: {}", bpfFilter);
            }
            
            captureStartTime = Instant.now();
            packetCounter.set(0);
            bytesCounter.set(0);
            
            // Start capture in background thread
            executorService.submit(this::captureLoop);
            
            log.info("Packet capture started on {}", interfaceName);
        } else {
            throw new IllegalStateException("Capture already running");
        }
    }
    
    /**
     * Stop packet capture
     */
    public void stopCapture() {
        if (capturing.compareAndSet(true, false)) {
            if (handle != null && handle.isOpen()) {
                try {
                    handle.breakLoop();
                    handle.close();
                    log.info("Packet capture stopped. Total packets: {}, Total bytes: {}", 
                            packetCounter.get(), bytesCounter.get());
                } catch (NotOpenException e) {
                    log.warn("Handle already closed: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Get reactive stream of captured packets
     */
    public Flux<CapturedPacket> getPacketStream() {
        return packetSink.asFlux();
    }
    
    /**
     * Check if currently capturing
     */
    public boolean isCapturing() {
        return capturing.get();
    }
    
    /**
     * Get capture statistics
     */
    public CaptureStatistics getStatistics() {
        PcapStat stats = null;
        if (handle != null && handle.isOpen()) {
            try {
                stats = handle.getStats();
            } catch (PcapNativeException e) {
                log.warn("Could not get pcap stats: {}", e.getMessage());
            } catch (NotOpenException e) {
                log.warn("Handle not open: {}", e.getMessage());
            }
        }
        
        return CaptureStatistics.builder()
                .totalPackets(packetCounter.get())
                .totalBytes(bytesCounter.get())
                .capturing(capturing.get())
                .captureStartTime(captureStartTime)
                .pcapStats(stats)
                .build();
    }
    
    // Capture loop (runs in background thread)
    
    private void captureLoop() {
        try {
            PacketListener listener = packet -> {
                try {
                    processPacket(packet);
                } catch (Exception e) {
                    log.error("Error processing packet", e);
                }
            };
            
            // Capture packets indefinitely until breakLoop() is called
            handle.loop(-1, listener);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Capture loop interrupted");
        } catch (Exception e) {
            log.error("Error in capture loop", e);
        } finally {
            capturing.set(false);
        }
    }
    
    private void processPacket(Packet packet) {
        packetCounter.incrementAndGet();
        bytesCounter.addAndGet(packet.length());
        
        // Build captured packet info
        CapturedPacket capturedPacket = analyzePacket(packet);
        
        // Emit to reactive stream
        packetSink.tryEmitNext(capturedPacket);
        
        if (log.isTraceEnabled()) {
            log.trace("Packet: {}", capturedPacket);
        }
    }
    
    private CapturedPacket analyzePacket(Packet packet) {
        CapturedPacket.CapturedPacketBuilder builder = CapturedPacket.builder()
                .timestamp(Instant.now())
                .length(packet.length())
                .rawData(packet.getRawData());
        
        // Ethernet layer
        if (packet.contains(EthernetPacket.class)) {
            EthernetPacket ethPacket = packet.get(EthernetPacket.class);
            EthernetPacket.EthernetHeader ethHeader = ethPacket.getHeader();
            
            builder.srcMac(ethHeader.getSrcAddr().toString())
                   .dstMac(ethHeader.getDstAddr().toString())
                   .etherType(ethHeader.getType().name());
        }
        
        // IP layer
        if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipPacket = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipHeader = ipPacket.getHeader();
            
            builder.srcIp(ipHeader.getSrcAddr().getHostAddress())
                   .dstIp(ipHeader.getDstAddr().getHostAddress())
                   .protocol(ipHeader.getProtocol().name())
                   .ttl(ipHeader.getTtl());
        }
        
        // TCP layer
        if (packet.contains(TcpPacket.class)) {
            TcpPacket tcpPacket = packet.get(TcpPacket.class);
            TcpPacket.TcpHeader tcpHeader = tcpPacket.getHeader();
            
            builder.srcPort(tcpHeader.getSrcPort().valueAsInt())
                   .dstPort(tcpHeader.getDstPort().valueAsInt())
                   .tcpFlags(String.format("%s%s%s%s%s%s",
                           tcpHeader.getSyn() ? "S" : "",
                           tcpHeader.getAck() ? "A" : "",
                           tcpHeader.getFin() ? "F" : "",
                           tcpHeader.getRst() ? "R" : "",
                           tcpHeader.getPsh() ? "P" : "",
                           tcpHeader.getUrg() ? "U" : ""));
        }
        
        // UDP layer
        if (packet.contains(UdpPacket.class)) {
            UdpPacket udpPacket = packet.get(UdpPacket.class);
            UdpPacket.UdpHeader udpHeader = udpPacket.getHeader();
            
            builder.srcPort(udpHeader.getSrcPort().valueAsInt())
                   .dstPort(udpHeader.getDstPort().valueAsInt());
        }
        
        // ICMP layer
        if (packet.contains(IcmpV4CommonPacket.class)) {
            IcmpV4CommonPacket icmpPacket = packet.get(IcmpV4CommonPacket.class);
            builder.icmpType(icmpPacket.getHeader().getType().name());
        }
        
        // DNS (basic detection)
        if (builder.build().getDstPort() == 53 || builder.build().getSrcPort() == 53) {
            builder.applicationProtocol("DNS");
        }
        
        // HTTP (basic detection)
        if (builder.build().getDstPort() == 80 || builder.build().getSrcPort() == 80) {
            builder.applicationProtocol("HTTP");
        }
        
        // HTTPS (basic detection)
        if (builder.build().getDstPort() == 443 || builder.build().getSrcPort() == 443) {
            builder.applicationProtocol("HTTPS");
        }
        
        return builder.build();
    }
    
    @PreDestroy
    public void cleanup() {
        stopCapture();
        executorService.shutdown();
        log.info("PacketCaptureService destroyed");
    }
    
    // Data models
    
    @Data
    @Builder
    public static class CapturedPacket {
        private Instant timestamp;
        private int length;
        private byte[] rawData;
        
        // Ethernet
        private String srcMac;
        private String dstMac;
        private String etherType;
        
        // IP
        private String srcIp;
        private String dstIp;
        private String protocol;
        private int ttl;
        
        // Transport
        private int srcPort;
        private int dstPort;
        private String tcpFlags;
        private String icmpType;
        
        // Application
        private String applicationProtocol;
        
        public String toSummary() {
            if (srcIp != null) {
                return String.format("%s:%d -> %s:%d [%s] %d bytes",
                        srcIp, srcPort, dstIp, dstPort, protocol, length);
            } else {
                return String.format("%s -> %s [%s] %d bytes",
                        srcMac, dstMac, etherType, length);
            }
        }
    }
    
    @Data
    @Builder
    public static class CaptureStatistics {
        private long totalPackets;
        private long totalBytes;
        private boolean capturing;
        private Instant captureStartTime;
        private PcapStat pcapStats;
        
        public double getPacketsPerSecond() {
            if (captureStartTime == null) return 0;
            long seconds = java.time.Duration.between(captureStartTime, Instant.now()).getSeconds();
            return seconds > 0 ? (double) totalPackets / seconds : 0;
        }
        
        public double getMegabytesPerSecond() {
            if (captureStartTime == null) return 0;
            long seconds = java.time.Duration.between(captureStartTime, Instant.now()).getSeconds();
            return seconds > 0 ? (double) totalBytes / 1024 / 1024 / seconds : 0;
        }
    }
}
