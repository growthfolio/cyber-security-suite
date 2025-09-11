
# Codex Raziel CS — Cyber Security Research Suite v1.0.0

<p align="center">
	<img src="https://img.shields.io/badge/Red%20Team-Ready-critical?style=for-the-badge&logo=redhat" alt="Red Team Ready"/>
	<img src="https://img.shields.io/badge/Java-17%2B-blue?style=for-the-badge&logo=java" alt="Java 17+"/>
	<img src="https://img.shields.io/badge/Linux-Preferred-success?style=for-the-badge&logo=linux" alt="Linux Preferred"/>
</p>

<p align="center">
	<b>Professional Red Team Operations Platform</b><br>
	<i>AI-powered, workflow-driven, and natively integrated with real offensive tools</i>
</p>

---

> ⚠️ <b>LEGAL NOTICE:</b> This software is intended <b>EXCLUSIVELY</b> for authorized cybersecurity research, education, and testing in controlled environments. <br>
> <b>Never use without explicit permission.</b>


## 🚀 What's New in v1.0.0

**Phase 6: Real Workflow Integration**
- 🛠️ <b>Real Tool Execution</b>: Native integration with C/Go cybersecurity tools
- 🤖 <b>Intelligent Workflows</b>: Automated Red Team attack scenarios
- 📊 <b>Result Correlation</b>: AI-powered analysis across multiple tools
- 🖥️ <b>Professional UI</b>: Complete workflow orchestration dashboard


## �️ System Requirements

**Java Runtime:**  
• Java 17 or higher  
• JavaFX runtime (included in most distributions)

**Operating System:**  
• Linux (Ubuntu 20.04+, Debian 11+, CentOS 8+)  
• Windows 10/11 (with WSL for full functionality)  
• macOS 11+ (limited functionality)

**Required Tools (for full functionality):**
```bash
# Network tools
sudo apt install network-manager wireless-tools nmap
# Security tools (optional)
sudo apt install aircrack-ng hydra john hashcat
# Development tools (for native compilation)
sudo apt install gcc make build-essential
```


## ⚡ Quick Start

1. <b>Download and Run:</b>
```bash
wget https://github.com/growthfolio/cyber-security-suite/releases/download/v1.0.0/CodexRazielCS-v1.0.0.jar
java -jar CodexRazielCS-v1.0.0.jar
```
2. <b>First Launch:</b>
	- The application will check system permissions
	- Install missing tools as suggested
	- Configure network interfaces in Settings
3. <b>Start Red Team Operations:</b>
	- Go to <b>Red Team Workflows</b> tab
	- Select an attack scenario
	- Configure parameters and start workflow
	- Monitor real-time execution and results


## 🧩 Available Workflows

<details>
<summary><b>Network Reconnaissance</b></summary>

- WiFi network discovery using nmcli/iwlist
- Network vulnerability scanning with nmap
- Target prioritization and selection
</details>

<details>
<summary><b>Credential Harvesting</b></summary>

- SSH brute force attacks with hydra
- Keylogger deployment (native C implementation)
- Credential extraction and analysis
</details>

<details>
<summary><b>Lateral Movement</b></summary>

- Privilege escalation techniques
- Persistence establishment
- Network pivoting strategies
</details>

<details>
<summary><b>Full Red Team Engagement</b></summary>

- Complete 6-step attack chain
- Automated tool coordination
- Comprehensive reporting
</details>


## ✨ Features

**Real Tool Integration:**
- 🟢 Native keylogger (C implementation with X11 hooks)
- 🟢 Hybrid brute force framework (C+Go)
- 🟢 WiFi scanning (nmcli, iwlist integration)
- 🟢 Network analysis (nmap integration)

**Intelligent Analysis:**
- 🧠 Result correlation across tools
- 🧠 Attack path identification
- 🧠 Risk assessment generation
- 🧠 Context-aware parameter passing

**Professional Interface:**
- 🖥️ Real-time workflow monitoring
- 📈 Progress visualization
- 📝 Audit logging
- 📤 Export capabilities


## ⚠️ Security & Legal Warnings

> **This tool executes REAL cybersecurity tools.**<br>
> <b>Use only in authorized environments.</b> <br>
> Requires proper permissions and legal authorization.<br>
> All activities are logged for audit purposes.<br>
> <b>Respect all applicable laws and regulations.</b>

**Recommended Usage:**
- Isolated lab environments
- Authorized penetration testing
- Cybersecurity education and training
- Red Team exercises with proper approval


## � Troubleshooting & Tips

**Permission denied errors:**
```bash
sudo usermod -a -G netdev,wireshark $USER
# Logout and login again
```
**Missing tools:**
```bash
sudo apt update
sudo apt install network-manager wireless-tools nmap
```
**JavaFX not found:**
```bash
sudo apt install openjfx
# Or use Oracle JDK 17+
```


## � Community & Support

- 📚 **Documentation:** [GitHub Wiki](https://github.com/growthfolio/cyber-security-suite/wiki)
- 🐞 **Issues:** [GitHub Issues](https://github.com/growthfolio/cyber-security-suite/issues)
- 💬 **Discussions:** [GitHub Discussions](https://github.com/growthfolio/cyber-security-suite/discussions)


## 📄 License

This project is developed for educational and research purposes. Use responsibly and within legal boundaries.

---

<p align="center">
	<b>🔬 Professional Cybersecurity Research Platform</b><br>
	<i>Built with ❤️ for the cybersecurity community</i>
</p>