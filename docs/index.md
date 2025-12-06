# Documentation Varlor

## Vue d'Ensemble

**Type :** Multi-part avec 2 parties
**Langage Principal :** TypeScript
**Architecture :** Full-stack avec Frontend Next.js et Backend AdonisJS
**Statut :** MVP Complété - 100% des fonctionnalités implémentées (Décembre 2025)

## Référence Rapide

### Stack Technique

#### Frontend (client/web)
- **Framework :** Next.js 16.0.3 avec App Router
- **UI :** React 19 + Tailwind CSS 4.1.0
- **État :** Zustand + TanStack Query
- **Auth :** JWT avec refresh tokens
- **Tests :** Jest + Playwright

#### Backend (server)
- **Framework :** AdonisJS 6.18.0
- **Base de données :** PostgreSQL + Redis
- **ORM :** Lucid ORM
- **Auth :** JWT avec scrypt
- **Tests :** Japa

### Points d'Entrée
- **Frontend :** `http://localhost:3000`
- **Backend API :** `http://localhost:3001/api/v1`
- **Documentation API :** `/docs/api-contracts-server.md`

---

## Documentation Générée

### Documentation Principale
- [Vue d'Ensemble du Projet](./project-overview.md) - Présentation complète de Varlor
- [Arborescence Source](./source-tree-analysis.md) - Structure détaillée du code
- [Architecture d'Intégration](./integration-architecture.md) - Communication frontend/backend
- [Complétion du MVP](./MVP_REPORT_COMPLETION_SUMMARY.md) - Résumé de l'implémentation

### Documentation par Partie

#### Frontend - client/web
- [Architecture Frontend](./architecture-client.md) - Patterns et conception React/Next.js
- [Inventaire des Composants](./component-inventory-client.md) _(À générer)_
- [Guide de Développement Frontend](./development-guide-client.md) _(À générer)_

#### Backend - server
- [Architecture Backend](./architecture-server.md) - Patterns et conception AdonisJS
- [Contrats API](./api-contracts-server.md) - Endpoints et schémas REST
- [Modèles de Données](./data-models-server.md) - Schéma de base de données
- [Guide de Développement Backend](./development-guide-server.md) _(À générer)_

### Documentation Utilisateur
- [Guide Utilisateur](./user-guide.md) - Guide complet des fonctionnalités MVP
- [Guide de Déploiement](./deployment-guide.md) - Instructions de déploiement

---

## Documentation Existante

### Projet Principal
- [README.md](../README.md) - Instructions d'installation et démarrage rapide
- [PROJECT.md](../PROJECT.md) - Vision stratégique et conceptuelle
- [ROADMAP.md](../ROADMAP.md) - Feuille de route de développement
- [STACK.md](../STACK.md) - Stack technique détaillée

### Backend
- [README Server](../server/README.md) - Documentation backend spécifique
- [API Docs](../server/docs/API.md) - Documentation API détaillée

### Frontend
- [README Client](../client/web/README.md) - Documentation frontend spécifique
- [Frontend Architecture](../client/web/docs/ARCHITECTURE.md) - Notes d'architecture frontend

---

## Getting Started

### Pour les Développeurs

1. **Installation Locale**
   ```bash
   # Backend
   cd server
   npm install
   cp .env.example .env
   npm run dev  # Port 3001

   # Frontend
   cd client/web
   npm install
   cp .env.example .env.local
   npm run dev  # Port 3000
   ```

2. **Base de Données**
   ```bash
   cd server
   node ace migration:run
   npm run seed:admin
   ```

3. **Accès Application**
   - Frontend : http://localhost:3000
   - Backend : http://localhost:3001/api/v1
   - Login admin : vérifier `.env`

### Pour la Planification de Features

1. **Comprendre l'Architecture**
   - Lire [Architecture d'Intégration](./integration-architecture.md)
   - Consulter les [Contrats API](./api-contracts-server.md)
   - Analyser les [Modèles de Données](./data-models-server.md)

2. **Identifier les Patterns**
   - Patterns React dans [Architecture Frontend](./architecture-client.md)
   - Patterns AdonisJS dans [Architecture Backend](./architecture-server.md)
   - Convention de codage dans [Arborescence Source](./source-tree-analysis.md)

3. **Développer**
   - Suivre les guides de développement
   - Maintenir les standards de test
   - Documenter les changements

### Guidelines de Développement

#### Frontend
- Utiliser les composants de `/components/ui/`
- Suivre les patterns de `/lib/hooks/`
- Valider avec les schémas Zod de `/lib/schemas/`
- Gérer l'état avec Zustand/TanStack Query

#### Backend
- Créer des services dans `/app/services/`
- Ajouter des contrôleurs dans `/app/controllers/`
- Créer des migrations pour les changements de schéma
- Valider avec les validateurs VineJS

#### Testing
- Tests unitaires pour services et hooks
- Tests E2E pour workflows utilisateur
- Couverture minimale de 80%

---

## Conventions du Projet

### Structure des Fichiers
```
project-root/
├── client/web/          # Frontend Next.js
├── server/              # Backend AdonisJS
├── docs/                # Documentation (cette page)
└── .bmad/              # Configuration BMad
```

### Naming Conventions
- **Fichiers :** kebab-case (`dataset-service.ts`)
- **Composants React :** PascalCase (`DatasetTable.tsx`)
- **Variables/Functions :** camelCase (`getUserData`)
- **Constants :** UPPER_SNAKE_CASE (`API_BASE_URL`)
- **Types/Interfaces :** PascalCase (`DatasetType`)

### Git Workflow
- Branches feature : `feature/dataset-upload`
- Pull requests requises
- Tests requis avant merge
- Convention de commits : Conventional Commits

---

## IA-Assisted Development

### Utilisation de cette Documentation

Cette documentation est optimisée pour l'assistance IA :

1. **Pour Comprendre le Contexte**
   - L'index.md fournit une vue d'ensemble complète
   - Les documents d'architecture détaillent les patterns
   - Les contrats API définissent les interfaces

2. **Pour Génération de Code**
   - Référencer les patterns existants
   - Maintenir la cohérence avec les conventions
   - Suivre les exemples de documentation

3. **Pour Réfactoring**
   - Consulter l'arborescence source
   - Identifier les patterns réutilisables
   - Maintenir l'intégrité des tests

### Points d'Attention pour l'IA

1. **Multi-tenant** : Tous les accès doivent vérifier `tenant_id`
2. **Sécurité** : Ne jamais exposer de données sensibles
3. **Performance** : Utiliser le streaming pour gros volumes
4. **Types** : Maintenir la cohérence TypeScript entre frontend/backend
5. **Tests** : Documenter les cas limites dans les tests

---

## Fonctionnalités MVP (Complétées)

### 1. Import de Données ✅
- Support des fichiers CSV et Excel
- Parsing automatique avec détection de types
- Gestion des erreurs de format
- Preview avant import

### 2. Nettoyage Automatique ✅
- Détection et correction des valeurs aberrantes
- Standardisation des formats
- Gestion des doublons
- Validation des données

### 3. Analyse Algorithmique ✅
- Statistiques descriptives complètes
- Détection de corrélations
- Visualisations automatiques
- Identification de patterns

### 4. Insights IA ✅
- Génération d'insights pertinents
- Détection d'anomalies
- Recommandations d'actions
- Interprétation en langage naturel

### 5. Génération de Rapports ✅
- Rapports PDF professionnels
- Synthèse de qualité
- Visualisations intégrées
- Export sécurisé

### Infrastructure ✅
- Multi-tenancy prêt
- Stockage S3 configurable
- Rate limiting Redis
- Logging structuré
- Authentification JWT sécurisée

---

## Roadmap Prochaine Étape

### Alpha/Beta (Q1 2025)
- 🔄 Connecteurs API et bases de données
- 🔄 Dashboard administrateur
- 🔄 Export multi-formats (JSON, XML)
- 🔄 API publique pour intégrations
- 🔄 Tests automatisés CI/CD

### V1 Enterprise (Q2 2025)
- ⏳ Déploiement multi-tenant isolé
- ⏳ SSO entreprise (SAML, OpenID)
- ⏳ Audit logging avancé
- ⏳ Support SLA entreprise
- ⏳ Migration automatique depuis outils existants

### V2 Platform (2025+)
- ⏳ Workflow designer visuel
- ⏳ Machine learning intégré
- ⏳ API marketplace
- ⏳ Edge deployment
- ⏳ Streaming temps réel

---

## Support et Contribution

### Obtenir de l'Aide
- **Documentation technique** : Cette documentation
- **Guide utilisateur** : [Guide Utilisateur](./user-guide.md)
- **Issues GitHub** : [GitHub Issues](https://github.com/varlor/issues)
- **Team** : `team@varlor.com`

### Contribuer
1. Forker le dépôt
2. Créer une branche feature
3. Implémenter avec tests
4. Soumettre une PR avec description
5. Attendre review et merge

### Standards de Qualité
- Coverage tests >80%
- Documentation des APIs
- Performance benchmarks
- Security reviews
- Code reviews obligatoires

---

*Ce document est généré automatiquement et maintenu à jour avec l'évolution du projet. Dernière mise à jour : 6 Décembre 2025*