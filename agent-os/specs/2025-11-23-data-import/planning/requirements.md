# Spec Requirements: Data Import (files only)

## Initial Description

#### B. Data Import (files only)
- [ ] "Import Dataset" page:
  - [ ] CSV or Excel file upload
  - [ ] Reasonable max size (e.g., a few hundred thousand rows initially)
- [ ] After upload, user sees:
  - [ ] Preview of first rows
  - [ ] Detected columns with assumed type: text, number, date

## Requirements Discussion

### First Round Questions

**Q1:** For the file upload interface, I'm assuming we'll create a dedicated page at something like `/dashboard/datasets/import` with a drag-and-drop zone as the primary upload method and a "Browse files" button as an alternative. Should we also include a multi-step wizard approach (upload → configure → confirm) or keep it simpler with everything on one page?

**Answer:** Dedicated page at `/dashboard/datasets/import`. Drag-and-drop zone central with alternative "Browse" button. Simple interface, no multi-step wizard for MVP. Modern Dropzone-inspired design.

**Q2:** I'm thinking 100MB max file size and 500,000 rows maximum for the MVP, with clear error messages if exceeded. Does this align with your expectations, or should we support larger datasets initially?

**Answer:** 100MB max per file, 500,000 rows max. Clear error message if exceeded. These limits are perfect for MVP.

**Q3:** For the upload progress and processing feedback, should we show a loading state on the same page, or redirect to a separate "processing status" page? I'm assuming inline loading indicators with phases like "Uploading..." → "Analyzing..." → "Preview ready" would be clearest.

**Answer:** Loading states on the same page. Three phases:
- Phase 1: Upload progress bar (0-100%)
- Phase 2: Spinner + "Analyzing..." message
- Phase 3: Transition to preview
No redirect to status page (too complex for MVP).

**Q4:** For the preview of "first rows", I'm assuming we show the first 20 rows in a scrollable table. Should users be able to adjust this (e.g., "show first 50 rows" or "random sample") or is a fixed preview sufficient for MVP?

**Answer:** Show first 20 rows only. Table scrollable horizontally if many columns. No adjustment options for MVP. Footer mention: "Preview of first 20 rows out of {total} detected rows".

**Q5:** Regarding type detection (text, number, date), should we display these types with visual indicators (icons, color coding) and allow users to override them immediately, or just show them as informational labels at this stage?

**Answer:** Visual display with icons, no immediate override. Icons:
- 📝 Text
- 🔢 Number
- 📅 Date
- ❓ Unknown
Color badge per type. No override at this stage (handled in cleaning phase). Just display visual indicator.

**Q6:** After viewing the preview, what actions should be available? I'm assuming a "Confirm and import" button that saves the dataset to the catalog, but should there also be options to go back and upload a different file, or configure column mappings before confirming?

**Answer:** After preview, "Confirm and analyze" button. Saves to catalog + launches cleaning pipeline. Redirects to `/dashboard/datasets/{id}` (dataset detail page). Status display: "Analysis in progress..." → "Complete".

**Q7:** For error handling during upload or parsing, should we show detailed error messages (e.g., "Error at row 347: invalid date format") or more general feedback like "Unable to read file, please check format"? I'm thinking general errors with a "try again" option would be less overwhelming for MVP.

**Answer:** Simple error with retry. Generic message: "Unable to read file". Possible sub-messages:
- "Unsupported format"
- "Corrupted file"
- "Unrecognized encoding"
"Try again with another file" button. No detailed diagnostics for MVP.

**Q8:** Should the interface support uploading multiple files at once, or is single-file upload sufficient for the MVP? I'm assuming single-file to keep the scope focused.

**Answer:** Single-file only for MVP. One file at a time. If need multiple, repeat import process. "Batch import" feature deferred to Alpha/Beta.

**Q9:** For CSV and Excel file support, are there specific details we should handle:
- CSV: delimiter auto-detection (comma, semicolon, tab)? Encoding detection (UTF-8, Latin-1)?
- Excel: should we support both .xlsx and .xls? What about multiple sheets—import first sheet only, or let user choose?

**Answer:**

CSV:
- Auto-detect delimiters: , ; \t |
- Auto-detect encoding: UTF-8, Latin-1, Windows-1252
- Support double quotes for escaping

Excel:
- Support .xlsx and .xls
- Multi-sheets:
  - If 1 sheet: automatic import
  - If multiple sheets: dropdown to choose which sheet to import
  - No simultaneous multi-sheet import for MVP

**Q10:** What should explicitly NOT be included in this MVP? For example: data validation rules, transformation options, scheduled imports, importing from URLs/APIs, etc.? This helps set clear boundaries.

**Answer:** Explicitly OUT of scope:
- Inline data editing in preview
- Import from URL
- Saved upload templates
- Cloud storage connection (S3, Drive, Dropbox)
- Scheduled/recurring imports
- Custom validation rules during upload
- Custom column mapping
- Merging multiple files
- Data transformation before import
- File version history
- Comments/annotations on columns

### Existing Code to Reference

**User Response:** "Watch in the code" for dataset storage and catalog architecture.

**Codebase Analysis Findings:**

Based on exploration of the Varlor codebase, the following patterns and infrastructure were identified:

#### 1. File Upload Components
**Status:** ❌ None existing - First upload feature in Varlor

**To Create:**
- New upload component using react-dropzone library
- Build with Shadcn UI components
- This will establish the pattern for all future file uploads in Varlor

**Available Foundation:**
- Existing Shadcn UI components: Button, Input, Label, Form, Skeleton
- Button component already has loading state with spinner (Loader2 from lucide-react)
- Form handling pattern established in LoginForm using React Hook Form + Zod

#### 2. Data Table/Grid Components
**Status:** ❌ No existing table component

**To Create:**
- First data table in the application
- Use Shadcn UI `<Table>` component (needs to be added)
- Integrate @tanstack/react-table for advanced features (already in package.json v5.90.10)
- This will be THE reference pattern for all future data tables in Varlor

**Available in package.json:**
- @tanstack/react-query: v5.90.10 (for data fetching, not table display)
- Need to add: @tanstack/react-table

#### 3. Progress Indicators
**Status:** ⚠️ Partial - Only in LoginForm

**Existing Pattern:**
- Button component has `loading` prop with Loader2 spinner
- LoginForm demonstrates loading state: `<Button loading={isLoading}>`
- Pattern: disabled state + spinner icon + text change

**To Create:**
- Progress bar component (0-100%) for file upload
- Global spinner component for analysis phase
- Establish comprehensive loading states pattern

#### 4. Dataset Storage & Catalog Architecture
**Status:** 🔨 Does not exist yet - needs to be built

**Current Database State:**
- **Database:** PostgreSQL 16 (docker-compose.yml)
- **ORM:** AdonisJS Lucid (Lucid ORM)
- **Existing Tables:** Only authentication-related
  - users (id, email, password, role, tenant_id, timestamps)
  - refresh_tokens
  - roles
  - auth_access_tokens

**Storage Infrastructure:**
- **No S3/MinIO configuration found** in current codebase
- Tech stack specifies: S3-compatible storage (MinIO for self-hosted)
- Data lake format: Parquet, ORC, Aveo (per tech-stack.md)

**Needs to be Created:**
1. **Dataset Model & Migration:**
   - Database table for datasets catalog
   - Fields likely needed: id, name, file_name, file_size, row_count, column_count, format (CSV/Excel), status, tenant_id, user_id, uploaded_at, processed_at, storage_path
   - Follow existing migration pattern in `server/database/migrations/`

2. **Dataset Columns Metadata:**
   - Table for column metadata
   - Fields: id, dataset_id, column_name, column_index, detected_type, sample_values

3. **File Storage Service:**
   - Initially: Local filesystem storage (simple for MVP)
   - Path structure: `/storage/datasets/{tenant_id}/{dataset_id}/raw/{filename}`
   - Future: MinIO/S3 integration (deferred to Alpha/Beta)

4. **Dataset Processing Pipeline:**
   - Upload → Parse → Store raw file → Extract metadata → Save to DB
   - Status tracking: UPLOADING → PARSING → READY → FAILED

**Backend Patterns to Follow:**
- Service layer pattern: `app/services/` (e.g., auth_service.ts, users_service.ts)
- Create: `datasets_service.ts`, `file_storage_service.ts`, `dataset_parser_service.ts`
- Controller pattern: `app/controllers/` (e.g., auth_controller.ts)
- Create: `datasets_controller.ts`
- Model pattern: `app/models/` using Lucid ORM BaseModel
- Create: `dataset.ts`, `dataset_column.ts`

**API Routing Pattern:**
```typescript
// server/start/routes.ts
router
  .group(() => {
    router.post('/datasets/upload', '#controllers/datasets_controller.upload')
    router.get('/datasets/:id', '#controllers/datasets_controller.show')
    router.get('/datasets/:id/preview', '#controllers/datasets_controller.preview')
  })
  .prefix('/datasets')
  .use(middleware.auth())
```

#### 5. Frontend Architecture Patterns

**Existing Structure:**
- **Framework:** Next.js 16 with App Router
- **Styling:** Tailwind CSS v4.1.0
- **State Management:**
  - Zustand v5.0.8 for client state
  - @tanstack/react-query v5.90.10 for server state
- **Forms:** React Hook Form v7.66.1 + Zod v4.1.12
- **HTTP Client:** Axios v1.13.2
- **Icons:** Lucide React v0.554.0

**Route Structure:**
- Auth pages: `app/(auth)/login/page.tsx`
- Dashboard pages: `app/(dashboard)/dashboard/page.tsx`
- Layouts: `components/layouts/auth-layout.tsx`, `dashboard-layout.tsx`

**API Client Pattern:**
```typescript
// lib/api/client.ts - centralized axios instance
// lib/api/auth.ts - auth-specific API calls
// Pattern: API functions return typed responses, errors handled centrally
```

**To Create for Data Import:**
- Page: `app/(dashboard)/dashboard/datasets/import/page.tsx`
- Page: `app/(dashboard)/dashboard/datasets/[id]/page.tsx`
- API: `lib/api/datasets.ts`
- Schema: `lib/schemas/dataset.schema.ts` (Zod validation)
- Components:
  - `components/datasets/file-upload-zone.tsx`
  - `components/datasets/dataset-preview-table.tsx`
  - `components/datasets/upload-progress.tsx`
  - `components/ui/table.tsx` (Shadcn component - needs adding)
  - `components/ui/progress.tsx` (Shadcn component - needs adding)

#### 6. Libraries to Add

**Frontend:**
- `react-dropzone` - for drag & drop file upload
- `@tanstack/react-table` - for data table display
- Shadcn UI components to add:
  - Table component
  - Progress component
  - Badge component (for type indicators)

**Backend:**
- `csv-parser` or `papaparse` - for CSV parsing
- `xlsx` - for Excel parsing
- `file-type` - for file format detection
- `iconv-lite` - for encoding detection
- `multer` or AdonisJS BodyParser (already configured) - for file uploads

### Follow-up Questions

None required - comprehensive technical exploration completed.

## Visual Assets

### Files Provided:

**Mandatory Check Performed:** ✅

```bash
ls -la /Users/hugo/Perso/Projets/varlor/agent-os/specs/2025-11-23-data-import/planning/visuals/
```

**Result:** No visual files found

### Visual Insights:

No visual assets provided. Specification will proceed based on functional requirements only.

## Requirements Summary

### Functional Requirements

**File Upload Interface:**
- Dedicated page at `/dashboard/datasets/import`
- Drag-and-drop zone as primary upload method
- "Browse files" button as alternative
- Modern Dropzone-inspired design
- Single-file upload only for MVP

**File Constraints:**
- Maximum file size: 100MB
- Maximum rows: 500,000
- Clear error messages if limits exceeded

**Supported Formats:**
- CSV files:
  - Auto-detect delimiters: comma, semicolon, tab, pipe
  - Auto-detect encoding: UTF-8, Latin-1, Windows-1252
  - Support double quotes for escaping
- Excel files (.xlsx and .xls):
  - Single sheet: automatic import
  - Multiple sheets: dropdown to select sheet

**Upload Progress Feedback:**
- Phase 1: Upload progress bar (0-100%)
- Phase 2: Spinner with "Analyzing..." message
- Phase 3: Transition to preview
- All states displayed inline on same page

**Data Preview:**
- Display first 20 rows only
- Horizontally scrollable table for many columns
- Footer text: "Preview of first 20 rows out of {total} detected rows"

**Type Detection Display:**
- Visual indicators with icons and color badges:
  - 📝 Text
  - 🔢 Number
  - 📅 Date
  - ❓ Unknown
- Informational display only (no override at this stage)

**Post-Upload Actions:**
- "Confirm and analyze" button
- Saves dataset to catalog
- Launches cleaning pipeline
- Redirects to `/dashboard/datasets/{id}`
- Status indicator: "Analysis in progress..." → "Complete"

**Error Handling:**
- Generic error message: "Unable to read file"
- Sub-messages for context:
  - "Unsupported format"
  - "Corrupted file"
  - "Unrecognized encoding"
- "Try again with another file" button
- No detailed diagnostic information (e.g., specific row/line errors)

### Reusability Opportunities

**Existing Patterns to Leverage:**

1. **Form Handling Pattern** (from LoginForm):
   - React Hook Form + Zod validation
   - Error display with AlertCircle icon
   - Loading states with button spinner
   - Form field structure and styling

2. **Service Layer Pattern** (backend):
   - Model services in `app/services/` (auth_service.ts, users_service.ts, token_service.ts)
   - Clean separation of business logic from controllers
   - Error handling with custom exceptions
   - TypeScript interfaces for type safety

3. **API Controller Pattern** (backend):
   - Route grouping with prefix and middleware
   - Controller methods with clear responsibilities
   - Authentication middleware integration
   - Standard response formats

4. **API Client Pattern** (frontend):
   - Centralized axios instance in `lib/api/client.ts`
   - Type-safe API calls
   - Centralized error handling
   - React Query integration for caching and loading states

5. **Database Model Pattern** (backend):
   - Lucid ORM BaseModel extension
   - Column decorators (@column, @column.dateTime)
   - Relationships (@hasMany, @belongsTo)
   - Migration structure with up/down methods

6. **UI Component Patterns**:
   - Shadcn UI component structure
   - Tailwind CSS utility classes
   - Class variance authority for variants
   - Lucide icons for visual indicators

**No Similar Features Exist:**
- This is the first file upload feature in Varlor
- This is the first data table/grid in the application
- This establishes patterns for future dataset management features
- Dataset catalog and storage architecture needs to be built from scratch

### Scope Boundaries

**In Scope:**
- File upload interface with drag-and-drop
- CSV and Excel file support with auto-detection
- File size and row limits
- Upload progress indicators
- Preview of first 20 rows
- Type detection visualization
- Confirmation and import to catalog
- Basic error handling with retry
- Excel multi-sheet selection
- Dataset database schema and models
- File storage service (local filesystem for MVP)
- Dataset parsing service
- REST API endpoints for upload and preview
- Frontend pages and components

**Out of Scope:**
- Inline data editing in preview
- Import from URL or APIs
- Saved upload templates
- Cloud storage integrations (S3, Drive, Dropbox)
- Scheduled or recurring imports
- Custom validation rules during upload
- Custom column mapping interface
- Batch/multiple file upload
- Merging multiple files
- Data transformation before import
- File version history
- Comments or annotations on columns
- Detailed error diagnostics (specific row/line errors)
- Preview customization (row count, random sampling)
- Type override during preview
- Multi-sheet simultaneous import
- MinIO/S3 integration (deferred)
- Advanced cleaning pipeline (handled in separate roadmap item)
- Data quality metrics (separate feature)

**Future Enhancements (deferred to Alpha/Beta):**
- Batch import feature
- Advanced preview options
- Column type override during import
- Data transformation capabilities
- S3/MinIO storage backend
- Import from external sources (URLs, APIs, databases)
- Scheduled imports
- Advanced data validation rules

### Technical Considerations

**Database Architecture:**

*Needs to be Created:*

1. **Datasets Table:**
```typescript
// Migration: create_datasets_table.ts
{
  id: serial (primary key)
  tenant_id: string (foreign key to users.tenant_id, indexed)
  user_id: integer (foreign key to users.id)
  name: string (user-provided or auto-generated from filename)
  file_name: string (original uploaded filename)
  file_size: bigint (bytes)
  file_format: enum('CSV', 'EXCEL')
  storage_path: string (path to raw file)
  row_count: integer
  column_count: integer
  status: enum('UPLOADING', 'PARSING', 'READY', 'FAILED')
  error_message: text (nullable)
  uploaded_at: timestamp
  processed_at: timestamp (nullable)
  created_at: timestamp
  updated_at: timestamp
}
```

2. **Dataset Columns Table:**
```typescript
// Migration: create_dataset_columns_table.ts
{
  id: serial (primary key)
  dataset_id: integer (foreign key to datasets.id)
  column_name: string
  column_index: integer
  detected_type: enum('TEXT', 'NUMBER', 'DATE', 'UNKNOWN')
  sample_values: jsonb (array of sample values for preview)
  created_at: timestamp
  updated_at: timestamp
}
```

**Backend Services to Create:**

1. **FileStorageService** (`app/services/file_storage_service.ts`):
   - Store uploaded files to local filesystem
   - Generate unique storage paths
   - Validate file size limits
   - Handle file deletion
   - Path pattern: `/storage/datasets/{tenant_id}/{dataset_id}/raw/{filename}`

2. **DatasetParserService** (`app/services/dataset_parser_service.ts`):
   - Detect file format
   - Parse CSV (detect delimiter, encoding)
   - Parse Excel (list sheets, select sheet)
   - Extract column metadata
   - Generate preview data (first 20 rows)
   - Detect column types (text, number, date, unknown)
   - Handle parsing errors gracefully

3. **DatasetsService** (`app/services/datasets_service.ts`):
   - Create dataset records
   - Update dataset status
   - Fetch dataset by ID
   - List datasets for tenant
   - Handle dataset lifecycle
   - Coordinate file storage + parsing

**Backend Models to Create:**

1. **Dataset** (`app/models/dataset.ts`):
   - Lucid ORM model
   - Relationships: belongsTo User, hasMany DatasetColumn
   - Status enum
   - File format enum

2. **DatasetColumn** (`app/models/dataset_column.ts`):
   - Lucid ORM model
   - Relationship: belongsTo Dataset
   - Type enum

**Backend Controllers to Create:**

1. **DatasetsController** (`app/controllers/datasets_controller.ts`):
   - `upload()` - Handle file upload, parse, store
   - `show()` - Get dataset details
   - `preview()` - Get preview data (first 20 rows)

**Backend Validators to Create:**

1. **DatasetValidator** (`app/validators/dataset_validator.ts`):
   - File upload validation (size, format)
   - Dataset ID validation
   - Sheet selection validation (for multi-sheet Excel)

**Backend Routes:**
```typescript
// server/start/routes.ts
router
  .group(() => {
    router.post('/upload', '#controllers/datasets_controller.upload')
    router.get('/:id', '#controllers/datasets_controller.show')
    router.get('/:id/preview', '#controllers/datasets_controller.preview')
  })
  .prefix('/datasets')
  .use(middleware.auth())
```

**Frontend Pages to Create:**

1. **Import Page** (`app/(dashboard)/dashboard/datasets/import/page.tsx`):
   - File upload zone
   - Upload progress display
   - Preview display
   - Confirm button

2. **Dataset Detail Page** (`app/(dashboard)/dashboard/datasets/[id]/page.tsx`):
   - Dataset metadata
   - Processing status
   - Full data view (future)

**Frontend Components to Create:**

1. **FileUploadZone** (`components/datasets/file-upload-zone.tsx`):
   - Drag-and-drop area using react-dropzone
   - Browse button
   - File validation
   - File preview (name, size)

2. **UploadProgress** (`components/datasets/upload-progress.tsx`):
   - Phase 1: Progress bar (0-100%)
   - Phase 2: Spinner + "Analyzing..."
   - Phase 3: Transition to preview

3. **DatasetPreviewTable** (`components/datasets/dataset-preview-table.tsx`):
   - Table header with column names and type icons
   - First 20 rows
   - Horizontal scroll
   - Footer with row count

4. **ExcelSheetSelector** (`components/datasets/excel-sheet-selector.tsx`):
   - Dropdown to select sheet (multi-sheet Excel only)
   - Sheet name display

5. **UI Components to Add** (Shadcn):
   - `components/ui/table.tsx` - Table component
   - `components/ui/progress.tsx` - Progress bar
   - `components/ui/badge.tsx` - Type indicator badges

**Frontend API Client:**

1. **Datasets API** (`lib/api/datasets.ts`):
```typescript
uploadDataset(file: File, sheetIndex?: number)
getDataset(id: string)
getDatasetPreview(id: string)
```

**Frontend Schemas:**

1. **Dataset Schema** (`lib/schemas/dataset.schema.ts`):
   - Zod schemas for dataset data
   - Type definitions
   - Validation rules

**Libraries to Install:**

*Frontend:*
- `react-dropzone` - Drag & drop file upload
- `@tanstack/react-table` - Table component (if using advanced features)

*Backend:*
- `csv-parser` OR `papaparse` - CSV parsing with auto-detection
- `xlsx` - Excel file parsing (.xlsx and .xls)
- `file-type` - File format detection
- `iconv-lite` - Character encoding detection
- Consider: `@adonisjs/drive` if planning S3/MinIO in near future

**File Processing Requirements:**
- Delimiter auto-detection for CSV (comma, semicolon, tab, pipe)
- Encoding auto-detection (UTF-8, Latin-1, Windows-1252)
- Excel sheet enumeration and selection
- Type inference for columns (text, number, date, unknown)
- Row counting for large files (up to 500,000 rows)
- Efficient preview generation (only first 20 rows extracted)

**UI/UX Patterns:**
- Dropzone-style file upload component
- Progress bars and spinners (reuse Button loading pattern)
- Scrollable data tables
- Icon-based type indicators with color coding
- Inline error messages with retry capability
- Follow Shadcn UI design system

**Performance Considerations:**
- Handle files up to 100MB
- Process up to 500,000 rows
- Preview generation limited to first 20 rows for performance
- Stream parsing for large files (don't load entire file in memory)
- Background processing for parsing (consider async job if slow)
- Progress updates during upload via chunked upload or WebSocket (future)

**Security Considerations:**
- File type validation (MIME type + extension check)
- File size limits enforced on backend
- Tenant isolation (user can only access their tenant's datasets)
- Authentication required for all dataset endpoints
- Sanitize filenames to prevent path traversal
- Validate file contents before parsing (check for malicious code)

**Multi-Tenancy:**
- Dataset records include tenant_id
- Storage paths include tenant_id for isolation
- Queries filtered by authenticated user's tenant_id
- Follow existing User model pattern (tenant_id field)

**Error Handling Strategy:**
- Frontend: Generic user-friendly messages with retry
- Backend: Detailed logging for debugging
- Status tracking: UPLOADING → PARSING → READY → FAILED
- Error messages stored in dataset.error_message for admin review
- Graceful degradation (e.g., if type detection fails, mark as UNKNOWN)

**Testing Considerations:**
- Unit tests for parser service (various CSV/Excel formats)
- Integration tests for upload flow
- Edge cases: empty files, malformed data, encoding issues
- Follow existing test patterns (Jest for frontend, Japa for backend)
