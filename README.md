# Varlor Platform

## Backend

- **Stack :** Kotlin 1.9, Spring Boot 3.3.4 (servlet/Spring MVC), Gradle Kotlin DSL  
- **Modules :** `product` (authentification, gestion utilisateurs) et `analysis` (prétraitement et indicateurs)  
- **Principales dépendances :** Spring MVC, Spring Security OAuth2 Resource Server, JPA/Flyway, PostgreSQL, OpenAPI (springdoc), Jackson YAML

### Conventions clés

- API REST bâtie sur Spring MVC (stack servlet)  
- Contrats stables via DTOs dédiés, entités JPA jamais exposées  
- Gestion d’erreurs JSON unifiée (`error`, `message`, `timestamp`, `details`) sur `analysis` et `product`  
- Les mots de passe sont fournis en clair par l’API et hachés côté service avant stockage  
- Suppression des sessions utilisateurs : suppression physique (`DELETE /api/user-sessions/{id}`)

## Démarrage rapide

```bash
cd backend
./gradlew bootRun
```

Tests :

```bash
./gradlew test
```

La documentation OpenAPI est disponible sur `/swagger-ui/index.html`.