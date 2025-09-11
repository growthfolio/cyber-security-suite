
# Cyber Security Research Suite

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-blue?style=flat-square&logo=java" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/Linux-Preferred-success?style=flat-square&logo=linux" alt="Linux Preferred"/>
</p>

<p align="center">
  <b>Professional GUI for Scientific Cybersecurity Research</b><br>
  <i>Modular, workflow-driven, and integrates real offensive tools</i>
</p>

## Features

- BruteForce Management (Red Team)
- Keylogger Control (Stealth, native C)
- Android Monitor integration
- Real-time results and statistics
- YAML-based configuration profiles
- Exportable scientific reports

## Architecture

- JavaFX (fx:root pattern, modular UI)
- Spring Boot (dependency injection, config)
- Lombok (clean code)
- Real-time UI updates (JavaFX Properties)

## Quick Start

**Prerequisites:**
- Java 17+
- Maven 3.6+
- Built tools in `tools/` directory

**Build & Run:**
```bash
mvn clean compile
mvn javafx:run   # or: mvn spring-boot:run
```

## Project Structure (Simplified)

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

## Configuration

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

## Development

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

## Integration

The GUI integrates with existing tools via:
- **Process execution** and output monitoring
- **File-based configuration** management
- **Real-time result parsing** and display
- **Status monitoring** and control

## Scientific Use

Perfect for:
- **Red Team training** and exercises
- **Blue Team detection** development
- **Academic research** in cybersecurity
- **Tool demonstration** and education


> This software is intended **EXCLUSIVELY** for:
> - Academic cybersecurity research
> - Blue Team/Red Team training (in labs)
> - Scientific analysis and education
> - Controlled, authorized environments only

**Never use for illegal activities.**

---

<p align="center">
  <i>Discreet, modular, and research-focused.</i>
</p>