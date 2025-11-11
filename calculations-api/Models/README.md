# Modèles de données - Moteur de pré-analyse

Ce dossier contient les modèles internes utilisés par le moteur de pré-analyse (`DataPreprocessor`) du projet `calculations-api`. Ces structures représentent les données à traiter et les résultats des opérations de nettoyage.

## Vue d'ensemble

La couche `Models` fournit les structures de base pour :
- **Représenter** les données brutes et transformées
- **Typer** les champs de données
- **Tracer** les opérations de pré-traitement effectuées

Ces modèles sont conçus pour être :
- **Légers** : dépendances limitées à la STL C++20
- **Performants** : utilisation de structures optimisées (unordered_map, variant)
- **Type-safe** : utilisation d'énumérations et de variants pour la sécurité des types
- **Extensibles** : architecture permettant l'ajout de fonctionnalités futures

## Architecture des modèles

### Hiérarchie des modèles

```
FieldType (enum)
    ↓
    └─→ Utilisé pour identifier le type des colonnes

DataPoint
    ├─→ Utilise FieldValue (variant) pour stocker les valeurs
    └─→ Représente une ligne individuelle

Dataset
    ├─→ Contient une collection de DataPoint
    ├─→ Gère les noms de colonnes
    └─→ Point d'entrée principal pour DataPreprocessor

PreprocessingReport
    └─→ Trace les transformations appliquées à un Dataset
```

### Relation entre les modèles

1. **FieldType** → Identifie le type de chaque colonne (Numeric, Text, Boolean, Unknown)
2. **DataPoint** → Stocke les valeurs d'une ligne avec des types variants
3. **Dataset** → Agence une collection de `DataPoint` avec leurs métadonnées (noms de colonnes)
4. **PreprocessingReport** → Enregistre les statistiques des opérations effectuées sur un `Dataset`

## Modèles détaillés

### 1. FieldType

**Fichier** : `FieldType.hpp`

**Rôle** : Énumération des types de données supportés pour les champs.

**Valeurs** :
- `Numeric` : Valeurs numériques (entier ou décimal)
- `Text` : Chaînes de caractères
- `Boolean` : Valeurs booléennes (vrai/faux)
- `Unknown` : Type non détecté ou indéterminé

**Utilisation** : Utilisée lors de la phase de détection automatique du schéma des données pour identifier le type de chaque colonne.

**Exemple** :
```cpp
varlor::models::FieldType type = varlor::models::FieldType::Numeric;
```

---

### 2. DataPoint

**Fichier** : `DataPoint.hpp`

**Rôle** : Représente une ligne individuelle du jeu de données.

**Structure** :
- Utilise `std::unordered_map<std::string, FieldValue>` pour un accès rapide aux champs par nom
- `FieldValue` est un `std::variant<double, std::string, bool, std::nullptr_t>` permettant de stocker différents types

**Fonctionnalités principales** :
- Ajout/modification/suppression de champs
- Accès rapide par nom de colonne (O(1) en moyenne)
- Support des valeurs nulles via `nullptr`
- Méthodes utilitaires : `size()`, `empty()`, `hasField()`

**Exemple** :
```cpp
varlor::models::DataPoint point;
point.setField("age", 30.0);
point.setField("name", std::string("Alice"));
point.setField("active", true);
point.setField("optional", nullptr);

auto age = point.getField("age");
if (age.has_value()) {
    double value = std::get<double>(age.value());
}
```

---

### 3. Dataset

**Fichier** : `Dataset.hpp`

**Rôle** : Représente l'ensemble complet des données à analyser.

**Structure** :
- Collection de `DataPoint` (chaque ligne)
- Liste des noms de colonnes (`std::vector<std::string>`)

**Fonctionnalités principales** :
- Gestion des lignes : ajout, récupération, suppression
- Gestion des colonnes : définition et modification des noms
- Itérateurs pour parcourir les `DataPoint`
- Compteurs : nombre de lignes et de colonnes
- Méthodes de nettoyage : `clear()`

**Exemple** :
```cpp
varlor::models::Dataset dataset;
dataset.addColumnName("name");
dataset.addColumnName("age");

varlor::models::DataPoint point;
point.setField("name", std::string("Bob"));
point.setField("age", 25.0);
dataset.addDataPoint(point);

// Parcourir le dataset
for (const auto& row : dataset) {
    auto name = row.getField("name");
    // ...
}
```

---

### 4. PreprocessingReport

**Fichier** : `PreprocessingReport.hpp`

**Rôle** : Fournit un résumé détaillé des opérations de pré-traitement effectuées.

**Statistiques enregistrées** :
- `inputRowCount` : Nombre de lignes en entrée
- `outputRowCount` : Nombre de lignes en sortie
- `outliersRemoved` : Nombre d'outliers supprimés
- `missingValuesReplaced` : Nombre de valeurs manquantes remplacées
- `normalizedFields` : Liste des champs normalisés

**Fonctionnalités principales** :
- Getters/Setters pour tous les compteurs
- Méthodes d'incrémentation : `incrementOutliersRemoved()`, `incrementMissingValuesReplaced()`
- Calcul automatique : `getRowsRemoved()` (différence entre entrée et sortie)
- Réinitialisation : `reset()`

**Exemple** :
```cpp
varlor::models::PreprocessingReport report;
report.setInputRowCount(1000);

// Pendant le traitement
report.incrementOutliersRemoved(50);
report.incrementMissingValuesReplaced(25);
report.addNormalizedField("age");
report.addNormalizedField("salary");

report.setOutputRowCount(950);

// Récupérer les statistiques
std::size_t removed = report.getRowsRemoved(); // 50
```

---

## Namespace et conventions

### Namespace

Tous les modèles sont dans l'espace de noms `varlor::models` pour éviter les collisions de noms.

### Conventions C++20

- **`#pragma once`** : Protection contre les inclusions multiples
- **Attributs modernes** : `[[nodiscard]]` pour les getters
- **Move semantics** : Support complet du déplacement pour les performances
- **STL uniquement** : Aucune dépendance externe (sauf Oat++ pour l'API)
- **Documentation Doxygen** : Tous les éléments publics sont documentés

### Bonnes pratiques

- Utiliser les références constantes pour les paramètres de lecture
- Utiliser le move semantics pour les grandes structures
- Vérifier l'existence des champs avant accès (via `hasField()` ou `getField()` qui retourne `std::optional`)
- Utiliser les itérateurs pour parcourir les collections

---

## Intégration avec DataPreprocessor

### Flux de traitement

```
1. Données brutes (CSV, JSON, etc.)
   ↓
2. Conversion → Dataset
   ↓
3. DataPreprocessor.process(Dataset)
   ├─→ Détection des types (FieldType)
   ├─→ Nettoyage des données (modification des DataPoint)
   ├─→ Suppression d'outliers (modification du Dataset)
   ├─→ Remplacement de valeurs manquantes
   └─→ Normalisation de champs
   ↓
4. Dataset nettoyé + PreprocessingReport
   ↓
5. Sérialisation JSON pour l'API
```

### Points d'intégration

- **Entrée** : `DataPreprocessor` reçoit un `Dataset` non nettoyé
- **Traitement** : Modification des `DataPoint` dans le `Dataset`
- **Sortie** : `Dataset` nettoyé + `PreprocessingReport` avec les statistiques
- **Sérialisation** : Les modèles seront convertis en DTOs Oat++ pour l'API REST

### Exemple d'utilisation future

```cpp
// Dans DataPreprocessor
varlor::models::Dataset dataset = loadFromCSV("data.csv");
varlor::models::PreprocessingReport report;

// Détecter les types
detectFieldTypes(dataset);

// Nettoyer les données
removeOutliers(dataset, report);
replaceMissingValues(dataset, report);
normalizeFields(dataset, report);

report.setInputRowCount(dataset.getRowCount());
// ... traitement ...
report.setOutputRowCount(dataset.getRowCount());

return {dataset, report};
```

---

## Tests unitaires

Les modèles sont couverts par des tests unitaires complets dans `tests/models/`.

### Exécution des tests

```bash
# Compiler et exécuter tous les tests
cd build
cmake ..
ninja
ctest

# Ou exécuter directement
./test_models
```

### Couverture

- ✅ **FieldType** : Valeurs de l'énumération, utilisation dans switch
- ✅ **DataPoint** : Ajout, lecture, suppression, modification de tous les types de champs
- ✅ **Dataset** : Gestion des lignes et colonnes, itérateurs, cohérence des données
- ✅ **PreprocessingReport** : Compteurs, incrémentations, calculs, réinitialisation

---

## Extensions futures

### Améliorations possibles

1. **Schéma de données** : Ajouter une classe `Schema` pour stocker les types détectés par colonne
2. **Validation** : Ajouter des méthodes de validation des données dans `DataPoint`
3. **Sérialisation** : Ajouter des méthodes de conversion vers JSON (Oat++ DTOs)
4. **Métadonnées** : Enrichir `Dataset` avec des informations supplémentaires (source, date, etc.)
5. **Performance** : Ajouter des méthodes de traitement par lots pour de gros volumes

### Notes d'implémentation

- Les modèles sont conçus pour être **immutables par défaut** (sauf via les méthodes explicites)
- Les performances sont optimisées pour des jeux de données de taille moyenne (milliers à millions de lignes)
- La mémoire est gérée automatiquement via les smart pointers et le RAII

---

## Références

- **C++20 Standard** : Utilisation des fonctionnalités modernes (variants, optionals, concepts)
- **STL** : Structures de données standard uniquement
- **Oat++ 1.3.0** : Framework web pour l'exposition via API REST (future intégration)

---

## Auteur

Modèles créés pour le projet Varlor - Moteur de pré-analyse des données.

