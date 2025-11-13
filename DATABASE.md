# Documentation de la Base de Données

## Vue d'ensemble

Le backend Varlor utilise **PostgreSQL** comme base de données principale. La gestion du schéma est assurée par **Flyway** pour les migrations.

## Schéma de Base de Données

### Tables Principales

#### `clients`
Table principale pour les clients (organisations).

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Identifiant unique |
| `name` | VARCHAR(255) | NOT NULL | Nom du client |
| `type` | VARCHAR(50) | NOT NULL | Type de client (ENTERPRISE, INDIVIDUAL, etc.) |
| `status` | VARCHAR(50) | NOT NULL | Statut (ACTIVE, INACTIVE, etc.) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Date de création |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Date de mise à jour |
| `deleted_at` | TIMESTAMP | NULL | Date de suppression logique |

**Index :**
- `idx_clients_name` sur `name`
- `idx_clients_status` sur `status`

#### `users`
Table des utilisateurs.

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Identifiant unique |
| `client_id` | UUID | NOT NULL, FK → clients(id) | Référence au client |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Email (normalisé) |
| `password_hash` | VARCHAR(255) | NOT NULL | Hash BCrypt du mot de passe |
| `first_name` | VARCHAR(100) | NOT NULL | Prénom |
| `last_name` | VARCHAR(100) | NOT NULL | Nom de famille |
| `role` | VARCHAR(50) | NOT NULL | Rôle (MEMBER, ADMIN, OWNER, SERVICE) |
| `status` | VARCHAR(50) | NOT NULL | Statut (ACTIVE, INACTIVE) |
| `last_login_at` | TIMESTAMP | NULL | Dernière connexion |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Date de création |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Date de mise à jour |
| `deleted_at` | TIMESTAMP | NULL | Date de suppression logique |

**Index :**
- `idx_users_client_id` sur `client_id`
- `idx_users_status` sur `status`

**Contraintes :**
- Foreign key vers `clients(id)` avec `ON DELETE CASCADE`

#### `user_preferences`
Préférences utilisateur (thème, langue, notifications).

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Identifiant unique |
| `user_id` | UUID | NOT NULL, UNIQUE, FK → users(id) | Référence à l'utilisateur |
| `theme` | VARCHAR(50) | NOT NULL | Thème (LIGHT, DARK) |
| `language` | VARCHAR(10) | NOT NULL | Code langue (ISO 639-1) |
| `notifications_enabled` | BOOLEAN | NOT NULL, DEFAULT TRUE | Activation notifications |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Date de création |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Date de mise à jour |

**Contraintes :**
- Foreign key vers `users(id)` avec `ON DELETE CASCADE`
- Contrainte UNIQUE sur `user_id` (un utilisateur = une préférence)

#### `user_sessions`
Sessions utilisateur et refresh tokens.

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Identifiant unique |
| `user_id` | UUID | NOT NULL, FK → users(id) | Référence à l'utilisateur |
| `token_id` | VARCHAR(255) | NOT NULL, UNIQUE | Identifiant unique du token |
| `token_hash` | VARCHAR(128) | NOT NULL, UNIQUE | Hash SHA-256 du refresh token |
| `ip_address` | VARCHAR(45) | NOT NULL | Adresse IP du client |
| `user_agent` | VARCHAR(500) | NOT NULL | User-Agent du client |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Date de création |
| `expires_at` | TIMESTAMP | NOT NULL | Date d'expiration |
| `revoked_at` | TIMESTAMP | NULL | Date de révocation |
| `replaced_by_token_id` | VARCHAR(255) | NULL | Token de remplacement (rotation) |
| `revocation_reason` | VARCHAR(255) | NULL | Raison de la révocation |

**Index :**
- `idx_user_sessions_user_id` sur `user_id`
- `idx_user_sessions_expires_at` sur `expires_at`

**Contraintes :**
- Foreign key vers `users(id)` avec `ON DELETE CASCADE`
- Contrainte UNIQUE sur `token_id` et `token_hash`

## Relations

```
clients (1) ──< (N) users
users (1) ──< (1) user_preferences
users (1) ──< (N) user_sessions
```

## Migrations Flyway

Les migrations sont gérées par Flyway et se trouvent dans :
- Production : `backend/src/main/resources/db/migration/`
- Tests : `backend/src/test/resources/db/migration/`

### Convention de nommage
- Format : `V{version}__{description}.sql`
- Exemple : `V1__init_product_tables.sql`

### Migration initiale
La migration `V1__init_product_tables.sql` crée :
1. L'extension PostgreSQL `pgcrypto` (pour `gen_random_uuid()`)
2. Les tables `clients`, `users`, `user_preferences`, `user_sessions`
3. Les index nécessaires

## Soft Delete

Les tables `clients` et `users` implémentent le **soft delete** via la colonne `deleted_at` :
- `deleted_at IS NULL` : Entité active
- `deleted_at IS NOT NULL` : Entité supprimée logiquement

Les requêtes doivent filtrer sur `deleted_at IS NULL` pour exclure les entités supprimées.

## Sécurité

### Mots de passe
- Stockés sous forme de hash BCrypt (via Spring Security)
- Jamais stockés en clair
- Validation côté API avant hachage (8+ caractères, majuscule, minuscule, chiffre, caractère spécial)

### Refresh Tokens
- Stockés sous forme de hash SHA-256 dans `token_hash`
- Jamais stockés en clair
- Rotation automatique lors du renouvellement

## Index et Performances

### Index créés
- `clients` : `name`, `status`
- `users` : `client_id`, `status`
- `user_sessions` : `user_id`, `expires_at`

### Optimisations recommandées
- Index composite sur `users(client_id, deleted_at)` pour les requêtes filtrées
- Index sur `user_sessions(expires_at)` pour le nettoyage automatique
- Index sur `users(email, deleted_at)` pour les recherches par email

## Maintenance

### Nettoyage des sessions expirées
Les sessions expirées sont automatiquement supprimées lors de :
- La connexion d'un utilisateur (`AuthService.login()`)
- Le renouvellement d'un token (`AuthService.refreshToken()`)

### Sauvegarde
- Recommandation : Sauvegarde quotidienne PostgreSQL
- Rétention : 30 jours minimum

## Environnements

### Développement
- Base de données : PostgreSQL locale
- Configuration : `application.yaml`
- Variables d'environnement : `DB_HOST`, `DB_PORT`, `DB_NAME`, etc.

### Tests
- Base de données : H2 en mémoire
- Migrations Flyway : Désactivées
- Schéma créé via JPA/Hibernate

## Extensions PostgreSQL

- `pgcrypto` : Pour la génération d'UUIDs (`gen_random_uuid()`)

