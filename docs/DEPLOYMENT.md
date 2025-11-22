# Varlor Deployment Guide

## Overview

This guide covers deploying Varlor to production environments. Varlor is designed to run in various deployment scenarios: cloud, on-premise, or air-gapped environments.

**Prerequisites:**
- Ubuntu 22.04 LTS or similar Linux distribution
- Root or sudo access
- Domain name (for HTTPS)
- PostgreSQL 14+
- Node.js 18+
- nginx or similar reverse proxy

---

## Deployment Architecture

```
┌──────────────────────────────────────────────────────┐
│                    Internet                          │
└───────────────────┬──────────────────────────────────┘
                    │ HTTPS (443)
                    ▼
┌──────────────────────────────────────────────────────┐
│            Reverse Proxy (nginx)                     │
│  - SSL/TLS termination                               │
│  - Request routing                                   │
│  - Rate limiting                                     │
│  - Static file serving                               │
└───────┬──────────────────────┬───────────────────────┘
        │ HTTP (3001)          │ HTTP (3000)
        ▼                      ▼
┌───────────────────┐   ┌──────────────────────┐
│   Backend API     │   │  Frontend (Next.js)  │
│   (AdonisJS)      │   │  - Server Components │
│   - Port 3001     │   │  - Port 3000         │
│   - Node process  │   │  - Node process      │
└─────────┬─────────┘   └──────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────────┐
│              PostgreSQL Database                     │
│              - Port 5432 (internal)                  │
│              - SSL/TLS connections                   │
└──────────────────────────────────────────────────────┘
```

---

## Environment Setup

### 1. Server Preparation

**Update system:**
```bash
sudo apt update && sudo apt upgrade -y
```

**Install dependencies:**
```bash
# Node.js 18.x
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# PostgreSQL
sudo apt install -y postgresql postgresql-contrib

# nginx
sudo apt install -y nginx

# Process manager (PM2)
sudo npm install -g pm2

# Build tools
sudo apt install -y build-essential
```

**Create deployment user:**
```bash
sudo useradd -m -s /bin/bash varlor
sudo usermod -aG sudo varlor
```

---

### 2. Database Setup

**Configure PostgreSQL:**

```bash
# Switch to postgres user
sudo -u postgres psql

# Create database and user
CREATE USER varlor_prod WITH PASSWORD '<strong-password>';
CREATE DATABASE varlor_production OWNER varlor_prod;
GRANT ALL PRIVILEGES ON DATABASE varlor_production TO varlor_prod;

# Enable SSL (recommended)
\c varlor_production
ALTER DATABASE varlor_production SET ssl = on;

\q
```

**Configure PostgreSQL for remote connections (if needed):**

Edit `/etc/postgresql/14/main/postgresql.conf`:
```conf
listen_addresses = 'localhost'  # Or specific IP for remote DB
ssl = on
ssl_cert_file = '/etc/ssl/certs/server.crt'
ssl_key_file = '/etc/ssl/private/server.key'
```

Edit `/etc/postgresql/14/main/pg_hba.conf`:
```conf
# Allow local connections with SSL
hostssl    varlor_production    varlor_prod    127.0.0.1/32    scram-sha-256
```

**Restart PostgreSQL:**
```bash
sudo systemctl restart postgresql
```

---

### 3. SSL/TLS Certificates

**Option A: Let's Encrypt (Free, recommended for public deployments)**

```bash
# Install certbot
sudo apt install -y certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d app.yourdomain.com -d api.yourdomain.com

# Auto-renewal is configured automatically
```

**Option B: Self-Signed Certificate (Development/Internal)**

```bash
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/ssl/private/varlor.key \
  -out /etc/ssl/certs/varlor.crt \
  -subj "/CN=app.yourdomain.com"
```

**Option C: Corporate Certificate**

Copy your certificate files:
```bash
sudo cp certificate.crt /etc/ssl/certs/varlor.crt
sudo cp private.key /etc/ssl/private/varlor.key
sudo chmod 600 /etc/ssl/private/varlor.key
```

---

## Application Deployment

### 1. Clone Repository

```bash
# As varlor user
sudo su - varlor
cd /home/varlor

git clone https://github.com/yourusername/varlor.git
cd varlor
```

---

### 2. Backend Deployment

**Install dependencies:**
```bash
cd /home/varlor/varlor/server
npm ci --production
```

**Configure environment:**
```bash
cp .env.example .env.production
nano .env.production
```

**Production .env.production:**
```env
# =============================================================================
# PRODUCTION ENVIRONMENT CONFIGURATION
# =============================================================================

# -----------------------------------------------------------------------------
# Application Settings
# -----------------------------------------------------------------------------
TZ=UTC
PORT=3001
HOST=0.0.0.0
LOG_LEVEL=error
NODE_ENV=production

# App Key - CRITICAL: Generate with: node ace generate:key
APP_KEY=<generated-secure-key>

# -----------------------------------------------------------------------------
# Database Configuration (Production)
# -----------------------------------------------------------------------------
DB_HOST=127.0.0.1
DB_PORT=5432
DB_USER=varlor_prod
DB_PASSWORD=<strong-database-password>
DB_DATABASE=varlor_production

# Database SSL is automatically enabled when NODE_ENV=production

# -----------------------------------------------------------------------------
# CORS Configuration
# -----------------------------------------------------------------------------
# Frontend application URL - must match exactly
CORS_ORIGIN=https://app.yourdomain.com
CORS_CREDENTIALS=true

# -----------------------------------------------------------------------------
# Admin Seed Configuration
# -----------------------------------------------------------------------------
ADMIN_EMAIL=admin@yourdomain.com
ADMIN_PASSWORD=<very-strong-password-min-12-chars>

# -----------------------------------------------------------------------------
# Authentication Configuration
# -----------------------------------------------------------------------------
# Access Token Secret - CRITICAL: Generate with: node ace generate:key
ACCESS_TOKEN_SECRET=<generated-secure-secret>

ACCESS_TOKEN_EXPIRES_IN=15m
REFRESH_TOKEN_EXPIRES_IN=7d

# =============================================================================
# PRODUCTION SECURITY CHECKLIST
# =============================================================================
# ✓ NODE_ENV=production (enables Secure cookie flag)
# ✓ Strong APP_KEY generated
# ✓ Strong ACCESS_TOKEN_SECRET generated
# ✓ Strong database password
# ✓ Strong admin password
# ✓ CORS_ORIGIN set to production domain
# ✓ Database SSL enabled
# ✓ HTTPS configured (via nginx)
# =============================================================================
```

**Generate secrets:**
```bash
# Generate APP_KEY
node ace generate:key
# Copy output to APP_KEY in .env.production

# Generate ACCESS_TOKEN_SECRET
node ace generate:key
# Copy output to ACCESS_TOKEN_SECRET in .env.production
```

**Build application:**
```bash
npm run build
```

**Run migrations:**
```bash
NODE_ENV=production node ace migration:run --force
```

**Seed admin account:**
```bash
NODE_ENV=production npm run seed:admin
```

**Test backend:**
```bash
NODE_ENV=production npm start
# Should start on port 3001
# Press Ctrl+C to stop
```

---

### 3. Frontend Deployment

**Install dependencies:**
```bash
cd /home/varlor/varlor/client/web
npm ci --production
```

**Configure environment:**
```bash
cp .env.example .env.production
nano .env.production
```

**Production .env.production:**
```env
# Production API endpoint
NEXT_PUBLIC_API_URL=https://api.yourdomain.com

# Production frontend URL
NEXT_PUBLIC_APP_URL=https://app.yourdomain.com
```

**Build application:**
```bash
npm run build
```

**Test frontend:**
```bash
npm start
# Should start on port 3000
# Press Ctrl+C to stop
```

---

### 4. Process Management with PM2

**Create PM2 ecosystem file:**

`/home/varlor/varlor/ecosystem.config.js`:
```javascript
module.exports = {
  apps: [
    {
      name: 'varlor-backend',
      cwd: '/home/varlor/varlor/server',
      script: 'node',
      args: 'build/bin/server.js',
      instances: 2,
      exec_mode: 'cluster',
      env: {
        NODE_ENV: 'production',
        PORT: 3001,
      },
      error_file: '/home/varlor/logs/backend-error.log',
      out_file: '/home/varlor/logs/backend-out.log',
      time: true,
      max_memory_restart: '500M',
    },
    {
      name: 'varlor-frontend',
      cwd: '/home/varlor/varlor/client/web',
      script: 'node_modules/.bin/next',
      args: 'start',
      instances: 2,
      exec_mode: 'cluster',
      env: {
        NODE_ENV: 'production',
        PORT: 3000,
      },
      error_file: '/home/varlor/logs/frontend-error.log',
      out_file: '/home/varlor/logs/frontend-out.log',
      time: true,
      max_memory_restart: '500M',
    },
  ],
};
```

**Create logs directory:**
```bash
mkdir -p /home/varlor/logs
```

**Start applications:**
```bash
cd /home/varlor/varlor
pm2 start ecosystem.config.js

# Save PM2 configuration
pm2 save

# Setup PM2 to start on boot
pm2 startup
# Follow the command output instructions
```

**Monitor applications:**
```bash
pm2 status
pm2 logs
pm2 monit
```

---

### 5. nginx Configuration

**Create nginx config:**

`/etc/nginx/sites-available/varlor`:
```nginx
# Rate limiting zones
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/m;

# Upstream servers
upstream backend {
    least_conn;
    server 127.0.0.1:3001;
}

upstream frontend {
    least_conn;
    server 127.0.0.1:3000;
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name app.yourdomain.com api.yourdomain.com;

    return 301 https://$server_name$request_uri;
}

# Backend API
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name api.yourdomain.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logging
    access_log /var/log/nginx/varlor-api-access.log;
    error_log /var/log/nginx/varlor-api-error.log;

    # Rate limiting on login endpoint
    location /auth/login {
        limit_req zone=login_limit burst=2 nodelay;
        limit_req_status 429;

        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # All other API endpoints
    location / {
        limit_req zone=api_limit burst=20 nodelay;

        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}

# Frontend Application
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name app.yourdomain.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/app.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logging
    access_log /var/log/nginx/varlor-app-access.log;
    error_log /var/log/nginx/varlor-app-error.log;

    # Proxy to Next.js
    location / {
        proxy_pass http://frontend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;

        # Timeouts for Next.js
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Static files caching
    location /_next/static {
        proxy_pass http://frontend;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }
}
```

**Enable site:**
```bash
sudo ln -s /etc/nginx/sites-available/varlor /etc/nginx/sites-enabled/
sudo nginx -t  # Test configuration
sudo systemctl reload nginx
```

---

## Security Hardening

### 1. Firewall Configuration

```bash
# UFW (Uncomplicated Firewall)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# Optional: Restrict SSH to specific IPs
sudo ufw delete allow 22/tcp
sudo ufw allow from YOUR_IP_ADDRESS to any port 22
```

### 2. Fail2Ban for SSH Protection

```bash
sudo apt install -y fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

### 3. Automatic Security Updates

```bash
sudo apt install -y unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

### 4. Application Security

**Verify environment variables:**
```bash
# Check secrets are strong
grep -E 'APP_KEY|ACCESS_TOKEN_SECRET|ADMIN_PASSWORD' /home/varlor/varlor/server/.env.production

# Ensure file permissions
sudo chmod 600 /home/varlor/varlor/server/.env.production
sudo chown varlor:varlor /home/varlor/varlor/server/.env.production
```

**Database security:**
```sql
-- Ensure strong password
ALTER USER varlor_prod WITH PASSWORD '<new-strong-password>';

-- Revoke unnecessary permissions
REVOKE ALL ON DATABASE postgres FROM varlor_prod;
```

---

## Monitoring and Logging

### 1. Application Logs

**PM2 logs:**
```bash
# View logs
pm2 logs varlor-backend
pm2 logs varlor-frontend

# Log rotation
pm2 install pm2-logrotate
pm2 set pm2-logrotate:max_size 10M
pm2 set pm2-logrotate:retain 7
```

### 2. nginx Logs

**View logs:**
```bash
sudo tail -f /var/log/nginx/varlor-api-access.log
sudo tail -f /var/log/nginx/varlor-api-error.log
```

**Log rotation (automatic with logrotate):**
```bash
# Configuration in /etc/logrotate.d/nginx
/var/log/nginx/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 www-data adm
    sharedscripts
    postrotate
        [ -f /var/run/nginx.pid ] && kill -USR1 `cat /var/run/nginx.pid`
    endscript
}
```

### 3. Database Monitoring

**PostgreSQL log location:**
```bash
/var/log/postgresql/postgresql-14-main.log
```

**Monitor connections:**
```sql
SELECT count(*) FROM pg_stat_activity WHERE datname = 'varlor_production';
```

---

## Backup Strategy

### 1. Database Backups

**Automated backup script:**

`/home/varlor/scripts/backup-database.sh`:
```bash
#!/bin/bash

# Configuration
DB_NAME="varlor_production"
DB_USER="varlor_prod"
BACKUP_DIR="/home/varlor/backups/database"
RETENTION_DAYS=30

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup filename with timestamp
BACKUP_FILE="$BACKUP_DIR/varlor_db_$(date +%Y%m%d_%H%M%S).sql.gz"

# Perform backup
pg_dump -U $DB_USER -d $DB_NAME | gzip > $BACKUP_FILE

# Delete old backups
find $BACKUP_DIR -name "varlor_db_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# Verify backup
if [ -f "$BACKUP_FILE" ]; then
    echo "Backup completed: $BACKUP_FILE"
else
    echo "Backup failed!" >&2
    exit 1
fi
```

**Make executable and schedule:**
```bash
chmod +x /home/varlor/scripts/backup-database.sh

# Add to crontab (daily at 2 AM)
crontab -e
0 2 * * * /home/varlor/scripts/backup-database.sh >> /home/varlor/logs/backup.log 2>&1
```

### 2. Application Backups

```bash
# Backup configuration and code
tar -czf /home/varlor/backups/app_$(date +%Y%m%d).tar.gz \
    /home/varlor/varlor \
    --exclude=node_modules \
    --exclude=.next \
    --exclude=build

# Copy to remote backup server
scp /home/varlor/backups/app_*.tar.gz backup@remote-server:/backups/varlor/
```

---

## Updates and Maintenance

### 1. Application Updates

**Update procedure:**
```bash
# As varlor user
cd /home/varlor/varlor

# Pull latest code
git pull origin main

# Backend update
cd server
npm ci --production
npm run build
NODE_ENV=production node ace migration:run --force

# Frontend update
cd ../client/web
npm ci --production
npm run build

# Restart applications
pm2 restart ecosystem.config.js
```

### 2. Zero-Downtime Updates

**Using PM2 reload:**
```bash
pm2 reload ecosystem.config.js
# Gracefully reloads without downtime
```

### 3. Database Migrations

**Production migration checklist:**
- [ ] Test migration in staging environment
- [ ] Backup database before migration
- [ ] Review migration SQL for destructive operations
- [ ] Schedule maintenance window if needed
- [ ] Monitor application after migration

```bash
# Backup first!
/home/varlor/scripts/backup-database.sh

# Run migration
NODE_ENV=production node ace migration:run --force

# Verify
NODE_ENV=production node ace migration:status
```

---

## Troubleshooting

### Application Won't Start

**Check logs:**
```bash
pm2 logs varlor-backend --lines 100
pm2 logs varlor-frontend --lines 100
```

**Common issues:**
- Environment variables not set correctly
- Database connection fails
- Port already in use
- Build artifacts missing

**Verify configuration:**
```bash
# Check .env exists
ls -la /home/varlor/varlor/server/.env.production

# Check Node version
node --version  # Should be 18+

# Check database connection
psql -U varlor_prod -d varlor_production -c "SELECT 1;"
```

### HTTPS/SSL Issues

**Test SSL certificate:**
```bash
sudo certbot certificates
sudo nginx -t
```

**Renew certificate manually:**
```bash
sudo certbot renew
sudo systemctl reload nginx
```

### Database Connection Issues

**Check PostgreSQL status:**
```bash
sudo systemctl status postgresql

# Check connections
sudo -u postgres psql -c "SELECT count(*) FROM pg_stat_activity;"
```

**Test connection:**
```bash
psql -U varlor_prod -h localhost -d varlor_production
```

### Performance Issues

**Check resource usage:**
```bash
pm2 monit
htop
free -h
df -h
```

**Optimize database:**
```sql
VACUUM ANALYZE;
REINDEX DATABASE varlor_production;
```

---

## Production Checklist

### Pre-Deployment

- [ ] Strong passwords generated for all services
- [ ] SSL/TLS certificates obtained and configured
- [ ] Firewall configured (only 22, 80, 443 open)
- [ ] Database user and database created
- [ ] Environment variables configured
- [ ] Application built successfully
- [ ] Migrations run successfully
- [ ] Admin account seeded

### Post-Deployment

- [ ] Application accessible via HTTPS
- [ ] Login functionality working
- [ ] API endpoints responding correctly
- [ ] Database connections working
- [ ] Logs being written correctly
- [ ] PM2 processes running
- [ ] nginx serving requests
- [ ] Backups configured and tested
- [ ] Monitoring setup (optional)
- [ ] Security headers verified

### Security Verification

- [ ] `NODE_ENV=production` set
- [ ] Secure cookie flag enabled (verify in browser DevTools)
- [ ] CORS restricted to production domain
- [ ] Database SSL enabled
- [ ] No secrets in git repository
- [ ] File permissions correct (600 for .env files)
- [ ] Fail2Ban active
- [ ] Automatic security updates enabled

---

## Alternative Deployment: Docker

**Dockerfile (Backend):**
```dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --production

COPY . .
RUN npm run build

EXPOSE 3001

CMD ["node", "build/bin/server.js"]
```

**Dockerfile (Frontend):**
```dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --production

COPY . .
RUN npm run build

EXPOSE 3000

CMD ["npm", "start"]
```

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  db:
    image: postgres:14
    environment:
      POSTGRES_DB: varlor_production
      POSTGRES_USER: varlor_prod
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - varlor-network

  backend:
    build: ./server
    ports:
      - "3001:3001"
    environment:
      NODE_ENV: production
      DB_HOST: db
    depends_on:
      - db
    networks:
      - varlor-network

  frontend:
    build: ./client/web
    ports:
      - "3000:3000"
    environment:
      NODE_ENV: production
      NEXT_PUBLIC_API_URL: https://api.yourdomain.com
    networks:
      - varlor-network

volumes:
  postgres_data:

networks:
  varlor-network:
```

---

## Support

For deployment issues:
- Review logs: `pm2 logs`, `/var/log/nginx/`
- Check system resources: `htop`, `df -h`
- Verify configuration: `.env.production`, nginx config
- Test individual components: database, backend, frontend
- Consult documentation: This guide, API docs, database docs

---

## References

- [PM2 Documentation](https://pm2.keymetrics.io/docs/)
- [nginx Documentation](https://nginx.org/en/docs/)
- [Let's Encrypt](https://letsencrypt.org/)
- [PostgreSQL Security](https://www.postgresql.org/docs/14/security.html)
- [Node.js Production Best Practices](https://nodejs.org/en/docs/guides/nodejs-docker-webapp/)
