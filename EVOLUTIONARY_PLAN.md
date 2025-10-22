# 🚀 Plano Evolutivo - Cyber Security Research Suite

> **Modo:** Experimental, Colaborativo, Profissional  
> **Objetivo:** Evoluir o projeto com integrações reais e arquitetura moderna  
> **Status:** 🟢 Em Desenvolvimento Ativo

---

## 📋 Estado Atual (Baseline)

### ✅ O Que Já Temos

#### Arquitetura
- **Backend:** Spring Boot 3.1.5 + Dependency Injection
- **Frontend:** JavaFX 19 (fx:root pattern)
- **Persistência:** SQLite
- **SSH:** JSch para operações remotas
- **Icons:** Kordamp Ikonli + FontAwesome

#### Módulos Implementados
1. **KeyloggerView** - Monitoramento de eventos de teclado
2. **BruteForceView** - Gerador de ataques de força bruta
3. **WiFiView** - Pentest WiFi
4. **PerformanceView** - Monitoramento de performance
5. **SecurityView** - Análise de segurança
6. **ToolsView** - Gestão de ferramentas
7. **WorkflowView** - Workflows de Red Team
8. **Results** - Visualização de resultados

#### Estrutura de Packages
```
com.codexraziel.cybersec/
├── CodexRazielCSApplication (Main)
├── controllers/
├── models/
├── network/
├── security/
├── services/
├── ui/
├── utils/
├── views/
├── wifi/
└── workflow/
```

---

## 🎯 Evolução Planejada

### FASE 1: Fundação Profissional (Sprint 1 - 2 semanas)

#### 1.1 Process Management Evolution
**De:** `ProcessBuilder` customizado  
**Para:** Apache Commons Exec + NuProcess

**Entregas:**
- [x] Adicionar dependências Maven
- [ ] Criar `ProcessExecutorService` abstrato
- [ ] Implementar `AircrackNgAdapter`
- [ ] Implementar `HashcatAdapter`
- [ ] Implementar `HydraAdapter`
- [ ] Testes de integração

**Arquitetura Proposta:**
```java
interface ToolAdapter {
    CompletableFuture<ExecutionResult> execute(ToolConfig config);
    void stop();
    Observable<String> outputStream();
}

@Service
class AircrackNgAdapter implements ToolAdapter {
    @Autowired private CommandExecutor executor;
    // Integration with airodump-ng, aireplay-ng, aircrack-ng
}
```

#### 1.2 Native Keylogger Migration
**De:** C binário customizado  
**Para:** JNativeHook (Java puro, cross-platform)

**Entregas:**
- [x] Adicionar JNativeHook dependency
- [ ] Criar `NativeKeyloggerService`
- [ ] Implementar listeners para keyboard/mouse
- [ ] Buffer circular para eventos
- [ ] Encryption de dados capturados
- [ ] UI updates em tempo real

**Benefícios:**
- ✅ Cross-platform (Win/Linux/Mac)
- ✅ Mais estável
- ✅ Menos código nativo
- ✅ Mais fácil de manter

#### 1.3 WiFi Integration Upgrades
**De:** nmcli/iwlist parsing  
**Para:** Aircrack-ng Suite + Kismet REST API

**Entregas:**
- [ ] `AircrackNgService` para capture real
- [ ] `KismetRestClient` para monitoring
- [ ] `WiFiPacketAnalyzer` com pcap4j
- [ ] Handshake capture workflow
- [ ] WPA/WPA2 cracking com Hashcat integration

---

### FASE 2: Advanced Features (Sprint 2 - 2 semanas)

#### 2.1 Packet Analysis Engine
**Nova Funcionalidade:** Deep packet inspection

**Entregas:**
- [ ] `PacketCaptureService` usando pcap4j
- [ ] `PacketAnalyzerEngine` com filtros BPF
- [ ] `WiFiBeaconAnalyzer` para beacon frames
- [ ] `TrafficStatisticsCollector`
- [ ] Real-time visualization no UI

#### 2.2 Professional Reporting
**De:** CSV/JSON básico  
**Para:** Excel (POI) + PDF (PDFBox)

**Entregas:**
- [ ] `ReportGeneratorService`
- [ ] Templates profissionais
- [ ] Excel com gráficos e formatação
- [ ] PDF com branding e tabelas
- [ ] Automated report scheduling

#### 2.3 Covert Channels
**Nova Funcionalidade:** DNS/ICMP tunneling

**Entregas:**
- [ ] `CovertChannelService` abstrato
- [ ] `DNSTunnelImpl` usando dnsjava
- [ ] `ICMPTunnelImpl` usando raw sockets
- [ ] `HTTPStegoImpl` para steganography
- [ ] C2 command framework

---

### FASE 3: Observability & Monitoring (Sprint 3 - 1-2 semanas)

#### 3.1 Metrics Collection
**Nova Funcionalidade:** Micrometer + Prometheus

**Entregas:**
- [ ] `MetricsConfiguration`
- [ ] Custom metrics por módulo:
  - `cybersec.wifi.scans.total`
  - `cybersec.attacks.duration`
  - `cybersec.keylogger.events.rate`
  - `cybersec.process.active.count`
- [ ] Prometheus endpoint `/actuator/prometheus`
- [ ] Grafana dashboards

#### 3.2 Logging Enhancement
**De:** SLF4J básico  
**Para:** Structured logging + ELK integration

**Entregas:**
- [ ] Logback config avançado
- [ ] JSON logging com logstash encoder
- [ ] Audit trail separado
- [ ] Log rotation configurável
- [ ] ELK Stack docker-compose

---

### FASE 4: Reactive & Performance (Sprint 4 - 2 semanas)

#### 4.1 Reactive Streams
**Nova Arquitetura:** Project Reactor

**Entregas:**
- [ ] Migrar serviços críticos para Reactor
- [ ] `Flux<WiFiNetwork>` para scan contínuo
- [ ] `Mono<AttackResult>` para operações assíncronas
- [ ] Backpressure handling
- [ ] WebFlux endpoints (opcional)

#### 4.2 Caching Layer
**Nova Funcionalidade:** Redis/Caffeine cache

**Entregas:**
- [ ] Cache de scan results
- [ ] Cache de wordlists
- [ ] TTL configurável
- [ ] Cache invalidation strategy

---

## 🏗️ Arquitetura Moderna Proposta

### Core Abstraction Layers

```
┌─────────────────────────────────────────────┐
│          JavaFX UI Layer                     │
│  (Views, Controllers, FXML)                  │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│       Application Service Layer              │
│  (Spring Services, Business Logic)           │
│  • WiFiPentestService                        │
│  • BruteForceService                         │
│  • KeyloggerService                          │
│  • ReportGeneratorService                    │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│        Tool Adapter Layer                    │
│  (Facade Pattern for External Tools)         │
│  • AircrackNgAdapter                         │
│  • HashcatAdapter                            │
│  • KismetAdapter                             │
│  • HydraAdapter                              │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│      Execution Engine Layer                  │
│  (Process Management, Streams)               │
│  • CommandExecutor (Commons Exec)            │
│  • ProcessStreamHandler                      │
│  • OutputParser                              │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│    Infrastructure Layer                      │
│  • Metrics (Micrometer)                      │
│  • Logging (Logback + Logstash)              │
│  • Cache (Caffeine)                          │
│  • Persistence (SQLite/H2)                   │
└─────────────────────────────────────────────┘
```

### Reactive Flow Example

```java
// Modern reactive WiFi scanning
Flux<WiFiNetwork> scanStream = wifiScannerService
    .startContinuousScan("wlan0")
    .filter(network -> network.getSignalStrength() > -70)
    .map(network -> enrichWithVendorInfo(network))
    .buffer(Duration.ofSeconds(5))
    .flatMap(batch -> saveToDatabase(batch))
    .doOnNext(networks -> updateUI(networks))
    .doOnError(error -> handleError(error))
    .retry(3);
```

---

## 🔧 Próximas Ações Imediatas

### 1️⃣ HOJE (Próximas Horas)

- [x] ✅ Expandir pom.xml com dependências profissionais
- [ ] 🔄 Criar `ProcessExecutorService` base
- [ ] 🔄 Implementar `AircrackNgAdapter` (primeiro adapter)
- [ ] 🔄 Criar testes de integração

### 2️⃣ ESTA SEMANA

- [ ] Completar 3 adapters principais (Aircrack, Hashcat, Hydra)
- [ ] Migrar KeyloggerService para JNativeHook
- [ ] Criar dashboard de métricas básico
- [ ] Implementar Excel reporting

### 3️⃣ PRÓXIMAS 2 SEMANAS

- [ ] Fase 1 completa (Fundação Profissional)
- [ ] Packet analysis engine funcional
- [ ] Prometheus + Grafana integration
- [ ] Code review e refactoring

---

## 📊 Métricas de Sucesso

### Performance
- [ ] Password cracking: 100x+ faster (CPU → GPU via Hashcat)
- [ ] WiFi scanning: Real handshake capture
- [ ] Keylogger: 99%+ uptime, <1ms latency

### Code Quality
- [ ] Test coverage: >70%
- [ ] Sonar quality gate: Pass
- [ ] Zero critical vulnerabilities
- [ ] Documentation: 100% public APIs

### Functionality
- [ ] 10+ tool adapters implementados
- [ ] 5+ export formats (CSV, JSON, Excel, PDF, HTML)
- [ ] Real-time monitoring functional
- [ ] Workflow automation completo

---

## 🔬 Experimental Features (Opcional)

### Machine Learning
- [ ] Anomaly detection em traffic patterns
- [ ] Password pattern recognition
- [ ] WiFi AP fingerprinting

### Advanced C2
- [ ] Multi-protocol C2 (DNS, ICMP, HTTP)
- [ ] Encrypted channels
- [ ] Beacon simulation

### Cloud Integration
- [ ] AWS/Azure deploy scripts
- [ ] Distributed cracking (multi-node)
- [ ] Cloud storage para results

---

## 📚 Referências Técnicas

### Ferramentas Integradas
- **Aircrack-ng:** https://www.aircrack-ng.org/
- **Hashcat:** https://hashcat.net/
- **Kismet:** https://www.kismetwireless.net/
- **JNativeHook:** https://github.com/kwhat/jnativehook

### Bibliotecas
- **Apache Commons Exec:** https://commons.apache.org/proper/commons-exec/
- **pcap4j:** https://www.pcap4j.org/
- **Micrometer:** https://micrometer.io/
- **Project Reactor:** https://projectreactor.io/

---

**Última Atualização:** 2024-10-22  
**Status:** 🟢 Fundação em progresso  
**Próximo Checkpoint:** Fim da Semana 1
