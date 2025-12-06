# Task Breakdown: MVP Report Feature

## Overview
Total Tasks: 28 (distributed across 5 task groups)

This feature adds a "Rapport" tab to the dataset detail page that displays a comprehensive report preview and allows users to export it as a professionally formatted PDF document.

## Task List

---

### Task Group 1: Backend - Report Data Aggregation Service
**Dependencies:** None (uses existing models and data)
**Estimated Effort:** Medium

This group creates the service layer for aggregating report data from existing models and implementing the chart selection algorithm.

- [x] 1.0 Complete report data aggregation service
  - [x] 1.1 Write 4-6 focused tests for report service functionality
    - Test report data aggregation from existing dataset data
    - Test chart selection algorithm (priority: outliers > variance)
    - Test problematic columns filtering (>5% threshold)
    - Test narrative text transformation from AI insights
    - Test edge cases: no charts available, no AI insights
  - [x] 1.2 Create ReportService at `/server/app/services/report_service.ts`
    - `getReportData(datasetId: number)` - aggregate all report data
    - `selectCharts(columnStats: DatasetColumnStats[])` - implement chart selection algorithm
    - `getProblematicColumns(cleaningResults)` - filter columns >5% issues
    - `transformNarrativeText(aiInsights)` - convert bullet points to prose
  - [x] 1.3 Implement chart selection algorithm
    - Filter histograms: sort by outlier count desc, then variance (stdDev), take top 2
    - Filter bar charts: include only 5 <= uniqueCount <= 20, sort by diversity, take top 2
    - Filter time curves: include if temporalAxis.available = true, take 1
    - Combine all (max 6 total)
  - [x] 1.4 Implement narrative text transformation
    - Group AI insights by category
    - Convert bullet-point insights to flowing paragraphs
    - Limit output to 2-3 paragraphs (~150-200 words)
  - [x] 1.5 Ensure report service tests pass
    - Run ONLY the tests written in 1.1
    - Verify all aggregation functions work correctly

**Acceptance Criteria:**
- ReportService aggregates data from Dataset, DatasetColumn, DatasetColumnStats, cleaning results, and AI insights
- Chart selection returns maximum 6 charts with proper prioritization
- Problematic columns filtering correctly identifies columns with >5% issues
- Narrative text is coherent prose (not bullet points)

---

### Task Group 2: Backend - PDF Generation Service & API
**Dependencies:** Task Group 1
**Estimated Effort:** High

This group implements server-side PDF generation using Puppeteer and creates the API endpoints.

- [x] 2.0 Complete PDF generation service and API endpoints
  - [x] 2.1 Write 4-6 focused tests for PDF generation and API
    - Test report data endpoint returns correct structure
    - Test generate endpoint creates valid download URL
    - Test download endpoint returns PDF with correct headers
    - Test authorization checks (dataset ownership)
    - Test validation (analysis must be completed)
  - [x] 2.2 Add Puppeteer dependency to server
    - Add `puppeteer` to package.json
    - Configure for server-side rendering
  - [x] 2.3 Create PdfGenerationService at `/server/app/services/pdf_generation_service.ts`
    - `generateReportPdf(reportData)` - render HTML and convert to PDF
    - `createHtmlTemplate(reportData)` - create print-optimized HTML
    - `renderChartsToImages(chartData)` - generate static chart images for PDF
    - `storeTemporaryPdf(pdfBuffer)` - store with signed URL
    - `cleanupExpiredPdfs()` - remove files after 15 minutes
  - [x] 2.4 Create HTML template for PDF
    - A4 portrait format with professional margins
    - Varlor branding (logo, colors: primary blue #3b82f6)
    - Header with logo
    - Footer with "Genere par Varlor - [date]" and page number
    - Print media queries for optimal rendering
    - Inline CSS for reliability
    - Page breaks between sections
  - [x] 2.5 Create ReportController at `/server/app/controllers/report_controller.ts`
    - `data()` - GET /datasets/:id/report/data (preview data)
    - `generate()` - POST /datasets/:id/report/generate (create PDF)
    - `download()` - GET /datasets/:id/report/download (download PDF)
  - [x] 2.6 Add API routes in `/server/start/routes.ts`
    ```
    GET  /datasets/:id/report/data
    POST /datasets/:id/report/generate (rate limited)
    GET  /datasets/:id/report/download
    ```
  - [x] 2.7 Implement security measures
    - Validate dataset ownership before generating
    - Signed download URLs with 15-minute expiration
    - Rate limit on generate endpoint
    - Cleanup temporary PDF files after download
  - [x] 2.8 Ensure API tests pass
    - Run ONLY the tests written in 2.1
    - Verify all endpoints work correctly

**Acceptance Criteria:**
- GET /datasets/:id/report/data returns complete report preview data
- POST /datasets/:id/report/generate creates PDF and returns download URL
- GET /datasets/:id/report/download serves PDF file with correct headers
- PDF renders correctly at A4 size with Varlor branding
- Security: ownership validation, signed URLs, rate limiting

---

### Task Group 3: Frontend - API Client & Hooks
**Dependencies:** Task Group 2
**Estimated Effort:** Low-Medium

This group creates the frontend API integration layer for the report feature.

- [x] 3.0 Complete frontend API integration
  - [x] 3.1 Write 3-4 focused tests for API client and hooks
    - Test getReportData returns correctly typed data
    - Test generateReport mutation triggers PDF generation
    - Test useReportData hook handles loading/error states
    - Test download URL handling
  - [x] 3.2 Create report API client at `/client/web/lib/api/report.ts`
    - `getReportData(datasetId)` - fetch preview data
    - `generateReport(datasetId)` - trigger PDF generation
    - `downloadReport(downloadUrl)` - handle PDF download
    - Define TypeScript interfaces matching spec
  - [x] 3.3 Create report schema at `/client/web/lib/schemas/report.schema.ts`
    - ReportDataResponse interface
    - ReportSummary interface
    - ReportQuality interface
    - SelectedChart interface
    - GenerateReportResponse interface
  - [x] 3.4 Create useReport hook at `/client/web/lib/hooks/use-report.ts`
    - `useReportData(datasetId)` - React Query hook for preview data
    - `useGenerateReport()` - mutation hook for PDF generation
    - Handle loading, success, and error states
    - Cache report data for 5 minutes
  - [x] 3.5 Ensure API integration tests pass
    - Run ONLY the tests written in 3.1
    - Verify type safety and error handling

**Acceptance Criteria:**
- API client functions properly typed
- React Query hooks manage state correctly
- PDF download triggers browser save dialog
- Error states handled with French messages

---

### Task Group 4: Frontend - UI Components
**Dependencies:** Task Group 3
**Estimated Effort:** High

This group creates all UI components for the Report tab following the existing component patterns.

- [x] 4.0 Complete Report tab UI components
  - [x] 4.1 Write 4-6 focused tests for UI components
    - Test ReportTab renders when analysis is completed
    - Test ReportPreview displays all 4 sections
    - Test ReportDownloadButton loading states
    - Test conditional rendering based on data availability
    - Test error state rendering with retry option
  - [x] 4.2 Create ReportTab component at `/client/web/components/datasets/report-tab.tsx`
    - Orchestrates report preview and download
    - Handles loading/error/ready states
    - Uses existing Skeleton, Alert, Button patterns
    - Props: `datasetId`, `datasetName`, `analysisStatus`
  - [x] 4.3 Create ReportPreview component at `/client/web/components/datasets/report-preview.tsx`
    - Container for full report preview
    - Renders all 4 sections in order
    - Matches PDF layout closely
    - Responsive for screen viewing
  - [x] 4.4 Create ReportSummary component at `/client/web/components/datasets/report-summary.tsx`
    - Dataset name, filename, upload date
    - Row count, column count, file size, format
    - List of columns with detected types
    - Reuse `formatFileSize()`, `formatDate()` utilities
  - [x] 4.5 Create ReportQuality component at `/client/web/components/datasets/report-quality.tsx`
    - Global quality score with badge styling
    - Total corrections applied
    - Remaining issues count
    - Table of problematic columns (>5% missing/invalid)
    - Reuse `getSeverityConfig()` pattern from cleaning-results-section
  - [x] 4.6 Create ReportCharts component at `/client/web/components/datasets/report-charts.tsx`
    - Display 4-6 auto-selected charts
    - Reuse existing HistogramChart, BarChart, TimeCurveChart components
    - Responsive grid layout
    - Show chart selection reason (outliers_detected, good_diversity, etc.)
  - [x] 4.7 Create ReportNarrative component at `/client/web/components/datasets/report-narrative.tsx`
    - Display AI-generated narrative text
    - Professional typography
    - 2-3 paragraphs of flowing prose
  - [x] 4.8 Create ReportDownloadButton component at `/client/web/components/datasets/report-download-button.tsx`
    - Primary button style with Download icon
    - Text: "Telecharger PDF"
    - Loading state: spinner + "Generation en cours..."
    - Error state with retry option
    - Triggers browser download on success
  - [x] 4.9 Ensure UI component tests pass
    - Run ONLY the tests written in 4.1
    - Verify component rendering and interactions

**Acceptance Criteria:**
- All components render correctly with proper data
- Loading states show skeleton loaders
- Error states display French messages with retry options
- Download button triggers PDF generation and download
- Components match existing design system

---

### Task Group 5: Integration & Polish
**Dependencies:** Task Groups 1-4
**Estimated Effort:** Medium

This group integrates the Report tab into the dataset detail page and handles final polish.

- [x] 5.0 Complete integration and final polish
  - [x] 5.1 Write 4-6 focused integration tests
    - Test Report tab appears when analysis is completed
    - Test full flow: preview -> download -> receive PDF
    - Test tab navigation between Preview/Quality/Analysis/Report
    - Test conditional visibility (only shows when analysis completed)
  - [x] 5.2 Update dataset detail page at `/client/web/app/(dashboard)/dashboard/datasets/[id]/page.tsx`
    - Add "Rapport" tab to TabNavigation (fourth tab)
    - Import FileText icon from lucide-react
    - Add TabType: `'preview' | 'quality' | 'analysis' | 'report'`
    - Conditional display: only when `analysisStatus === 'completed'`
    - Render ReportTab component when active
  - [x] 5.3 Add Varlor logo asset
    - Create or obtain Varlor logo SVG
    - Add to `/client/web/public/varlor-logo.svg`
    - Ensure logo is also available in server for PDF generation
  - [x] 5.4 Verify PDF quality and rendering
    - PDF file size under 5MB for typical datasets
    - All text readable (no truncation)
    - Charts render at print quality
    - Proper page breaks between sections
  - [x] 5.5 Verify UX requirements
    - Clear loading states during PDF generation
    - Error messages in French with recovery options
    - Download triggers browser save dialog
    - Report preview matches PDF output closely
  - [x] 5.6 Ensure integration tests pass
    - Run full feature tests
    - Verify end-to-end workflow

**Acceptance Criteria:**
- Report tab visible only when analysis is completed
- Tab navigation works seamlessly
- PDF generation completes within 10 seconds
- PDF renders correctly on screen and when printed
- User experience is smooth with clear feedback

---

## Execution Order

Recommended implementation sequence:

1. **Task Group 1: Backend - Report Data Aggregation Service**
   - Start here as it has no dependencies
   - Implements core data logic needed by other groups

2. **Task Group 2: Backend - PDF Generation Service & API**
   - Depends on Group 1 for report data
   - Creates all backend infrastructure

3. **Task Group 3: Frontend - API Client & Hooks**
   - Depends on Group 2 for API endpoints
   - Creates frontend data layer

4. **Task Group 4: Frontend - UI Components**
   - Depends on Group 3 for data hooks
   - Creates all visual components

5. **Task Group 5: Integration & Polish**
   - Depends on Groups 1-4
   - Final integration and quality verification

---

## File Paths Summary

### Server (Backend)
```
server/
  app/
    controllers/
      report_controller.ts          # New - API endpoints
    services/
      report_service.ts             # New - Data aggregation
      pdf_generation_service.ts     # New - PDF generation
  start/
    routes.ts                       # Modified - Add report routes
  tests/
    unit/
      report_service.spec.ts        # New - Service tests
      pdf_generation_service.spec.ts # New - PDF tests
    functional/
      report_controller.spec.ts     # New - API tests
  package.json                      # Modified - Add puppeteer
```

### Client (Frontend)
```
client/web/
  app/(dashboard)/dashboard/datasets/[id]/
    page.tsx                        # Modified - Add Report tab
  components/datasets/
    report-tab.tsx                  # New - Main tab component
    report-preview.tsx              # New - Preview container
    report-summary.tsx              # New - Dataset summary section
    report-quality.tsx              # New - Quality synthesis section
    report-charts.tsx               # New - Charts section
    report-narrative.tsx            # New - AI narrative section
    report-download-button.tsx      # New - Download button
  lib/
    api/
      report.ts                     # New - API client
    hooks/
      use-report.ts                 # New - React Query hooks
    schemas/
      report.schema.ts              # New - TypeScript types
  public/
    varlor-logo.svg                 # New - Logo asset
  __tests__/
    components/datasets/
      report-tab.test.tsx           # New - Component tests
    lib/
      report.test.ts                # New - API/hooks tests
```

---

## Dependencies to Add

### Server
```json
{
  "dependencies": {
    "puppeteer": "^21.x.x"
  }
}
```

### Notes on Puppeteer
- Puppeteer requires Chromium (~170MB download on first run)
- Consider using `puppeteer-core` + system Chrome in production
- Set `PUPPETEER_CACHE_DIR` environment variable for consistent cache location

---

## Risk Considerations

1. **PDF Generation Performance**: Large datasets with many charts may take longer to generate. Consider implementing progress feedback or background job processing if generation exceeds 10 seconds.

2. **Chart Rendering for PDF**: ECharts is client-side. Options:
   - Use `echarts` server-side rendering with `canvas`
   - Use `node-canvas` for static images
   - Capture images from frontend and send to backend

3. **File Cleanup**: Ensure temporary PDF files are cleaned up after download or expiration to prevent disk space issues.

4. **Puppeteer in Production**: May need special configuration for containerized environments (Docker, Kubernetes).