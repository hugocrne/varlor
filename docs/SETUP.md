# Varlor Developer Setup Guide

## Welcome!

This guide will help you set up your local development environment for Varlor. Follow these steps in order for a smooth onboarding experience.

---

## Prerequisites

### Required Software

Before you begin, ensure you have the following installed:

| Software | Version | Installation |
|----------|---------|--------------|
| **Node.js** | 18.x or higher | [nodejs.org](https://nodejs.org/) |
| **npm** | 9.x or higher | Comes with Node.js |
| **PostgreSQL** | 14.x or higher | [postgresql.org](https://www.postgresql.org/download/) |
| **Git** | Latest | [git-scm.com](https://git-scm.com/) |

**Verify installations:**

```bash
node --version   # Should be v18.x or higher
npm --version    # Should be 9.x or higher
psql --version   # Should be 14.x or higher
git --version    # Should be recent
```

### Recommended Software

| Tool | Purpose | Installation |
|------|---------|--------------|
| **VS Code** | Code editor | [code.visualstudio.com](https://code.visualstudio.com/) |
| **Postman** | API testing | [postman.com](https://www.postman.com/) |
| **pgAdmin** or **TablePlus** | Database management | [pgadmin.org](https://www.pgadmin.org/), [tableplus.com](https://tableplus.com/) |

---

## Step 1: Clone the Repository

```bash
# Clone the repository
git clone https://github.com/yourusername/varlor.git
cd varlor

# Verify directory structure
ls -la
# You should see: client/, server/, docs/, README.md, etc.
```

---

## Step 2: PostgreSQL Setup

### macOS (using Homebrew)

```bash
# Install PostgreSQL
brew install postgresql@14

# Start PostgreSQL service
brew services start postgresql@14

# Verify it's running
psql postgres -c "SELECT version();"
```

### Ubuntu/Debian Linux

```bash
# Install PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib

# Start PostgreSQL service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify it's running
sudo systemctl status postgresql
```

### Windows

1. Download installer from [postgresql.org](https://www.postgresql.org/download/windows/)
2. Run installer and follow wizard
3. Set password for `postgres` user
4. Verify installation in Services (should be running)

### Create Development Database

```bash
# Connect to PostgreSQL
psql -U postgres  # or: psql -U <your-username>

# Create user
CREATE USER varlor_dev WITH PASSWORD 'varlor_dev_password';

# Create database
CREATE DATABASE varlor OWNER varlor_dev;

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE varlor TO varlor_dev;

# Create test database (for running tests)
CREATE DATABASE varlor_test OWNER varlor_dev;
GRANT ALL PRIVILEGES ON DATABASE varlor_test TO varlor_dev;

# Exit psql
\q
```

**Test connection:**
```bash
psql -U varlor_dev -d varlor -h localhost
# Should connect successfully
# Type \q to exit
```

---

## Step 3: Backend Setup

### Install Dependencies

```bash
cd server
npm install
```

**If you encounter permission errors:**
```bash
sudo npm install -g npm@latest  # Update npm globally
npm cache clean --force          # Clear npm cache
rm -rf node_modules package-lock.json
npm install
```

### Configure Environment

```bash
# Copy example environment file
cp .env.example .env

# Edit .env file
nano .env  # or use your preferred editor
```

**Update these critical values in `.env`:**

```env
# Application
PORT=3001
HOST=localhost
NODE_ENV=development

# Generate APP_KEY
# Run: node ace generate:key
APP_KEY=<paste-generated-key-here>

# Database (match PostgreSQL setup from Step 2)
DB_HOST=127.0.0.1
DB_PORT=5432
DB_USER=varlor_dev
DB_PASSWORD=varlor_dev_password
DB_DATABASE=varlor

# CORS (frontend URL)
CORS_ORIGIN=http://localhost:3000

# Generate ACCESS_TOKEN_SECRET
# Run: node ace generate:key
ACCESS_TOKEN_SECRET=<paste-generated-key-here>

# Authentication
ACCESS_TOKEN_EXPIRES_IN=15m
REFRESH_TOKEN_EXPIRES_IN=7d

# Admin account for seeding
ADMIN_EMAIL=admin@varlor.local
ADMIN_PASSWORD=AdminPassword123!
```

**Generate secrets:**

```bash
# Generate APP_KEY
node ace generate:key
# Copy the output and paste into .env as APP_KEY

# Generate ACCESS_TOKEN_SECRET
node ace generate:key
# Copy the output and paste into .env as ACCESS_TOKEN_SECRET
```

### Run Database Migrations

```bash
# Run migrations to create tables
node ace migration:run

# Verify migrations succeeded
node ace migration:status
# Should show all migrations as "completed"
```

### Seed Admin Account

```bash
# Create initial admin user
npm run seed:admin

# You should see:
# [ success ] Admin user created successfully!
# [ info ]  Email: admin@varlor.local
```

### Start Development Server

```bash
# Start backend
npm run dev

# You should see:
# [ info ] Server started on http://localhost:3001
```

**Keep this terminal open and running.**

### Test Backend API

Open a new terminal and test the API:

```bash
# Test health endpoint (when implemented)
curl http://localhost:3001/

# Test login endpoint
curl -X POST http://localhost:3001/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@varlor.local",
    "password": "AdminPassword123!"
  }'

# Should return user data and access token
```

---

## Step 4: Frontend Setup

### Install Dependencies

Open a new terminal (keep backend running):

```bash
cd client/web
npm install
```

### Configure Environment

```bash
# Copy example environment file
cp .env.example .env.local

# Edit .env.local
nano .env.local
```

**Default values should work (no changes needed for local dev):**

```env
NEXT_PUBLIC_API_URL=http://localhost:3001
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

### Start Development Server

```bash
npm run dev

# You should see:
# ▲ Next.js 16.x.x
# - Local: http://localhost:3000
```

**Keep this terminal open and running.**

### Test Frontend

1. Open browser: http://localhost:3000
2. You should be redirected to http://localhost:3000/login
3. Login with:
   - Email: `admin@varlor.local`
   - Password: `AdminPassword123!`
4. You should be redirected to http://localhost:3000/dashboard
5. Dashboard should display with your email

---

## Step 5: VS Code Setup (Recommended)

### Install Extensions

Open VS Code and install these extensions:

**Essential:**
- **ESLint** (dbaeumer.vscode-eslint)
- **Prettier** (esbenp.prettier-vscode)
- **TypeScript** (built-in, ensure it's enabled)
- **Tailwind CSS IntelliSense** (bradlc.vscode-tailwindcss)

**Recommended:**
- **AdonisJS** (jripouteau.adonis-vscode-extension)
- **GitLens** (eamodio.gitlens)
- **Error Lens** (usernamehw.errorlens)
- **Auto Rename Tag** (formulahendry.auto-rename-tag)
- **Path Intellisense** (christian-kohler.path-intellisense)
- **Better Comments** (aaron-bond.better-comments)

### Configure VS Code Settings

Create `.vscode/settings.json` in project root:

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "typescript.tsdk": "node_modules/typescript/lib",
  "typescript.enablePromptUseWorkspaceTsdk": true,
  "tailwindCSS.experimental.classRegex": [
    ["cn\\(([^)]*)\\)", "(?:'|\"|`)([^']*)(?:'|\"|`)"]
  ],
  "files.exclude": {
    "**/.git": true,
    "**/.next": true,
    "**/build": true,
    "**/node_modules": true
  },
  "[typescript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[typescriptreact]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[json]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  }
}
```

### Workspace Recommended Extensions

Create `.vscode/extensions.json`:

```json
{
  "recommendations": [
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "bradlc.vscode-tailwindcss",
    "jripouteau.adonis-vscode-extension",
    "eamodio.gitlens",
    "usernamehw.errorlens"
  ]
}
```

---

## Step 6: Run Tests

### Backend Tests

```bash
cd server

# Run all tests
npm test

# Run specific test suites
node ace test unit
node ace test functional

# Run with coverage
npm test -- --coverage
```

**You should see all tests passing:**
```
✓ Backend initialization tests (3)
✓ Database model tests (4)
✓ Users service tests (6)
✓ Token service tests (7)
✓ Auth service tests (7)
... (total 69 tests)

Tests: 69 passed (69 total)
```

### Frontend Tests

```bash
cd client/web

# Run all tests
npm test

# Run in watch mode
npm test -- --watch

# Run with coverage
npm test -- --coverage
```

---

## Git Workflow

### Branch Naming Convention

```
feature/<feature-name>     # New features
fix/<bug-description>      # Bug fixes
docs/<doc-update>          # Documentation updates
refactor/<refactor-name>   # Code refactoring
test/<test-addition>       # Test additions
```

**Examples:**
- `feature/user-profile-page`
- `fix/login-validation-error`
- `docs/api-endpoint-documentation`

### Commit Message Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <description>

[optional body]

[optional footer]
```

**Types:**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting, etc.)
- `refactor:` Code refactoring
- `test:` Test additions/modifications
- `chore:` Build process or tooling changes

**Examples:**
```bash
git commit -m "feat: add password reset functionality"
git commit -m "fix: resolve login redirect loop on mobile"
git commit -m "docs: update API endpoint documentation"
git commit -m "test: add unit tests for auth service"
```

### Workflow

```bash
# 1. Create feature branch
git checkout -b feature/my-new-feature

# 2. Make changes and commit
git add .
git commit -m "feat: add new feature"

# 3. Push to remote
git push origin feature/my-new-feature

# 4. Create Pull Request on GitHub
# - Describe changes
# - Link related issues
# - Request review

# 5. After PR approval, merge to main
# (Usually done via GitHub UI)

# 6. Delete feature branch
git branch -d feature/my-new-feature
git push origin --delete feature/my-new-feature
```

---

## Code Style and Linting

### Backend (AdonisJS)

**Linting:**
```bash
cd server
npm run lint        # Check for issues
npm run lint:fix    # Auto-fix issues
```

**Formatting:**
```bash
npm run format      # Format all files with Prettier
```

**Type Checking:**
```bash
npm run typecheck   # Check TypeScript types
```

### Frontend (Next.js)

**Linting:**
```bash
cd client/web
npm run lint        # Check for issues
npm run lint:fix    # Auto-fix issues
```

**Type Checking:**
```bash
npx tsc --noEmit    # Check TypeScript types
```

### Pre-commit Hooks (Future)

We plan to add Husky for automatic linting before commits:

```bash
# Will be added in future
npm install -D husky lint-staged
```

---

## Common Issues and Troubleshooting

### Issue: Port Already in Use

**Error:**
```
Error: listen EADDRINUSE: address already in use :::3001
```

**Solution:**
```bash
# Find process using port 3001
lsof -i :3001  # macOS/Linux
netstat -ano | findstr :3001  # Windows

# Kill process
kill -9 <PID>  # macOS/Linux
taskkill /PID <PID> /F  # Windows

# Or use different port
# Edit server/.env: PORT=3002
```

---

### Issue: Database Connection Failed

**Error:**
```
Error: connect ECONNREFUSED 127.0.0.1:5432
```

**Solution:**
```bash
# Check PostgreSQL is running
psql -U postgres  # Should connect

# If not running:
# macOS
brew services start postgresql@14

# Linux
sudo systemctl start postgresql

# Windows
# Start PostgreSQL service in Services app

# Verify .env has correct credentials
cat server/.env | grep DB_
```

---

### Issue: Migration Failed

**Error:**
```
Migration failed: relation already exists
```

**Solution:**
```bash
# Check migration status
node ace migration:status

# Rollback and re-run
node ace migration:rollback
node ace migration:run

# If completely stuck, reset database
node ace migration:reset  # WARNING: Deletes all data!
node ace migration:run
```

---

### Issue: Module Not Found

**Error:**
```
Cannot find module '@/components/ui/button'
```

**Solution:**
```bash
# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install

# Restart TypeScript server in VS Code
# Command Palette (Cmd+Shift+P) → "TypeScript: Restart TS Server"

# Restart Next.js dev server
# Ctrl+C to stop, then npm run dev
```

---

### Issue: ESLint/Prettier Conflicts

**Error:**
```
Unexpected error running Prettier: Cannot find module 'prettier'
```

**Solution:**
```bash
# Install Prettier
npm install -D prettier

# Verify ESLint and Prettier configs
cat .eslintrc.json
cat .prettierrc

# Format all files
npm run format
```

---

## Development Best Practices

### 1. Always Run Tests Before Committing

```bash
# Backend
cd server && npm test

# Frontend
cd client/web && npm test
```

### 2. Keep Dependencies Updated

```bash
# Check for outdated packages
npm outdated

# Update packages (be careful with major versions)
npm update

# Update specific package
npm install package-name@latest
```

### 3. Use TypeScript Strictly

- Never use `any` type
- Enable strict mode in `tsconfig.json`
- Define interfaces for all data structures

### 4. Write Meaningful Commit Messages

**Good:**
```
feat: add user profile editing functionality

- Added ProfileEditForm component
- Implemented PATCH /users/:id endpoint
- Added validation for profile fields
```

**Bad:**
```
updates
fixed stuff
WIP
```

### 5. Comment Complex Logic

```typescript
/**
 * Rate limiting logic: tracks failed login attempts per user account.
 * After 5 failed attempts, account is locked for 15 minutes.
 *
 * @param userId - User ID to check
 * @returns true if account is locked, false otherwise
 */
async function isAccountLocked(userId: number): Promise<boolean> {
  // Implementation
}
```

---

## IDE Configuration

### Debugging in VS Code

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Backend: Debug",
      "type": "node",
      "request": "launch",
      "cwd": "${workspaceFolder}/server",
      "runtimeExecutable": "npm",
      "runtimeArgs": ["run", "dev"],
      "console": "integratedTerminal",
      "skipFiles": ["<node_internals>/**"]
    },
    {
      "name": "Frontend: Debug",
      "type": "node",
      "request": "launch",
      "cwd": "${workspaceFolder}/client/web",
      "runtimeExecutable": "npm",
      "runtimeArgs": ["run", "dev"],
      "console": "integratedTerminal",
      "skipFiles": ["<node_internals>/**"]
    }
  ]
}
```

### Tasks for VS Code

Create `.vscode/tasks.json`:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Backend: Dev",
      "type": "shell",
      "command": "npm run dev",
      "options": {
        "cwd": "${workspaceFolder}/server"
      },
      "isBackground": true,
      "problemMatcher": []
    },
    {
      "label": "Frontend: Dev",
      "type": "shell",
      "command": "npm run dev",
      "options": {
        "cwd": "${workspaceFolder}/client/web"
      },
      "isBackground": true,
      "problemMatcher": []
    },
    {
      "label": "Run All Dev Servers",
      "dependsOn": ["Backend: Dev", "Frontend: Dev"],
      "problemMatcher": []
    }
  ]
}
```

---

## Next Steps

Now that your environment is set up:

1. **Explore the codebase:**
   - Read the architecture documentation
   - Review existing components
   - Understand the patterns

2. **Pick up a task:**
   - Check the project board
   - Find a "good first issue"
   - Ask questions in team chat

3. **Make your first contribution:**
   - Create a feature branch
   - Implement the change
   - Write tests
   - Submit a PR

4. **Learn more:**
   - [API Documentation](../server/docs/API.md)
   - [Database Schema](../server/docs/DATABASE.md)
   - [Frontend Architecture](../client/web/docs/ARCHITECTURE.md)
   - [Deployment Guide](./DEPLOYMENT.md)

---

## Getting Help

### Internal Resources

- **Documentation:** Check `/docs` and `/server/docs` directories
- **Code Comments:** Complex logic is documented inline
- **Tests:** See `/server/tests` and `/client/web/__tests__` for examples

### External Resources

- **AdonisJS:** https://docs.adonisjs.com/
- **Next.js:** https://nextjs.org/docs
- **React:** https://react.dev/
- **TypeScript:** https://www.typescriptlang.org/docs/
- **Tailwind CSS:** https://tailwindcss.com/docs
- **PostgreSQL:** https://www.postgresql.org/docs/

### Team Communication

- **Slack/Discord:** (Add your team chat link)
- **GitHub Discussions:** For longer-form discussions
- **GitHub Issues:** For bug reports and feature requests

---

## Checklist

Use this checklist to verify your setup:

- [ ] Node.js 18+ installed and verified
- [ ] PostgreSQL 14+ installed and running
- [ ] Git configured with your name and email
- [ ] Repository cloned
- [ ] PostgreSQL databases created (`varlor` and `varlor_test`)
- [ ] Backend dependencies installed
- [ ] Backend `.env` configured with generated secrets
- [ ] Database migrations run successfully
- [ ] Admin account seeded
- [ ] Backend server starts on port 3001
- [ ] Frontend dependencies installed
- [ ] Frontend `.env.local` configured
- [ ] Frontend server starts on port 3000
- [ ] Can login with admin credentials
- [ ] VS Code extensions installed
- [ ] All tests passing (backend and frontend)
- [ ] Linting works without errors
- [ ] TypeScript compilation successful

**If all checkboxes are ticked, you're ready to develop!**

---

## Welcome to the Team!

You're all set up and ready to contribute to Varlor. Remember:

- Ask questions when you're stuck
- Write tests for your code
- Keep commits small and focused
- Review others' code thoughtfully
- Have fun building amazing features!

Happy coding!
