# Analyse de l'Arborescence Source - Varlor

## Structure du Dépôt

```
varlor/
├── .bmad/                    # Configuration BMad pour le développement assisté par IA
│   ├── bmm/                 # Module de gestion de projet BMad
│   ├── core/                # Cœur du système BMad
│   └── cis/                 # Modules d'innovation et de créativité
├── agent-os/                 # Système d'agents pour l'automatisation
├── client/                   # Sous-module Git - Applications client
│   └── web/                 # Frontend Next.js 16
│       ├── app/             # Pages App Router
│       │   ├── (auth)/     # Routes d'authentification
│       │   ├── (dashboard)/# Routes du dashboard
│       │   ├── api/        # Routes API (si utilisées)
│       │   ├── globals.css # Styles globaux Tailwind
│       │   └── layout.tsx  # Layout racine
│       ├── components/     # Composants React
│       │   ├── ui/        # Composants de base Shadcn/ui
│       │   ├── auth/      # Composants d'authentification
│       │   ├── datasets/  # Composants de gestion de données
│       │   ├── layouts/   # Composants de layout
│       │   └── providers/ # Providers React
│       ├── lib/            # Bibliothèques utilitaires
│       │   ├── api/       # Client API (5 modules)
│       │   ├── hooks/     # Hooks personnalisés (10 hooks)
│       │   ├── schemas/   # Schémas Zod (4 schémas)
│       │   ├── stores/    # Stores Zustand (2 stores)
│       │   └── utils.ts   # Utilitaires généraux
│       ├── types/          # Types TypeScript
│       └── tests/          # Tests unitaires et E2E
├── server/                  # Backend AdonisJS 6
│   ├── app/
│   │   ├── controllers/    # Contrôleurs HTTP
│   │   │   ├── auth_controller.ts      # Authentification
│   │   │   ├── datasets_controller.ts  # Gestion datasets
│   │   │   ├── cleaning_controller.ts  # Nettoyage données
│   │   │   ├── analysis_controller.ts  # Analyse statistique
│   │   │   ├── ai_insights_controller.ts # Insights IA
│   │   │   └── report_controller.ts    # Génération rapports
│   │   ├── middleware/      # Middleware personnalisés
│   │   ├── models/          # Modèles de données Lucid
│   │   │   ├── dataset.ts              # Dataset principal
│   │   │   ├── dataset_column.ts       # Colonnes
│   │   │   ├── dataset_column_stats.ts # Statistiques
│   │   │   └── dataset_cleaning_log.ts # Logs de nettoyage
│   │   ├── services/        # Logique métier
│   │   │   ├── auth_service.ts         # Service auth
│   │   │   ├── datasets_service.ts     # Service datasets
│   │   │   ├── cleaning_service.ts     # Service nettoyage
│   │   │   ├── analysis_service.ts     # Service analyse
│   │   │   ├── ai_insights_service.ts  # Service IA
│   │   │   └── pdf_generation_service.ts # Génération PDF
│   │   ├── validators/      # Validation VineJS
│   │   └── utils/           # Utilitaires
│   ├── config/              # Configuration AdonisJS
│   ├── database/
│   │   └── migrations/      # Migrations PostgreSQL
│   └── tests/               # Tests backend
└── docs/                    # Documentation générée
    ├── index.md             # Index principal
    ├── project-overview.md  # Vue d'ensemble
    ├── architecture-client.md # Architecture frontend
    ├── architecture-server.md # Architecture backend
    ├── api-contracts-server.md # Contrats API
    ├── data-models-server.md  # Modèles de données
    ├── component-inventory-client.md # Inventaire composants
    ├── development-guide-client.md # Guide dev frontend
    ├── development-guide-server.md # Guide dev backend
    └── integration-architecture.md # Architecture intégration
```

## Points d'Entrée Principaux

### Frontend (client/web)
- **Entry Point**: `next.config.ts` → App Router → `app/layout.tsx`
- **Authentication**: `/app/(auth)/login/page.tsx`
- **Dashboard**: `/app/(dashboard)/dashboard/page.tsx`
- **API Client**: `/lib/api/client.ts`

### Backend (server)
- **Entry Point**: `bin/server.js` → `start/kernel.ts`
- **API Routes**: `/start/routes.ts` → Préfixe `/api/v1`
- **Database**: `config/database.ts`

## Points d'Intégration

1. **Authentification**:
   - Frontend: `lib/stores/auth.store.ts` ↔ Backend: `app/controllers/auth_controller.ts`
   - Tokens JWT avec refresh automatique

2. **Upload de Fichiers**:
   - Frontend: `components/datasets/file-upload-zone.tsx`
   - Backend: `app/controllers/datasets_controller.ts` → `POST /datasets/upload`

3. **Communication API**:
   - Base URL: `NEXT_PUBLIC_API_URL` (frontend)
   - Toutes les routes backend préfixées par `/api/v1`

## Dossiers Critiques

- **`/server/app/services/`**: Contient toute la logique métier
- **`/client/web/lib/api/`**: Client API avec intercepteurs
- **`/server/database/migrations/`**: Schéma de base de données
- **`/client/web/components/datasets/`**: UI de gestion des données

## Organisation du Code

### Patterns Architecturaux
- **Frontend**: Architecture par composants avec App Router
- **Backend**: Service Layer avec Dependency Injection
- **Communication**: REST API avec JWT

### Conventions
- **TypeScript**: Partout avec mode strict
- **Naming**: kebab-case pour les fichiers, PascalCase pour les composants
- **Imports**: Chemins absolus avec alias `@/`

## Technologies Clés par Partie

### Client/Web
- Next.js 16 (App Router)
- React 19
- TypeScript 5.7
- Tailwind CSS 4
- Zustand (state)
- TanStack Query (server state)
- Shadcn/ui (components)

### Server
- AdonisJS 6
- Node.js
- PostgreSQL
- Redis
- Puppeteer (PDF)
- PapaParse (CSV)
- ExcelJS (Excel)