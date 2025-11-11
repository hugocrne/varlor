# AnalysisController

Le contrôleur `AnalysisController` expose le pipeline de prétraitement de Varlor Calculations API. Il reçoit des jeux de données semi-structurés en JSON ou YAML, les valide, les convertit en `Dataset`, puis déclenche le moteur `DataPreprocessor`. Le résultat complet est renvoyé à la gateway afin d’alimenter les étapes suivantes de la chaîne analytique.

## Endpoint `/api/analyses/preprocess`

- **Méthode** : `POST`
- **Route** : `/api/analyses/preprocess`
- **Content-Type supportés** : `application/json`, `application/x-yaml`
- **Accept** : JSON par défaut (`application/json`), YAML (`application/x-yaml`) si explicitement demandé
- **Réponse** : `PreprocessingResult` sérialisé (dataset nettoyé, dataset des outliers, rapport)

Le contrôleur détermine le format d’entrée grâce au header `Content-Type` et renvoie la réponse au format demandé via le header `Accept`. Le champ `data_descriptor.content_type` est vérifié pour s’assurer que la gateway annonce un type cohérent avec le corps reçu.

### Exemple de requête (JSON)

```http
POST /api/analyses/preprocess HTTP/1.1
Content-Type: application/json
Accept: application/json

{
  "data_descriptor": {
    "origin": "inline",
    "content_type": "application/json",
    "autodetect": false
  },
  "options": {
    "drop_outliers_percent": 1.5
  },
  "data": [
    { "temperature": 20.5, "status": "nominal" },
    { "temperature": 19.8, "status": "alert" },
    { "temperature": 21.0, "status": "nominal" }
  ]
}
```

### Exemple de requête (YAML)

```http
POST /api/analyses/preprocess HTTP/1.1
Content-Type: application/x-yaml
Accept: application/x-yaml

---
data_descriptor:
  origin: inline
  content_type: application/x-yaml
  autodetect: false
options:
  drop_outliers_percent: 1.5
data:
  - temperature: 20.5
    status: nominal
  - temperature: 19.8
    status: alert
  - temperature: 21.0
    status: nominal
```

### Exemple de réponse (JSON)

```json
{
  "cleaned_dataset": {
    "columns": ["temperature", "status"],
    "rows": [
      {
        "values": {
          "temperature": 20.5,
          "status": "nominal"
        }
      },
      {
        "values": {
          "temperature": 21.0,
          "status": "nominal"
        }
      }
    ]
  },
  "outliers_dataset": {
    "columns": ["temperature", "status"],
    "rows": [
      {
        "values": {
          "temperature": 19.8,
          "status": "alert"
        }
      }
    ]
  },
  "report": {
    "input_row_count": 3,
    "output_row_count": 2,
    "outliers_removed": 1,
    "missing_values_replaced": 0,
    "normalized_fields": ["temperature", "status"]
  }
}
```

## Rôle du champ `content_type`

Le champ `data_descriptor.content_type` est renseigné par le client et validé par la gateway avant d’être envoyé à `calculations-api`. Ce champ doit refléter le format réel du corps de la requête. Si un décalage est détecté (ex. annonce de `text/csv` mais corps JSON), le contrôleur retourne `422 Unprocessable Entity`.

## Flux de traitement

1. **Client** – émet une requête JSON ou YAML et décrit le format via `content_type`.
2. **Gateway** – contrôle la cohérence et transmet la requête à `calculations-api`.
3. **Calculations-API (AnalysisController)** – valide le header `Content-Type`, convertit le payload en `Dataset` et appelle `DataPreprocessor`.
4. **DataPreprocessor** – normalise les colonnes, sépare les outliers, calcule un rapport.
5. **Réponse** – renvoie `PreprocessingResult` (dataset nettoyé, outliers, rapport) dans le format demandé.

### Schéma ASCII

```
Input (JSON/YAML)
     ↓
Validation du format
     ↓
Conversion en Dataset
     ↓
DataPreprocessor.process()
     ↓
Retour du PreprocessingResult (cleaned + outliers + report)
```

## Codes de statut HTTP

| Code | Signification | Exemple de message |
|------|---------------|--------------------|
| 200  | Prétraitement réussi | Résultat complet retourné (cleaned/outliers/report) |
| 400  | Requête mal formée | JSON/YAML invalide, header `Content-Type` manquant |
| 422  | Incohérence détectée | `content_type` déclaré différent du corps effectif |
| 500  | Erreur interne | Exception dans `DataPreprocessor` (ex. multiplicateur invalide) |

## Notes supplémentaires

- L’API respecte le header `Accept` pour renvoyer le résultat en JSON ou en YAML.
- Le traitement est non destructif : le dataset d’entrée n’est jamais modifié directement, toutes les transformations sont réalisées sur des copies.
- Les tests d’intégration `tests/controllers/test_AnalysisController.cpp` couvrent l’ensemble des scénarios décrits ci-dessus.
