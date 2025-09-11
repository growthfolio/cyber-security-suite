#!/bin/bash

# Codex Raziel CS - Launcher Script
# Professional Cyber Security Research Suite

echo "🔬 Codex Raziel CS - Cyber Security Research Suite v1.0.0"
echo "=========================================================="
echo ""

# Check Java version
echo "🔍 Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17 or higher."
    echo "   Ubuntu/Debian: sudo apt install openjdk-17-jre"
    echo "   CentOS/RHEL: sudo yum install java-17-openjdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "⚠️  Java $JAVA_VERSION detected. Java 17+ recommended for best performance."
    echo "   Consider upgrading: sudo apt install openjdk-17-jre"
fi

echo "✅ Java $JAVA_VERSION detected"
echo ""

# Check for JavaFX
echo "🔍 Checking JavaFX availability..."
if java -cp "CodexRazielCS-v1.0.0.jar" --list-modules 2>/dev/null | grep -q javafx; then
    echo "✅ JavaFX available"
else
    echo "⚠️  JavaFX not detected. Installing OpenJFX recommended:"
    echo "   Ubuntu/Debian: sudo apt install openjfx"
fi
echo ""

# Check system tools
echo "🔍 Checking system tools..."
TOOLS_MISSING=0

check_tool() {
    if command -v "$1" &> /dev/null; then
        echo "✅ $1 available"
    else
        echo "❌ $1 missing - $2"
        TOOLS_MISSING=1
    fi
}

check_tool "nmcli" "sudo apt install network-manager"
check_tool "iwlist" "sudo apt install wireless-tools"
check_tool "nmap" "sudo apt install nmap"
check_tool "gcc" "sudo apt install build-essential"

if [ $TOOLS_MISSING -eq 1 ]; then
    echo ""
    echo "⚠️  Some tools are missing. Install them for full functionality."
    echo "   The application will still run with limited features."
fi

echo ""
echo "🚀 Starting Codex Raziel CS..."
echo ""
echo "⚠️  LEGAL NOTICE:"
echo "   This software executes REAL cybersecurity tools."
echo "   Only use in authorized environments with proper permissions."
echo "   Monitor all activities and respect applicable laws."
echo ""

# Launch application
java -jar CodexRazielCS-v1.0.0.jar

echo ""
echo "👋 Codex Raziel CS session ended."