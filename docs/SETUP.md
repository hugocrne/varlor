# Developer Setup Guide

This guide helps developers set up Varlor for local development and contribution.

## Prerequisites

### Required Software

- **Node.js** 18.x or higher
- **npm** 9.x or higher
- **PostgreSQL** 14.x or higher
- **Redis** 6.x or higher
- **Git**

### Optional but Recommended

- **Docker** & Docker Compose (for containerized development)
- **PostgreSQL Client** (pgAdmin, DBeaver, or similar)
- **Redis Desktop Manager**
- **VS Code** with recommended extensions

---

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/varlor/varlor.git
cd varlor
```

### 2. Setup Backend

```bash
cd server

# Install dependencies
npm install

# Copy environment file
cp .env.example .env

# Edit .env with your configuration
```

### 3. Setup Database

```bash
# Create PostgreSQL database
createdb varlor_dev

# Run migrations
npm run migrate

# Seed admin user
npm run seed:admin
```

### 4. Setup Frontend

```bash
cd ../client/web

# Install dependencies
npm install

# Copy environment file
cp .env.example .env.local
```

### 5. Start Development Servers

```bash
# Terminal 1: Backend
cd server
npm run dev

# Terminal 2: Frontend
cd client/web
npm run dev
```

Visit http://localhost:3000 to access the application.

---

## Detailed Setup

### Backend Configuration

#### Environment Variables

Edit `server/.env`:

```env
# Application
NODE_ENV=development
PORT=3001
HOST=localhost

# Database
DB_HOST=127.0.0.1
DB_PORT=5432
DB_USER=your_db_user
DB_PASSWORD=your_db_password
DB_DATABASE=varlor_dev

# Redis (optional for dev)
REDIS_HOST=127.0.0.1
REDIS_PORT=6379

# CORS
CORS_ORIGIN=http://localhost:3000

# Auth
ACCESS_TOKEN_SECRET=your_secret_key_here
REFRESH_TOKEN_SECRET=your_refresh_secret_here

# Admin (for seeding)
ADMIN_EMAIL=admin@varlor.com
ADMIN_PASSWORD=YourSecurePassword123!
```

#### Generate Keys

```bash
# Generate APP_KEY
node ace generate:key

# Generate token secrets
node ace generate:key  # Use for ACCESS_TOKEN_SECRET
node ace generate:key  # Use for REFRESH_TOKEN_SECRET
```

#### Database Setup

1. **Create Database User (PostgreSQL):**

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create user
CREATE USER varlor_dev WITH PASSWORD 'your_password';

-- Create database
CREATE DATABASE varlor_dev OWNER varlor_dev;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE varlor_dev TO varlor_dev;
```

2. **Run Migrations:**

```bash
cd server
node ace migration:run

# Check status
node ace migration:status
```

3. **Seed Data:**

```bash
# Create admin user
npm run seed:admin

# Add sample data (optional)
npm run seed:samples
```

### Frontend Configuration

#### Environment Variables

Edit `client/web/.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:3001
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

#### Install Dependencies

```bash
cd client/web

# Install all dependencies
npm install

# Install specific types if needed
npm install --save-dev @types/node
```

---

## Development Workflow

### 1. Code Organization

```
varlor/
├── server/                 # Backend application
│   ├── app/
│   │   ├── controllers/    # Route handlers
│   │   ├── models/         # Database models
│   │   ├── services/       # Business logic
│   │   ├── validators/     # Input validation
│   │   └── middleware/     # Custom middleware
│   ├── config/             # App configuration
│   ├── database/           # Migrations and seeds
│   └── tests/              # Backend tests
├── client/web/             # Frontend application
│   ├── app/                # Next.js app router
│   ├── components/         # React components
│   ├── lib/                # Utilities and API
│   ├── types/              # TypeScript types
│   └── tests/              # Frontend tests
└── docs/                   # Documentation
```

### 2. Making Changes

#### Backend Changes

1. **Create a new route:**

```typescript
// server/routes.ts
Route.get('/example', 'ExampleController.index')
```

2. **Create a controller:**

```bash
node ace make:controller ExampleController
```

3. **Create a model:**

```bash
node ace make:model Example -m
```

4. **Create migration:**

```bash
node ace make:migration create_examples_table
```

#### Frontend Changes

1. **Create a new page:**

```typescript
// app/dashboard/new-page/page.tsx
export default function NewPage() {
  return <div>New Page Content</div>
}
```

2. **Create a component:**

```typescript
// components/ui/NewComponent.tsx
interface NewComponentProps {
  // props definition
}

export function NewComponent(props: NewComponentProps) {
  // component logic
}
```

3. **API Integration:**

```typescript
// lib/api/new-endpoint.ts
export async function getNewData() {
  const response = await fetch('/api/new-endpoint')
  return response.json()
}
```

### 3. Running Tests

#### Backend Tests

```bash
cd server

# Run all tests
npm test

# Run specific test suite
npm run test:unit
npm run test:functional

# Run with coverage
npm run test:coverage
```

#### Frontend Tests

```bash
cd client/web

# Run unit tests
npm test

# Run E2E tests
npm run test:e2e

# Run with coverage
npm run test:coverage
```

### 4. Code Quality

#### Linting

```bash
# Backend
cd server
npm run lint
npm run lint:fix

# Frontend
cd client/web
npm run lint
npm run lint:fix
```

#### Formatting

```bash
# Backend
cd server
npm run format

# Frontend
cd client/web
npm run format
```

#### Type Checking

```bash
# Frontend only
cd client/web
npm run type-check
```

---

## Useful Commands

### Backend

```bash
# Development
npm run dev          # Start dev server
npm run build        # Build for production
npm start            # Start production server

# Database
npm run migrate      # Run migrations
npm run migrate:rollback  # Rollback migrations
npm run seed         # Run seeds
npm run seed:admin   # Create admin user

# Testing
npm test             # Run tests
npm run test:watch   # Watch mode
npm run test:coverage  # Coverage report

# Utilities
node ace --list      # List all ace commands
```

### Frontend

```bash
# Development
npm run dev          # Start dev server
npm run build        # Build for production
npm run start        # Start production server

# Testing
npm test             # Run tests
npm run test:watch   # Watch mode
npm run test:e2e     # E2E tests
npm run test:coverage  # Coverage report

# Linting/Formatting
npm run lint         # ESLint
npm run lint:fix     # Fix linting issues
npm run format       # Prettier
npm run type-check   # TypeScript check
```

---

## VS Code Extensions

Recommended extensions for development:

```json
{
  "recommendations": [
    "ms-vscode.vscode-typescript-next",
    "bradlc.vscode-tailwindcss",
    "esbenp.prettier-vscode",
    "dbaeumer.vscode-eslint",
    "ms-vscode.vscode-json",
    "adonisjs.vscode-adonisjs",
    "ms-vscode.vscode-postgresql",
    "cweijan.vscode-redis-client"
  ]
}
```

---

## Debugging

### Backend Debugging

1. **Using VS Code:**

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Debug AdonisJS",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/server/bin/server.js",
      "env": {
        "NODE_ENV": "development"
      },
      "console": "integratedTerminal",
      "restart": true,
      "runtimeExecutable": "npm"
    }
  ]
}
```

2. **Using Node Inspector:**

```bash
cd server
node --inspect bin/server.js
```

### Frontend Debugging

1. **React DevTools:** Install browser extension
2. **Network Tab:** Monitor API calls
3. **Console:** Check for errors
4. **Redux DevTools:** If using Redux (not in MVP)

---

## Common Issues

### 1. Database Connection Errors

```bash
# Check PostgreSQL is running
pg_isready

# Check connection
psql -h localhost -U varlor_dev -d varlor_dev
```

### 2. Permission Errors

```bash
# Fix npm permissions
sudo chown -R $(whoami) ~/.npm

# Fix node_modules permissions
sudo chown -R $(whoami) node_modules
```

### 3. Port Already in Use

```bash
# Find process using port 3001
lsof -i :3001

# Kill process
kill -9 <PID>
```

### 4. Migration Issues

```bash
# Reset migrations (dev only)
node ace migration:refresh

# Check migration status
node ace migration:status
```

---

## Contributing

### 1. Branching

```bash
# Create feature branch
git checkout -b feature/new-feature

# Commit changes
git add .
git commit -m "feat: add new feature"

# Push branch
git push origin feature/new-feature
```

### 2. Commit Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: New feature
fix: Bug fix
docs: Documentation changes
style: Code style changes
refactor: Code refactoring
test: Adding or updating tests
chore: Maintenance changes
```

### 3. Pull Request Process

1. Ensure all tests pass
2. Update documentation if needed
3. Submit PR with clear description
4. Request code review
5. Address feedback
6. Merge after approval

---

## Performance Tips

### Development

1. **Hot Reload:** Both frontend and backend support hot reload
2. **Database Indexes:** Add indexes for slow queries
3. **Caching:** Use Redis for caching API responses
4. **Lazy Loading:** Implement for large datasets

### Before Production

1. **Bundle Analysis:** Check frontend bundle size
2. **Database Optimization:** Add missing indexes
3. **Environment Variables:** Review all settings
4. **Security Check:** Run security audit

```bash
# Frontend bundle analysis
cd client/web
npm run build
npm run analyze

# Security audit
npm audit
```

---

## Getting Help

### Resources

1. **Documentation:** `/docs` folder
2. **AdonisJS Docs:** https://docs.adonisjs.com
3. **Next.js Docs:** https://nextjs.org/docs
4. **React Docs:** https://react.dev

### Troubleshooting

1. **Check logs:** `server/logs/` and browser console
2. **Search issues:** GitHub issues
3. **Ask for help:** Team chat or email
4. **Debug step-by-step:** Use debugger

---

## Next Steps

After setup:

1. **Explore the codebase:** Read through the code
2. **Make small changes:** Fix a bug or add a feature
3. **Write tests:** Ensure code quality
4. **Contribute:** Submit your first PR
5. **Learn more:** Study the architecture

Happy coding! 🚀