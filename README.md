# 🔬 Cyber Security Research Suite

Professional JavaFX GUI for managing cyber security research tools with Spring Boot integration.

## 🎯 Features

- **🔴 BruteForce Management** - Control Red Team BruteForce attacks
- **🎯 Keylogger Control** - Manage Stealth Keylogger operations  
- **🤖 Android Monitor** - Control Android monitoring tools
- **📊 Real-time Results** - Live charts and statistics
- **⚙️ Configuration Management** - YAML-based profiles
- **📈 Scientific Reports** - Export results for research

## 🏗️ Architecture

- **JavaFX** with fx:root pattern for modular components
- **Spring Boot** for dependency injection and configuration
- **Lombok** for clean, concise code
- **Real-time UI updates** with JavaFX Properties

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Built cyber security tools in `tools/` directory

### Build & Run
```bash
# Build the application
mvn clean compile

# Run with JavaFX
mvn javafx:run

# Or run with Spring Boot
mvn spring-boot:run
```

## 📁 Project Structure

```
cyber-security-suite/
├── src/main/java/com/research/cybersec/
│   ├── CyberSecurityApplication.java     # Main application
│   ├── components/                       # JavaFX components
│   │   ├── base/CyberSecComponent.java   # Base fx:root component
│   │   ├── dashboard/DashboardView.java  # Main dashboard
│   │   ├── bruteforce/BruteForcePanel.java
│   │   └── results/ResultsPanel.java
│   ├── services/                         # Business logic
│   │   ├── ProcessManager.java           # Process control
│   │   └── ConfigManager.java            # Configuration
│   └── models/                           # Data models
├── src/main/resources/
│   ├── *.fxml                           # JavaFX layouts
│   ├── css/cybersec.css                 # Dark theme
│   └── application.yml                  # Spring config
└── tools/                               # Linked tools
    ├── redteam-bruteforce/
    ├── stealth-keylogger/
    └── android-monitor/
```

## 🎨 UI Components

### Dashboard
- Tabbed interface for each tool
- Global status and progress monitoring
- Menu system for configuration

### BruteForce Panel
- Attack configuration (target, protocol, threads)
- Real-time output with syntax highlighting
- Progress tracking and control buttons

### Results Panel
- Tabular results with filtering
- Success rate charts over time
- Protocol distribution pie charts
- Export functionality

## ⚙️ Configuration

Edit `src/main/resources/application.yml`:

```yaml
cybersec:
  tools:
    bruteforce:
      path: "./tools/redteam-bruteforce/bin/redteam-bf"
    keylogger:
      path: "./tools/stealth-keylogger/bin/system-monitor"
    android:
      adb-path: "/usr/bin/adb"
```

## 🔧 Development

### Adding New Components
1. Extend `CyberSecComponent<T>`
2. Create corresponding FXML file
3. Register as Spring `@Component`
4. Add to dashboard tabs

### fx:root Pattern
```java
@Component
@Scope("prototype")
public class MyPanel extends CyberSecComponent<VBox> {
    @FXML private Button myButton;
    
    @Override
    protected void onComponentLoaded() {
        // Initialize component
    }
}
```

## 📊 Integration

The GUI integrates with existing tools via:
- **Process execution** and output monitoring
- **File-based configuration** management
- **Real-time result parsing** and display
- **Status monitoring** and control

## 🎓 Scientific Use

Perfect for:
- **Red Team training** and exercises
- **Blue Team detection** development
- **Academic research** in cybersecurity
- **Tool demonstration** and education

## ⚠️ Legal Notice

This software is intended **EXCLUSIVELY** for:
- 🎓 Academic cybersecurity research
- 🔵 Blue Team training (detection and response)
- 🔴 Red Team training (evasion techniques)
- 🧪 Scientific analysis of system behavior
- 📚 Educational purposes in controlled environments

**🚨 DO NOT USE FOR ILLEGAL ACTIVITIES! 🚨**

---

**🔬 Professional GUI for Scientific Cybersecurity Research** ⚡