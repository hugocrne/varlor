# Specification: MVP Report Feature

## Goal

Add a "Rapport" tab to the dataset detail page that displays a comprehensive report preview and allows users to export it as a professionally formatted PDF document.

## User Stories

- As a data analyst, I want to see a consolidated report of my dataset so that I can quickly understand its characteristics and quality
- As a team member, I want to export a PDF report of a dataset so that I can share findings with stakeholders who don't have platform access
- As a business user, I want a professional-looking report with Varlor branding so that I can include it in presentations

## Core Requirements

### Report Tab
- New "Rapport" tab in dataset detail view (fourth tab after Apercu, Qualite, Analyse)
- Tab only visible when dataset status is READY and analysis is completed
- "Telecharger PDF" button prominently displayed within the tab

### Report Content
1. **Dataset Summary**
   - Dataset name
   - Original source filename
   - Upload date
   - Row count, column count
   - File size and format (CSV/Excel)
   - List of columns with detected types

2. **Quality Synthesis**
   - Global quality score (percentage)
   - Total corrections applied
   - Remaining issues count
   - Table showing only problematic columns (>5% missing or invalid)

3. **Key Charts (Auto-selected)**
   - Maximum 4-6 charts total
   - Histograms (max 2): Numeric columns with highest variance or detected outliers
   - Bar charts (max 2): Categorical columns with 5-20 categories
   - Time curves (max 1): Only if date column detected
   - Selection priority: outlier columns first, then highest variance

4. **Explanatory Text**
   - Transform AI insights into flowing narrative paragraphs
   - 2-3 paragraphs maximum
   - Professional, coherent prose (not bullet points)

### PDF Export
- Server-side generation using Puppeteer
- A4 portrait format
- Professional margins and print-optimized typography
- Header with Varlor logo
- Footer with "Genere par Varlor - [date]" and page number
- Brand colors applied throughout

## UI/UX Specifications

### Tab Navigation
- Tab label: "Rapport" with FileText icon (lucide-react)
- Position: Fourth tab in the existing TabNavigation component
- Tab appears conditionally: `analysisStatus === 'completed'`

### Report Preview Layout
```
+------------------------------------------+
|  [Varlor Logo]                           |
|                                          |
|  Rapport d'Analyse de Donnees            |
|  [Dataset Name]                          |
|                                          |
|  1. Informations Generales               |
|  +------------------------------------+  |
|  | Fichier: data.csv                 |  |
|  | Date: 27 nov. 2025                |  |
|  | Lignes: 50,000 | Colonnes: 15     |  |
|  | Taille: 2.5 MB | Format: CSV      |  |
|  +------------------------------------+  |
|                                          |
|  Colonnes detectees:                     |
|  - id (NUMBER)                           |
|  - name (TEXT)                           |
|  - date (DATE)                           |
|  ...                                     |
|                                          |
|  2. Synthese Qualite                     |
|  [Quality Score Badge: 92% Excellent]    |
|  Corrections appliquees: 150             |
|  Problemes restants: 23                  |
|                                          |
|  Colonnes problematiques:                |
|  +------------------------------------+  |
|  | Colonne  | Manquants | Invalides  |  |
|  |----------|-----------|------------|  |
|  | email    | 8.2%      | 3.1%       |  |
|  | phone    | 12.5%     | 0%         |  |
|  +------------------------------------+  |
|                                          |
|  3. Visualisations                       |
|  [Chart 1: Histogram - Amount]           |
|  [Chart 2: Bar Chart - Category]         |
|  [Chart 3: Time Curve - Sales]           |
|                                          |
|  4. Analyse                              |
|  [Narrative paragraphs from AI insights] |
|                                          |
+------------------------------------------+
|  Genere par Varlor - 27/11/2025  Page 1  |
+------------------------------------------+
```

### Download Button
- Primary button style with Download icon
- Text: "Telecharger PDF"
- Loading state during generation with spinner
- Position: Top-right of report preview section

### Report Tab Component States
1. **Loading**: Skeleton loaders for each section
2. **Ready**: Full report preview with download button
3. **Generating PDF**: Button shows spinner, "Generation en cours..."
4. **Error**: Alert with retry option

## API Specifications

### Endpoint: Generate Report PDF

```
POST /datasets/:id/report/generate
```

**Request**: Empty body (uses existing data)

**Response** (success):
```json
{
  "downloadUrl": "/datasets/123/report/download?token=abc123",
  "expiresAt": "2025-11-27T15:00:00Z"
}
```

**Response** (error 400):
```json
{
  "statusCode": 400,
  "message": "Analysis must be completed before generating report",
  "error": "Bad Request"
}
```

### Endpoint: Download Report PDF

```
GET /datasets/:id/report/download?token=abc123
```

**Response**: Binary PDF file with headers:
- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="rapport-[dataset-name]-[date].pdf"`

### Endpoint: Get Report Data (Preview)

```
GET /datasets/:id/report/data
```

**Response**:
```json
{
  "datasetId": 123,
  "summary": {
    "name": "sales_data",
    "fileName": "sales_data.csv",
    "uploadedAt": "2025-11-27T10:00:00Z",
    "rowCount": 50000,
    "columnCount": 15,
    "fileSize": 2621440,
    "fileFormat": "CSV",
    "columns": [
      { "name": "id", "type": "NUMBER" },
      { "name": "date", "type": "DATE" }
    ]
  },
  "quality": {
    "globalScore": 92.5,
    "totalCorrections": 150,
    "remainingIssues": 23,
    "problematicColumns": [
      {
        "name": "email",
        "missingPercentage": 8.2,
        "invalidPercentage": 3.1
      }
    ]
  },
  "selectedCharts": [
    {
      "columnName": "amount",
      "chartType": "histogram",
      "reason": "outliers_detected"
    },
    {
      "columnName": "category",
      "chartType": "bar",
      "reason": "good_diversity"
    }
  ],
  "narrativeText": "Ce jeu de donnees contient 50 000 lignes..."
}
```

## Data Models

### No new database models required

All data is derived from existing models:
- `Dataset` - for metadata
- `DatasetColumn` - for column info
- `CleaningResults` - for quality metrics
- `AnalysisResults` - for charts and statistics
- `AIInsights` - for narrative text generation

### Chart Selection Algorithm (Server-side)

```
1. Get all columns with chartData from analysis results
2. Filter histograms:
   - Sort by outlier count (descending)
   - If no outliers, sort by variance (stdDev)
   - Take top 2
3. Filter bar charts:
   - Include only if 5 <= uniqueCount <= 20
   - Sort by uniqueCount (prefer higher diversity)
   - Take top 2
4. Filter time curves:
   - Include if temporalAxis.available = true
   - Take 1 (first numeric column with time data)
5. Combine all selected charts (max 6 total)
```

## Technical Architecture

### Frontend Components

```
components/
  datasets/
    report-tab.tsx           # Main tab component
    report-preview.tsx       # Report preview container
    report-summary.tsx       # Dataset summary section
    report-quality.tsx       # Quality synthesis section
    report-charts.tsx        # Selected charts section
    report-narrative.tsx     # AI narrative text section
    report-download-button.tsx
```

### Backend Services

```
app/
  controllers/
    report_controller.ts     # New controller
  services/
    report_service.ts        # Report data aggregation
    pdf_generation_service.ts # Puppeteer PDF generation
```

### Reusable Components

**Existing components to leverage:**
- `HistogramChart`, `BarChart`, `TimeCurveChart` - for chart rendering
- `Badge`, `Alert`, `Button`, `Skeleton` - UI primitives
- `getSeverityConfig()` from cleaning-results-section - quality score styling
- `formatFileSize()`, `formatDate()` from dataset detail page
- Tab navigation pattern from dataset detail page

**New components required:**
- `ReportTab` - orchestrates report preview and download
- `ReportPreview` - renders full report for screen
- `ReportDownloadButton` - handles PDF generation and download

### PDF Generation Flow

1. Frontend calls `POST /datasets/:id/report/generate`
2. Backend aggregates all report data
3. Backend renders HTML template with data
4. Puppeteer converts HTML to PDF
5. PDF stored temporarily with signed URL
6. Frontend receives download URL
7. User downloads PDF file

### HTML Template for PDF

Server-side HTML template optimized for print:
- Uses inline CSS for reliability
- Static chart images (generated server-side or base64)
- Varlor brand colors: Primary blue (#3b82f6)
- A4 page breaks between sections
- Print media queries for optimal rendering

## Dependencies

### Frontend (existing)
- `echarts` - Already used for charts
- `lucide-react` - For icons
- `@tanstack/react-query` - For data fetching

### Backend (new)
- `puppeteer` - PDF generation from HTML
- `handlebars` or inline template strings - HTML templating

### Assets Required
- Varlor logo SVG (needs to be added to `/public/varlor-logo.svg`)

## Out of Scope

- Scheduled/automated reports
- Multiple export formats (Word, Excel, HTML)
- Customizable report templates
- Report sharing/collaboration features
- Report history/versioning
- User annotations/comments
- Customizable sections ordering
- Multi-dataset comparison reports
- Recurring email reports
- Batch report generation
- Custom watermark options
- PDF password protection
- Language selection (French only for MVP)

## Success Criteria

### Functional
- [ ] Report tab appears when analysis is completed
- [ ] Report preview displays all 4 sections correctly
- [ ] PDF download completes within 10 seconds
- [ ] PDF renders correctly on screen and when printed
- [ ] Chart selection algorithm produces relevant visualizations

### Quality
- [ ] PDF file size under 5MB for typical datasets
- [ ] All text readable in PDF (no truncation)
- [ ] Charts render at print quality (300 DPI equivalent)
- [ ] Varlor branding consistent with platform design

### User Experience
- [ ] Clear loading states during PDF generation
- [ ] Error messages in French with recovery options
- [ ] Download triggers browser save dialog
- [ ] Report preview matches PDF output closely

## Implementation Notes

### Chart Rendering for PDF
Since ECharts renders client-side, for PDF generation either:
1. Use ECharts server-side rendering with node-canvas
2. Generate static chart images using a charting library that supports Node.js
3. Capture chart images from the frontend and send to backend

Recommended: Option 1 or 2 for reliability and consistency.

### Narrative Text Generation
Transform existing AI insights (bullet points) into prose:
- Group insights by category
- Use template sentences to connect insights
- Limit to 2-3 paragraphs (~150-200 words)

### Security Considerations
- Validate dataset ownership before generating report
- Signed download URLs with short expiration (15 minutes)
- Rate limit report generation endpoint
- Clean up temporary PDF files after download

### Performance
- Cache aggregated report data for 5 minutes
- Generate PDF asynchronously if >5 seconds expected
- Consider background job for large datasets
