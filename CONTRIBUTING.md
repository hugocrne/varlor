# Guide de Contribution

Merci de votre intérêt pour contribuer au projet Varlor ! Ce guide vous aidera à comprendre nos conventions et processus.

## Table des Matières

1. [Conventions de Code](#conventions-de-code)
2. [Processus de Contribution](#processus-de-contribution)
3. [Standards de Documentation](#standards-de-documentation)
4. [Tests](#tests)
5. [Checklist avant Commit](#checklist-avant-commit)

## Conventions de Code

### Kotlin Style Guide

Nous suivons les conventions officielles Kotlin :
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

#### Points clés :

**Nommage :**
- Classes : `PascalCase` (ex: `UserService`)
- Fonctions/variables : `camelCase` (ex: `findById`)
- Constantes : `UPPER_SNAKE_CASE` (ex: `SECURITY_SCHEME_NAME`)
- Packages : `lowercase` (ex: `com.varlor.backend.product`)

**Formatage :**
- Indentation : 4 espaces
- Longueur de ligne : 120 caractères maximum
- Pas de point-virgule en fin de ligne

**Structure :**
```kotlin
package com.varlor.backend.product.service

import ...

/**
 * Documentation KDoc
 */
@Service
class UserService(
    private val repository: UserRepository
) {
    fun method(): ReturnType { ... }
}
```

### Architecture

**Séparation des responsabilités :**
- **Controllers** : Gestion des requêtes HTTP, validation, transformation DTO
- **Services** : Logique métier
- **Repositories** : Accès aux données
- **DTOs** : Contrats API (jamais d'entités JPA exposées)

**Patterns utilisés :**
- Repository Pattern
- DTO Pattern
- Service Layer Pattern
- Soft Delete Pattern

## Processus de Contribution

### 1. Créer une Branche

```bash
git checkout -b feature/nom-de-la-fonctionnalite
# ou
git checkout -b fix/description-du-bug
```

**Conventions de nommage des branches :**
- `feature/` : Nouvelles fonctionnalités
- `fix/` : Corrections de bugs
- `docs/` : Documentation
- `refactor/` : Refactoring
- `test/` : Ajout/modification de tests

### 2. Développer

- Écrire du code propre et documenté
- Suivre les conventions de code
- Ajouter des tests pour les nouvelles fonctionnalités
- Mettre à jour la documentation si nécessaire

### 3. Commit

**Format des messages de commit :**
```
type(scope): description courte

Description détaillée si nécessaire
```

**Types :**
- `feat` : Nouvelle fonctionnalité
- `fix` : Correction de bug
- `docs` : Documentation
- `style` : Formatage (pas de changement de code)
- `refactor` : Refactoring
- `test` : Tests
- `chore` : Tâches de maintenance

**Exemples :**
```
feat(auth): ajouter endpoint de validation de token

fix(user): corriger validation email unique

docs(api): améliorer documentation Swagger
```

### 4. Push et Pull Request

```bash
git push origin feature/nom-de-la-fonctionnalite
```

**Créer une Pull Request :**
1. Aller sur GitHub/GitLab
2. Créer une PR depuis votre branche vers `main`
3. Remplir le template de PR
4. Assigner des reviewers

**Template de PR :**
- Description de la modification
- Type de changement (feature/fix/docs/etc.)
- Tests effectués
- Checklist complétée

### 5. Review

- Répondre aux commentaires
- Faire les modifications demandées
- Répondre "Done" aux commentaires résolus

### 6. Merge

Une fois approuvée, la PR sera mergée dans `main`.

## Standards de Documentation

### KDoc (Documentation Kotlin)

**Tous les services doivent avoir :**
```kotlin
/**
 * Description courte du service.
 *
 * Description détaillée si nécessaire.
 *
 * @property repository Description de la propriété
 */
@Service
class UserService(...) {
    /**
     * Description de la méthode.
     *
     * @param param Description du paramètre
     * @return Description du retour
     * @throws Exception Quand et pourquoi
     */
    fun method(param: Type): ReturnType { ... }
}
```

### Swagger/OpenAPI

**Tous les contrôleurs doivent avoir :**
- `@Tag` avec description
- `@Operation` sur chaque endpoint avec `summary` et `description`
- `@ApiResponses` documentant les codes de réponse (200, 400, 401, 500)

**Tous les DTOs doivent avoir :**
- `@Schema` sur la classe avec `description`
- `@Schema` sur chaque champ avec `description` et `example`

### Exemples

Voir les fichiers existants pour des exemples :
- `AuthController.kt` : Exemple de documentation Swagger complète
- `AuthService.kt` : Exemple de documentation KDoc complète

## Tests

### Types de Tests

1. **Tests Unitaires** : Services, utilitaires
2. **Tests d'Intégration** : Controllers, repositories
3. **Tests de Contrat** : Validation des DTOs

### Structure

```
backend/src/test/kotlin/com/varlor/backend/
├── product/
│   ├── controller/
│   ├── service/
│   └── security/
└── analysis/
    ├── controller/
    └── service/
```

### Exécution

```bash
# Tous les tests
./gradlew test

# Tests spécifiques
./gradlew test --tests "com.varlor.backend.product.service.AuthServiceTest"

# Avec couverture
./gradlew test jacocoTestReport
```

### Couverture

- Objectif : 80% minimum
- Services critiques : 90% minimum

## Checklist avant Commit

### Code
- [ ] Code conforme aux conventions Kotlin
- [ ] Pas de warnings du compilateur
- [ ] Pas de code mort (unused imports, variables, etc.)
- [ ] Gestion d'erreurs appropriée

### Documentation
- [ ] KDoc ajouté sur les nouvelles classes/services
- [ ] Swagger/OpenAPI documenté pour les nouveaux endpoints
- [ ] `@Schema` ajouté sur les nouveaux DTOs
- [ ] README mis à jour si nécessaire

### Tests
- [ ] Tests unitaires ajoutés pour les nouvelles fonctionnalités
- [ ] Tests d'intégration ajoutés pour les nouveaux endpoints
- [ ] Tous les tests passent (`./gradlew test`)
- [ ] Couverture de code maintenue

### Sécurité
- [ ] Pas de secrets hardcodés
- [ ] Validation des entrées utilisateur
- [ ] Gestion appropriée des erreurs (pas d'exposition d'informations sensibles)

### Performance
- [ ] Pas de requêtes N+1
- [ ] Index de base de données appropriés
- [ ] Pagination pour les listes volumineuses

## Ressources

- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- [Spring Boot Best Practices](https://spring.io/guides)
- [REST API Design](https://restfulapi.net/)
- [OpenAPI Specification](https://swagger.io/specification/)

## Questions ?

N'hésitez pas à ouvrir une issue ou contacter l'équipe pour toute question.

