# Download Large Wordlists

Due to GitHub's file size limitations (100MB), some large wordlists are not included in this repository.

## Required Large Files

### 1. RockYou Password List (133MB)
```bash
# Download from SecLists
curl -o wordlists/passwords/rockyou.txt https://github.com/danielmiessler/SecLists/raw/master/Passwords/Leaked-Databases/rockyou.txt.tar.gz
tar -xzf wordlists/passwords/rockyou.txt.tar.gz -C wordlists/passwords/
rm wordlists/passwords/rockyou.txt.tar.gz
```

### 2. 10 Million Usernames (81MB)
```bash
# Download from SecLists
curl -o wordlists/usernames/10m-usernames.txt https://github.com/insidetrust/statistically-likely-usernames/raw/master/jeanropke-1-billion-usernames-sorted.txt
```

## Alternative Sources

### RockYou
- Direct: https://www.kaggle.com/datasets/wjburns/common-password-list-rockyoutxt
- Mirror: https://github.com/brannondorsey/naive-hashcat/releases/download/data/rockyou.txt

### Large Username Lists
- 10M Usernames: https://github.com/insidetrust/statistically-likely-usernames
- Billion Usernames: https://weakpass.com/lists

## Quick Setup Script

```bash
#!/bin/bash
# Run this script from the cyber-security-suite directory

echo "Downloading large wordlists..."

# Create directories
mkdir -p wordlists/passwords
mkdir -p wordlists/usernames

# Download RockYou (if not exists)
if [ ! -f "wordlists/passwords/rockyou.txt" ]; then
    echo "Downloading RockYou wordlist..."
    curl -L -o rockyou.txt.tar.gz "https://github.com/danielmiessler/SecLists/raw/master/Passwords/Leaked-Databases/rockyou.txt.tar.gz"
    tar -xzf rockyou.txt.tar.gz
    mv rockyou.txt wordlists/passwords/
    rm rockyou.txt.tar.gz
fi

# Download 10M Usernames (if not exists)
if [ ! -f "wordlists/usernames/10m-usernames.txt" ]; then
    echo "Downloading 10M usernames..."
    curl -L -o wordlists/usernames/10m-usernames.txt "https://github.com/insidetrust/statistically-likely-usernames/raw/master/jeanropke-1-billion-usernames-sorted.txt"
fi

echo "Download complete!"
echo "Large wordlists are now available in:"
echo "  - wordlists/passwords/rockyou.txt"
echo "  - wordlists/usernames/10m-usernames.txt"
```

## Usage Notes

1. These files are automatically ignored by Git (see `.gitignore`)
2. Download them after cloning the repository
3. The application will detect missing files and show download instructions
4. Total download size: ~215MB

## Legal Notice

These wordlists are for educational and authorized security testing only. Ensure you have proper authorization before using them in any security assessments.
