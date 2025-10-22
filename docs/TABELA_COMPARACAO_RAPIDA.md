# Tabela de Comparação Rápida - Ferramentas Profissionais

## 📊 Quick Reference Table

Esta tabela fornece uma visão rápida das funcionalidades implementadas e suas alternativas profissionais.

---

## Comparação Funcionalidade x Ferramenta

| # | Funcionalidade Atual | Arquivo Principal | Ferramenta Pro Recomendada | Tipo | Instalação | Motivo da Escolha |
|---|---------------------|-------------------|---------------------------|------|------------|-------------------|
| 1 | WiFi Scanner | `WiFiScanner.java` | **Aircrack-ng** (airodump-ng) | CLI Suite | `apt install aircrack-ng` | Padrão da indústria, injection, capture |
| 2 | WiFi Monitor | `WiFiScannerService.java` | **Kismet** | Daemon + API | `apt install kismet` | WIDS, detecção de ataques, GPS tracking |
| 3 | Password Cracker | `BruteForceGenerator.java` | **Hashcat** | CLI + GPU | `apt install hashcat` | 1000x+ rápido (GPU), mais algoritmos |
| 4 | Network BruteForce | `ProcessManager.java` | **Hydra (THC-Hydra)** | CLI | `apt install hydra` | 50+ protocolos (SSH, FTP, HTTP, etc.) |
| 5 | Keylogger | `KeyloggerService.java` | **JNativeHook** | Java Library | Maven dependency | Cross-platform, bem mantido, estável |
| 6 | Process Manager | `ProcessManager.java` | **Apache Commons Exec** | Java Library | Maven dependency | Production-ready, thread-safe |
| 7 | Packet Capture | Custom implementation | **pcap4j** | Java Library | Maven dependency | Wrapper Java para libpcap |
| 8 | Packet Analysis | - | **Wireshark/tshark** | CLI/GUI | `apt install wireshark` | Análise profunda de pacotes |
| 9 | DNS Tunnel | `CovertChannelManager.java` | **Iodine / dnscat2** | CLI | `apt install iodine` | Tunelamento real bi-direcional |
| 10 | Report Export CSV | `ResultsExporter.java` | **Apache POI** | Java Library | Maven dependency | Excel profissional |
| 11 | Report Export PDF | `ResultsExporter.java` | **iText7** | Java Library | Maven dependency | PDFs avançados |
| 12 | Memory Management | `MemoryManager.java` | **Micrometer + Prometheus** | Java Library | Maven dependency | Métricas profissionais |
| 13 | Log Management | Custom logging | **ELK Stack** | Platform | Docker Compose | Centralizado, analytics, dashboards |
| 14 | Audit Logging | `AuditLogger.java` | **Wazuh / OSSEC** | SIEM | `apt install wazuh` | HIDS, compliance, alerting |
| 15 | WiFi Pentest | `WiFiPentestService.java` | **Wifite2** | Python | `pip install wifite2` | Automated, múltiplos ataques |
| 16 | MITM Attacks | - | **Bettercap** | Go Binary | `apt install bettercap` | Swiss army knife, API REST |
| 17 | Web Security | - | **OWASP ZAP** | GUI/API | Download oficial | Web app security scanning |
| 18 | Vulnerability Scan | - | **Nmap + NSE** | CLI | `apt install nmap` | Port scanning, service detection |
| 19 | Config Management | `ConfigManager.java` | **Spring Cloud Config** | Framework | Spring Boot | Centralizado, profiles |
| 20 | Post-Exploitation | - | **Metasploit Framework** | Framework | `apt install metasploit-framework` | Framework completo, Meterpreter |

---

## 🎯 Priorização por Impacto

### 🔴 **CRÍTICO** (Implementar Imediatamente)

| Funcionalidade | Substituir Por | Ganho Esperado | Complexidade | Tempo Estimado |
|----------------|----------------|----------------|--------------|----------------|
| WiFi Scanner | Aircrack-ng | ⭐⭐⭐⭐⭐ Capture real de handshakes | 🟡 Média | 2-3 dias |
| Password Cracking | Hashcat | ⭐⭐⭐⭐⭐ 1000x+ performance (GPU) | 🟡 Média | 2-3 dias |
| Keylogger | JNativeHook | ⭐⭐⭐⭐ Estabilidade + cross-platform | 🟢 Baixa | 1-2 dias |
| Process Manager | Commons Exec | ⭐⭐⭐⭐ Confiabilidade + features | 🟢 Baixa | 1 dia |

**Total Estimado:** 1-2 semanas

### 🟡 **ALTA PRIORIDADE** (Próximos Passos)

| Funcionalidade | Substituir Por | Ganho Esperado | Complexidade | Tempo Estimado |
|----------------|----------------|----------------|--------------|----------------|
| Network Monitor | Kismet | ⭐⭐⭐⭐ WIDS + detecção ataques | 🟡 Média | 3-4 dias |
| Packet Analysis | pcap4j | ⭐⭐⭐⭐ Análise profunda | 🟡 Média | 3-5 dias |
| Reporting | Apache POI + iText | ⭐⭐⭐⭐ Profissionalismo | 🟡 Média | 3-4 dias |
| Network BruteForce | Hydra | ⭐⭐⭐⭐ 50+ protocolos | 🟢 Baixa | 1-2 dias |

**Total Estimado:** 2-3 semanas

### 🟢 **MÉDIA PRIORIDADE** (Melhorias)

| Funcionalidade | Substituir Por | Ganho Esperado | Complexidade | Tempo Estimado |
|----------------|----------------|----------------|--------------|----------------|
| Metrics | Micrometer | ⭐⭐⭐ Observability | 🟡 Média | 2-3 dias |
| Logging | ELK Stack | ⭐⭐⭐ Centralized logs | 🔴 Alta | 1 semana |
| Audit | Wazuh | ⭐⭐⭐ Compliance | 🔴 Alta | 1 semana |
| WiFi Automation | Wifite2 | ⭐⭐⭐ Automation | 🟢 Baixa | 2-3 dias |

**Total Estimado:** 3-4 semanas

---

## 📦 Comparação de Bibliotecas Java

### Network & Packet Analysis

| Biblioteca | Versão Maven | Propósito | Vantagens | Desvantagens |
|-----------|--------------|-----------|-----------|--------------|
| **pcap4j** | 1.8.2 | Packet capture | ✅ Wrapper Java para libpcap<br>✅ Cross-platform<br>✅ API limpa | ❌ Requer libpcap nativo<br>❌ Performance overhead |
| **jNetPcap** | 2.0 | Packet capture | ✅ JNI direto<br>✅ Performance | ❌ Menos mantido<br>❌ Complexo |
| **dnsjava** | 3.5.2 | DNS operations | ✅ Completo<br>✅ Bem mantido | ❌ Apenas DNS |
| **OkHttp** | 4.11.0 | HTTP client | ✅ Moderno<br>✅ Async<br>✅ HTTP/2 | ❌ Web apenas |

**Recomendação:** `pcap4j` para capture geral + `dnsjava` para DNS específico

### Process Management

| Biblioteca | Versão Maven | Propósito | Vantagens | Desvantagens |
|-----------|--------------|-----------|-----------|--------------|
| **Apache Commons Exec** | 1.3 | Process execution | ✅ Production-ready<br>✅ Watchdog<br>✅ Stream handling | ❌ API um pouco antiga |
| **NuProcess** | 2.0.6 | Process execution | ✅ Baixa latência<br>✅ Performance | ❌ Menos features |
| **ProcessHandle** | Java 9+ | Process management | ✅ Nativo<br>✅ Moderno | ❌ Limitado |

**Recomendação:** `Apache Commons Exec` para produção

### Keylogging

| Biblioteca | Versão Maven | Propósito | Vantagens | Desvantagens |
|-----------|--------------|-----------|-----------|--------------|
| **JNativeHook** | 2.2.2 | Global keyboard/mouse | ✅ Cross-platform<br>✅ Ativo<br>✅ Completo | ❌ Requer privilégios |
| **Java Robot** | Built-in | GUI automation | ✅ Nativo<br>✅ Simples | ❌ Limitado<br>❌ GUI focus apenas |
| **JNA** | 5.13.0 | Native access | ✅ Flexível | ❌ Requer código nativo |

**Recomendação:** `JNativeHook` definitivamente

### Cryptography

| Biblioteca | Versão Maven | Propósito | Vantagens | Desvantagens |
|-----------|--------------|-----------|-----------|--------------|
| **Bouncy Castle** | 1.70 | Crypto completo | ✅ Comprehensive<br>✅ Certificações<br>✅ Algoritmos raros | ❌ Curva de aprendizado |
| **JCE** | Built-in | Crypto padrão | ✅ Nativo<br>✅ Simples | ❌ Limitado |

**Recomendação:** `Bouncy Castle` para pentest

### Reporting

| Biblioteca | Versão Maven | Propósito | Vantagens | Desvantagens |
|-----------|--------------|-----------|-----------|--------------|
| **Apache POI** | 5.2.3 | Excel files | ✅ Completo<br>✅ XLSX moderno | ❌ Complexo para casos simples |
| **iText7** | 7.2.5 | PDF generation | ✅ Profissional<br>✅ Features | ❌ Licença AGPL (comercial pago) |
| **JasperReports** | 6.20.5 | Report engine | ✅ Templates<br>✅ Múltiplos formatos | ❌ Muito complexo |

**Recomendação:** `Apache POI` + `iText7` para relatórios profissionais

### Metrics & Monitoring

| Biblioteca | Versão Maven | Propósito | Vantagens | Desvantagens |
|-----------|--------------|-----------|-----------|--------------|
| **Micrometer** | 1.11.4 | Metrics facade | ✅ Vendor-neutral<br>✅ Spring integration<br>✅ Múltiplos backends | ❌ Overhead mínimo |
| **Dropwizard Metrics** | 4.2.19 | Metrics library | ✅ Simples<br>✅ Direto | ❌ Menos integração |

**Recomendação:** `Micrometer` (padrão Spring Boot)

---

## 🛠️ Ferramentas CLI Essenciais

### WiFi & Network

| Ferramenta | Comando Básico | Output | Parsing Requerido |
|-----------|----------------|--------|-------------------|
| **airodump-ng** | `airodump-ng --output-format csv --write /tmp/scan wlan0mon` | CSV | ✅ Fácil (CSV) |
| **nmcli** | `nmcli -t -f SSID,BSSID,SIGNAL dev wifi` | TSV | ✅ Fácil (delimitado) |
| **iwlist** | `iwlist wlan0 scan` | Text | ⚠️ Regex complexo |
| **kismet** | REST API: `curl http://localhost:2501/devices/...` | JSON | ✅ Fácil (JSON) |
| **bettercap** | REST API: `curl http://localhost:8083/api/session` | JSON | ✅ Fácil (JSON) |

**Recomendação:** Kismet/Bettercap (API REST) > airodump-ng (CSV) > nmcli (TSV) > iwlist (Texto)

### Password Cracking

| Ferramenta | Comando Básico | Speed | GPU Support |
|-----------|----------------|-------|-------------|
| **hashcat** | `hashcat -m 22000 -a 0 hash.hc22000 wordlist.txt` | ⭐⭐⭐⭐⭐ | ✅ NVIDIA/AMD |
| **john** | `john --wordlist=rockyou.txt hashes.txt` | ⭐⭐⭐ | ✅ OpenCL |
| **hydra** | `hydra -l user -P wordlist.txt ssh://target` | ⭐⭐⭐⭐ | ❌ CPU |

**Recomendação:** Hashcat (offline) + Hydra (online services)

### Packet Analysis

| Ferramenta | Comando Básico | Output | Real-time |
|-----------|----------------|--------|-----------|
| **tshark** | `tshark -i wlan0 -T json` | JSON | ✅ |
| **tcpdump** | `tcpdump -i wlan0 -w capture.pcap` | PCAP | ✅ |
| **wireshark** | GUI | GUI | ✅ |

**Recomendação:** tshark (automação) + Wireshark (análise manual)

---

## 💰 Custo de Implementação

### Open Source (Grátis)

| Ferramenta | Licença | Suporte Comercial | Comunidade |
|-----------|---------|-------------------|------------|
| Aircrack-ng | GPL v2 | ❌ | ⭐⭐⭐⭐ |
| Kismet | GPL v2 | ❌ | ⭐⭐⭐⭐ |
| Hashcat | MIT | ❌ | ⭐⭐⭐⭐⭐ |
| Metasploit | BSD | ✅ Rapid7 | ⭐⭐⭐⭐⭐ |
| Bettercap | GPL v3 | ❌ | ⭐⭐⭐⭐ |
| ELK Stack | Elastic License | ✅ Elastic | ⭐⭐⭐⭐⭐ |
| Wazuh | GPL v2 | ✅ Wazuh Inc | ⭐⭐⭐⭐ |

### Bibliotecas Java (Grátis)

| Biblioteca | Licença | Uso Comercial | Restrições |
|-----------|---------|---------------|------------|
| JNativeHook | LGPL v3 / GPL v3 | ✅ (LGPL) | Linking dinâmico OK |
| pcap4j | MIT | ✅ | Nenhuma |
| Apache Commons Exec | Apache 2.0 | ✅ | Nenhuma |
| Apache POI | Apache 2.0 | ✅ | Nenhuma |
| iText7 | AGPL | ⚠️ Requer licença comercial | AGPL é viral |
| Bouncy Castle | MIT-like | ✅ | Nenhuma |

**⚠️ Atenção:** iText7 requer licença comercial para uso em software proprietário (AGPL)

### Ferramentas Comerciais (Pagas)

| Ferramenta | Tipo | Preço Aproximado | Vale a Pena? |
|-----------|------|------------------|--------------|
| **Cobalt Strike** | Red Team C2 | $3,500/ano | ✅ Para empresas |
| **Burp Suite Pro** | Web Security | $449/ano | ✅ Web pentest |
| **Nessus Professional** | Vulnerability Scanner | $2,990/ano | ⚠️ Para compliance |
| **Acunetix** | Web Scanner | $4,500/ano | ⚠️ Alternativas OSS |
| **JProfiler** | Java Profiler | €499 | ⚠️ VisualVM grátis |

**Recomendação:** Começar com Open Source, considerar comercial apenas quando necessário

---

## 🎓 Curva de Aprendizado

| Ferramenta/Lib | Dificuldade | Tempo para Proficiência | Documentação | Comunidade |
|---------------|-------------|-------------------------|--------------|------------|
| Aircrack-ng | 🟡 Média | 1-2 semanas | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Kismet | 🔴 Alta | 2-3 semanas | ⭐⭐⭐ | ⭐⭐⭐ |
| Hashcat | 🟡 Média | 1 semana | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Hydra | 🟢 Baixa | 2-3 dias | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| JNativeHook | 🟢 Baixa | 1-2 dias | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| pcap4j | 🟡 Média | 1 semana | ⭐⭐⭐ | ⭐⭐⭐ |
| Apache POI | 🟡 Média | 3-5 dias | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| iText7 | 🟡 Média | 3-5 dias | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| Metasploit | 🔴 Alta | 1-2 meses | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Bettercap | 🟡 Média | 1-2 semanas | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| ELK Stack | 🔴 Alta | 2-4 semanas | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## 📊 Matriz de Decisão

### Quando Usar Cada Ferramenta

| Cenário | Ferramenta Recomendada | Alternativa | Justificativa |
|---------|----------------------|-------------|---------------|
| **Audit WiFi (Pentest)** | Aircrack-ng + Wifite2 | Bettercap | Padrão indústria, injection |
| **Monitoring WiFi (Blue Team)** | Kismet | Wireshark | WIDS, detecção contínua |
| **Crack WPA2 Hash** | Hashcat | John | GPU acceleration |
| **Bruteforce SSH/FTP** | Hydra | Medusa | Mais protocolos |
| **Keylogging Cross-platform** | JNativeHook | Custom C | Manutenção, estabilidade |
| **Packet Analysis** | pcap4j | tshark via Process | API Java nativa |
| **Relatórios Excel** | Apache POI | Manual CSV | Formatação avançada |
| **Relatórios PDF** | iText7 | JasperReports | Controle fino |
| **Logging Centralizado** | ELK Stack | Splunk | Open source, escalável |
| **Métricas** | Micrometer | Custom | Padrão Spring Boot |
| **Post-Exploitation** | Metasploit | Empire | Mais maduro, Meterpreter |
| **MITM Attacks** | Bettercap | Ettercap | Moderno, API REST |

---

## 🚀 Quick Start - Top 5 Prioridades

### 1️⃣ **Aircrack-ng Integration** (2-3 dias)
```bash
# Instalar
sudo apt install aircrack-ng

# Integrar
// Ver: ROADMAP_INTEGRACAO_FERRAMENTAS_PRO.md - Seção AircrackAdapter
```

### 2️⃣ **Hashcat Integration** (2-3 dias)
```bash
# Instalar
sudo apt install hashcat

# Integrar
// Ver: ROADMAP_INTEGRACAO_FERRAMENTAS_PRO.md - Seção HashcatAdapter
```

### 3️⃣ **JNativeHook Migration** (1-2 dias)
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.github.kwhat</groupId>
    <artifactId>jnativehook</artifactId>
    <version>2.2.2</version>
</dependency>
```

### 4️⃣ **Apache Commons Exec** (1 dia)
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-exec</artifactId>
    <version>1.3</version>
</dependency>
```

### 5️⃣ **pcap4j for Analysis** (3-5 dias)
```xml
<dependency>
    <groupId>org.pcap4j</groupId>
    <artifactId>pcap4j-core</artifactId>
    <version>1.8.2</version>
</dependency>
```

**Total:** 9-14 dias (2-3 semanas)

---

## 📖 Recursos de Aprendizado

### Documentação Oficial (Essencial)

| Ferramenta | Link | Qualidade |
|-----------|------|-----------|
| Aircrack-ng | https://aircrack-ng.org/documentation.html | ⭐⭐⭐⭐ |
| Hashcat | https://hashcat.net/wiki/ | ⭐⭐⭐⭐⭐ |
| Kismet | https://kismetwireless.net/docs/ | ⭐⭐⭐⭐ |
| JNativeHook | https://github.com/kwhat/jnativehook/wiki | ⭐⭐⭐⭐ |
| pcap4j | https://www.pcap4j.org/ | ⭐⭐⭐ |

### Tutoriais Práticos

| Recurso | Tipo | Link |
|---------|------|------|
| **Aircrack-ng Tutorial** | Text | https://www.aircrack-ng.org/doku.php?id=tutorial |
| **Hashcat Examples** | Text | https://hashcat.net/wiki/doku.php?id=example_hashes |
| **HackTheBox** | Platform | https://www.hackthebox.com/ |
| **TryHackMe** | Platform | https://tryhackme.com/ |

---

## ✅ Checklist Final

### Fase 1: Fundação (2 semanas)
- [ ] Adicionar todas dependências Maven
- [ ] Implementar AircrackAdapter
- [ ] Implementar HashcatAdapter  
- [ ] Migrar KeyloggerService para JNativeHook
- [ ] Migrar ProcessManager para Commons Exec
- [ ] Testes unitários

### Fase 2: Network (2 semanas)
- [ ] Integrar pcap4j
- [ ] Implementar KismetService
- [ ] Melhorar WiFiScanner
- [ ] Testes de integração

### Fase 3: Reporting (1 semana)
- [ ] Implementar Excel reports (POI)
- [ ] Implementar PDF reports (iText)
- [ ] Templates profissionais

### Fase 4: Observability (2 semanas)
- [ ] Configurar Micrometer
- [ ] Integrar Prometheus
- [ ] Setup ELK Stack
- [ ] Dashboards Kibana/Grafana

---

**Documento criado em:** Outubro 2024  
**Versão:** 1.0  
**Para:** Cyber Security Suite - Professional Upgrade
