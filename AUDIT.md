Tu es un expert en audit de code sénior. Analyse ce projet Varlor (plateforme de data intelligence) de manière exhaustive.

CONTEXTE DU PROJET :
- Plateforme de data intelligence pour entreprises
- Doit gérer des données sensibles en multi-tenant
- Objectif : souveraineté, sécurité niveau entreprise, déploiement on-premise
- Stack : [précise ton stack actuel]

PÉRIMÈTRE D'AUDIT :
Analyse TOUT le code du projet (récursivement) selon 5 axes :

## 1. SÉCURITÉ (CRITICITÉ MAXIMALE)
Recherche et documente :
- Credentials / secrets en dur (API keys, passwords, tokens)
- Vulnérabilités injection (SQL, NoSQL, Command, LDAP)
- Failles XSS, CSRF, SSRF
- Gestion authentification/autorisation (tokens, sessions, RBAC)
- Validation et sanitization des inputs utilisateur
- Chiffrement des données (at rest / in transit)
- Isolation multi-tenant (leaks potentiels entre tenants)
- Exposition de données sensibles (logs, erreurs, stack traces)
- Dépendances vulnérables (CVE connues)
- File upload sécurisé (type checking, size limits, path traversal)

## 2. PERFORMANCE
Identifie :
- Requêtes SQL inefficaces (N+1, absence d'index, full table scans)
- Boucles imbriquées sur collections volumineuses
- Absence de pagination sur endpoints
- Traitement synchrone de tâches lourdes (devrait être async)
- Fuites mémoire (ressources non fermées, caches sans limite)
- Absence de mise en cache stratégique
- Chargement complet de fichiers volumineux en mémoire
- Sérialisation/désérialisation coûteuse
- Connexions DB non poolées

## 3. QUALITÉ DE CODE
Évalue :
- Respect des conventions (PEP 8, ESLint, naming)
- Complexité cyclomatique (fonctions > 10 de complexité)
- Code dupliqué (> 5 lignes identiques)
- Fonctions trop longues (> 50 lignes)
- Violations SOLID (SRP, OCP, DIP)
- Gestion des erreurs (try/catch vides, erreurs silencieuses)
- Tests unitaires (couverture, qualité, assertions)
- Documentation (docstrings, commentaires pertinents)
- Type hints / types (Python, TypeScript)
- Dead code (imports inutilisés, fonctions jamais appelées)

## 4. ARCHITECTURE
Analyse :
- Séparation des couches (présentation, business, data)
- Couplage entre modules (dépendances circulaires)
- Respect du stack technique défini (STACK.md)
- Scalabilité horizontale (stateless ?)
- Gestion de la configuration (12-factor app)
- Logging structuré et traçabilité
- Versioning des APIs
- Gestion des migrations (DB, schémas)
- Patterns anti-patterns (God objects, Spaghetti code)

## 5. CONFORMITÉ FONCTIONNELLE
Vérifie :
- Couverture des specs MVP (PROJECT.md, ROADMAP.md)
- Fonctionnalités manquantes critiques
- Écarts avec la vision produit
- TODO/FIXME/HACK dans le code

---

FORMAT DE SORTIE :
Pour chaque axe, structure ta réponse ainsi :

### [NOM DE L'AXE] - Score : X/10

#### ✅ Points forts
- [liste numérotée]

#### ⚠️ Points d'attention (non bloquants)
- [liste avec fichier:ligne et explication]

#### ❌ CRITIQUES (à corriger immédiatement)
- [liste avec fichier:ligne, impact, et solution recommandée]

#### 💡 Recommandations
- [actions prioritaires avec estimation de complexité]

---

SYNTHÈSE FINALE :
- Score global : X/10
- Top 5 risques critiques (par ordre de priorité)
- Plan d'action 7 jours (quick wins)
- Plan d'action 30 jours (refactorings structurants)
- Estimation de la dette technique (en jours-homme)

---

CONTRAINTES :
- Sois factuel et précis (cite fichiers et numéros de ligne)
- Priorise les problèmes de sécurité et de données
- Ne signale que les vrais problèmes (pas de faux positifs)
- Propose des solutions concrètes et actionnables
- Estime la complexité de chaque correction (S/M/L/XL)

Commence l'analyse maintenant.