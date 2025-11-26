# Specification: Algorithmic Analysis (MVP)

## Overview

Algorithmic Analysis MVP provides automated statistical analysis and visualization for imported datasets. After a dataset is uploaded and cleaned, users can access an "Analysis" tab that displays basic statistics per column, outlier detection results, and auto-generated charts (histograms, bar charts, time curves).

## Goals

1. Enable users to understand their data through automated statistical summaries
2. Identify potential data quality issues via outlier detection
3. Provide visual exploration through auto-generated charts
4. Integrate seamlessly with existing dataset workflow (upload -> clean -> **analyze**)

## User Stories

- As a data analyst, I want to see basic statistics (min, max, mean, median, std dev) for numeric columns so that I can understand the distribution of my data.
- As a data analyst, I want to see the top 10 most frequent values for categorical columns so that I can identify common patterns.
- As a data analyst, I want to see date range information for date columns so that I understand the temporal scope of my data.
- As a data analyst, I want outliers to be automatically detected and displayed so that I can investigate potential data quality issues.
- As a data analyst, I want to see auto-generated histograms for numeric columns so that I can visualize distributions.
- As a data analyst, I want to see bar charts for categorical columns so that I can compare category frequencies.
- As a data analyst, I want to see time curves when a date column is present so that I can observe trends over time.

## Core Requirements

### Statistics Calculation

#### Numeric Columns
- Min, max, mean, median, standard deviation
- Distribution data for histogram generation
- Total count and non-null count

#### Categorical/Text Columns
- Count of unique values
- Top 10 most frequent values with percentages
- If fewer than 10 unique values, display all

#### Date Columns
- Min date and max date
- Temporal range description (e.g., "3 months of data", "2 years of data")

#### Mixed Type Columns
- No numeric statistics attempted
- Treat as text by default
- Flag "mixed type detected" in analysis results

### Outlier Detection

- Method: +/- 3 standard deviations from mean (simple rule)
- Applied to: Numeric columns only
- Output per column:
  - Total outlier count
  - Percentage of total values
  - Up to 5 extreme high values with row references
  - Up to 5 extreme low values with row references
- No correction applied (detection only)
- No IQR method for MVP (future Alpha)
- No configurable thresholds for MVP (future Alpha)

### Chart Generation

#### Histograms (Numeric Columns)
- Auto-generated for each numeric column
- Appropriate bin sizing based on data distribution
- Hover tooltips showing bin range and count

#### Bar Charts (Categorical Columns)
- Auto-generated for categorical columns
- Show top 10 categories
- "Other" category for remaining values if more than 10 unique
- Hover tooltips showing category and count/percentage

#### Time Curves (Date Columns)
- Auto-detection: Reuse type detection from import step
- If single date column detected, use automatically
- If multiple date columns, use the first one detected
- Plot numeric columns against the date axis
- Display message: "Temporal axis: [column_name]"
- No user selection for MVP (future Alpha: dropdown selector)

### Chart Interactivity
- Hover tooltips (ECharts native)
- Basic zoom if supported by ECharts
- No pan/drag
- No range selection
- No chart export (future Alpha)

## Visual Design

No mockups provided. UI follows existing Varlor design patterns.

### Page Layout Structure

```
/dashboard/datasets/{id} - Dataset Detail Page
  |-- Tab: Preview (existing)
  |-- Tab: Quality (existing - cleaning results)
  |-- Tab: Analysis (NEW)
      |
      |-- Section: Statistics by Column
      |   [Expandable/collapsible cards per column]
      |
      |-- Section: Charts
      |   [Sub-tabs or toggles: Histograms | Bar Charts | Time Curves]
      |
      |-- Section: Outliers Detected
          [List of columns with outliers, expandable details]
```

### UI Components

1. **Statistics Card (per column)**
   - Column name and detected type badge
   - Collapsible content with stats based on type
   - Visual indicator for data quality

2. **Chart Container**
   - Full-width responsive chart
   - Column selector or tabs for multiple charts
   - Loading state while generating

3. **Outlier Alert Section**
   - Warning badge with count
   - Expandable list per column
   - Sample extreme values with row references

## Reusable Components

### Existing Code to Leverage

**Backend:**
- `CleaningService` - outlier detection already exists (IQR method) - adapt to 3-sigma method
- `DatasetColumn` model - already has `detectedType`, `outlierCount` fields
- `DatasetsController` pattern for new analysis endpoints
- `DatasetParserService` - type detection logic can be reused
- `FileStorageService` - for caching analysis results

**Frontend:**
- `cleaning-results-section.tsx` - pattern for displaying per-column quality info
- `cleaning-status-indicator.tsx` - pattern for status display
- `dataset-preview-table.tsx` - table rendering patterns
- Shadcn UI components: `Badge`, `Card`, `Table`, `Collapsible`, `Progress`, `Skeleton`
- ECharts 6.0.0 (already in stack) - for all chart rendering

### New Components Required

**Backend:**
- `AnalysisService` - orchestrates statistics calculation and chart data generation
- `AnalysisController` - HTTP endpoints for analysis operations
- `DatasetColumnStats` model - stores computed statistics per column

**Frontend:**
- `analysis-stats-section.tsx` - displays statistics per column
- `analysis-chart-container.tsx` - ECharts wrapper with responsive sizing
- `histogram-chart.tsx` - histogram configuration for ECharts
- `bar-chart.tsx` - bar chart configuration for ECharts
- `time-curve-chart.tsx` - time series configuration for ECharts
- `outlier-section.tsx` - displays outlier detection results

## Technical Approach

### Backend Architecture

Analysis runs as a background job triggered after cleaning completes. This follows the same pattern as the existing cleaning pipeline.

**Processing Pipeline:**
1. Cleaning completes -> triggers analysis job
2. Analysis job reads cleaned data file
3. Calculates statistics per column based on detected type
4. Detects outliers using 3-sigma method
5. Generates chart data structures
6. Stores results in database + metadata file
7. Updates dataset status to include analysis status

**Data Flow:**
```
Dataset (READY)
  -> Start Analysis (background)
  -> Read cleaned data from storage
  -> Calculate stats per column
  -> Detect outliers
  -> Generate chart data
  -> Store results
  -> Dataset analysis_status = 'completed'
```

### Frontend Architecture

New "Analysis" tab in dataset detail page. Follows existing tab pattern from cleaning results.

**Component Hierarchy:**
```
DatasetDetailPage
  |-- Tabs
      |-- AnalysisTab
          |-- AnalysisStatusIndicator (if processing)
          |-- StatisticsSection
          |   |-- ColumnStatsCard (repeated)
          |-- ChartsSection
          |   |-- ChartTabSelector
          |   |-- HistogramChart / BarChart / TimeCurveChart
          |-- OutlierSection
              |-- OutlierColumnCard (repeated)
```

### Chart Rendering Strategy

Use ECharts 6.0.0 with React wrapper pattern:
- Dynamic import for code splitting
- Responsive container with ResizeObserver
- Theme integration with Tailwind CSS variables
- Skeleton loading states

## Data Models

### New Model: DatasetColumnStats

Stores computed statistics per column for quick retrieval.

**Fields:**
- `id`: Primary key
- `datasetId`: Foreign key to Dataset
- `columnName`: Column identifier
- `detectedType`: 'NUMBER' | 'TEXT' | 'DATE' | 'UNKNOWN'
- `statsJson`: JSONB field containing type-specific statistics
- `outlierCount`: Number of outliers detected
- `outliersJson`: JSONB field with outlier details (sample values, thresholds)
- `chartDataJson`: JSONB field with pre-computed chart data
- `createdAt`, `updatedAt`: Timestamps

### Dataset Model Updates

Add new fields to existing `Dataset` model:
- `analysisStatus`: 'pending' | 'processing' | 'completed' | 'failed'
- `analysisProcessingTimeMs`: Processing time in milliseconds
- `analysisCompletedAt`: Timestamp when analysis finished

### Statistics JSON Structures

**Numeric Column Stats:**
```json
{
  "min": 0,
  "max": 100,
  "mean": 50.5,
  "median": 48.0,
  "stdDev": 15.2,
  "count": 1000,
  "nonNullCount": 985
}
```

**Categorical Column Stats:**
```json
{
  "uniqueCount": 25,
  "topValues": [
    { "value": "France", "count": 450, "percentage": 45.0 },
    { "value": "Germany", "count": 230, "percentage": 23.0 }
  ],
  "totalCount": 1000
}
```

**Date Column Stats:**
```json
{
  "minDate": "2023-01-15",
  "maxDate": "2024-06-30",
  "rangeDescription": "17 months of data",
  "totalCount": 1000,
  "nonNullCount": 998
}
```

**Outlier Details:**
```json
{
  "count": 23,
  "percentage": 2.3,
  "threshold": {
    "lower": -15.6,
    "upper": 116.1,
    "method": "3-sigma"
  },
  "extremeHigh": [
    { "value": 99999, "rowIndex": 342 },
    { "value": 87500, "rowIndex": 1203 }
  ],
  "extremeLow": [
    { "value": -500, "rowIndex": 89 }
  ]
}
```

## API Specification

### POST /datasets/:id/analysis/start

Triggers analysis job for a dataset (after cleaning is complete).

**Request:** Empty body

**Response (202 Accepted):**
```json
{
  "message": "Analysis started",
  "datasetId": 123,
  "analysisStatus": "processing"
}
```

**Error Responses:**
- 400: Cleaning not completed
- 404: Dataset not found
- 409: Analysis already in progress

### GET /datasets/:id/analysis/status

Returns current analysis status.

**Response (200 OK):**
```json
{
  "datasetId": 123,
  "analysisStatus": "completed",
  "analysisProcessingTimeMs": 2350,
  "analysisCompletedAt": "2024-11-25T15:30:00Z"
}
```

### GET /datasets/:id/analysis/results

Returns full analysis results including stats, outliers, and chart data.

**Response (200 OK):**
```json
{
  "datasetId": 123,
  "status": "completed",
  "columns": [
    {
      "columnName": "amount",
      "detectedType": "NUMBER",
      "stats": {
        "min": 0,
        "max": 100,
        "mean": 50.5,
        "median": 48.0,
        "stdDev": 15.2,
        "count": 1000,
        "nonNullCount": 985
      },
      "outliers": {
        "count": 23,
        "percentage": 2.3,
        "threshold": { "lower": -15.6, "upper": 116.1 },
        "extremeHigh": [...],
        "extremeLow": [...]
      },
      "chartData": {
        "type": "histogram",
        "bins": [...]
      }
    },
    {
      "columnName": "country",
      "detectedType": "TEXT",
      "stats": {
        "uniqueCount": 25,
        "topValues": [...],
        "totalCount": 1000
      },
      "outliers": null,
      "chartData": {
        "type": "bar",
        "categories": [...],
        "values": [...]
      }
    }
  ],
  "temporalAxis": {
    "columnName": "order_date",
    "available": true
  }
}
```

### GET /datasets/:id/analysis/chart/:columnName

Returns chart data for a specific column (for lazy loading large datasets).

**Response (200 OK):**
```json
{
  "columnName": "amount",
  "chartType": "histogram",
  "data": {
    "bins": [
      { "min": 0, "max": 10, "count": 50 },
      { "min": 10, "max": 20, "count": 120 }
    ]
  }
}
```

## UI/UX Design

### Analysis Tab Layout

**Desktop (>1024px):**
- Statistics section: 2-3 column grid of collapsible cards
- Charts section: Full-width with tab selector
- Outliers section: Collapsible accordion

**Tablet (768-1024px):**
- Statistics section: 2 column grid
- Charts section: Full-width
- Outliers section: Full-width accordion

**Mobile (<768px):**
- Statistics section: Single column stack
- Charts section: Full-width, scrollable
- Outliers section: Full-width accordion

### Interaction States

1. **Loading:** Skeleton loaders for stats cards and chart containers
2. **Processing:** Progress indicator with "Analysis in progress..." message
3. **Error:** Error alert with retry button
4. **Empty:** Message when no data available for a section

### Accessibility

- Charts include aria-labels describing data
- Keyboard navigation for expandable sections
- Color contrast compliance for chart elements
- Screen reader friendly statistics display

## Out of Scope

Explicit MVP exclusions:

- Correlation between columns
- Grouping/aggregation by category
- Export of charts (PNG, PDF)
- Comparison between datasets
- Filters on the data
- Conditional analyses (if X then Y)
- Predictions/forecasting
- Clustering/segmentation
- Statistical tests (t-test, chi-square, etc.)
- Regression analysis
- Chart customization (colors, labels)
- User selection of columns to analyze
- Analysis on data subsamples
- Configurable outlier thresholds
- IQR outlier method
- User-selectable date column for time charts

## Future Considerations

### Alpha Phase
- Slider to adjust Top N (1-50) for frequent values
- IQR method for outlier detection (optional toggle)
- Configurable outlier thresholds
- Dropdown to select date column for time charts
- Chart export (PNG/PDF)

### Beta Phase
- Variable correlation matrix
- Subgroup comparisons
- User-selectable analysis columns
- Analysis on filtered subsets

### V1 Phase
- Advanced multi-variable correlations
- Predictive models
- Time series anomaly detection
- Recurring pattern detection

## Success Criteria

1. **Functional:** Analysis runs automatically after cleaning completes without user intervention
2. **Performance:** Analysis completes within 30 seconds for datasets up to 100K rows
3. **Accuracy:** Statistics match expected values (validated via unit tests)
4. **Usability:** Users can access analysis results within 2 clicks from dataset list
5. **Visual:** Charts render correctly on all breakpoints (desktop, tablet, mobile)
6. **Integration:** Analysis tab follows same design patterns as existing Quality tab

## Technical Notes

- ECharts 6.0.0 is already in the frontend stack (see ARCHITECTURE.md)
- Backend uses AdonisJS 6 with Lucid ORM
- PostgreSQL with JSONB support for flexible stats storage
- Follow existing service/controller pattern from CleaningService
- Use existing FileStorageService for caching analysis results
- TypeScript strict mode on both frontend and backend
