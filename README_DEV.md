# 🚀 Cyber Security Research Suite - Developer Guide

> **Status:** 🟢 Active Development  
> **Sprint:** 1 - Foundation  
> **Last Update:** 2024-10-22

---

## 📊 Project Status

### ✅ Completed Today

1. **Professional Dependencies** - 20+ libraries added
2. **Core Execution Layer** - Production-ready tool executor
3. **Aircrack-ng Adapter** - Full WiFi pentesting integration
4. **Build System** - Compiling successfully
5. **Documentation** - Complete guides and plans

### 🎯 Current Architecture

```
┌─────────────────────────────────────────┐
│     JavaFX GUI (Views & Controllers)     │
│  • WiFiView • BruteForceView • etc      │
└─────────────────────────────────────────┘
              ↓ uses
┌─────────────────────────────────────────┐
│    Spring Services (Business Logic)      │
│  • WiFiPentestService                   │
│  • BruteForceService                    │
│  • KeyloggerService                     │
└─────────────────────────────────────────┘
              ↓ uses
┌─────────────────────────────────────────┐
│      Tool Adapters (Facades)            │
│  • AircrackNgAdapter ✅                 │
│  • HashcatAdapter 🔜                    │
│  • HydraAdapter 🔜                      │
│  • KismetAdapter 🔜                     │
└─────────────────────────────────────────┘
              ↓ uses
┌─────────────────────────────────────────┐
│   ToolExecutor (Process Management)     │
│  • CommonsExecToolExecutor ✅           │
└─────────────────────────────────────────┘
              ↓ executes
┌─────────────────────────────────────────┐
│     External Tools (Aircrack, etc)      │
└─────────────────────────────────────────┘
```

---

## 🏗️ Project Structure

```
src/main/java/com/codexraziel/cybersec/
├── CodexRazielCSApplication.java          # Main app
│
├── core/                                   # NEW! Core abstractions
│   └── execution/
│       ├── ExecutionResult.java           # Result model
│       ├── ToolConfig.java                # Config model
│       ├── ToolExecutor.java              # Interface
│       └── CommonsExecToolExecutor.java   # Implementation ✅
│
├── adapters/                               # NEW! Tool adapters
│   └── wifi/
│       └── AircrackNgAdapter.java         # Aircrack-ng integration ✅
│
├── services/                               # Business logic
│   ├── BruteForceGenerator.java
│   ├── KeyloggerService.java
│   ├── WiFiScannerService.java
│   └── ...
│
├── views/                                  # JavaFX UI
│   ├── WiFiView.java
│   ├── BruteForceView.java
│   └── ...
│
├── models/                                 # Data models
├── network/                                # Network operations
├── security/                               # Security utilities
└── ...

docs/                                       # Documentation
├── FUNCIONALIDADES_E_ALTERNATIVAS_PROFISSIONAIS.md  (43KB)
├── ROADMAP_INTEGRACAO_FERRAMENTAS_PRO.md            (25KB)
├── TABELA_COMPARACAO_RAPIDA.md                      (16KB)
└── README_DOCUMENTACAO.md                            (17KB)

EVOLUTIONARY_PLAN.md                        # 8-week roadmap
PROGRESS_REPORT.md                          # Daily progress
TEST_TOOLEXECUTOR.md                        # Testing guide
```

---

## ⚡ Quick Start

### 1. Build & Compile

```bash
# Install dependencies
mvn clean install

# Compile only
mvn clean compile

# Run application
mvn spring-boot:run

# Or with JavaFX plugin
mvn javafx:run
```

### 2. Install Security Tools (Optional)

```bash
# Kali Linux / Ubuntu
sudo apt update
sudo apt install -y \
  aircrack-ng \
  hashcat \
  nmap \
  hydra \
  wireshark

# Check versions
aircrack-ng --version
hashcat --version
```

### 3. Test ToolExecutor

```java
@Autowired
private ToolExecutor toolExecutor;

// Check if tool is available
boolean hasAircrack = toolExecutor.isToolAvailable("aircrack-ng");

// Get version
String version = toolExecutor.getToolVersion("aircrack-ng");

// Execute simple command
ToolConfig config = ToolConfig.simple("nmap", "-sP", "192.168.1.0/24");
ExecutionResult result = toolExecutor.execute(config);

if (result.isSuccess()) {
    System.out.println(result.getStdout());
}
```

### 4. Use Aircrack-ng Adapter

```java
@Autowired
private AircrackNgAdapter aircrackNg;

// Enable monitor mode
aircrackNg.enableMonitorMode("wlan0")
    .thenAccept(result -> {
        if (result.success()) {
            System.out.println("Monitor interface: " + result.monitorInterface());
        }
    });

// Start scanning
aircrackNg.startScan(
    "wlan0mon",
    "scan-output",
    line -> System.out.println("Output: " + line)
).thenAccept(scanResult -> {
    System.out.println("Networks found: " + scanResult.networks().size());
});
```

---

## 📚 Documentation Index

| Document | Size | Purpose |
|----------|------|---------|
| **EVOLUTIONARY_PLAN.md** | 9.5KB | 8-week development roadmap |
| **PROGRESS_REPORT.md** | 8.6KB | Daily progress updates |
| **TEST_TOOLEXECUTOR.md** | 5.2KB | Testing guide |
| **docs/FUNCIONALIDADES...** | 43KB | Complete analysis |
| **docs/ROADMAP...** | 25KB | Integration guide |
| **docs/TABELA...** | 16KB | Quick reference |

---

## 🎯 Next Steps (This Week)

### Priority 1: HashcatAdapter
```java
// Create: src/main/java/com/codexraziel/cybersec/adapters/cracking/HashcatAdapter.java
@Component
public class HashcatAdapter {
    CompletableFuture<CrackResult> crack(String hashFile, String wordlist);
    CompletableFuture<BenchmarkResult> benchmark();
    boolean isGPUAvailable();
}
```

### Priority 2: JNativeHook Keylogger
```java
// Migrate: src/main/java/com/codexraziel/cybersec/services/KeyloggerService.java
@Service
public class NativeKeyloggerService implements NativeKeyListener {
    void start();
    void stop();
    Flux<KeyEvent> eventStream();
}
```

### Priority 3: Kismet Integration
```java
// Create: src/main/java/com/codexraziel/cybersec/adapters/wifi/KismetAdapter.java
@Component
public class KismetAdapter {
    CompletableFuture<List<WiFiDevice>> getDevices();
    void startMonitoring(String iface);
}
```

### Priority 4: Update UI
- Connect `WiFiView` to `AircrackNgAdapter`
- Real-time progress in UI
- Results table updates
- Tool status indicators

---

## 🔧 Development Guidelines

### Code Style
- **Lombok:** Use for boilerplate reduction
- **Spring:** Dependency injection everywhere
- **Async:** CompletableFuture for long operations
- **Reactive:** Consider Reactor for streaming data

### Testing
```java
// Unit tests
@SpringBootTest
class AircrackNgAdapterTest {
    @Autowired AircrackNgAdapter adapter;
    
    @Test
    void testAvailability() {
        // Skip if not installed
        assumeTrue(adapter.isAvailable());
        // ... test
    }
}

// Integration tests
@Component
public class IntegrationTester implements CommandLineRunner {
    // See TEST_TOOLEXECUTOR.md
}
```

### Logging
```java
@Slf4j
public class MyService {
    public void doSomething() {
        log.info("Starting operation...");
        log.debug("Details: {}", details);
        log.error("Failed", exception);
    }
}
```

---

## 🚨 Security & Legal

### ⚠️ IMPORTANT

- **Authorized Use Only:** Only test on your own equipment or with written permission
- **Research Purpose:** This is for cybersecurity research and education
- **Legal Compliance:** Follow local laws (LGPD, GDPR, CFAA, etc.)
- **No Malicious Use:** Not intended for unauthorized access or harm

### Disclaimer

```
This software is intended EXCLUSIVELY for:
✅ Academic cybersecurity research
✅ Authorized penetration testing
✅ Security training in controlled environments
✅ Scientific analysis and education

❌ Unauthorized use is ILLEGAL and may result in:
- Criminal prosecution
- Heavy fines
- Imprisonment

The developers are NOT responsible for misuse.
```

---

## 📞 Support & Resources

### Documentation
- Internal docs in `docs/` folder
- Javadoc: `mvn javadoc:javadoc`
- Test guide: `TEST_TOOLEXECUTOR.md`

### External Resources
- **Aircrack-ng:** https://www.aircrack-ng.org/
- **Hashcat:** https://hashcat.net/
- **Spring Boot:** https://spring.io/projects/spring-boot
- **JavaFX:** https://openjfx.io/

### Community
- **HackTheBox:** https://www.hackthebox.com/
- **TryHackMe:** https://tryhackme.com/
- **Offensive Security:** https://www.offensive-security.com/

---

## 📊 Progress Tracking

### Sprint 1 (Current)
- [x] Setup professional dependencies
- [x] Create core execution layer
- [x] Implement AircrackNgAdapter
- [ ] Implement HashcatAdapter
- [ ] Migrate to JNativeHook
- [ ] Update UI with new backends

### Sprint 2 (Next)
- [ ] Kismet REST API integration
- [ ] pcap4j packet analysis
- [ ] Professional reporting (POI + PDFBox)
- [ ] Metrics with Micrometer

---

## 🎉 Contributing

Since this is experimental and collaborative:

1. **Experiment freely** - Try new approaches
2. **Refactor boldly** - Improve architecture
3. **Document changes** - Update PROGRESS_REPORT.md
4. **Test thoroughly** - Ensure functionality
5. **Keep it legal** - Always authorized use only

---

**Last Build:** ✅ SUCCESS (18s)  
**Coverage:** TBD  
**Lines of Code:** ~10,000  
**Contributors:** Active Development

**Made with ❤️ for Cybersecurity Research**
