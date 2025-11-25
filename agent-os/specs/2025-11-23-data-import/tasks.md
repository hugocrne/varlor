# Task Breakdown: Data Import Feature

## Overview
Total Tasks: 7 Task Groups
Implementation Strategy: Sequential phases from backend foundation to deployment

## Task List

### Phase 1: Backend Foundation

#### Task Group 1.1: Database Schema and Models
**Dependencies:** None
**Specialist:** Backend Engineer / Database Engineer

- [x] 1.1.0 Complete database foundation layer
  - [x] 1.1.1 Write 2-8 focused tests for Dataset and DatasetColumn models
    - Test critical model behaviors only (validations, associations, status transitions)
    - Test Dataset model: status enum validation, file format validation
    - Test DatasetColumn model: type enum validation, belongsTo association
    - Skip exhaustive coverage of all methods and edge cases
  - [x] 1.1.2 Create datasets table migration
    - File: `server/database/migrations/TIMESTAMP_create_datasets_table.ts`
    - Columns: id (serial PK), tenant_id (string, indexed), user_id (integer FK to users.id), name (string), file_name (string), file_size (bigint), file_format (enum: 'CSV', 'EXCEL'), storage_path (string), row_count (integer), column_count (integer), status (enum: 'UPLOADING', 'PARSING', 'READY', 'FAILED'), error_message (text, nullable), uploaded_at (timestamp), processed_at (timestamp, nullable), created_at, updated_at
    - Foreign keys: tenant_id references users.tenant_id, user_id references users.id
    - Indexes: tenant_id, user_id, status, created_at
    - Follow pattern from existing migrations in `database/migrations/`
  - [x] 1.1.3 Create dataset_columns table migration
    - File: `server/database/migrations/TIMESTAMP_create_dataset_columns_table.ts`
    - Columns: id (serial PK), dataset_id (integer FK to datasets.id), column_name (string), column_index (integer), detected_type (enum: 'TEXT', 'NUMBER', 'DATE', 'UNKNOWN'), sample_values (jsonb array), created_at, updated_at
    - Foreign key: dataset_id references datasets.id with CASCADE DELETE
    - Index: dataset_id
  - [x] 1.1.4 Create Dataset model with Lucid ORM
    - File: `server/app/models/dataset.ts`
    - Extend BaseModel
    - Define @column decorators for all fields
    - Define status enum: UPLOADING, PARSING, READY, FAILED
    - Define file_format enum: CSV, EXCEL
    - Add @belongsTo relationship to User model
    - Add @hasMany relationship to DatasetColumn model
    - Include tenantId field for multi-tenancy
    - Reuse pattern from `app/models/user.ts`
  - [x] 1.1.5 Create DatasetColumn model with Lucid ORM
    - File: `server/app/models/dataset_column.ts`
    - Extend BaseModel
    - Define @column decorators for all fields
    - Define detected_type enum: TEXT, NUMBER, DATE, UNKNOWN
    - Add @belongsTo relationship to Dataset model
    - Include JSON column for sample_values
  - [x] 1.1.6 Run database migrations
    - Execute: `node ace migration:run`
    - Verify tables created in PostgreSQL
    - Check foreign key constraints established
  - [x] 1.1.7 Ensure database layer tests pass
    - Run ONLY the 2-8 tests written in 1.1.1
    - Verify migrations run successfully
    - Verify model associations work correctly
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- The 2-8 tests written in 1.1.1 pass
- Database migrations execute without errors
- Dataset and DatasetColumn models are functional with correct associations
- Status and file_format enums work correctly
- Multi-tenancy fields (tenant_id) are properly configured

---

#### Task Group 1.2: File Storage Service
**Dependencies:** Task Group 1.1 (COMPLETED)
**Specialist:** Backend Engineer

- [x] 1.2.0 Complete file storage service
  - [x] 1.2.1 Write 2-8 focused tests for FileStorageService
    - Test critical storage operations only (save file, generate path, delete file)
    - Test path generation with tenant isolation
    - Test file size validation
    - Skip exhaustive testing of all edge cases
  - [x] 1.2.2 Create FileStorageService implementation
    - File: `server/app/services/file_storage_service.ts`
    - Method: `saveFile(file: MultipartFile, tenantId: string, datasetId: number): Promise<string>`
    - Method: `deleteFile(storagePath: string): Promise<void>`
    - Method: `getFilePath(tenantId: string, datasetId: number, filename: string): string`
    - Path pattern: `/storage/datasets/{tenant_id}/{dataset_id}/raw/{filename}`
    - Implement filename sanitization to prevent path traversal attacks
    - Validate file size (max 100MB)
    - Create directories if they don't exist
    - Return storage path for database storage
  - [x] 1.2.3 Configure storage paths in environment
    - Add `STORAGE_ROOT_PATH` to `.env` and `.env.example`
    - Default value: `./storage` for local development
    - Create storage directory structure
  - [x] 1.2.4 Ensure file storage tests pass
    - Run ONLY the 2-8 tests written in 1.2.1
    - Verify files are saved to correct paths with tenant isolation
    - Verify file deletion works correctly
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- The 2-8 tests written in 1.2.1 pass
- Files are saved to tenant-isolated paths
- Filename sanitization prevents path traversal
- File size validation enforces 100MB limit
- Storage configuration works in development environment

---

### Phase 2: File Parsing Service

#### Task Group 2.1: CSV and Excel Parsing Infrastructure
**Dependencies:** Task Group 1.2 (COMPLETED)
**Specialist:** Backend Engineer

- [x] 2.1.0 Complete file parsing infrastructure
  - [x] 2.1.1 Install required backend dependencies
    - Install: `npm install papaparse xlsx file-type iconv-lite`
    - Install types: `npm install --save-dev @types/papaparse @types/file-type`
    - Verify installations in `package.json`
  - [x] 2.1.2 Write 2-8 focused tests for DatasetParserService
    - Test critical parsing operations only (CSV delimiter detection, Excel sheet enumeration, type detection for common cases)
    - Test CSV parsing with comma delimiter and UTF-8 encoding
    - Test Excel parsing with single sheet
    - Test basic type detection (text, number, date)
    - Skip exhaustive testing of all delimiter/encoding combinations
  - [x] 2.1.3 Create DatasetParserService skeleton
    - File: `server/app/services/dataset_parser_service.ts`
    - Interface definitions for ParseResult, ColumnMetadata, PreviewData
    - Method signatures: `parseFile()`, `detectFileFormat()`, `detectCSVDelimiter()`, `detectEncoding()`, `detectColumnTypes()`, `generatePreview()`, `countRows()`
  - [x] 2.1.4 Implement file format detection
    - Method: `detectFileFormat(filePath: string): Promise<'CSV' | 'EXCEL'>`
    - Use `file-type` library to check magic numbers
    - Fallback to file extension (.csv, .xlsx, .xls)
    - Throw error for unsupported formats
  - [x] 2.1.5 Implement CSV delimiter auto-detection
    - Method: `detectCSVDelimiter(filePath: string, encoding: string): Promise<string>`
    - Test for delimiters: comma, semicolon, tab, pipe
    - Use papaparse's delimiter detection feature
    - Read first 5 rows for detection accuracy
    - Default to comma if detection fails
  - [x] 2.1.6 Implement CSV encoding detection
    - Method: `detectEncoding(filePath: string): Promise<string>`
    - Use iconv-lite to detect encoding
    - Support: UTF-8, Latin-1, Windows-1252
    - Default to UTF-8 if detection fails
    - Handle BOM markers
  - [x] 2.1.7 Implement CSV parsing
    - Method: `parseCSV(filePath: string, options: CSVOptions): Promise<ParseResult>`
    - Use papaparse for streaming CSV parsing
    - Apply detected delimiter and encoding
    - Handle quoted fields with double quotes
    - Extract first 20 rows for preview
    - Count total rows (up to 500,000 limit)
    - Return column names from header row
  - [x] 2.1.8 Implement Excel sheet enumeration
    - Method: `getExcelSheets(filePath: string): Promise<string[]>`
    - Use xlsx library to read workbook
    - Return array of sheet names
    - Support both .xlsx and .xls formats
  - [x] 2.1.9 Implement Excel parsing
    - Method: `parseExcel(filePath: string, sheetIndex: number): Promise<ParseResult>`
    - Use xlsx library with streaming for large files
    - Extract specified sheet by index
    - Convert sheet to JSON format
    - Extract first 20 rows for preview
    - Count total rows (up to 500,000 limit)
    - Detect column names from first row
  - [x] 2.1.10 Implement column type detection algorithm
    - Method: `detectColumnTypes(rows: any[][]): ColumnMetadata[]`
    - Number detection: regex for integers, floats, scientific notation
    - Date detection: regex for ISO, US (MM/DD/YYYY), EU (DD/MM/YYYY) formats
    - Validate dates using date parser
    - Text: default for non-number, non-date values
    - Unknown: mixed types, all nulls/empty values
    - Analyze first 20 rows for type inference
    - Return detected type with confidence level (optional)
  - [x] 2.1.11 Implement preview generation
    - Method: `generatePreview(rows: any[][], columnMetadata: ColumnMetadata[]): PreviewData`
    - Extract first 20 rows
    - Include column metadata (name, type, sample values)
    - Format data for frontend consumption
    - Include total row count
  - [x] 2.1.12 Implement main parseFile orchestration method
    - Method: `parseFile(filePath: string, options?: ParseOptions): Promise<ParseResult>`
    - Detect file format (CSV vs Excel)
    - For CSV: detect delimiter and encoding, then parse
    - For Excel: enumerate sheets, parse specified sheet (default: first sheet)
    - Detect column types
    - Generate preview data
    - Validate row count (max 500,000)
    - Handle errors gracefully (corrupted files, unsupported formats)
    - Return ParseResult with preview, metadata, row count, column count
  - [x] 2.1.13 Ensure parsing service tests pass
    - Run ONLY the 2-8 tests written in 2.1.2
    - Verify CSV and Excel files parse correctly
    - Verify type detection works for common cases
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- The 2-8 tests written in 2.1.2 pass
- CSV files parse with correct delimiter and encoding auto-detection
- Excel files (.xlsx and .xls) parse correctly with sheet enumeration
- Column type detection achieves >90% accuracy for common formats
- Preview generation returns first 20 rows with metadata
- Row counting enforces 500,000 row limit
- Error handling provides clear messages for unsupported/corrupted files

---

### Phase 3: Backend API

#### Task Group 3.1: Dataset Service and Controller
**Dependencies:** Task Group 2.1 (COMPLETED)
**Specialist:** Backend Engineer / API Engineer

- [x] 3.1.0 Complete backend API layer
  - [x] 3.1.1 Write 2-8 focused tests for DatasetsService and DatasetsController
    - Test critical API operations only (upload endpoint with valid file, show endpoint, preview endpoint)
    - Test POST /datasets/upload with CSV file
    - Test GET /datasets/:id with authentication
    - Test authentication/authorization checks
    - Skip exhaustive testing of all error scenarios
  - [x] 3.1.2 Create DatasetsService orchestration layer
    - File: `server/app/services/datasets_service.ts`
    - Method: `createDataset(file: MultipartFile, userId: number, tenantId: string, sheetIndex?: number): Promise<Dataset>`
    - Orchestrates: file storage → parsing → database save
    - Updates dataset status through lifecycle: UPLOADING → PARSING → READY/FAILED
    - Method: `getDataset(datasetId: number, tenantId: string): Promise<Dataset>`
    - Method: `getDatasetPreview(datasetId: number, tenantId: string): Promise<PreviewData>`
    - Enforce tenant isolation in all queries
    - Handle errors and update dataset.error_message
    - Follow pattern from `app/services/users_service.ts`
  - [x] 3.1.3 Create DatasetValidator for request validation
    - File: `server/app/validators/dataset_validator.ts`
    - Validate file upload: required file, max size 100MB, allowed formats (.csv, .xlsx, .xls)
    - Validate sheetIndex: optional integer for Excel multi-sheet
    - Validate datasetId: required integer for show/preview endpoints
    - Follow pattern from `app/validators/login_validator.ts`
  - [x] 3.1.4 Create DatasetsController
    - File: `server/app/controllers/datasets_controller.ts`
    - Method: `upload(ctx: HttpContext)` - Handle multipart file upload
    - Extract file from request using bodyparser
    - Extract authenticated user's ID and tenantId from auth context
    - Extract optional sheetIndex from request body
    - Call DatasetsService.createDataset()
    - Return: dataset ID, preview data, column metadata, status
    - HTTP 201 Created on success
    - Method: `show(ctx: HttpContext)` - Get dataset details
    - Validate datasetId from params
    - Enforce tenant isolation (user can only access their tenant's datasets)
    - Call DatasetsService.getDataset()
    - Return: dataset metadata, status, row count, column count
    - HTTP 200 OK on success
    - Method: `preview(ctx: HttpContext)` - Get dataset preview
    - Validate datasetId from params
    - Enforce tenant isolation
    - Call DatasetsService.getDatasetPreview()
    - Return: first 20 rows with column metadata
    - HTTP 200 OK on success
    - Follow pattern from `app/controllers/auth_controller.ts`
  - [x] 3.1.5 Configure routes with authentication
    - File: `server/start/routes.ts`
    - Add dataset routes group with `/datasets` prefix
    - POST /datasets/upload - requires authentication
    - GET /datasets/:id - requires authentication
    - GET /datasets/:id/preview - requires authentication
    - Apply authentication middleware: `.use(middleware.auth())`
    - Follow existing route grouping pattern
  - [x] 3.1.6 Configure multipart file upload handling
    - Update `server/config/bodyparser.ts` if needed
    - Configure max file size: 100MB
    - Configure allowed file types: .csv, .xlsx, .xls
    - Configure temporary upload directory
  - [x] 3.1.7 Implement error handling and response formatting
    - Generic error messages for users: "Unable to read file"
    - Sub-messages: "Unsupported format", "Corrupted file", "Unrecognized encoding"
    - Detailed error logging for debugging (server logs only)
    - HTTP status codes: 400 Bad Request, 401 Unauthorized, 413 Payload Too Large, 500 Internal Server Error
    - Consistent JSON response format
  - [x] 3.1.8 Add rate limiting for upload endpoint
    - Prevent abuse with rate limiting middleware
    - Limit: 10 uploads per hour per user (configurable)
    - Return HTTP 429 Too Many Requests when exceeded
  - [x] 3.1.9 Ensure API layer tests pass
    - Run ONLY the 2-8 tests written in 3.1.1
    - Verify upload endpoint works with valid files
    - Verify authentication is enforced
    - Verify tenant isolation works correctly
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- The 2-8 tests written in 3.1.1 pass
- POST /datasets/upload accepts CSV and Excel files up to 100MB
- File upload returns dataset ID and preview data
- GET /datasets/:id returns dataset metadata with tenant isolation
- GET /datasets/:id/preview returns first 20 rows with column types
- All endpoints require authentication
- Error messages are user-friendly with clear next steps
- Rate limiting prevents upload abuse

---

### Phase 4: Frontend Components

#### Task Group 4.1: UI Foundation Components
**Dependencies:** Task Group 3.1 (COMPLETED)
**Specialist:** Frontend Engineer / UI Designer

- [x] 4.1.0 Complete UI foundation components
  - [x] 4.1.1 Install required frontend dependencies
    - Install: `npm install react-dropzone`
    - Install types: `npm install --save-dev @types/react-dropzone`
    - Add Shadcn UI components: `npx shadcn@latest add table progress badge`
    - Verify installations in `package.json`
  - [x] 4.1.2 Write 2-8 focused tests for UI components
    - Test critical component behaviors only (file drop handling, progress display, table rendering)
    - Test FileUploadZone accepts file drop
    - Test UploadProgress renders three phases correctly
    - Test DatasetPreviewTable renders with sample data
    - Skip exhaustive testing of all component states
  - [x] 4.1.3 Create dataset schema with Zod validation
    - File: `client/lib/schemas/dataset.schema.ts`
    - Define Dataset type: id, tenantId, userId, name, fileName, fileSize, fileFormat, storagePath, rowCount, columnCount, status, errorMessage, uploadedAt, processedAt
    - Define DatasetColumn type: id, datasetId, columnName, columnIndex, detectedType, sampleValues
    - Define PreviewData type: rows, columns, totalRowCount
    - Define ParseResult type for API responses
    - Zod schemas for validation
    - Export TypeScript types derived from schemas
  - [x] 4.1.4 Create FileUploadZone component
    - File: `client/components/datasets/file-upload-zone.tsx`
    - Use react-dropzone for drag-and-drop functionality
    - Dropzone-style design with dashed border
    - Hover state highlighting (border color change)
    - Accept: .csv, .xlsx, .xls files
    - Max size: 100MB client-side validation
    - Display dropped file info: name, size, type
    - "Browse files" button as alternative to drag-and-drop
    - Clear error messages for invalid files (wrong format, too large)
    - Props: onFileSelect, maxSize, acceptedFormats, disabled
    - Follow Shadcn UI design system
    - Reuse Button component for "Browse files"
  - [x] 4.1.5 Create UploadProgress component
    - File: `client/components/datasets/upload-progress.tsx`
    - Three-phase progress display
    - Phase 1: Progress bar (0-100%) using Shadcn Progress component
    - Phase 2: Spinner (Loader2 from lucide-react) with "Analyzing..." text
    - Phase 3: Smooth fade transition to preview display
    - Props: phase (1 | 2 | 3), progress (0-100), fileName
    - Animations: smooth transitions between phases
    - Follow existing Button loading pattern (Loader2 spinner)
  - [x] 4.1.6 Create DatasetPreviewTable component
    - File: `client/components/datasets/dataset-preview-table.tsx`
    - Use Shadcn Table component
    - Header row with column names and type badges
    - Type indicators using Shadcn Badge component with icons:
      - Text: 📝 with text badge variant
      - Number: 🔢 with number badge variant
      - Date: 📅 with date badge variant
      - Unknown: ❓ with unknown badge variant
    - Display first 20 rows in table body
    - Horizontal scrolling for tables with many columns
    - Footer text: "Preview of first 20 rows out of {totalRowCount} detected rows"
    - Props: previewData (rows, columns, totalRowCount)
    - Responsive design (scrollable on mobile)
    - Follow TanStack React Table patterns if advanced features needed
  - [x] 4.1.7 Create ExcelSheetSelector component
    - File: `client/components/datasets/excel-sheet-selector.tsx`
    - Dropdown selector for Excel files with multiple sheets
    - Display sheet names
    - Props: sheets (string[]), selectedIndex, onSelectSheet
    - Only render if multiple sheets detected
    - Use Shadcn Select component
    - Clear label: "Select sheet to import"
  - [x] 4.1.8 Ensure UI component tests pass
    - Run ONLY the 2-8 tests written in 4.1.2
    - Verify FileUploadZone accepts file drops correctly
    - Verify UploadProgress displays all three phases
    - Verify DatasetPreviewTable renders with sample data
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- The 2-8 tests written in 4.1.2 pass
- FileUploadZone component renders with drag-and-drop functionality
- UploadProgress component displays three phases with smooth transitions
- DatasetPreviewTable renders first 20 rows with type indicators
- ExcelSheetSelector displays sheet options for multi-sheet Excel files
- All components follow Shadcn UI design system
- Components are responsive on mobile devices

---

### Phase 5: Frontend Pages and Integration

#### Task Group 5.1: API Client and State Management
**Dependencies:** Task Group 4.1 (COMPLETED)
**Specialist:** Frontend Engineer

- [x] 5.1.0 Complete API client and state management
  - [x] 5.1.1 Create datasets API client
    - File: `client/web/lib/api/datasets.ts`
    - Function: `uploadDataset(file: File, sheetIndex?: number): Promise<UploadResponse>`
    - Uses multipart/form-data with FormData API
    - POST /datasets/upload with file and optional sheetIndex
    - Returns: dataset ID, preview data, column metadata
    - Function: `getDataset(datasetId: string): Promise<Dataset>`
    - GET /datasets/:id
    - Returns: dataset metadata
    - Function: `getDatasetPreview(datasetId: string): Promise<PreviewData>`
    - GET /datasets/:id/preview
    - Returns: first 20 rows with column metadata
    - Use centralized axios instance from `lib/api/client.ts`
    - Include authentication token in all requests
    - Handle errors with user-friendly messages
    - Follow pattern from `lib/api/auth.ts`
  - [x] 5.1.2 Create React Query hooks for datasets
    - File: `client/web/lib/hooks/use-datasets.ts`
    - Hook: `useUploadDataset()` - mutation for file upload with upload progress tracking
    - Hook: `useDataset(datasetId)` - query for dataset details
    - Hook: `useDatasetPreview(datasetId)` - query for preview data
    - Use @tanstack/react-query for caching and state management
    - Handle loading, error, and success states
    - Implement optimistic updates if applicable

**Acceptance Criteria:**
- API client functions make authenticated requests to backend
- React Query hooks manage loading, error, and success states
- Upload progress is tracked and exposed to components
- Error handling provides clear messages to users

---

#### Task Group 5.2: Import Page Implementation
**Dependencies:** Task Group 5.1 (COMPLETED)
**Specialist:** Frontend Engineer / UI Designer

- [x] 5.2.0 Complete import page
  - [x] 5.2.1 Write 2-8 focused tests for import page workflow
    - Test critical user workflows only (file upload, progress display, preview display, confirm action)
    - Test file upload initiates correctly
    - Test preview displays after upload
    - Test confirm button redirects to dataset detail page
    - Skip exhaustive testing of all states and error scenarios
  - [x] 5.2.2 Create import page component
    - File: `client/web/app/(dashboard)/dashboard/datasets/import/page.tsx`
    - Page layout with header "Import Dataset"
    - Integrate FileUploadZone component
    - Integrate UploadProgress component (conditional rendering based on upload state)
    - Integrate ExcelSheetSelector component (conditional: only for multi-sheet Excel)
    - Integrate DatasetPreviewTable component (conditional: after parsing completes)
    - "Confirm and analyze" button (visible after preview loads)
    - State management using React Query hooks
    - Upload flow:
      1. User drops/selects file
      2. Display Phase 1 progress (0-100% upload)
      3. Call uploadDataset API
      4. Display Phase 2 spinner ("Analyzing...")
      5. Receive preview data
      6. Display Phase 3 transition to preview table
      7. User clicks "Confirm and analyze"
      8. Redirect to `/dashboard/datasets/{id}`
    - Handle multi-sheet Excel: show sheet selector after upload, re-parse on sheet selection
    - Error handling: display error message with "Try again with another file" button
    - Loading states using Button component's loading pattern
  - [x] 5.2.3 Implement upload progress tracking
    - Track upload progress using axios onUploadProgress callback
    - Update progress state (0-100%)
    - Display in UploadProgress component Phase 1
    - Transition to Phase 2 when upload completes
    - Transition to Phase 3 when parsing completes and preview loads
  - [x] 5.2.4 Implement error handling UI
    - Generic error message: "Unable to read file"
    - Sub-messages based on error type: "Unsupported format", "Corrupted file", "Unrecognized encoding", "File too large", "Too many rows"
    - "Try again with another file" button resets state and shows upload zone again
    - Use Shadcn Alert component for error display
    - Follow error display pattern from LoginForm
  - [x] 5.2.5 Implement confirm action
    - "Confirm and analyze" button visible after preview loads
    - On click: save dataset to catalog (already done by backend during upload)
    - Redirect to `/dashboard/datasets/{datasetId}` using Next.js router
    - Pass dataset status to detail page
    - Use React Query to invalidate and refetch if needed
  - [x] 5.2.6 Add responsive design
    - Mobile (320px - 768px): stacked layout, horizontal scroll for table
    - Tablet (768px - 1024px): optimized column widths
    - Desktop (1024px+): full layout with wide table
    - Test on various screen sizes
  - [x] 5.2.7 Ensure import page tests pass
    - Run ONLY the 2-8 tests written in 5.2.1
    - Verify upload flow works end-to-end
    - Verify progress displays correctly through all phases
    - Verify preview displays after upload
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- The 2-8 tests written in 5.2.1 pass
- Import page renders with file upload zone
- Upload progress displays through three phases
- Preview table displays first 20 rows after upload
- Excel multi-sheet files show sheet selector
- Confirm button redirects to dataset detail page
- Error handling displays clear messages with retry option
- Page is responsive on mobile, tablet, and desktop

---

#### Task Group 5.3: Dataset Detail Page (Basic)
**Dependencies:** Task Group 5.2 (COMPLETED)
**Specialist:** Frontend Engineer

- [x] 5.3.0 Complete basic dataset detail page
  - [x] 5.3.1 Create dataset detail page component
    - File: `client/web/app/(dashboard)/dashboard/datasets/[id]/page.tsx`
    - Page layout with dataset name as header
    - Display dataset metadata: file name, file size, format, row count, column count, uploaded date
    - Display processing status indicator:
      - "Analysis in progress..." (spinner) when status is PARSING
      - "Complete" (checkmark) when status is READY
      - "Failed" (error icon) when status is FAILED with error message
    - Status badge using Shadcn Badge component
    - Use useDataset hook to fetch dataset details
    - Polling for status updates when status is PARSING (refresh every 2 seconds)
    - Stop polling when status changes to READY or FAILED
  - [x] 5.3.2 Add navigation back to datasets list
    - "Back to Datasets" link/button (navigate to `/dashboard/datasets` - future page)
    - Breadcrumb navigation: Dashboard > Datasets > {dataset name}
  - [x] 5.3.3 Implement error state handling
    - Display error message if dataset not found (404)
    - Display error message if user lacks permission (403)
    - Display dataset.errorMessage if status is FAILED
    - Provide retry or delete options if applicable (future enhancement)

**Acceptance Criteria:**
- Dataset detail page displays metadata correctly
- Status indicator shows current processing state
- Polling updates status until READY or FAILED
- Error states are handled gracefully
- Navigation back to datasets list works

---

### Phase 6: Testing and Polish

#### Task Group 6.1: Test Review and Gap Analysis
**Dependencies:** Task Groups 1-5
**Specialist:** QA Engineer / Test Engineer

- [x] 6.1.0 Review existing tests and fill critical gaps only
  - [x] 6.1.1 Review tests from previous task groups
    - Review the 2-8 tests written by database engineer (Task 1.1.1)
    - Review the 2-8 tests written by backend engineer for file storage (Task 1.2.1)
    - Review the 2-8 tests written by backend engineer for parsing (Task 2.1.2)
    - Review the 2-8 tests written by API engineer (Task 3.1.1)
    - Review the 2-8 tests written by UI designer (Task 4.1.2)
    - Review the 2-8 tests written by frontend engineer (Task 5.2.1)
    - Total existing tests: approximately 12-48 tests
  - [x] 6.1.2 Analyze test coverage gaps for data import feature only
    - Identify critical user workflows lacking test coverage
    - Focus ONLY on gaps related to data import feature
    - Do NOT assess entire application test coverage
    - Prioritize end-to-end workflows over unit test gaps
    - Examples of critical gaps to check:
      - Multi-sheet Excel file upload workflow
      - CSV with various delimiters (semicolon, tab, pipe)
      - CSV with various encodings (Latin-1, Windows-1252)
      - File size limit enforcement (upload >100MB file)
      - Row count limit enforcement (upload >500k rows)
      - Error handling for corrupted files
      - Tenant isolation enforcement (user cannot access other tenant's datasets)
  - [x] 6.1.3 Write up to 10 additional strategic tests maximum
    - Add maximum of 10 new tests to fill identified critical gaps
    - Focus on integration points and end-to-end workflows
    - Examples:
      - Test CSV upload with semicolon delimiter
      - Test Excel upload with multi-sheet selection
      - Test file size limit rejection (>100MB)
      - Test row count limit rejection (>500k rows)
      - Test encoding detection for Latin-1 CSV
      - Test type detection accuracy for date columns
      - Test tenant isolation (user A cannot access user B's dataset)
      - Test error handling for corrupted Excel file
      - Test complete workflow: upload → preview → confirm → redirect
    - Do NOT write comprehensive coverage for all scenarios
    - Skip edge cases, performance tests, and accessibility tests unless business-critical
  - [x] 6.1.4 Run feature-specific tests only
    - Run ONLY tests related to data import feature
    - Expected total: approximately 22-58 tests maximum
    - Backend tests: `node ace test` (filter for dataset-related tests)
    - Frontend tests: `npm test` (filter for dataset-related tests)
    - Do NOT run the entire application test suite
    - Verify all critical workflows pass

**Acceptance Criteria:**
- All feature-specific tests pass (approximately 22-58 tests total)
- Critical user workflows for data import are covered
- No more than 10 additional tests added when filling in testing gaps
- Testing focused exclusively on data import feature requirements
- Multi-sheet Excel workflow tested
- Various CSV formats (delimiters, encodings) tested
- File and row limits enforced and tested
- Tenant isolation verified

---

#### Task Group 6.2: Edge Case Handling and Polish
**Dependencies:** Task Group 6.1 (COMPLETED)
**Specialist:** QA Engineer / Frontend Engineer

- [x] 6.2.0 Complete edge case handling and polish
  - [x] 6.2.1 Test with various CSV formats manually
    - Test CSV with comma delimiter
    - Test CSV with semicolon delimiter
    - Test CSV with tab delimiter
    - Test CSV with pipe delimiter
    - Test CSV with UTF-8 encoding
    - Test CSV with Latin-1 encoding
    - Test CSV with Windows-1252 encoding
    - Test CSV with BOM marker
    - Test CSV with quoted fields containing delimiters
    - Verify delimiter and encoding auto-detection works
  - [x] 6.2.2 Test with various Excel files manually
    - Test .xlsx file with single sheet
    - Test .xlsx file with multiple sheets
    - Test .xls file (legacy format)
    - Test Excel with empty cells
    - Test Excel with formula cells (should display calculated values)
    - Verify sheet enumeration and selection works
  - [x] 6.2.3 Test edge cases manually
    - Test empty CSV file (0 rows)
    - Test empty Excel file (0 rows)
    - Test CSV with only header row (1 row)
    - Test file with exactly 500,000 rows (should succeed)
    - Test file with 500,001 rows (should fail with clear message)
    - Test file with exactly 100MB size (should succeed)
    - Test file with >100MB size (should fail with clear message)
    - Test malformed CSV (inconsistent column counts)
    - Test corrupted Excel file
    - Test file with very long column names (>255 characters)
    - Test file with very wide tables (>100 columns)
    - Verify error messages are clear and actionable
  - [x] 6.2.4 Performance testing with large files
    - Test upload of 100MB CSV file
    - Measure upload time (target: <30 seconds)
    - Test parsing of 500,000 row CSV file
    - Measure parsing time (target: <5 seconds for preview)
    - Verify page remains responsive during upload and parsing
    - Monitor memory usage (no memory leaks)
    - Use browser DevTools for performance profiling
  - [x] 6.2.5 Mobile responsiveness verification
    - Test on mobile viewport (320px - 768px)
    - Verify file upload zone is usable on mobile
    - Verify preview table scrolls horizontally
    - Verify buttons and text are readable
    - Test on tablet viewport (768px - 1024px)
    - Test on desktop viewport (1024px+)
  - [x] 6.2.6 Accessibility audit
    - Keyboard navigation: Tab through all interactive elements
    - Ensure drag-and-drop zone has keyboard alternative (Browse button)
    - Screen reader testing: ARIA labels, alt text, semantic HTML
    - Color contrast checks for type badges
    - Focus indicators visible on all interactive elements
    - Use Lighthouse for automated accessibility audit (target: >90 score)
  - [x] 6.2.7 Refine error messages based on testing
    - Review all error messages for clarity
    - Ensure error messages provide clear next steps
    - Update messages if user feedback indicates confusion
    - Verify error messages match spec requirements:
      - "Unable to read file"
      - "Unsupported format"
      - "Corrupted file"
      - "Unrecognized encoding"
      - "File too large"
      - "Too many rows"

**Acceptance Criteria:**
- All CSV formats (various delimiters and encodings) parse correctly
- All Excel formats (.xlsx, .xls, multi-sheet) parse correctly
- Edge cases (empty files, size limits, row limits) are handled gracefully
- Performance targets met: 100MB upload <30s, 500k row preview <5s
- Page is responsive on mobile, tablet, and desktop
- Accessibility score >90 on Lighthouse
- Error messages are clear and provide actionable next steps

---

### Phase 7: Deployment Preparation

#### Task Group 7.1: Production Configuration
**Dependencies:** Task Group 6.2 (COMPLETED)
**Specialist:** DevOps Engineer / Backend Engineer

- [x] 7.1.0 Complete production configuration
  - [x] 7.1.1 Configure environment variables for production
    - Add production values to `.env.production` or environment config
    - `STORAGE_ROOT_PATH` - production file storage path
    - `MAX_FILE_SIZE` - 100MB (100 * 1024 * 1024 bytes)
    - `MAX_ROW_COUNT` - 500000
    - `UPLOAD_RATE_LIMIT` - 10 uploads per hour per user
    - Document all dataset-related environment variables
  - [x] 7.1.2 Create production database migration plan
    - Document migration execution steps
    - Test migrations on staging database
    - Create rollback plan if needed
    - Verify foreign key constraints work in production PostgreSQL
  - [x] 7.1.3 Configure storage paths and permissions
    - Create storage directory structure on production server
    - Set proper file permissions (owner: app user, read/write access)
    - Ensure storage path is excluded from version control (.gitignore)
    - Configure backup for uploaded files (separate from database backups)
  - [x] 7.1.4 Set up logging for dataset operations
    - Configure structured logging for upload operations
    - Log: file uploads, parsing results, errors, tenant isolation violations
    - Use log levels: INFO for uploads, WARN for limit violations, ERROR for failures
    - Include context: userId, tenantId, datasetId, fileName, fileSize
    - Follow existing AdonisJS logging patterns
  - [x] 7.1.5 Set up monitoring and alerts
    - Monitor file storage disk usage
    - Alert when storage exceeds 80% capacity
    - Monitor upload endpoint response times
    - Alert when parsing takes >10 seconds
    - Monitor error rates for dataset operations
    - Alert when error rate exceeds 5%
    - Use existing monitoring infrastructure (e.g., Prometheus, Grafana)
  - [x] 7.1.6 Configure rate limiting for production
    - Apply rate limiting middleware to POST /datasets/upload
    - Limit: 10 uploads per hour per user (configurable via env)
    - Return HTTP 429 Too Many Requests with clear message
    - Log rate limit violations
  - [x] 7.1.7 Security audit for file upload
    - Verify file type validation (MIME type + extension)
    - Verify file size limits enforced on backend
    - Verify filename sanitization prevents path traversal
    - Verify tenant isolation prevents unauthorized access
    - Verify authentication required for all dataset endpoints
    - Test for common file upload vulnerabilities:
      - Path traversal attacks
      - File type spoofing
      - Malicious file uploads (e.g., executables disguised as CSV)
      - SQL injection via filename or data
    - Document security measures in `SECURITY.md`
  - [x] 7.1.8 Plan backup strategy for uploaded files
    - Schedule automated backups of `/storage/datasets/` directory
    - Backup frequency: daily (retain 30 days)
    - Backup storage: separate from application server (e.g., S3, network storage)
    - Test backup restoration process
    - Document backup and restore procedures
  - [x] 7.1.9 Create deployment documentation
    - File: `agent-os/specs/2025-11-23-data-import/deployment.md`
    - Document deployment steps:
      1. Run database migrations
      2. Install npm dependencies (backend and frontend)
      3. Configure environment variables
      4. Create storage directories with correct permissions
      5. Build frontend application
      6. Restart backend server
      7. Verify health checks pass
    - Document rollback procedures
    - Document monitoring and logging setup
  - [x] 7.1.10 Create user documentation
    - File: `agent-os/specs/2025-11-23-data-import/user-guide.md`
    - Document how to upload CSV and Excel files
    - Document file size and row limits
    - Document supported formats and auto-detection features
    - Document error messages and troubleshooting
    - Include screenshots of upload interface and preview table
    - Link to documentation from import page (help icon or tooltip)

**Acceptance Criteria:**
- Environment variables configured for production
- Database migrations tested and documented
- Storage paths configured with correct permissions
- Logging captures all dataset operations with context
- Monitoring and alerts configured for disk usage, response times, errors
- Rate limiting prevents upload abuse
- Security audit passed with no critical vulnerabilities
- Backup strategy implemented and tested
- Deployment documentation complete and accurate
- User documentation provides clear instructions

---

## Implementation Notes

### Critical Dependencies
- **Task Group 1.1** must complete before **1.2** (models needed for file storage)
- **Task Group 1.2** must complete before **2.1** (storage needed for parsing)
- **Task Group 2.1** must complete before **3.1** (parser needed for API)
- **Task Group 3.1** must complete before **4.1** (API needed for frontend integration)
- **Task Group 4.1** must complete before **5.1** and **5.2** (components needed for pages)
- **Task Groups 1-5** must complete before **6.1** (cannot test what doesn't exist)
- **Task Group 6.2** must complete before **7.1** (polish before deployment)

### Testing Strategy
- Each development task group (1.1, 1.2, 2.1, 3.1, 4.1, 5.2) writes **2-8 focused tests** for critical behaviors only
- Tests run ONLY for the specific task group, not the entire suite
- Task Group 6.1 adds **up to 10 additional tests** to fill critical gaps
- Total expected tests: approximately **22-58 tests** for the entire data import feature
- Focus on integration workflows, not exhaustive unit coverage

### Technical Patterns to Follow
- **Backend Models**: Follow `app/models/user.ts` pattern with Lucid ORM
- **Backend Services**: Follow `app/services/users_service.ts` pattern for business logic
- **Backend Controllers**: Follow `app/controllers/auth_controller.ts` pattern for route handling
- **Backend Validators**: Follow `app/validators/login_validator.ts` pattern for request validation
- **Frontend Forms**: Follow LoginForm pattern with React Hook Form + Zod
- **Frontend API**: Follow `lib/api/auth.ts` pattern with centralized axios instance
- **Frontend State**: Use React Query for server state, Zustand for client state
- **UI Components**: Follow Shadcn UI design system with Tailwind CSS

### File Paths Reference
**Backend:**
- Models: `server/app/models/`
- Services: `server/app/services/`
- Controllers: `server/app/controllers/`
- Validators: `server/app/validators/`
- Migrations: `server/database/migrations/`
- Routes: `server/start/routes.ts`
- Config: `server/config/`

**Frontend:**
- Pages: `client/web/app/(dashboard)/dashboard/datasets/`
- Components: `client/web/components/datasets/` and `client/web/components/ui/`
- API: `client/web/lib/api/`
- Schemas: `client/web/lib/schemas/`
- Hooks: `client/web/lib/hooks/`

### Libraries to Install
**Backend:**
- `papaparse` - CSV parsing with delimiter and encoding auto-detection
- `xlsx` - Excel file parsing (.xlsx and .xls)
- `file-type` - File format detection via magic numbers
- `iconv-lite` - Character encoding detection and conversion

**Frontend:**
- `react-dropzone` - Drag-and-drop file upload
- Shadcn UI components: `table`, `progress`, `badge`, `alert`

### Success Metrics
- Users can upload CSV and Excel files up to 100MB and 500k rows
- Preview displays first 20 rows within 5 seconds
- Column type detection >90% accurate for common formats
- Upload interface requires no documentation to use
- Mobile-responsive design works on all screen sizes
- Multi-tenant data isolation enforced
- Test coverage >80% for parser service
- No security vulnerabilities in file upload
