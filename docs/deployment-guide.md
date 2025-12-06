# Guide de Déploiement Varlor MVP

Ce guide couvre le déploiement de Varlor MVP en environnement de production.

## Table des Matières

1. [Prérequis](#prérequis)
2. [Architecture de Déploiement](#architecture-de-déploiement)
3. [Configuration de l'Environnement](#configuration-de-lenvironnement)
4. [Base de Données](#base-de-données)
5. [Backend](#backend)
6. [Frontend](#frontend)
7. [Configuration HTTPS/SSL](#configuration-https-ssl)
8. [Stockage de Fichiers](#stockage-de-fichiers)
9. [Monitoring et Logging](#monitoring-et-logging)
10. [Sécurité](#sécurité)
11. [Docker (Optionnel)](#docker-optionnel)
12. [Dépannage](#dépannage)

---

## Prérequis

### Infrastructure Minimale

#### Configuration Matérielle Recommandée
- **CPU** : 4 coeurs minimum
- **RAM** : 8 GB minimum
- **Stockage** : 100 GB SSD
- **Réseau** : Bande passante 100 Mbps

#### Logiciels Requis
- **Node.js** 18.x ou supérieur
- **PostgreSQL** 14.x ou supérieur
- **Redis** 6.x ou supérieur
- **Nginx** (reverse proxy)
- **Certbot** (certificats SSL)

### Système d'Exploitation
- **Recommandé** : Ubuntu 20.04 LTS ou supérieur
- **Supporté** : CentOS 8+, RHEL 8+, Debian 11+

---

## Architecture de Déploiement

### Schéma de Base

```
Internet
    |
[Nginx:443/80] - Reverse Proxy + SSL
    |
[Frontend:3000] - Next.js (Production)
    |
[Backend:3001] - AdonisJS API
    |
[PostgreSQL:5432] - Base de données
    |
[Redis:6379] - Cache & Sessions
    |
[Storage] - Fichiers uploadés
```

### Réseau et Ports
- **80** : HTTP (redirigé vers HTTPS)
- **443** : HTTPS (Nginx)
- **3000** : Frontend (interne)
- **3001** : Backend (interne)
- **5432** : PostgreSQL (interne)
- **6379** : Redis (interne)

---

## Configuration de l'Environnement

### 1. Créer l'utilisateur de déploiement

```bash
# Créer un utilisateur dédié
sudo adduser varlor
sudo usermod -aG sudo varlor

# Se connecter en varlor
su - varlor
```

### 2. Installer Node.js

```bash
# Installer NodeSource repository
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Vérifier l'installation
node --version  # doit être 18.x ou supérieur
npm --version
```

### 3. Cloner le dépôt

```bash
# Cloner depuis votre repository
git clone https://github.com/varlor/varlor.git
cd varlor
```

---

## Base de Données

### Installation PostgreSQL

```bash
# Installer PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib

# Démarrer et activer le service
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Configuration

```bash
# Se connecter à PostgreSQL
sudo -u postgres psql

# Créer la base et l'utilisateur
CREATE DATABASE varlor_prod;
CREATE USER varlor_user WITH PASSWORD 'VOTRE_MOT_DE_PASSE_SECURISE';
GRANT ALL PRIVILEGES ON DATABASE varlor_prod TO varlor_user;
\q

# Configurer PostgreSQL
sudo nano /etc/postgresql/14/main/postgresql.conf
# Modifier :
listen_addresses = 'localhost'
max_connections = 100
shared_buffers = 256MB

# Redémarrer PostgreSQL
sudo systemctl restart postgresql
```

### Installation Redis

```bash
# Installer Redis
sudo apt install redis-server

# Configurer Redis
sudo nano /etc/redis/redis.conf
# Modifier :
bind 127.0.0.1
requirepass VOTRE_MOT_DE_PASSE_REDIS
maxmemory 512mb
maxmemory-policy allkeys-lru

# Démarrer et activer Redis
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

---

## Backend

### 1. Configuration

```bash
cd server

# Installer les dépendances
npm ci --production

# Copier la configuration
cp .env.example .env
```

### 2. Variables d'Environnement

Éditez `.env` :

```env
# Application
NODE_ENV=production
PORT=3001
HOST=0.0.0.0
APP_KEY=VOTRE_APP_KEY_GENERE

# Base de données
DB_HOST=127.0.0.1
DB_PORT=5432
DB_USER=varlor_user
DB_PASSWORD=VOTRE_MOT_DE_PASSE_DB
DB_DATABASE=varlor_prod

# Redis
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=VOTRE_MOT_DE_PASSE_REDIS

# CORS
CORS_ORIGIN=https://votredomaine.com

# Authentification
ACCESS_TOKEN_SECRET=VOTRE_SECRET_TOKEN_GENERE
ACCESS_TOKEN_EXPIRES_IN=15m
REFRESH_TOKEN_EXPIRES_IN=7d

# Stockage (local pour MVP)
DRIVER=disk

# Admin
ADMIN_EMAIL=admin@votredomaine.com
ADMIN_PASSWORD=VOTRE_MOT_DE_PASSE_ADMIN
```

### 3. Générer les clés

```bash
# Générer APP_KEY
node ace generate:key

# Copier la valeur dans .env
# Générer ACCESS_TOKEN_SECRET
node ace generate:key
# Copier la valeur dans ACCESS_TOKEN_SECRET
```

### 4. Migrations et Seed

```bash
# Exécuter les migrations
node ace migration:run --env=production

# Créer l'admin
npm run seed:admin
```

### 5. Build Production

```bash
# Compiler TypeScript
npm run build

# Vérifier
node build/server.js --version
```

### 6. Service Systemd

Créer `/etc/systemd/system/varlor-backend.service` :

```ini
[Unit]
Description=Varlor Backend API
After=network.target postgresql.service redis.service

[Service]
Type=simple
User=varlor
WorkingDirectory=/home/varlor/varlor/server
ExecStart=/usr/bin/node build/server.js
Restart=always
RestartSec=10
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
```

Activer le service :

```bash
sudo systemctl daemon-reload
sudo systemctl enable varlor-backend
sudo systemctl start varlor-backend
```

Vérifier :

```bash
sudo systemctl status varlor-backend
curl http://localhost:3001/health
```

---

## Frontend

### 1. Configuration

```bash
cd ../client/web

# Installer les dépendances
npm ci --production
```

### 2. Variables d'Environnement

Créer `.env.production` :

```env
NEXT_PUBLIC_API_URL=https://votredomaine.com/api
NEXT_PUBLIC_APP_URL=https://votredomaine.com
```

### 3. Build Production

```bash
npm run build
npm run start
# Tester que l'application fonctionne sur http://localhost:3000
# Arrêter avec Ctrl+C
```

### 4. Service Systemd

Créer `/etc/systemd/system/varlor-frontend.service` :

```ini
[Unit]
Description=Varlor Frontend App
After=network.target

[Service]
Type=simple
User=varlor
WorkingDirectory=/home/varlor/varlor/client/web
ExecStart=/usr/bin/npm start
Restart=always
RestartSec=10
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
```

Activer le service :

```bash
sudo systemctl daemon-reload
sudo systemctl enable varlor-frontend
sudo systemctl start varlor-frontend
```

---

## Configuration HTTPS/SSL

### 1. Installer Nginx

```bash
sudo apt update
sudo apt install nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 2. Obtenir un Certificat SSL

```bash
# Installer Certbot
sudo apt install certbot python3-certbot-nginx

# Obtenir le certificat
sudo certbot --nginx -d votredomaine.com -d www.votredomaine.com
```

### 3. Configurer Nginx

Créer `/etc/nginx/sites-available/varlor` :

```nginx
server {
    listen 80;
    server_name votredomaine.com www.votredomaine.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name votredomaine.com www.votredomaine.com;

    # SSL (géré par Certbot)
    ssl_certificate /etc/letsencrypt/live/votredomaine.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/votredomaine.com/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # Sécurité
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Frontend
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # Backend API
    location /api/ {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # Taille max pour l'upload
    client_max_body_size 100M;
}
```

Activer le site :

```bash
sudo ln -s /etc/nginx/sites-available/varlor /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

---

## Stockage de Fichiers

### Configuration Local (MVP)

Par défaut, les fichiers sont stockés localement dans `/tmp/uploads`.

```bash
# Créer le répertoire
sudo mkdir -p /var/lib/varlor/uploads
sudo chown varlor:varlor /var/lib/varlor/uploads
sudo chmod 750 /var/lib/varlor/uploads

# Configurer le service pour sauvegarder
sudo crontab -e
# Ajouter :
0 2 * * * rsync -av /var/lib/varlor/uploads/ /backup/varlor/uploads/
```

### Configuration S3 (Production)

Pour une production robuste, configurez S3 :

Dans `.env` du backend :

```env
# Stockage S3
DRIVER=s3
S3_KEY=VOTRE_CLE_S3
S3_SECRET=VOTRE_SECRET_S3
S3_BUCKET=varlor-prod
S3_REGION=eu-west-3
S3_ENDPOINT=https://s3.eu-west-3.amazonaws.com
```

---

## Monitoring et Logging

### 1. Logs des Services

```bash
# Voir les logs backend
sudo journalctl -u varlor-backend -f

# Voir les logs frontend
sudo journalctl -u varlor-frontend -f

# Logs Nginx
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### 2. Rotation des Logs

Créer `/etc/logrotate.d/varlor` :

```
/home/varlor/varlor/server/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    copytruncate
}
```

### 3. Monitoring de base

```bash
# Script de monitoring
sudo nano /usr/local/bin/check-varlor.sh
```

```bash
#!/bin/bash
# Vérifier si les services tournent
if ! systemctl is-active --quiet varlor-backend; then
    echo "Backend arrêté!" | mail -s "Alerte Varlor" admin@votredomaine.com
fi

if ! systemctl is-active --quiet varlor-frontend; then
    echo "Frontend arrêté!" | mail -s "Alerte Varlor" admin@votredomaine.com
fi

# Vérifier l'espace disque
USAGE=$(df /var/lib/varlor/uploads | tail -1 | awk '{print $5}' | sed 's/%//')
if [ $USAGE -gt 80 ]; then
    echo "Espace disque à ${USAGE}%" | mail -s "Alerte Varlor" admin@votredomaine.com
fi
```

Rendre exécutable et planifier :

```bash
sudo chmod +x /usr/local/bin/check-varlor.sh
sudo crontab -e
# Ajouter :
*/5 * * * * /usr/local/bin/check-varlor.sh
```

---

## Sécurité

### 1. Pare-feu

```bash
# Configurer UFW
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

### 2. Mises à Jour Automatiques

```bash
# Installer les mises à jour de sécurité
sudo apt install unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

### 3. Backup de la Base

Créer `/usr/local/bin/backup-varlor.sh` :

```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/varlor"
mkdir -p $BACKUP_DIR

# Backup PostgreSQL
pg_dump -h localhost -U varlor_user varlor_prod > $BACKUP_DIR/db_$DATE.sql
gzip $BACKUP_DIR/db_$DATE.sql

# Supprimer les anciens backups (+7 jours)
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +7 -delete
```

Planifier :

```bash
sudo crontab -e
# Ajouter :
0 3 * * * /usr/local/bin/backup-varlor.sh
```

---

## Docker (Optionnel)

### docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: varlor_prod
      POSTGRES_USER: varlor_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - varlor-net

  redis:
    image: redis:6-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    networks:
      - varlor-net

  backend:
    build: ./server
    environment:
      NODE_ENV: production
      DB_HOST: postgres
      REDIS_HOST: redis
    depends_on:
      - postgres
      - redis
    networks:
      - varlor-net

  frontend:
    build: ./client/web
    ports:
      - "3000:3000"
    depends_on:
      - backend
    networks:
      - varlor-net

volumes:
  postgres_data:

networks:
  varlor-net:
    driver: bridge
```

---

## Dépannage

### Problèmes Communs

#### 1. Le backend ne démarre pas
```bash
# Vérifier les logs
sudo journalctl -u varlor-backend -n 50

# Vérifier la configuration
cd /home/varlor/varlor/server
node ace test
```

#### 2. Erreur de connexion à la base
```bash
# Vérifier PostgreSQL
sudo -u postgres psql -c "\l"

# Tester la connexion
psql -h localhost -U varlor_user -d varlor_prod
```

#### 3. Upload ne fonctionne pas
```bash
# Vérifier les permissions
ls -la /var/lib/varlor/uploads/
sudo chown -R varlor:varlor /var/lib/varlor/uploads/
```

#### 4. Erreur 502 Bad Gateway
```bash
# Vérifier Nginx
sudo nginx -t
sudo systemctl reload nginx

# Vérifier si les backend tournent
curl http://localhost:3001/health
curl http://localhost:3000
```

#### 5. Certificat SSL expiré
```bash
# Renouveler
sudo certbot renew
# Renouvellement automatique déjà configuré
sudo systemctl status certbot.timer
```

### Tests Post-Déploiement

1. **Test de base** : Accédez à https://votredomaine.com
2. **Login** : Connectez-vous avec le compte admin
3. **Upload** : Testez l'import d'un petit fichier CSV
4. **Rapport** : Vérifiez la génération de PDF
5. **HTTPS** : Vérifiez que tout est en HTTPS

---

## Maintenance

### Tâches Mensuelles

1. **Mettre à jour les dépendances**
   ```bash
   cd /home/varlor/varlor
   git pull origin main
   cd server && npm ci
   cd ../client/web && npm ci
   sudo systemctl restart varlor-backend varlor-frontend
   ```

2. **Vérifier l'espace disque**
   ```bash
   df -h
   du -sh /var/lib/varlor/uploads/
   ```

3. **Revue des logs**
   ```bash
   sudo journalctl -u varlor-backend --since "1 month ago" | grep ERROR
   ```

---

*Ce guide couvre le déploiement MVP. Pour les configurations multi-tenants ou avancées, consultez la documentation V1 Enterprise.*