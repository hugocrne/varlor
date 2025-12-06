# Varlor Troubleshooting Guide

This guide helps diagnose and resolve common issues with Varlor MVP.

## Table of Contents

1. [Installation & Setup Issues](#installation--setup-issues)
2. [Database Problems](#database-problems)
3. [Authentication Issues](#authentication-issues)
4. [File Upload Problems](#file-upload-problems)
5. [Analysis & Report Issues](#analysis--report-issues)
6. [Performance Issues](#performance-issues)
7. [Frontend Issues](#frontend-issues)
8. [Backend Issues](#backend-issues)
9. [Production Issues](#production-issues)
10. [Getting Additional Help](#getting-additional-help)

---

## Installation & Setup Issues

### Node.js Version Errors

**Problem:** "Node version too old" or "Node version not supported"

**Solution:**
```bash
# Check current version
node --version

# Install correct version (18.x or higher)
# Using nvm (recommended)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
nvm use 18
```

### Port Already in Use

**Problem:** "Port 3000/3001 already in use"

**Solution:**
```bash
# Find process using port
# On macOS/Linux
lsof -i :3000
lsof -i :3001

# On Windows
netstat -ano | findstr :3000

# Kill process
kill -9 <PID>  # macOS/Linux
taskkill /PID <PID> /F  # Windows
```

### npm Install Fails

**Problem:** npm install throws errors

**Solution:**
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Try again
npm install

# If still fails, use different registry
npm install --registry https://registry.npmjs.org/
```

---

## Database Problems

### Connection Refused

**Problem:** "ECONNREFUSED" when connecting to PostgreSQL

**Checklist:**
1. PostgreSQL is running:
   ```bash
   pg_isready
   ```

2. Check PostgreSQL status:
   ```bash
   # macOS (with Homebrew)
   brew services list

   # Linux (systemd)
   sudo systemctl status postgresql
   ```

3. Verify database exists:
   ```bash
   psql -U postgres -c "\l"
   ```

### Migration Failures

**Problem:** Migrations fail to run

**Solutions:**

1. Check migration status:
   ```bash
   cd server
   node ace migration:status
   ```

2. Run specific migration:
   ```bash
   node ace migration:run --batch=1
   ```

3. Reset migrations (development only):
   ```bash
   node ace migration:refresh
   ```

### Permission Denied

**Problem:** Database permission errors

**Solution:**
```sql
-- Connect as postgres user
psql -U postgres

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE varlor_dev TO varlor_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO varlor_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO varlor_user;
```

---

## Authentication Issues

### Login Fails with Valid Credentials

**Problem:** Correct email/password but login fails

**Checks:**
1. Verify admin user exists:
   ```bash
   cd server
   npm run seed:admin
   ```

2. Check user in database:
   ```sql
   SELECT id, email FROM users WHERE email = 'admin@varlor.com';
   ```

3. Verify password hash:
   ```bash
   # Reset admin password
   node ace make:command ResetAdminPassword
   ```

### Token Expired Errors

**Problem:** "Invalid or expired token" frequently

**Solution:**
1. Check token expiration in `.env`:
   ```env
   ACCESS_TOKEN_EXPIRES_IN=15m
   REFRESH_TOKEN_EXPIRES_IN=7d
   ```

2. Verify refresh token is being sent:
   ```javascript
   // In browser dev tools
   console.log(document.cookie)
   ```

3. Check system time is correct

### CORS Errors

**Problem:** "CORS policy" errors in browser

**Solution:**
1. Verify CORS origin in `server/.env`:
   ```env
   CORS_ORIGIN=http://localhost:3000
   ```

2. Check API URL in frontend:
   ```env
   # client/web/.env.local
   NEXT_PUBLIC_API_URL=http://localhost:3001
   ```

---

## File Upload Problems

### File Too Large

**Problem:** Upload fails for large files

**Check:**
1. Check file size limit:
   - Default MVP limit: 100MB

2. Increase limit if needed:
   ```javascript
   // server/config/bodyparser.ts
   export const bodyParserConfig: BodyParserConfig = {
     sizeLimit: '100mb'
   }
   ```

3. Check Nginx limit (if using):
   ```nginx
   client_max_body_size 100M;
   ```

### Unsupported File Type

**Problem:** "File type not supported" error

**Solution:**
1. Check supported formats:
   - CSV (.csv)
   - Excel (.xlsx, .xls)

2. Convert file to supported format:
   - Excel → CSV: Use "Save As" in Excel
   - Other formats → CSV: Export from source application

### Upload Stuck/Parsing Takes Too Long

**Problem:** File uploads but parsing never completes

**Debug Steps:**
1. Check file size (very large files take time)
2. Verify file isn't corrupted
3. Check backend logs:
   ```bash
   cd server
   tail -f logs/app.log
   ```

4. Check if parsing job queue is running:
   ```bash
   # Check Redis connection
   redis-cli ping
   ```

---

## Analysis & Report Issues

### Analysis Never Completes

**Problem:** Analysis stuck at "Analyzing..."

**Checks:**
1. Verify dataset status is "parsed":
   ```bash
   # Check in database
   SELECT status FROM datasets WHERE id = 'your-dataset-id';
   ```

2. Check worker processes:
   ```bash
   # Check if analysis job is running
   ps aux | grep node
   ```

3. Review error logs:
   ```bash
   cd server
   tail -f logs/worker.log
   ```

### Report Generation Fails

**Problem:** PDF report won't generate

**Solutions:**
1. Check Puppeteer is installed correctly:
   ```bash
   cd server
   npm list puppeteer
   ```

2. Verify system dependencies:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install -y libnss3-dev libatk-bridge2.0-dev

   # macOS
   # Puppeteer should work out of the box
   ```

3. Check report job logs:
   ```bash
   cd server
   grep "Report" logs/app.log | tail -20
   ```

### Visualizations Not Showing

**Problem:** Charts/graphs don't display

**Debug:**
1. Check browser console for JavaScript errors
2. Verify data has numeric columns for charts
3. Check if analysis completed successfully:
   ```javascript
   // In API response
   GET /datasets/:id/analyze/results
   ```

---

## Performance Issues

### Slow File Upload

**Problem:** Uploads are very slow

**Optimizations:**
1. Check file size before upload
2. Use compressed CSV files
3. Check network bandwidth
4. Verify disk space on server

### Slow Page Loads

**Problem:** Frontend pages load slowly

**Check:**
1. Check bundle size:
   ```bash
   cd client/web
   npm run analyze
   ```

2. Enable lazy loading for large components
3. Check network tab in dev tools
4. Clear browser cache

### Database Slowness

**Problem:** API responses are slow

**Debug:**
1. Check slow queries:
   ```sql
   SELECT query, mean_time, calls
   FROM pg_stat_statements
   ORDER BY mean_time DESC
   LIMIT 10;
   ```

2. Add missing indexes:
   ```sql
   -- Example: Index on dataset tenant_id
   CREATE INDEX idx_datasets_tenant ON datasets(tenant_id);
   ```

3. Check connection pool size

---

## Frontend Issues

### White Screen/Blank Page

**Problem:** Page loads but shows blank screen

**Fix:**
1. Check browser console for errors
2. Clear browser data
3. Try incognito mode
4. Check if Next.js dev server is running

### API Calls Failing

**Problem**: "Network Error" when calling API

**Check:**
1. Backend server is running (port 3001)
2. Correct API URL in `.env.local`
3. No CORS issues
4. Check API response in Network tab

### State Management Issues

**Problem**: Data not updating after actions

**Debug:**
1. Check React DevTools state
2. Verify API calls are being made
3. Check for console errors
4. Review useEffect dependencies

---

## Backend Issues

### Server Won't Start

**Problem**: Backend fails to start

**Check:**
1. All dependencies installed:
   ```bash
   cd server
   npm install
   ```

2. Correct Node.js version
3. Valid `.env` configuration
4. Database connection

### Memory Leaks

**Problem**: Memory usage increases continuously

**Monitor:**
```bash
# Check Node process memory
ps aux | grep node

# Node inspector for memory
node --inspect bin/server.js
```

**Fix:**
1. Review file handling code
2. Close database connections
3. Clear caches periodically
4. Check for event listener leaks

### Job Queue Issues

**Problem**: Background jobs not processing

**Debug:**
1. Check Redis connection
2. Verify worker is running
3. Check job queue:
   ```bash
   redis-cli
   > KEYS bull:*
   > LLEN bull:default:waiting
   ```

---

## Production Issues

### SSL Certificate Errors

**Problem**: HTTPS/SSL not working

**Check:**
1. Certificate not expired:
   ```bash
   sudo certbot certificates
   ```

2. Nginx configuration correct
3. DNS points to correct IP
4. Port 443 open

### High CPU Usage

**Problem**: Server CPU at 100%

**Debug:**
```bash
# Check process usage
top -p $(pgrep node)

# Profile Node.js
node --prof bin/server.js
```

### Database Connection Pool Exhausted

**Problem**: "Too many connections" error

**Fix:**
1. Increase pool size in config
2. Check for connection leaks
3. Optimize query performance
4. Use connection pooling properly

---

## Getting Additional Help

### Log Locations

**Development:**
- Backend: `server/logs/app.log`
- Frontend: Browser dev tools console
- Database: PostgreSQL logs

**Production:**
- System logs: `/var/log/syslog`
- Nginx logs: `/var/log/nginx/`
- Application logs: Configured location

### Debug Mode

**Enable debug logging:**
```env
# server/.env
LOG_LEVEL=debug
DEBUG=adonis:*,-adonis:typescript
```

### Useful Commands

```bash
# Check all services
sudo systemctl status nginx postgresql redis

# Check disk space
df -h

# Check memory
free -h

# Check network
netstat -tulpn
```

### When to Ask for Help

1. **Critical Production Issues**
   - Service completely down
   - Data corruption
   - Security breach

2. **Persistent Development Issues**
   - Same error after multiple attempts
   - Unclear error messages
   - Documentation gaps

3. **Performance Problems**
   - Unexplained slowness
   - Memory leaks
   - Scaling issues

### How to Report Issues

Include in your report:

1. **Environment:**
   - OS version
   - Node.js version
   - Browser version

2. **Steps to Reproduce:**
   - What you did
   - Expected behavior
   - Actual behavior

3. **Error Messages:**
   - Full error text
   - Stack traces
   - Logs

4. **What You Tried:**
   - Solutions attempted
   - Research done

---

## Emergency Procedures

### Database Corruption

```bash
# Immediate backup
pg_dump varlor_prod > emergency_backup.sql

# Check integrity
pg_dump varlor_prod | psql varlor_test

# Restore from backup if needed
psql varlor_prod < backup.sql
```

### Security Incident

1. Change all secrets/tokens
2. Review access logs
3. Check for data exfiltration
4. Report to security team

### Service Down

```bash
# Quick restart
sudo systemctl restart varlor-backend varlor-frontend

# Check dependencies
sudo systemctl status postgresql redis nginx

# Last resort: Reboot
sudo reboot
```

---

Remember: Most issues have solutions. Don't panic, work systematically, and don't hesitate to ask for help when needed!