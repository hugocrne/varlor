# Architecture Backend - Varlor

## Résumé Exécutif

Le backend de Varlor est une API RESTful construite avec AdonisJS 6, conçue pour fournir des services de traitement et d'analyse de données robustes, évolutifs et sécurisés. L'architecture suit les principes SOLID avec une séparation claire des responsabilités et une forte emphasis sur la performance et la maintenabilité.

## Stack Technologique

### Framework Principal
- **AdonisJS 6.18.0**: Framework Node.js TypeScript-first
- **Node.js**: Runtime JavaScript
- **TypeScript**: Typage strict avec configuration étendue

### Base de Données
- **PostgreSQL**: Base de données relationnelle primaire
- **Lucid ORM**: Abstraction de base de données
- **Redis**: Cache et rate limiting

### Traitement Fichiers
- **PapaParse**: Parsing CSV avec détection encoding
- **ExcelJS**: Lecture fichiers Excel
- **Puppeteer**: Génération PDF
- **Sharp**: Traitement images (future)

### Infrastructure
- **AWS SDK**: Stockage S3
- **File-type**: Validation types MIME
- **ioredis**: Client Redis haute performance

## Architecture Modèle

### 1. Architecture en Couches

#### Presentation Layer (Controllers)
```typescript
app/controllers/
├── auth_controller.ts       # Authentification
├── datasets_controller.ts   # Gestion datasets
├── cleaning_controller.ts   # Nettoyage données
├── analysis_controller.ts   # Analyse statistique
├── ai_insights_controller.ts # Insights IA
└── report_controller.ts     # Génération rapports
```

#### Business Logic Layer (Services)
```typescript
app/services/
├── auth_service.ts          # Logique authentification
├── datasets_service.ts      # Gestion datasets
├── dataset_parser_service.ts # Parsing fichiers
├── cleaning_service.ts      # Nettoyage automatique
├── analysis_service.ts      # Analyse statistique
├── ai_insights_service.ts   # Génération insights
├── report_service.ts        # Agrégation rapports
├── pdf_generation_service.ts # Génération PDF
├── file_storage_service.ts  # Abstraction stockage
├── redis_service.ts         # Gestion Redis
└── token_service.ts         # Gestion tokens JWT
```

#### Data Access Layer (Models)
```typescript
app/models/
├── dataset.ts               # Dataset principal
├── dataset_column.ts        # Métadonnées colonnes
├── dataset_column_stats.ts  # Statistiques par colonne
└── dataset_cleaning_log.ts  # Logs opérations
```

### 2. Architecture de Données

#### Schéma Principal
```sql
-- Users: Gestion utilisateurs
users {
  id: PK
  email: UNIQUE, INDEXED
  password: HASHED
  tenant_id: INDEXED
  role: ENUM
  timestamps
}

-- Datasets: Métadonnées datasets
datasets {
  id: PK
  tenant_id: INDEXED
  user_id: FK users
  storage_path: STRING
  file_metadata: JSON
  processing_status: ENUM
  quality_metrics: JSONB
  timestamps
}

-- Dataset Columns: Structure dynamique
dataset_columns {
  id: PK
  dataset_id: FK datasets
  column_name: STRING
  detected_type: ENUM
  quality_stats: JSONB
}

-- Statistics: Résultats analyse
dataset_column_stats {
  id: PK
  dataset_id: FK datasets
  column_name: STRING
  stats_type: ENUM
  statistics: JSONB
  outliers: JSONB
}
```

### 3. Architecture API

#### Design RESTful
```typescript
// Routes API avec préfixe /api/v1
GET    /users/me                    // Profil utilisateur
POST   /auth/login                  // Connexion
POST   /auth/refresh                // Refresh token

// Datasets
POST   /datasets/upload             // Upload (rate limited)
GET    /datasets/:id                // Métadonnées
GET    /datasets/:id/preview        // Aperçu (20 lignes)

// Processing
POST   /datasets/:id/cleaning/start // Démarrer nettoyage
GET    /datasets/:id/cleaning/status // Statut nettoyage
GET    /datasets/:id/cleaning/results // Résultats paginés

// Analysis
POST   /datasets/:id/analysis/start // Démarrer analyse
GET    /datasets/:id/analysis/results // Résultats complets
GET    /datasets/:id/analysis/chart/:col // Visualisation

// AI Insights
GET    /datasets/:id/ai-insights    // Insights générés
POST   /datasets/:id/ai-insights/generate // Trigger IA (rate limited)

// Reports
POST   /datasets/:id/report/generate // Génération PDF
GET    /datasets/:id/report/download // Téléchargement sécurisé
```

## Patterns de Conception

### 1. Service Layer Pattern
```typescript
// Exemple: CleaningService
export default class CleaningService {
  constructor(
    private dataset: Dataset,
    private fileStorage: FileStorageService
  ) {}

  async startCleaning(): Promise<CleaningJob> {
    // 1. Validation préliminaire
    // 2. Détection problèmes
    // 3. Application corrections
    // 4. Génération logs
    // 5. Mise à jour stats
  }
}
```

### 2. Repository Pattern via Lucid
```typescript
// Abstraction de données via modèles
const dataset = await Dataset.query()
  .where('user_id', auth.user.id)
  .where('status', 'READY')
  .preload('columns', (query) => {
    query.preload('stats')
  })
  .firstOrFail()
```

### 3. Factory Pattern (Storage)
```typescript
// app/services/file_storage_service.ts
export default class FileStorageService {
  static create(): IFileStorage {
    const backend = Env.get('STORAGE_BACKEND', 'local')

    switch (backend) {
      case 's3': return new S3Storage()
      case 'local': return new LocalStorage()
      default: throw new Error('Storage backend not supported')
    }
  }
}
```

### 4. Observer Pattern (Events)
```typescript
// Événements de traitement
Event.dispatch('dataset:processing_started', {
  datasetId: dataset.id,
  userId: auth.user.id
})

// Listeners pour tâches asynchrones
Event.on('dataset:processing_started', async (payload) => {
  await AnalysisService.startAnalysis(payload.datasetId)
})
```

## Pipeline de Traitement

### 1. Upload et Parsing
```
File Upload → Validation MIME → Sauvegarde S3/Local → Parsing → Type Detection → Enregistrement Dataset
```

### 2. Nettoyage Automatique
```typescript
interface CleaningPipeline {
  // 1. Détection problèmes
  detectIssues(): CleaningIssue[]

  // 2. Application corrections
  applyCorrections(): CleaningResult

  // 3. Génération logs
  generateLogs(): CleaningLog[]

  // 4. Mise à jour métriques
  updateMetrics(): QualityScore
}
```

### 3. Analyse Statistique
```typescript
interface AnalysisPipeline {
  // Par type de colonne
  NumberColumn: DescriptiveStats + Outliers + Distribution
  TextColumn: Frequency Analysis + Patterns
  DateColumn: Time Series + Trends
  MixedColumn: Type Coercion + Validation
}
```

### 4. Génération Insights IA
```typescript
// Service IA avec retry et fallback
export default class AIInsightsService {
  async generateInsights(dataset: Dataset): Promise<Insights> {
    try {
      const response = await this.externalAIClient.generate({
        statistics: dataset.stats,
        context: this.buildContext(dataset)
      })
      return this.parseResponse(response)
    } catch (error) {
      return this.generateFallbackInsights(dataset)
    }
  }
}
```

## Gestion d'État

### 1. Base de Données
- **État persistent**: Métadonnées, résultats, logs
- **Transactions**: ACID compliance via PostgreSQL
- **Indexes**: Optimisés pour requêtes fréquentes

### 2. Cache Redis
```typescript
// Stratégies de cache
- Rate limiting: 10 uploads/hour/user
- Session tokens: TTL 15 min
- Processing status: TTL 24h
- Download tokens: TTL 5 min
```

### 3. File System
- **Temporaire**: Fichiers en cours de traitement
- **Permanent**: Données brutes et résultats
- **Cleanup**: Tâches automatiques de suppression

## Sécurité

### 1. Authentification
```typescript
// JWT avec refresh tokens
interface AuthConfig {
  accessToken: {
    expiresIn: '15m'
    algorithm: 'HS256'
  }
  refreshToken: {
    expiresIn: '7d'
    httpOnly: true
    secure: true
  }
}
```

### 2. Authorization
- **Tenant Isolation**: `tenant_id` dans toutes les requêtes
- **User Scoping**: Vérification ownership datasets
- **Role-based**: Admin/User roles

### 3. Input Validation
```typescript
// VineJS validators
export const uploadDatasetValidator = vine.create({
  file: vine.file({
    size: '500mb',
    extnames: ['csv', 'xlsx', 'xls']
  }).optional()
})

export const createDatasetValidator = vine.create({
  name: vine.string().minLength(3).maxLength(100),
  description: vine.string().maxLength(500)
})
```

### 4. Rate Limiting
```typescript
// Redis-based rate limiting
const limiter = new RedisRateLimiter({
  windowMs: 60 * 60 * 1000, // 1 hour
  max: 10, // 10 requests per hour
  keyGenerator: (req) => `upload:${req.user.id}`
})
```

## Performance

### 1. Streaming Support
```typescript
// Pour fichiers volumineux (>50MB)
if (file.size > LARGE_FILE_THRESHOLD) {
  await this.processInStream(file)
} else {
  await this.processInMemory(file)
}
```

### 2. Base de Données
```typescript
// Indexes optimisés
CREATE INDEX idx_datasets_user_status ON datasets(user_id, status)
CREATE INDEX idx_dataset_columns_dataset ON dataset_columns(dataset_id)
CREATE INDEX idx_cleaning_logs_dataset ON dataset_cleaning_logs(dataset_id)

// JSONB avec GIN indexes
CREATE INDEX idx_dataset_stats ON dataset_column_stats USING GIN(stats_json)
```

### 3. Cache Strategy
- **Write-through**: Mise à jour cache immédiate
- **Lazy loading**: Chargement à la demande
- **TTL variables**: Basé sur type de donnée

## Évolutivité

### 1. Multi-tenancy
- Isolation au niveau données
- Configuration par tenant
- Scaling horizontal

### 2. Background Processing
```typescript
// Queue jobs pour tâches longues
await dispatch(new CleaningJob(dataset.id))
await dispatch(new AnalysisJob(dataset.id))
await dispatch(new AIInsightsJob(dataset.id))
```

### 3. Microservices Ready
- Services découplés
- API versioning
- Event-driven architecture

## Surveillance & Logging

### 1. Structured Logging
```typescript
// Contexte avec correlation IDs
logger.info('Dataset processing started', {
  datasetId: dataset.id,
  userId: user.id,
  correlationId: ctx.request.id(),
  metadata: {
    fileSize: dataset.fileSize,
    columns: dataset.columnCount
  }
})
```

### 2. Métriques
```typescript
// Performance tracking
const metrics = {
  uploadSuccess: new Counter('uploads_success_total'),
  processingTime: new Histogram('processing_duration_seconds'),
  errorRate: new Counter('processing_errors_total')
}
```

## Tests

### 1. Structure Tests
```
tests/
├── unit/           # Tests isolés services
├── functional/     # Tests API endpoints
├── integration/    # Tests workflows complets
└── e2e/           # Tests user journey
```

### 2. Test Utilities
```typescript
// Factories pour données de test
export const DatasetFactory = factory
  .define(Dataset, () => ({
    name: faker.lorem.words(3),
    userId: 1,
    status: 'READY'
  }))

// Mocks external services
export const mockAIService = {
  generate: vi.fn().mockResolvedValue(mockInsights)
}
```

## Déploiement

### 1. Configuration
```typescript
// .env.example
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USER=varlor_user
DB_PASSWORD=password
DB_DATABASE=varlor_prod

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Storage
STORAGE_BACKEND=s3
AWS_ACCESS_KEY_ID=key
AWS_SECRET_ACCESS_KEY=secret
AWS_S3_BUCKET=varlor-files

# AI Service
AI_API_URL=https://api.openai.com/v1
AI_API_KEY=sk-...

# Security
JWT_SECRET=secret
ACCESS_TOKEN_SECRET=secret
```

### 2. Processus Build
```json
{
  "scripts": {
    "build": "node ace build",
    "start": "node build/server.js",
    "migration:run": "node ace migration:run",
    "seed:admin": "node ace seed:admin"
  }
}
```

Cette architecture backend robuste fournit une base solide pour le traitement de données à grande échelle tout en maintenant une excellente séparation des préoccupations et une forte testabilité.