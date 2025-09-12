# Wordlists Directory

This directory should contain wordlists for security testing. Due to GitHub file size limitations, large wordlists are not included in the repository.

## Required Wordlists

To use the full functionality of this security suite, download the following wordlists:

### Passwords
- **rockyou.txt** (133MB) - Most common passwords
  - Download from: https://github.com/brannondorsey/naive-hashcat/releases/download/data/rockyou.txt
  - Place in: `wordlists/passwords/rockyou.txt`

### Usernames
- **10m-usernames.txt** (81MB) - Common usernames
  - Download from: https://github.com/insidetrust/statistically-likely-usernames
  - Place in: `wordlists/usernames/10m-usernames.txt`

### Other Recommended Wordlists
- **SecLists** - Comprehensive security testing lists
  - Repository: https://github.com/danielmiessler/SecLists
  - Contains: subdomain lists, parameter names, common passwords, etc.

## Directory Structure
```
wordlists/
├── README.md (this file)
├── passwords/
│   └── rockyou.txt (download required)
├── usernames/
│   └── 10m-usernames.txt (download required)
├── subdomains/
├── parameters/
└── payloads/
```

## Legal Notice
These wordlists are intended for authorized security testing only. Ensure you have proper permission before using any security testing tools.

## Download Script
You can use the following commands to download the required wordlists:

```bash
# Create directories
mkdir -p wordlists/passwords wordlists/usernames

# Download rockyou.txt
wget -O wordlists/passwords/rockyou.txt https://github.com/brannondorsey/naive-hashcat/releases/download/data/rockyou.txt

# Download 10m-usernames.txt (alternative sources)
# Check https://github.com/insidetrust/statistically-likely-usernames for latest links
```

**Note:** Always verify the integrity and source of downloaded wordlists.
