# Contrats API - Varlor Backend

## Base URL
```
https://api.varlor.com/api/v1
```
**Local Development**: `http://localhost:3001/api/v1`

## Authentification

### Headers requis pour les routes protégées
```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

### Refresh Token
```http
Cookie: refresh_token=<http_only_cookie>
```

---

## 1. Authentification

### POST /auth/login
Connexion utilisateur

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "role": "admin",
      "tenantId": "tenant_123",
      "createdAt": "2025-01-01T00:00:00.000Z",
      "lastLoginAt": "2025-01-15T10:30:00.000Z"
    }
  }
}
```

**Cookies:**
```
refresh_token=<jwt_token>; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=604800
```

### POST /auth/refresh
Rafraîchir le token d'accès

**Request:** Token via cookie `refresh_token`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "role": "admin"
    }
  }
}
```

### POST /auth/logout
Déconnexion utilisateur

**Request:** Token via cookie `refresh_token`

**Response (200):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

**Cookies Clear:**
```
refresh_token=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0
```

---

## 2. Utilisateurs

### GET /users/me
Informations utilisateur courant

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "role": "admin",
    "tenantId": "tenant_123",
    "createdAt": "2025-01-01T00:00:00.000Z",
    "lastLoginAt": "2025-01-15T10:30:00.000Z"
  }
}
```

---

## 3. Datasets

### POST /datasets/upload
Upload d'un fichier dataset

**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: multipart/form-data`

**Rate Limit:** 10 uploads par heure par utilisateur

**Request:**
```multipart
name: "file"
filename: "data.csv"
content-type: text/csv
<file_data>

name: "dataset_name"
Sample Dataset

name: "description"
Description of my dataset
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "name": "Sample Dataset",
    "fileName": "data.csv",
    "fileSize": 1048576,
    "fileFormat": "CSV",
    "status": "UPLOADING",
    "uploadedAt": "2025-01-15T10:30:00.000Z"
  }
}
```

### GET /datasets/:id
Métadonnées d'un dataset

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "name": "Sample Dataset",
    "fileName": "data.csv",
    "fileSize": 1048576,
    "fileFormat": "CSV",
    "rowCount": 5000,
    "columnCount": 10,
    "status": "READY",
    "processingTimeMs": 2500,
    "qualityScore": 0.85,
    "uploadedAt": "2025-01-15T10:30:00.000Z",
    "processedAt": "2025-01-15T10:32:00.000Z"
  }
}
```

### GET /datasets/:id/preview
Aperçu des 20 premières lignes

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "columns": [
      {"name": "id", "type": "NUMBER", "sampleValues": [1, 2, 3]},
      {"name": "name", "type": "TEXT", "sampleValues": ["Alice", "Bob", "Charlie"]},
      {"name": "date", "type": "DATE", "sampleValues": ["2025-01-01", "2025-01-02"]}
    ],
    "rows": [
      [1, "Alice", "2025-01-01"],
      [2, "Bob", "2025-01-02"],
      [3, "Charlie", "2025-01-03"]
    ],
    "totalRows": 5000
  }
}
```

---

## 4. Nettoyage de Données

### POST /datasets/:id/cleaning/start
Démarrer le processus de nettoyage

**Headers:** `Authorization: Bearer <token>`

**Response (202):**
```json
{
  "success": true,
  "data": {
    "jobId": "clean_123456",
    "status": "processing",
    "startedAt": "2025-01-15T10:35:00.000Z"
  }
}
```

### GET /datasets/:id/cleaning/status
Statut du nettoyage

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "status": "completed",
    "progress": 100,
    "startedAt": "2025-01-15T10:35:00.000Z",
    "completedAt": "2025-01-15T10:37:00.000Z",
    "processingTimeMs": 120000
  }
}
```

### GET /datasets/:id/cleaning/results
Résultats du nettoyage (paginé)

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `page`: Numéro de page (défaut: 1)
- `limit`: Résultats par page (défaut: 50)
- `column`: Filtrer par colonne (optionnel)
- `severity`: Filtrer par sévérité (LOW/MEDIUM/HIGH)

**Response (200):**
```json
{
  "success": true,
  "data": {
    "summary": {
      "totalIssues": 150,
      "correctionsApplied": 142,
      "qualityScore": 0.92,
      "duplicateRows": 5
    },
    "logs": [
      {
        "id": 1,
        "columnName": "date",
        "issueType": "INVALID_FORMAT",
        "originalValue": "01/15/2025",
        "correctedValue": "2025-01-15",
        "severity": "MEDIUM",
        "rule": "Date format normalization"
      }
    ],
    "meta": {
      "currentPage": 1,
      "totalPages": 3,
      "totalItems": 150,
      "itemsPerPage": 50
    }
  }
}
```

---

## 5. Analyse Statistique

### POST /datasets/:id/analysis/start
Démarrer l'analyse statistique

**Headers:** `Authorization: Bearer <token>`

**Response (202):**
```json
{
  "success": true,
  "data": {
    "jobId": "analysis_123456",
    "status": "processing"
  }
}
```

### GET /datasets/:id/analysis/results
Résultats complets de l'analyse

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "datasetId": 123,
    "analysisStatus": "completed",
    "columns": [
      {
        "name": "age",
        "type": "NUMBER",
        "stats": {
          "count": 5000,
          "mean": 35.5,
          "median": 34.0,
          "stdDev": 12.3,
          "min": 18,
          "max": 75,
          "quartiles": [25, 34, 45]
        },
        "outliers": [
          {"value": 75, "index": 1234, "zScore": 3.2},
          {"value": 16, "index": 2345, "zScore": -1.6}
        ]
      }
    ],
    "correlations": [
      {"column1": "age", "column2": "salary", "coefficient": 0.65}
    ]
  }
}
```

### GET /datasets/:id/analysis/chart/:columnName
Données pour visualisation d'une colonne

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `type`: Type de graphique (histogram/boxplot/timeseries)

**Response (200):**
```json
{
  "success": true,
  "data": {
    "columnName": "age",
    "chartType": "histogram",
    "chartData": [
      {"range": "18-25", "count": 850},
      {"range": "26-35", "count": 1500},
      {"range": "36-45", "count": 1800},
      {"range": "46-55", "count": 650},
      {"range": "56+", "count": 200}
    ]
  }
}
```

---

## 6. Insights IA

### GET /datasets/:id/ai-insights
Insights générés par l'IA

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "insights": [
      {
        "category": "data_quality",
        "title": "High missing data rate in 'phone' column",
        "description": "45% of phone numbers are missing, which may impact analysis accuracy.",
        "severity": "HIGH",
        "recommendation": "Consider collecting phone numbers through a separate process or remove this field from analysis."
      },
      {
        "category": "patterns",
        "title": "Strong correlation between age and income",
        "description": "Income increases by approximately $2,000 per year of age until age 50.",
        "severity": "LOW",
        "recommendation": "This pattern could be useful for income prediction models."
      }
    ],
    "suggestedCharts": [
      {"column": "age", "type": "histogram"},
      {"column": "income", "type": "boxplot"},
      {"columns": ["age", "income"], "type": "scatter"}
    ]
  }
}
```

### POST /datasets/:id/ai-insights/generate
Déclencher la génération d'insights

**Headers:** `Authorization: Bearer <token>`

**Rate Limit:** 5 générations par heure par utilisateur

**Response (202):**
```json
{
  "success": true,
  "data": {
    "jobId": "ai_123456",
    "status": "processing"
  }
}
```

---

## 7. Rapports

### GET /datasets/:id/report/data
Données pour prévisualisation du rapport

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "dataset": {
      "name": "Sample Dataset",
      "rowCount": 5000,
      "columnCount": 10,
      "qualityScore": 0.92
    },
    "summary": {
      "cleaningIssues": 150,
      "correctionsApplied": 142,
      "outliersDetected": 23
    },
    "topInsights": [
      "Dataset shows excellent overall data quality",
      "Age distribution follows normal pattern",
      "Strong age-income correlation detected"
    ]
  }
}
```

### POST /datasets/:id/report/generate
Générer le rapport PDF

**Headers:** `Authorization: Bearer <token>`

**Rate Limit:** 3 rapports par heure par utilisateur

**Request:**
```json
{
  "includeCharts": true,
  "includeInsights": true,
  "template": "standard"
}
```

**Response (202):**
```json
{
  "success": true,
  "data": {
    "reportId": "report_123456",
    "status": "generating",
    "estimatedTime": 30
  }
}
```

### GET /datasets/:id/report/download
Télécharger le rapport PDF

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `token`: Token de téléchargement unique

**Response (200):**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="dataset-123-report.pdf"
Content-Length: 1048576

<binary_pdf_data>
```

---

## Gestion des Erreurs

### Format Réponse Erreur
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid file format. Only CSV and Excel files are supported.",
    "details": {
      "field": "file",
      "supportedFormats": ["csv", "xlsx", "xls"]
    }
  }
}
```

### Codes d'Erreur Communs
- `400 BAD_REQUEST`: Requête invalide
- `401 UNAUTHORIZED`: Non authentifié
- `403 FORBIDDEN`: Accès refusé
- `404 NOT_FOUND`: Ressource introuvable
- `429 TOO_MANY_REQUESTS`: Rate limit dépassé
- `500 INTERNAL_ERROR`: Erreur serveur

### Rate Limiting Headers
```http
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1705123200
```

---

## Pagination

### Format Réponse Paginée
```json
{
  "success": true,
  "data": {
    "items": [...],
    "meta": {
      "currentPage": 1,
      "totalPages": 5,
      "totalItems": 250,
      "itemsPerPage": 50,
      "hasNext": true,
      "hasPrev": false
    }
  }
}
```

### Paramètres de Pagination
- `page`: Numéro de page (défaut: 1)
- `limit`: Items par page (défaut: 50, max: 100)

---

## Webhooks (Future)

### Configuration
```json
{
  "url": "https://your-app.com/webhooks/varlor",
  "events": ["dataset.processed", "analysis.completed"],
  "secret": "webhook_secret"
}
```

### Événements
- `dataset.uploaded`: Dataset uploadé avec succès
- `dataset.processed`: Nettoyage terminé
- `analysis.completed`: Analyse statistique terminée
- `report.generated`: Rapport PDF généré

---

## Versioning

L'API utilise le versioning dans l'URL. La version actuelle est `v1`.

**Rétrocompatibilité:** Les versions majeures sont maintenues pendant 12 mois.

**Dépréciation:** Les champs dépréciés incluent un header `Deprecation` avec la date de fin de support.