# Requirements - Algorithmic Analysis MVP

## 1. Statistics Calculation Scope

**Decision**: Stats numériques uniquement sur colonnes numériques

### Colonnes numériques :
- Min, max, mean, median, écart-type
- Distribution (histogramme)

### Colonnes texte/catégorielles :
- Nombre de valeurs uniques
- Top N valeurs fréquentes
- Comptage par valeur

### Colonnes dates :
- Date min, date max
- Plage temporelle (ex: "3 mois de données")

### Colonnes mixtes :
- ❌ Pas de tentative de stats numériques
- Traiter comme texte par défaut
- Flag "type mixte détecté" dans rapport qualité

---

## 2. Top N Frequency Values

**Decision**: N=10 fixe pour MVP

- Pas de configuration utilisateur
- 10 valeurs les plus fréquentes
- Afficher pourcentage à côté de chaque valeur
- Si moins de 10 valeurs uniques, afficher toutes

### Affichage :
```
Top 10 valeurs - Colonne "Pays"
1. France (45%)
2. Allemagne (23%)
3. Espagne (12%)
...
```

**Future (Alpha)** : Slider pour ajuster N

---

## 3. Outlier Detection Threshold

**Decision**: Uniquement ±3 écarts-types pour MVP

- ❌ Pas de méthode IQR
- ❌ Pas de seuil configurable
- ❌ Pas d'algorithmes avancés (isolation forest, etc.)

### Règle simple :
- Valeur < (moyenne - 3σ) → outlier
- Valeur > (moyenne + 3σ) → outlier

**Future (Alpha/V1)** :
- Méthode IQR optionnelle
- Seuils configurables
- Détection avancée

---

## 4. Outlier Presentation

**Decision**: Comptage + échantillon

### Affichage pour chaque colonne concernée :
- Nombre total d'outliers
- Pourcentage du total
- 5 valeurs les plus extrêmes (échantillon)
- Indication si haute ou basse

### Exemple :
```
⚠️ Colonne "Montant" : 23 outliers détectés (2.3%)

Valeurs extrêmes hautes (5) :
- 99,999 (ligne 342)
- 87,500 (ligne 1,203)
- 75,000 (ligne 567)
...

Valeurs extrêmes basses (2) :
- -500 (ligne 89)
- -120 (ligne 1,456)
```

- ❌ Pas de liste complète - Trop verbeux, échantillon suffit

---

## 5. Date Column Detection for Time Charts

**Decision**: Auto-détection + première colonne date

### Comportement MVP :
- Réutiliser détection de type de l'import (étape B)
- Si une seule colonne date → utiliser automatiquement
- Si plusieurs colonnes dates → utiliser la première détectée
- Tracer colonnes numériques contre cette date

- ❌ Pas de sélection utilisateur pour MVP

### Affichage :
```
📅 Axe temporel : colonne "Date_Commande"
```

**Future (Alpha)** : Dropdown pour choisir colonne date

---

## 6. Chart Interactivity Level

**Decision**: Basic interactive

### Interactions MVP :
- ✅ Hover tooltips (valeur affichée au survol)
- ✅ Zoom basique (si supporté par librairie)
- ❌ Pas de pan/drag complexe
- ❌ Pas de sélection de plages
- ❌ Pas d'export chart

### Librairie : ECharts (déjà dans STACK.md)
- Interactivité native
- Léger et performant
- Tooltips par défaut

**Raison** : Interactivité basique améliore UX sans complexité

---

## 7. UI Integration

**Decision**: Page/onglet "Analyse" dédié dans le détail dataset

### Flow utilisateur :
1. Upload → Preview → Confirm
2. Redirect vers `/dashboard/datasets/{id}`
3. Page détail avec onglets :
   - Aperçu (preview données)
   - Qualité (résultats nettoyage)
   - **Analyse** ← Stats + Charts ici
   - Rapport (futur - étape F)

### Déclenchement :
- Analyse calculée automatiquement après cleaning (background)
- Status : "Analyse en cours..." → "Terminé"
- Résultats affichés quand prêts

### Structure page Analyse :
```
┌─────────────────────────────────────────────┐
│  📊 Statistiques par Colonne                │
│  [Section expandable par colonne]           │
├─────────────────────────────────────────────┤
│  📈 Graphiques                              │
│  [Histogrammes] [Barres] [Courbes temps]    │
├─────────────────────────────────────────────┤
│  ⚠️ Outliers Détectés                       │
│  [Liste colonnes avec outliers]             │
└─────────────────────────────────────────────┘
```

---

## 8. MVP Exclusions

**Decision**: Liste explicite des exclusions

### ❌ Hors scope MVP :
- ❌ Corrélation entre colonnes
- ❌ Grouping/aggregation par catégorie
- ❌ Export de graphiques (PNG, PDF)
- ❌ Comparaison entre datasets
- ❌ Filtres sur les données
- ❌ Analyses conditionnelles (si X alors Y)
- ❌ Prédictions/forecasting
- ❌ Clustering/segmentation
- ❌ Tests statistiques (t-test, chi², etc.)
- ❌ Régression
- ❌ Personnalisation des graphiques
- ❌ Sélection de colonnes à analyser
- ❌ Analyses sur sous-échantillons

**Raison** : Reporté en Alpha/Beta/V1 selon roadmap sections futures

---

## Visual Assets

No visual assets provided.
