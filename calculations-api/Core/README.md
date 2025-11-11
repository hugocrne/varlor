# DataPreprocessor

## Rôle dans `calculations-api`
Le `DataPreprocessor` constitue la brique centrale du moteur de pré-analyse : il consolide, normalise et documente l’état du jeu de données avant toute analyse statistique ou modélisation. Chaque étape est effectuée de manière **non destructive** : le dataset d’origine n’est jamais modifié, et toutes les décisions sont traçables.

## Principe non destructif
- *Entrée immuable* : le `Dataset` fourni à `process()` est copié avant tout traitement.
- *Séparation des outliers* : aucune ligne n’est supprimée ; les valeurs extrêmes sont déplacées dans `PreprocessingResult::outliersDataset`.
- *Traçabilité `_meta`* : chaque `DataPoint` modifié contient une section `_meta` décrivant précisément les ajustements effectués.
- *Rapport exhaustif* : `PreprocessingReport` résume l’intégralité des actions (normalisations, imputations, outliers, lignes en entrée/sortie).

## Format d’entrée / sortie
- **Entrée** : `models::Dataset` brut (valeurs hétérogènes, champs manquants, types ambigus).
- **Sortie** : `models::PreprocessingResult` contenant :
  - `cleanedDataset` : données normalisées, valeurs imputées.
  - `outliersDataset` : lignes marquées comme outliers, conservées pour audit.
  - `report` : métriques agrégées (lignes, outliers, valeurs imputées, champs normalisés).

## Fonctions supportées
Le moteur `IndicatorEngine` complète le prétraitement en exposant des opérations analytiques
dynamiques. Les fonctions ci-dessous sont injectées dans le DSL supporté par `MathOperationParser`
et `ExpressionExecutor`.

| Fonction | Description |
|----------|-------------|
| `mean(field)` | Moyenne arithmétique sur la colonne `field`. |
| `median(field)` | Médiane (avec interpolation) de la colonne numérique. |
| `variance(field)` | Variance populationnelle de la colonne. |
| `stddev(field)` | Écart-type (racine carrée de la variance). |
| `min(field)` / `max(field)` | Valeur minimale / maximale observée. |
| `percentile(field, p)` | Percentile `p` en pourcentage (0-100) avec interpolation linéaire. |
| `correlation(fieldA, fieldB)` | Corrélation de Pearson entre deux colonnes. |

Ces fonctions peuvent être utilisées seules (`mean(price)`) ou combinées dans des expressions libres
(`(max(price) - min(price)) / mean(price)`). Les paramètres additionnels sont passés via
`OperationDefinition::params` lorsque nécessaire (par exemple `percentile` sans second argument).

## Métadonnées `_meta`
Le champ `_meta` s’appuie sur `MetaInfo`, une structure hiérarchique sérialisable en YAML. Les clés standard sont :
- `status/outlier`, `status/reason`, `status/method`
- `columns/<colonne>/imputation/imputed`
- `columns/<colonne>/imputation/reason`
- `columns/<colonne>/imputation/strategy`
- `columns/<colonne>/imputation/value`

Des attributs additionnels (`confidence`, `source`, etc.) peuvent être ajoutés sans modifier le moteur.

## Flux de données
```
Raw Dataset
    ↓ analyse & normalisation
Normalized Dataset + Column Profiles
    ↓ séparation IQR
Cleaned Dataset      Outliers Dataset
    ↓ imputation
Cleaned Dataset (imputed)
    ↓ agrégation
PreprocessingResult (cleanedDataset, outliersDataset, report)
```

## Évolutions prévues
- **Parallélisation** des phases de normalisation / imputation pour les gros volumes.
- **Nettoyage adaptatif** : stratégies d’imputation conditionnées par le profil statistique.
- **Règles métier configurables** : surcharges dédiées pour les colonnes sensibles.
- **Export YAML dédié** : sérialisation directe du `_meta` pour audit externe.


