# 🧪 Test Guide - ToolExecutor System

## Quick Test Commands

### 1. Check System Tools

```bash
# Check if tools are available
which aircrack-ng
which hashcat
which nmap
which hydra

# Get versions
aircrack-ng --version
hashcat --version
nmap --version
```

### 2. Manual Test of ToolExecutor

Create a simple test class:

```java
package com.codexraziel.cybersec.test;

import com.codexraziel.cybersec.core.execution.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutorTester implements CommandLineRunner {
    
    @Autowired
    private ToolExecutor toolExecutor;
    
    @Override
    public void run(String... args) {
        System.out.println("=== Testing ToolExecutor ===\n");
        
        // Test 1: Check available tools
        testToolAvailability();
        
        // Test 2: Simple execution
        testSimpleExecution();
        
        // Test 3: With output callbacks
        testWithCallbacks();
    }
    
    private void testToolAvailability() {
        System.out.println("1. Checking tool availability:");
        
        String[] tools = {"aircrack-ng", "hashcat", "nmap", "hydra", "ls", "echo"};
        
        for (String tool : tools) {
            boolean available = toolExecutor.isToolAvailable(tool);
            String version = available ? toolExecutor.getToolVersion(tool) : "N/A";
            System.out.printf("  %s: %s (version: %s)%n", 
                tool, 
                available ? "✅" : "❌",
                version);
        }
        System.out.println();
    }
    
    private void testSimpleExecution() {
        System.out.println("2. Testing simple execution (ls -la):");
        
        ToolConfig config = ToolConfig.simple("ls", "-la", "/tmp");
        ExecutionResult result = toolExecutor.execute(config);
        
        System.out.println("  Exit code: " + result.getExitCode());
        System.out.println("  Success: " + result.isSuccess());
        System.out.println("  Output (first 200 chars): ");
        System.out.println("  " + result.getStdout().substring(0, Math.min(200, result.getStdout().length())));
        System.out.println();
    }
    
    private void testWithCallbacks() {
        System.out.println("3. Testing with real-time callbacks (echo test):");
        
        ToolConfig config = ToolConfig.simple("echo", "Hello from ToolExecutor!");
        
        toolExecutor.executeWithOutput(
            config,
            line -> System.out.println("  [STDOUT] " + line),
            line -> System.err.println("  [STDERR] " + line)
        ).thenAccept(result -> {
            System.out.println("  Async execution completed: " + result.isSuccess());
        }).join(); // Wait for completion
        
        System.out.println();
    }
}
```

### 3. Test Aircrack-ng Adapter (Dry Run)

```java
package com.codexraziel.cybersec.test;

import com.codexraziel.cybersec.adapters.wifi.AircrackNgAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// @Component // Uncomment to enable
public class AircrackNgTester implements CommandLineRunner {
    
    @Autowired
    private AircrackNgAdapter aircrackNg;
    
    @Override
    public void run(String... args) {
        System.out.println("=== Testing AircrackNgAdapter ===\n");
        
        // Test availability
        System.out.println("Aircrack-ng available: " + aircrackNg.isAvailable());
        System.out.println("Version: " + aircrackNg.getVersion());
        
        // Note: Actual WiFi tests require wireless interface and sudo
        System.out.println("\nTo test full WiFi functionality:");
        System.out.println("1. Ensure you have a wireless interface (wlan0)");
        System.out.println("2. Run with sudo privileges");
        System.out.println("3. Uncomment the WiFi test methods below");
    }
    
    // IMPORTANT: Only run these on authorized networks!
    private void testMonitorMode() {
        // aircrackNg.enableMonitorMode("wlan0")
        //     .thenAccept(result -> {
        //         System.out.println("Monitor mode: " + result);
        //     });
    }
}
```

### 4. Run Tests

```bash
# Compile
mvn clean compile

# Run with test component
mvn spring-boot:run
```

## Expected Output

```
=== Testing ToolExecutor ===

1. Checking tool availability:
  aircrack-ng: ✅ (version: Aircrack-ng 1.7)
  hashcat: ✅ (version: hashcat (v6.2.5))
  nmap: ✅ (version: Nmap 7.80)
  hydra: ❌ (version: N/A)
  ls: ✅ (version: ls (GNU coreutils) 8.32)
  echo: ✅ (version: echo (GNU coreutils) 8.32)

2. Testing simple execution (ls -la):
  Exit code: 0
  Success: true
  Output (first 200 chars): 
  total 48
  drwxrwxrwt 12 root root 4096 Oct 22 22:41 .
  drwxr-xr-x 19 root root 4096 Sep  3 23:21 ..
  ...

3. Testing with real-time callbacks (echo test):
  [STDOUT] Hello from ToolExecutor!
  Async execution completed: true

=== Testing AircrackNgAdapter ===

Aircrack-ng available: true
Version: Aircrack-ng 1.7

To test full WiFi functionality:
1. Ensure you have a wireless interface (wlan0)
2. Run with sudo privileges
3. Uncomment the WiFi test methods below
```

## Integration Tests (Advanced)

### Test with Docker

```bash
# Run in isolated environment
docker run -it --rm \
  --cap-add=NET_ADMIN \
  --cap-add=NET_RAW \
  -v $(pwd):/app \
  ubuntu:22.04 \
  bash

# Inside container
apt update
apt install -y aircrack-ng openjdk-17-jdk maven

cd /app
mvn clean test
```

## Troubleshooting

### Tool Not Found
```bash
# Install missing tools
sudo apt install aircrack-ng hashcat nmap hydra

# Or use Kali Linux
docker pull kalilinux/kali-rolling
```

### Permission Denied
```bash
# Run with sudo
sudo mvn spring-boot:run

# Or add user to netdev group
sudo usermod -a -G netdev $USER
```

### Wireless Interface Issues
```bash
# Check interfaces
ip link show
iwconfig

# Check if interface supports monitor mode
iw list | grep "Supported interface modes" -A 8
```

---

**Note:** This is a research project. Only use on authorized networks and equipment!
