#ifndef ADVANCED_STEALTH_H
#define ADVANCED_STEALTH_H

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#ifdef _WIN32
    #include <windows.h>
    #include <winternl.h>
    #include <psapi.h>
    #include <tlhelp32.h>
#elif __linux__
    #include <sys/sysinfo.h>
    #include <sys/utsname.h>
    #include <unistd.h>
    #include <fcntl.h>
#elif __APPLE__
    #include <sys/sysctl.h>
    #include <sys/types.h>
    #include <mach/mach.h>
#endif

// Advanced VM/Sandbox Detection Techniques
typedef struct {
    bool is_vm;
    bool is_sandbox;
    bool is_debugged;
    bool is_monitored;
    float confidence_score;
    char detection_method[256];
} detection_result_t;

// VM Detection Methods
static inline bool detect_vm_registry() {
#ifdef _WIN32
    HKEY keys[] = {
        HKEY_LOCAL_MACHINE,
        HKEY_CURRENT_USER
    };
    
    const char* vm_paths[] = {
        "SYSTEM\\CurrentControlSet\\Services\\VBoxService",
        "SYSTEM\\CurrentControlSet\\Services\\VMTools",
        "SOFTWARE\\VMware, Inc.\\VMware Tools",
        "SOFTWARE\\Oracle\\VirtualBox Guest Additions",
        "SYSTEM\\CurrentControlSet\\Services\\vmmouse",
        "SYSTEM\\CurrentControlSet\\Services\\vmhgfs"
    };
    
    for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 6; j++) {
            HKEY key;
            if (RegOpenKeyEx(keys[i], vm_paths[j], 0, KEY_READ, &key) == ERROR_SUCCESS) {
                RegCloseKey(key);
                return true;
            }
        }
    }
#endif
    return false;
}

static inline bool detect_vm_processes() {
#ifdef _WIN32
    const char* vm_processes[] = {
        "vmtoolsd.exe", "vmwaretray.exe", "vmwareuser.exe",
        "VBoxService.exe", "VBoxTray.exe", "VBoxClient.exe",
        "qemu-ga.exe", "xenservice.exe", "vmsrvc.exe",
        "vmusrvc.exe", "prl_cc.exe", "prl_tools.exe"
    };
    
    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snapshot == INVALID_HANDLE_VALUE) return false;
    
    PROCESSENTRY32 pe32;
    pe32.dwSize = sizeof(PROCESSENTRY32);
    
    if (Process32First(snapshot, &pe32)) {
        do {
            for (int i = 0; i < 12; i++) {
                if (strstr(pe32.szExeFile, vm_processes[i])) {
                    CloseHandle(snapshot);
                    return true;
                }
            }
        } while (Process32Next(snapshot, &pe32));
    }
    
    CloseHandle(snapshot);
#elif __linux__
    const char* vm_processes[] = {
        "vmtoolsd", "vmware-guestd", "vboxservice", "vboxclient",
        "qemu-ga", "xe-daemon", "xenstore"
    };
    
    for (int i = 0; i < 7; i++) {
        char cmd[256];
        snprintf(cmd, sizeof(cmd), "pgrep %s > /dev/null 2>&1", vm_processes[i]);
        if (system(cmd) == 0) return true;
    }
#endif
    return false;
}

static inline bool detect_vm_hardware() {
#ifdef _WIN32
    // Check CPU vendor
    int cpuInfo[4];
    __cpuid(cpuInfo, 0);
    
    char vendor[13];
    memcpy(vendor, &cpuInfo[1], 4);
    memcpy(vendor + 4, &cpuInfo[3], 4);
    memcpy(vendor + 8, &cpuInfo[2], 4);
    vendor[12] = '\0';
    
    if (strstr(vendor, "VMware") || strstr(vendor, "VBoxVBox")) {
        return true;
    }
    
    // Check MAC address
    IP_ADAPTER_INFO adapterInfo[16];
    DWORD bufLen = sizeof(adapterInfo);
    
    if (GetAdaptersInfo(adapterInfo, &bufLen) == NO_ERROR) {
        PIP_ADAPTER_INFO adapter = adapterInfo;
        while (adapter) {
            if (adapter->AddressLength == 6) {
                // VMware MAC prefixes: 00:05:69, 00:0C:29, 00:50:56
                if ((adapter->Address[0] == 0x00 && adapter->Address[1] == 0x05 && adapter->Address[2] == 0x69) ||
                    (adapter->Address[0] == 0x00 && adapter->Address[1] == 0x0C && adapter->Address[2] == 0x29) ||
                    (adapter->Address[0] == 0x00 && adapter->Address[1] == 0x50 && adapter->Address[2] == 0x56)) {
                    return true;
                }
                // VirtualBox MAC prefix: 08:00:27
                if (adapter->Address[0] == 0x08 && adapter->Address[1] == 0x00 && adapter->Address[2] == 0x27) {
                    return true;
                }
            }
            adapter = adapter->Next;
        }
    }
#elif __linux__
    // Check DMI information
    FILE* dmi_files[] = {
        fopen("/sys/class/dmi/id/product_name", "r"),
        fopen("/sys/class/dmi/id/sys_vendor", "r"),
        fopen("/sys/class/dmi/id/board_vendor", "r")
    };
    
    const char* vm_strings[] = {
        "VMware", "VirtualBox", "QEMU", "Xen", "KVM", "Microsoft Corporation"
    };
    
    for (int i = 0; i < 3; i++) {
        if (dmi_files[i]) {
            char buffer[256];
            if (fgets(buffer, sizeof(buffer), dmi_files[i])) {
                for (int j = 0; j < 6; j++) {
                    if (strstr(buffer, vm_strings[j])) {
                        fclose(dmi_files[i]);
                        return true;
                    }
                }
            }
            fclose(dmi_files[i]);
        }
    }
#endif
    return false;
}

static inline bool detect_vm_timing() {
    uint64_t start, end;
    
#ifdef _WIN32
    start = __rdtsc();
    Sleep(100);
    end = __rdtsc();
    
    // In VM, RDTSC might be much slower or inconsistent
    uint64_t cycles = end - start;
    return (cycles < 1000000 || cycles > 10000000000ULL);
#else
    struct timespec ts_start, ts_end;
    clock_gettime(CLOCK_MONOTONIC, &ts_start);
    usleep(100000);
    clock_gettime(CLOCK_MONOTONIC, &ts_end);
    
    uint64_t elapsed = (ts_end.tv_sec - ts_start.tv_sec) * 1000000000ULL + 
                      (ts_end.tv_nsec - ts_start.tv_nsec);
    
    // Should be around 100ms (100,000,000 ns)
    return (elapsed < 50000000ULL || elapsed > 200000000ULL);
#endif
}

static inline bool detect_sandbox_mouse() {
#ifdef _WIN32
    POINT pos1, pos2;
    GetCursorPos(&pos1);
    Sleep(1000);
    GetCursorPos(&pos2);
    
    // In sandbox, mouse typically doesn't move
    return (pos1.x == pos2.x && pos1.y == pos2.y);
#endif
    return false;
}

static inline bool detect_sandbox_files() {
#ifdef _WIN32
    const char* sandbox_files[] = {
        "C:\\analysis\\malware_analysis.txt",
        "C:\\sandbox\\sandbox_info.txt",
        "C:\\cuckoo\\cuckoo.txt",
        "C:\\sample\\sample.exe"
    };
    
    for (int i = 0; i < 4; i++) {
        if (GetFileAttributes(sandbox_files[i]) != INVALID_FILE_ATTRIBUTES) {
            return true;
        }
    }
#elif __linux__
    const char* sandbox_files[] = {
        "/tmp/cuckoo",
        "/tmp/analysis",
        "/home/malware",
        "/opt/cuckoo"
    };
    
    for (int i = 0; i < 4; i++) {
        if (access(sandbox_files[i], F_OK) == 0) {
            return true;
        }
    }
#endif
    return false;
}

static inline bool detect_debugger_advanced() {
#ifdef _WIN32
    // Multiple debugger detection methods
    if (IsDebuggerPresent()) return true;
    
    BOOL remote_debugger = FALSE;
    CheckRemoteDebuggerPresent(GetCurrentProcess(), &remote_debugger);
    if (remote_debugger) return true;
    
    // Check PEB flags
    PPEB peb = (PPEB)__readgsqword(0x60);
    if (peb->BeingDebugged) return true;
    if (peb->NtGlobalFlag & 0x70) return true;
    
    // Check heap flags
    PVOID heap = GetProcessHeap();
    DWORD flags = *(DWORD*)((BYTE*)heap + 0x40);
    DWORD force_flags = *(DWORD*)((BYTE*)heap + 0x44);
    if (flags & 0x2 || flags & 0x8000 || force_flags != 0) return true;
    
    return false;
#elif __linux__
    // Check TracerPid in /proc/self/status
    FILE* status = fopen("/proc/self/status", "r");
    if (status) {
        char line[256];
        while (fgets(line, sizeof(line), status)) {
            if (strstr(line, "TracerPid:")) {
                int tracer_pid = atoi(line + 10);
                fclose(status);
                return tracer_pid != 0;
            }
        }
        fclose(status);
    }
    
    // Check for ptrace
    if (ptrace(PTRACE_TRACEME, 0, 1, 0) == -1) return true;
    
    return false;
#endif
}

// Comprehensive detection function
static inline detection_result_t perform_advanced_detection() {
    detection_result_t result = {0};
    int detection_count = 0;
    char methods[1024] = {0};
    
    // VM Detection
    if (detect_vm_registry()) {
        result.is_vm = true;
        detection_count++;
        strcat(methods, "VM_Registry ");
    }
    
    if (detect_vm_processes()) {
        result.is_vm = true;
        detection_count++;
        strcat(methods, "VM_Processes ");
    }
    
    if (detect_vm_hardware()) {
        result.is_vm = true;
        detection_count++;
        strcat(methods, "VM_Hardware ");
    }
    
    if (detect_vm_timing()) {
        result.is_vm = true;
        detection_count++;
        strcat(methods, "VM_Timing ");
    }
    
    // Sandbox Detection
    if (detect_sandbox_mouse()) {
        result.is_sandbox = true;
        detection_count++;
        strcat(methods, "Sandbox_Mouse ");
    }
    
    if (detect_sandbox_files()) {
        result.is_sandbox = true;
        detection_count++;
        strcat(methods, "Sandbox_Files ");
    }
    
    // Debugger Detection
    if (detect_debugger_advanced()) {
        result.is_debugged = true;
        detection_count++;
        strcat(methods, "Debugger ");
    }
    
    // Calculate confidence score
    result.confidence_score = (float)detection_count / 7.0f;
    result.is_monitored = result.is_vm || result.is_sandbox || result.is_debugged;
    
    strncpy(result.detection_method, methods, sizeof(result.detection_method) - 1);
    
    return result;
}

// Anti-analysis delay with jitter
static inline void advanced_anti_analysis_delay() {
    srand(time(NULL));
    int base_delay = 50 + (rand() % 100); // 50-150ms base
    int jitter = rand() % 50; // 0-50ms jitter
    
    uint64_t start = 0, end = 0;
    
#ifdef _WIN32
    start = GetTickCount64();
    Sleep(base_delay + jitter);
    end = GetTickCount64();
#else
    struct timespec ts_start, ts_end;
    clock_gettime(CLOCK_MONOTONIC, &ts_start);
    usleep((base_delay + jitter) * 1000);
    clock_gettime(CLOCK_MONOTONIC, &ts_end);
    
    start = ts_start.tv_sec * 1000 + ts_start.tv_nsec / 1000000;
    end = ts_end.tv_sec * 1000 + ts_end.tv_nsec / 1000000;
#endif
    
    // If delay was significantly longer, likely being analyzed
    uint64_t expected = base_delay + jitter;
    uint64_t actual = end - start;
    
    if (actual > expected * 3) {
        // Detected analysis environment - exit gracefully
        exit(0);
    }
}

#endif // ADVANCED_STEALTH_H