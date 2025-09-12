#!/usr/bin/env python3
"""
Wordlist Downloader Expandido para Pentesting Lab
Versão com mais wordlists especializadas
"""

import os, sys, requests
from pathlib import Path

class ExpandedWordlistDownloader:
    def __init__(self, download_dir="wordlists"):
        self.download_dir = Path(download_dir)
        self.download_dir.mkdir(exist_ok=True)
        
        self.wordlists = {
            # === PASSWORDS ===
            "rockyou": {
                "url": "https://github.com/brannondorsey/naive-hashcat/releases/download/data/rockyou.txt",
                "filename": "rockyou.txt",
                "description": "14M senhas RockYou",
                "category": "passwords"
            },
            "top_1m_passwords": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/10-million-password-list-top-1000000.txt",
                "filename": "top-1m-passwords.txt",
                "description": "Top 1M senhas mais comuns",
                "category": "passwords"
            },
            "darkweb_2017": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/darkweb2017-top10000.txt",
                "filename": "darkweb-top10k.txt",
                "description": "Top 10k senhas darkweb 2017",
                "category": "passwords"
            },
            "common_passwords": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/10k-most-common.txt",
                "filename": "10k-common-passwords.txt",
                "description": "10k senhas mais comuns",
                "category": "passwords"
            },
            "probable_passwords": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/probable-v2-top1575.txt",
                "filename": "probable-top1575.txt",
                "description": "Senhas prováveis baseadas em análise",
                "category": "passwords"
            },
            
            # === USERNAMES ===
            "top_usernames": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Usernames/top-usernames-shortlist.txt",
                "filename": "top-usernames.txt",
                "description": "Usernames mais comuns",
                "category": "usernames"
            },
            "xato_usernames": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Usernames/xato-net-10-million-usernames.txt",
                "filename": "10m-usernames.txt",
                "description": "10M usernames coletados",
                "category": "usernames"
            },
            "names_usernames": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Usernames/Names/names.txt",
                "filename": "names-usernames.txt",
                "description": "Usernames baseados em nomes",
                "category": "usernames"
            },
            
            # === WEB DISCOVERY ===
            "directories_medium": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/directory-list-2.3-medium.txt",
                "filename": "directory-list-medium.txt",
                "description": "Diretórios web médio",
                "category": "web"
            },
            "directories_big": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/directory-list-2.3-big.txt",
                "filename": "directory-list-big.txt",
                "description": "Diretórios web grande",
                "category": "web"
            },
            "common_files": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/common.txt",
                "filename": "web-common.txt",
                "description": "Arquivos e diretórios web comuns",
                "category": "web"
            },
            "raft_files": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/raft-large-files.txt",
                "filename": "raft-large-files.txt",
                "description": "RAFT arquivos grandes",
                "category": "web"
            },
            "php_files": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/CommonBackdoors-PHP.fuzz.txt",
                "filename": "php-backdoors.txt",
                "description": "Backdoors PHP comuns",
                "category": "web"
            },
            
            # === DNS/SUBDOMAINS ===
            "subdomains_top1m": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/DNS/subdomains-top1million-5000.txt",
                "filename": "subdomains-top5k.txt",
                "description": "Top 5k subdomínios",
                "category": "dns"
            },
            "subdomains_bitquark": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/DNS/bitquark-subdomains-top100000.txt",
                "filename": "bitquark-subdomains-100k.txt",
                "description": "100k subdomínios BitQuark",
                "category": "dns"
            },
            "dns_all": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/DNS/dns-Jhaddix.txt",
                "filename": "dns-jhaddix.txt",
                "description": "DNS wordlist Jhaddix",
                "category": "dns"
            },
            
            # === DEFAULT CREDENTIALS ===
            "default_passwords": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Default-Credentials/default-passwords.csv",
                "filename": "default-passwords.csv",
                "description": "Senhas padrão equipamentos",
                "category": "default"
            },
            "router_defaults": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Default-Credentials/default-passwords.csv",
                "filename": "router-defaults.csv",
                "description": "Credenciais padrão roteadores",
                "category": "default"
            },
            
            # === FUZZING ===
            "big_list": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/big.txt",
                "filename": "big-fuzzing.txt",
                "description": "Big fuzzing wordlist",
                "category": "fuzzing"
            },
            "parameters": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/burp-parameter-names.txt",
                "filename": "burp-parameters.txt",
                "description": "Nomes de parâmetros web",
                "category": "fuzzing"
            },
            
            # === SPECIALIZED ===
            "api_endpoints": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/api/api-endpoints.txt",
                "filename": "api-endpoints.txt",
                "description": "Endpoints de API comuns",
                "category": "api"
            },
            "swagger_paths": {
                "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Discovery/Web-Content/swagger.txt",
                "filename": "swagger-paths.txt",
                "description": "Caminhos Swagger/OpenAPI",
                "category": "api"
            }
        }

    def download_file(self, url, filename, desc, category):
        filepath = self.download_dir / filename
        
        if filepath.exists():
            size_mb = filepath.stat().st_size / (1024 * 1024)
            print(f"[✓] {filename} já existe ({size_mb:.1f}MB)")
            return True
        
        print(f"[⬇] {category.upper()}: Baixando {desc}...")
        try:
            r = requests.get(url, stream=True, timeout=30)
            r.raise_for_status()
            
            total_size = int(r.headers.get('content-length', 0))
            downloaded = 0
            
            with open(filepath, 'wb') as f:
                for chunk in r.iter_content(chunk_size=8192):
                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)
                        
                        if total_size > 0:
                            percent = (downloaded * 100) // total_size
                            mb_downloaded = downloaded / (1024 * 1024)
                            mb_total = total_size / (1024 * 1024)
                            print(f"\r    {percent}% ({mb_downloaded:.1f}/{mb_total:.1f}MB)", end='')
            
            final_size = filepath.stat().st_size / (1024 * 1024)
            print(f"\n[✓] {filename} baixado! ({final_size:.1f}MB)")
            return True
            
        except Exception as e:
            print(f"\n[✗] Erro ao baixar {filename}: {e}")
            if filepath.exists():
                filepath.unlink()
            return False

    def list_by_category(self):
        """Lista wordlists por categoria"""
        categories = {}
        for key, info in self.wordlists.items():
            cat = info['category']
            if cat not in categories:
                categories[cat] = []
            categories[cat].append((key, info['description']))
        
        print("=== WORDLISTS DISPONÍVEIS POR CATEGORIA ===\n")
        for cat, items in categories.items():
            print(f"📁 {cat.upper()}:")
            for key, desc in items:
                print(f"   {key:20} - {desc}")
            print()

    def download_category(self, category):
        """Baixa todas as wordlists de uma categoria"""
        items = [k for k, v in self.wordlists.items() if v['category'] == category]
        
        if not items:
            print(f"Categoria '{category}' não encontrada!")
            return
        
        print(f"=== BAIXANDO CATEGORIA: {category.upper()} ===")
        successful = 0
        
        for key in items:
            info = self.wordlists[key]
            if self.download_file(info['url'], info['filename'], 
                                info['description'], info['category']):
                successful += 1
        
        print(f"\n[✓] {successful}/{len(items)} wordlists baixadas da categoria {category}")

    def download_specific(self, wordlist_keys):
        """Baixa wordlists específicas"""
        for key in wordlist_keys:
            if key not in self.wordlists:
                print(f"[✗] Wordlist '{key}' não encontrada!")
                continue
            
            info = self.wordlists[key]
            self.download_file(info['url'], info['filename'], 
                             info['description'], info['category'])

    def download_all(self):
        """Baixa todas as wordlists"""
        print("=== WORDLIST DOWNLOADER EXPANDIDO ===")
        print(f"Total de wordlists: {len(self.wordlists)}")
        print("=" * 50)
        
        successful = 0
        total = len(self.wordlists)
        
        for i, (key, info) in enumerate(self.wordlists.items(), 1):
            print(f"\n[{i}/{total}] ", end="")
            if self.download_file(info['url'], info['filename'], 
                                info['description'], info['category']):
                successful += 1
        
        print(f"\n{'='*50}")
        print(f"RESUMO: {successful}/{total} wordlists baixadas")
        print(f"Diretório: {self.download_dir.absolute()}")
        
        # Mostra estatísticas
        self.show_stats()

    def show_stats(self):
        """Mostra estatísticas dos arquivos baixados"""
        if not self.download_dir.exists():
            return
        
        files = list(self.download_dir.glob('*.txt')) + list(self.download_dir.glob('*.csv'))
        
        if not files:
            return
        
        total_size = sum(f.stat().st_size for f in files)
        total_mb = total_size / (1024 * 1024)
        
        print(f"\n📊 ESTATÍSTICAS:")
        print(f"   Arquivos: {len(files)}")
        print(f"   Tamanho total: {total_mb:.1f}MB")
        
        # Top 5 maiores arquivos
        largest = sorted(files, key=lambda f: f.stat().st_size, reverse=True)[:5]
        print(f"   Maiores arquivos:")
        for f in largest:
            size_mb = f.stat().st_size / (1024 * 1024)
            print(f"     {f.name}: {size_mb:.1f}MB")

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="Wordlist Downloader Expandido")
    parser.add_argument("-l", "--list", action="store_true", help="Lista por categoria")
    parser.add_argument("-c", "--category", help="Baixa categoria específica")
    parser.add_argument("-s", "--specific", nargs="+", help="Baixa wordlists específicas")
    parser.add_argument("-d", "--dir", default="wordlists", help="Diretório de download")
    
    args = parser.parse_args()
    
    downloader = ExpandedWordlistDownloader(args.dir)
    
    if args.list:
        downloader.list_by_category()
    elif args.category:
        downloader.download_category(args.category)
    elif args.specific:
        downloader.download_specific(args.specific)
    else:
        downloader.download_all()

if __name__ == "__main__":
    main()