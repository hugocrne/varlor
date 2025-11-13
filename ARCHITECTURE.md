# Architecture du Backend Varlor

## Vue d'ensemble

Le backend Varlor est une application Spring Boot écrite en Kotlin, organisée en modules fonctionnels. Elle expose une API REST pour la gestion des utilisateurs, clients et l'analyse de données.

## Stack Technique

- **Langage** : Kotlin 1.9
- **Framework** : Spring Boot 3.3.4 (Spring MVC / Servlet)
- **Build** : Gradle Kotlin DSL
- **Base de données** : PostgreSQL
- **Migrations** : Flyway
- **Sécurité** : Spring Security OAuth2 Resource Server (JWT)
- **Documentation API** : SpringDoc OpenAPI (Swagger)
- **Expressions** : EvalEx 3.1.1

## Structure des Modules

```
backend/src/main/kotlin/com/varlor/backend/
├── product/          # Module produit (authentification, utilisateurs)
│   ├── config/       # Configuration (OpenAPI, Security)
│   ├── controller/   # Contrôleurs REST
│   ├── model/        # Entités JPA et DTOs
│   ├── repository/   # Repositories JPA
│   ├── security/     # Sécurité (JWT, Rate Limiting)
│   └── service/      # Services métier
├── analysis/         # Module analyse (prétraitement, indicateurs)
│   ├── config/       # Configuration web
│   ├── controller/   # Contrôleurs REST
│   ├── model/        # Modèles de domaine et DTOs
│   ├── service/      # Services d'analyse
│   └── util/         # Utilitaires mathématiques
└── common/           # Module commun (réutilisable)
    ├── controller/   # Contrôleurs de base (BaseCrudController)
    ├── exception/    # Gestion globale des exceptions
    ├── extensions/   # Extensions Kotlin
    ├── model/        # Modèles de base (BaseDto, BaseEntity)
    ├── repository/   # Repositories de base (SoftDeleteRepository)
    ├── service/      # Services de base (BaseCrudService)
    └── util/         # Utilitaires communs
```

## Module `product`

### Responsabilités
- Authentification JWT (inscription, connexion, renouvellement)
- Gestion des utilisateurs (CRUD)
- Gestion des clients (CRUD)
- Gestion des sessions utilisateur
- Gestion des préférences utilisateur

### Composants Principaux

**Controllers :**
- `AuthController` : Endpoints d'authentification
- `UserController` : CRUD utilisateurs
- `ClientController` : CRUD clients
- `UserSessionController` : CRUD sessions
- `UserPreferenceController` : CRUD préférences

**Services :**
- `AuthService` : Logique d'authentification et gestion des tokens
- `UserService` : Logique métier utilisateurs
- `ClientService` : Logique métier clients
- `UserSessionService` : Gestion des sessions
- `UserPreferenceService` : Gestion des préférences

**Sécurité :**
- `SecurityConfig` : Configuration Spring Security
- `JwtProvider` : Génération et validation de jetons JWT
- `RateLimitingFilter` : Protection contre les abus

## Module `analysis`

### Responsabilités
- Prétraitement de datasets (nettoyage, normalisation, imputation)
- Calcul d'indicateurs statistiques
- Exécution d'expressions dynamiques (EvalEx)

### Composants Principaux

**Controllers :**
- `AnalysisController` : Endpoints d'analyse

**Services :**
- `DataPreprocessorService` : Prétraitement (détection types, outliers, imputation)
- `IndicatorEngineService` : Calcul d'indicateurs (mean, median, correlation, etc.)
- `AnalysisPipelineService` : Pipeline complet (prétraitement + indicateurs)

**Modèles :**
- `Dataset` : Représentation d'un dataset tabulaire
- `PreprocessingResult` : Résultat du prétraitement
- `OperationResult` : Résultat d'une opération d'indicateur

## Module `common`

### Responsabilités
- Composants réutilisables entre modules
- Patterns communs (CRUD, Soft Delete)
- Gestion globale des erreurs
- Extensions Kotlin utilitaires

### Composants Principaux

**Controllers :**
- `BaseCrudController` : Contrôleur CRUD générique

**Services :**
- `BaseCrudService` : Service CRUD générique
- `CrudService` : Interface CRUD

**Repositories :**
- `SoftDeleteRepository` : Repository avec soft delete

**Exceptions :**
- `GlobalExceptionHandler` : Gestionnaire global d'exceptions

**Extensions :**
- `HttpServletRequestExtensions` : Extraction token, infos client
- `RepositoryExtensions` : Méthodes utilitaires repositories
- `StringExtensions` : Normalisation email, etc.

## Flux de Données

### Authentification

```
1. POST /api/auth/login
   ↓
2. AuthController.login()
   ↓
3. AuthService.login()
   ↓
4. Validation credentials → Génération JWT
   ↓
5. Création UserSession
   ↓
6. Retour TokenPairResponseDto
```

### Requête Protégée

```
1. Requête HTTP avec header Authorization: Bearer <token>
   ↓
2. SecurityConfig → JwtDecoder
   ↓
3. Validation JWT
   ↓
4. Extraction des rôles/autorités
   ↓
5. @PreAuthorize vérification
   ↓
6. Exécution du contrôleur
```

### Analyse de Données

```
1. POST /api/analyses/full
   ↓
2. AnalysisController.full()
   ↓
3. AnalysisPipelineService.executeFullAnalysis()
   ↓
4. DataPreprocessorService.preprocess()
   │   ├─ Inférence types
   │   ├─ Normalisation
   │   ├─ Détection outliers
   │   └─ Imputation valeurs manquantes
   ↓
5. IndicatorEngineService.execute()
   │   ├─ Détection fonctions builtin
   │   ├─ Exécution expressions EvalEx
   │   └─ Calcul résultats
   ↓
6. Retour FullAnalysisResult
```

## Sécurité

### Authentification JWT

- **Access Token** : Durée de vie courte (15 minutes par défaut)
- **Refresh Token** : Durée de vie longue (7 jours par défaut)
- **Rotation** : Nouveau refresh token à chaque renouvellement
- **Stockage** : Refresh tokens hashés (SHA-256) dans la base

### Autorisation

**Rôles :**
- `MEMBER` : Utilisateur standard
- `ADMIN` : Administrateur
- `OWNER` : Propriétaire
- `SERVICE` : Service interne

**Hiérarchie :**
```
OWNER > ADMIN > MEMBER
SERVICE (séparé)
```

### Rate Limiting

- **Limite** : 5 requêtes par minute par IP (configurable)
- **Implémentation** : `RateLimitingFilter`
- **Exclusions** : Endpoints d'authentification

## Patterns Utilisés

### Repository Pattern
- Abstraction de l'accès aux données
- Support du soft delete
- Méthodes de recherche personnalisées

### DTO Pattern
- Séparation entre entités JPA et contrats API
- Validation via Bean Validation
- Documentation Swagger intégrée

### Service Layer Pattern
- Logique métier isolée
- Transactions gérées au niveau service
- Réutilisabilité

### Soft Delete Pattern
- Suppression logique via `deleted_at`
- Filtrage automatique dans les repositories
- Conservation de l'historique

## Gestion des Erreurs

### Structure Standardisée

```json
{
  "timestamp": "2025-01-27T10:00:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "La requête est invalide.",
  "path": "/api/users",
  "details": {
    "email": "L'email doit être valide"
  }
}
```

### Codes HTTP

- `200` : Succès
- `201` : Créé
- `204` : Succès sans contenu
- `400` : Requête invalide
- `401` : Non authentifié
- `404` : Non trouvé
- `409` : Conflit
- `422` : Erreur de traitement
- `500` : Erreur serveur

## Configuration

### Fichiers Principaux

- `application.yaml` : Configuration principale
- `OpenApiConfig.kt` : Configuration Swagger
- `SecurityConfig.kt` : Configuration sécurité
- `JwtConfiguration.kt` : Configuration JWT

### Variables d'Environnement

- `DB_HOST`, `DB_PORT`, `DB_NAME` : Base de données
- `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH` : Clés JWT
- `APP_CORS_ALLOWED_ORIGINS` : Origines CORS autorisées
- `APP_SWAGGER_ENABLED` : Activation Swagger
- `APP_RATE_LIMITING_*` : Configuration rate limiting

## Tests

### Structure

- Tests unitaires : Services, utilitaires
- Tests d'intégration : Controllers, repositories
- Tests avec Testcontainers : Base de données PostgreSQL

### Exécution

```bash
./gradlew test
```

## Déploiement

### Prérequis

- Java 17+
- PostgreSQL 12+
- Variables d'environnement configurées

### Build

```bash
./gradlew bootJar
```

### Exécution

```bash
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar
```

## Évolutions Futures

- [ ] Cache Redis pour les sessions
- [ ] WebSockets pour les notifications temps réel
- [ ] Support de plusieurs bases de données (multi-tenancy)
- [ ] Export de données (CSV, Excel)
- [ ] API GraphQL optionnelle

