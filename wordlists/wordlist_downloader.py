#!/usr/bin/env python3
"""
Wordlist Downloader para Laboratório de Pentesting
Script para baixar wordlists legítimas de fontes confiáveis
Uso educacional e testes autorizados apenas
"""

import os
import sys
import requests
import zipfile
import gzip
from urllib.parse import urlparse
from pathlib import Path
import hashlib

class WordlistDownloader:
    def __init__(self, download_dir="wordlists"):
        self.download_dir = Path(download_dir)
        self.download_dir.mkdir(exist_ok=True)
        
        # URLs de wordlists legítimas
        self.wordlists = {
            "rockyou": {
                "url": "https://github.com/brannondorsey/naive-hashcat/releases/download/data/rockyou.txt",
                "filename": "rockyou.txt",
                "description": "14M senhas do famoso vazamento RockYou"
            },
            "common_passwords": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/10-million-password-list-top-1000000.txt",
                "filename": "top-1m-passwords.txt",
                "description": "Top 1 milhão de senhas mais comuns"
            },
            "usernames": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Usernames/top-usernames-shortlist.txt",
                "filename": "top-usernames.txt",
                "description": "Lista de usernames mais comuns"
            },
            "directories": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/directory-list-2.3-medium.txt",
                "filename": "directory-list-medium.txt",
                "description": "Lista de diretórios web comuns"
            },
            "subdomains": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/DNS/subdomains-top1million-5000.txt",
                "filename": "subdomains-top5k.txt",
                "description": "Top 5k subdomínios mais comuns"
            },
            "default_passwords": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Default-Credentials/default-passwords.csv",
                "filename": "default-passwords.csv",
                "description": "Senhas padrão de equipamentos/sistemas"
            }
        }

    def download_file(self, url, filename, description=""):
        """Download de arquivo com barra de progresso"""
        filepath = self.download_dir / filename
        
        if filepath.exists():
            print(f"[✓] {filename} já existe, pulando...")
            return True
            
        print(f"[⬇] Baixando {description}...")
        print(f"    URL: {url}")
        
        try:
            response = requests.get(url, stream=True)
            response.raise_for_status()
            
            total_size = int(response.headers.get('content-length', 0))
            downloaded = 0
            
            with open(filepath, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)
                        
                        if total_size > 0:
                            percent = (downloaded * 100) // total_size
                            print(f"\r    Progresso: {percent}% ({downloaded}/{total_size} bytes)", end='')
            
            print(f"\n[✓] {filename} baixado com sucesso!")
            return True
            
        except Exception as e:
            print(f"\n[✗] Erro ao baixar {filename}: {str(e)}")
            if filepath.exists():
                filepath.unlink()
            return False

    def verify_file_size(self, filename, min_size_kb=10):
        """Verifica se o arquivo foi baixado corretamente"""
        filepath = self.download_dir / filename
        if not filepath.exists():
            return False
            
        size_kb = filepath.stat().st_size // 1024
        return size_kb >= min_size_kb

    def download_all(self):
        """Baixa todas as wordlists"""
        print("=" * 60)
        print("WORDLIST DOWNLOADER PARA PENTESTING LAB")
        print("Fontes legítimas: SecLists, GitHub")
        print("=" * 60)
        
        successful = 0
        failed = 0
        
        for key, info in self.wordlists.items():
            print(f"\n[{successful + failed + 1}/{len(self.wordlists)}] {info['description']}")
            
            if self.download_file(info['url'], info['filename'], info['description']):
                if self.verify_file_size(info['filename']):
                    successful += 1
                else:
                    print(f"[⚠] Arquivo {info['filename']} parece estar corrompido (muito pequeno)")
                    failed += 1
            else:
                failed += 1
        
        print("\n" + "=" * 60)
        print(f"RESUMO: {successful} sucessos, {failed} falhas")
        print(f"Arquivos salvos em: {self.download_dir.absolute()}")
        
        if successful > 0:
            self.show_usage_examples()

    def download_specific(self, wordlist_keys):
        """Baixa wordlists específicas"""
        for key in wordlist_keys:
            if key not in self.wordlists:
                print(f"[✗] Wordlist '{key}' não encontrada!")
                continue
                
            info = self.wordlists[key]
            print(f"\nBaixando: {info['description']}")
            self.download_file(info['url'], info['filename'], info['description'])

    def list_available(self):
        """Lista wordlists disponíveis"""
        print("Wordlists disponíveis:")
        print("-" * 40)
        for key, info in self.wordlists.items():
            print(f"{key:15} - {info['description']}")

    def show_usage_examples(self):
        """Mostra exemplos de uso das wordlists"""
        print("\n" + "=" * 60)
        print("EXEMPLOS DE USO:")
        print("=" * 60)
        
        examples = [
            ("Hashcat (quebrar MD5)", f"hashcat -a 0 -m 0 hash.txt {self.download_dir}/rockyou.txt"),
            ("John the Ripper", f"john --wordlist={self.download_dir}/rockyou.txt hash.txt"),
            ("Hydra SSH", f"hydra -L {self.download_dir}/top-usernames.txt -P {self.download_dir}/top-1m-passwords.txt ssh://target-ip"),
            ("Dirb (web dirs)", f"dirb http://target-url {self.download_dir}/directory-list-medium.txt"),
            ("Gobuster", f"gobuster dir -u http://target-url -w {self.download_dir}/directory-list-medium.txt")
        ]
        
        for desc, cmd in examples:
            print(f"\n{desc}:")
            print(f"  {cmd}")

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="Download wordlists para laboratório de pentesting")
    parser.add_argument("-d", "--dir", default="wordlists", help="Diretório de download (padrão: wordlists)")
    parser.add_argument("-l", "--list", action="store_true", help="Lista wordlists disponíveis")
    parser.add_argument("-s", "--specific", nargs="+", help="Baixa wordlists específicas")
    parser.add_argument("--examples", action="store_true", help="Mostra apenas exemplos de uso")
    
    args = parser.parse_args()
    
    downloader = WordlistDownloader(args.dir)
    
    if args.list:
        downloader.list_available()
    elif args.specific:
        downloader.download_specific(args.specific)
    elif args.examples:
        downloader.show_usage_examples()
    else:
        downloader.download_all()

if __name__ == "__main__":
    main()