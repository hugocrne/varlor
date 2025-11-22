# Varlor

## La plateforme de Data Intelligence universelle et souveraine

Varlor est une plateforme de **data intelligence** conçue pour permettre aux entreprises de reprendre le contrôle total sur leurs données — quelle que soit leur forme, leur qualité, leur source ou leur volume.

Notre mission :
**Transformer les données brutes en décisions précises, actionnables et contextualisées.**

Varlor s'inspire des meilleures plateformes de data intelligence au monde (dont Palantir Foundry), tout en proposant une approche plus moderne, plus flexible, souveraine et adaptée aux besoins européens.

---

## Quick Start

### Prerequisites

- **Node.js** 18+ and npm
- **PostgreSQL** 14+
- **Git**

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/varlor.git
   cd varlor
   ```

2. **Backend Setup**
   ```bash
   cd server
   npm install

   # Configure environment
   cp .env.example .env
   # Edit .env with your configuration

   # Generate app key and access token secret
   node ace generate:key  # Copy this to APP_KEY
   node ace generate:key  # Copy this to ACCESS_TOKEN_SECRET

   # Run database migrations
   node ace migration:run

   # Seed admin account
   npm run seed:admin
   ```

3. **Frontend Setup**
   ```bash
   cd ../client/web
   npm install

   # Configure environment
   cp .env.example .env.local
   # Edit .env.local if needed (defaults should work)
   ```

4. **Start Development Servers**

   Terminal 1 (Backend):
   ```bash
   cd server
   npm run dev
   # Server starts on http://localhost:3001
   ```

   Terminal 2 (Frontend):
   ```bash
   cd client/web
   npm run dev
   # Frontend starts on http://localhost:3000
   ```

5. **Login**

   Open http://localhost:3000/login and use the admin credentials from your `.env` file.

---

## Project Architecture

```
varlor/
├── client/
│   └── web/              # Next.js 16 frontend application
│       ├── app/          # App Router pages and layouts
│       ├── components/   # React components
│       ├── lib/          # Utilities, API client, state management
│       └── types/        # TypeScript type definitions
│
├── server/               # AdonisJS backend API
│   ├── app/
│   │   ├── controllers/  # HTTP request handlers
│   │   ├── middleware/   # Custom middleware
│   │   ├── models/       # Lucid ORM models
│   │   ├── services/     # Business logic layer
│   │   └── validators/   # Request validation (VineJS)
│   ├── config/           # Configuration files
│   ├── database/
│   │   └── migrations/   # Database migrations
│   ├── scripts/          # Utility scripts
│   └── tests/            # Unit and functional tests
│
├── docs/                 # Documentation
└── agent-os/             # AI-assisted development specs
```

---

## Technology Stack

### Backend
- **AdonisJS 6** - Modern Node.js framework
- **PostgreSQL** - Relational database
- **Lucid ORM** - Database abstraction
- **VineJS** - Request validation
- **JWT** - Authentication with access/refresh tokens
- **Scrypt** - Password hashing

### Frontend
- **Next.js 16** - React framework with App Router
- **React 19** - UI library with Server Components
- **TypeScript 5.7** - Type safety
- **Tailwind CSS 4** - Utility-first styling
- **Shadcn/ui** - Component library
- **Zustand** - Client state management
- **TanStack Query** - Server state management
- **React Hook Form + Zod** - Form handling and validation
- **Axios** - HTTP client

---

## Environment Variables

### Backend (.env)

```env
# Application
PORT=3001
HOST=localhost
NODE_ENV=development
APP_KEY=<generated-key>

# Database
DB_HOST=127.0.0.1
DB_PORT=5432
DB_USER=varlor_user
DB_PASSWORD=<your-password>
DB_DATABASE=varlor_dev

# CORS
CORS_ORIGIN=http://localhost:3000

# Authentication
ACCESS_TOKEN_SECRET=<generated-secret>
ACCESS_TOKEN_EXPIRES_IN=15m
REFRESH_TOKEN_EXPIRES_IN=7d

# Admin Seeding
ADMIN_EMAIL=admin@varlor.com
ADMIN_PASSWORD=<secure-password>
```

See `server/.env.example` for detailed documentation.

### Frontend (.env.local)

```env
NEXT_PUBLIC_API_URL=http://localhost:3001
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

---

## Available Scripts

### Backend (server/)
- `npm run dev` - Start development server with hot reload
- `npm run build` - Build for production
- `npm start` - Start production server
- `npm test` - Run all tests
- `npm run lint` - Lint code
- `npm run format` - Format code with Prettier
- `npm run seed:admin` - Seed initial admin account
- `node ace migration:run` - Run database migrations
- `node ace migration:rollback` - Rollback last migration batch

### Frontend (client/web/)
- `npm run dev` - Start development server (port 3000)
- `npm run build` - Create production build
- `npm run start` - Start production server
- `npm run lint` - Run ESLint
- `npm test` - Run tests

---

## Database Setup

### Create PostgreSQL Database and User

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create user
CREATE USER varlor_user WITH PASSWORD 'your_password';

-- Create database
CREATE DATABASE varlor_dev OWNER varlor_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE varlor_dev TO varlor_user;
```

### Run Migrations

```bash
cd server
node ace migration:run
```

This creates the following tables:
- `users` - User accounts
- `refresh_tokens` - JWT refresh tokens
- `roles` - User roles (prepared for future)
- `auth_access_tokens` - Access token storage

---

## Testing

### Run Backend Tests
```bash
cd server
npm test

# Or specific suites
node ace test unit
node ace test functional
```

### Run Frontend Tests
```bash
cd client/web
npm test
```

---

## Vision

Les entreprises croulent sous les données, mais très peu parviennent réellement à en tirer une valeur exploitable.
La majorité du temps est perdue à :

- retrouver les données,
- les nettoyer,
- les rapprocher,
- comprendre leur signification,
- harmoniser des sources hétérogènes,
- analyser des métriques pourtant fondamentales.

Varlor résout ce problème en **automatisant l'ensemble de la chaîne de valeur**, de l'ingestion à la décision.

---

## Objectif principal

### **Se brancher partout. Comprendre tout. S'adapter à n'importe quel client.**

Varlor est construit sur une idée simple :
**chaque entreprise est unique, mais toutes doivent pouvoir exploiter pleinement leurs données.**

Cela implique que la plateforme soit capable de :

- accepter **tous les formats** (CSV, Excel, JSON, XML, logs, formats propriétaires…),
- se connecter à **toutes les sources** (bases, API, ERP, CRM, systèmes legacy),
- traiter **tous les niveaux de qualité** (incomplet, incohérent, bruité, massif),
- s'adapter à **tous les environnements** (cloud, on-premise, air-gapped),
- modéliser **tous les métiers** via une représentation abstraite et universelle.

Varlor est conçu comme une solution **réellement universelle**, modulaire et durable.

---

## Ce que fait Varlor

### 1. Ingestion universelle

Varlor sait ingérer toute forme de données, des systèmes modernes jusqu'aux environnements legacy.
Aucune structure n'est imposée. Aucune donnée n'est rejetée.
La plateforme absorbe et comprend ce que les entreprises lui donnent.

---

### 2. Nettoyage, validation et normalisation

Les données brutes sont :

- analysées,
- typées,
- corrigées,
- enrichies,
- normalisées,
- structurées,

afin de produire une base propre, fiable et prête à l'analyse.

Varlor détecte automatiquement :

- valeurs aberrantes,
- incohérences,
- formats incorrects,
- doublons,
- données contradictoires.

Chaque anomalie est traçée, expliquée et contextualisée.

---

### 3. Modélisation ontologique

Le cœur de Varlor repose sur une **ontologie métier**, c'est-à-dire une couche sémantique qui unifie toutes les données selon des objets et relations compréhensibles.

Exemples :

- *Commande*,
- *Machine*,
- *Produit*,
- *Événement*,
- *Client*,
- *Flux logistique*, etc.

Cette représentation abstraite permet :

- l'unification de sources hétérogènes,
- des analyses croisées cohérentes,
- une compréhension automatique du métier,
- des suggestions intelligentes,
- une exploitation beaucoup plus simple des données.

---

### 4. Analyse avancée et intelligence artificielle

Une fois les données propres et unifiées, Varlor exécute :

- des analyses statistiques,
- des corrélations,
- du clustering,
- de la détection d'anomalies,
- des prévisions,
- et des modèles IA adaptés aux données du client.

L'objectif n'est pas de produire de simples graphiques,
mais **d'expliquer ce qui se passe, pourquoi cela se produit, et ce qui pourrait arriver.**

---

### 5. Génération de rapports intelligents

Varlor génère automatiquement des rapports :

- lisibles,
- contextualisés,
- illustrés,
- versionnés,
- accompagnés d'insights compréhensibles par les équipes métier.

Ces rapports incluent :

- visualisations intelligentes,
- interprétations,
- recommandations stratégiques,
- alertes potentielles,
- pistes d'investigation.

---

## Souveraineté, sécurité et maîtrise totale

Varlor est conçu pour fonctionner dans des environnements aux exigences extrêmes :

- on-premise,
- cloud privé,
- infrastructures souveraines,
- zones air-gappées (sans Internet),
- organisations nécessitant des audits stricts.

L'authentification est entièrement interne, sans dépendance à des services externes.

La philosophie est simple :
**les données de l'entreprise ne sortent jamais de l'entreprise.**

---

## Pourquoi Varlor existe

Parce que les entreprises veulent :

- comprendre leurs données,
- détecter les risques et opportunités,
- automatiser les analyses répétitives,
- avoir une source de vérité unique,
- améliorer leur prise de décision,
- exploiter réellement l'IA dans leur fonctionnement quotidien.

Varlor répond à ces besoins en offrant :

- une ingestion universelle,
- une modélisation unifiée,
- une analyse intelligente,
- une adaptabilité totale,
- une souveraineté non négociable,
- une qualité et une cohérence irréprochables.

---

## Positionnement

Varlor se positionne comme une alternative européenne moderne aux plateformes de data intelligence existantes, en combinant :

- **la flexibilité**
d'une solution d'ingestion universelle,

- **la puissance**
d'une ontologie métier automatisée,

- **la souveraineté**
d'une infrastructure totalement maîtrisée,

- **l'intelligence**
d'analyses avancées et de rapports interprétés.

Contrairement aux outils traditionnels de BI ou d'analyse,
**Varlor ne se contente pas de visualiser des données :
il les comprend, les structure et les interprète.**

---

## Documentation

- [Backend README](./server/README.md) - Backend setup and API documentation
- [Frontend README](./client/web/README.md) - Frontend setup and architecture
- [API Documentation](./server/docs/API.md) - Detailed API endpoint reference
- [Database Schema](./server/docs/DATABASE.md) - Database structure and relationships
- [Frontend Architecture](./client/web/docs/ARCHITECTURE.md) - Frontend patterns and organization
- [Deployment Guide](./docs/DEPLOYMENT.md) - Production deployment instructions
- [Developer Setup](./docs/SETUP.md) - Complete developer onboarding guide

---

## Security

- JWT-based authentication with httpOnly cookies
- Password hashing with scrypt (bcrypt-equivalent)
- Rate limiting on login attempts (5 attempts, 15-minute lockout)
- CSRF protection via SameSite cookies
- XSS prevention through httpOnly cookies and input sanitization
- SQL injection prevention via parameterized queries
- Production-ready security configurations

---

## Ambition

Construire la plateforme la plus adaptative, la plus compréhensive et la plus souveraine du marché.

Créer un outil capable de donner à chaque entreprise — quelle que soit sa taille —
**un avantage stratégique fondé sur la maîtrise totale de sa donnée.**

---

## License

UNLICENSED - Private project
