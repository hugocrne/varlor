# Specification: Data Import Feature

## Goal
Enable users to upload CSV and Excel files, preview their contents with automatic type detection, and save them to the dataset catalog for analysis. This is the first data ingestion feature for Varlor.

## User Stories
- As a data analyst, I want to upload CSV files so that I can analyze my data in Varlor
- As a business user, I want to upload Excel spreadsheets so that I can leverage my existing reports
- As a user, I want to see a preview of my data before confirming the import so that I can verify the file is correct
- As a user, I want the system to automatically detect column types so that I don't have to manually configure them
- As a user, I want clear feedback during file upload and processing so that I know what's happening

## Core Requirements

### File Upload Interface
- Dedicated import page at `/dashboard/datasets/import`
- Drag-and-drop zone as primary upload method with modern Dropzone-inspired design
- "Browse files" button as alternative upload method
- Single-file upload only for MVP
- Maximum file size: 100MB per file
- Maximum rows: 500,000 rows
- Clear error messages when limits are exceeded

### Supported File Formats
**CSV Files:**
- Auto-detect delimiters: comma, semicolon, tab, pipe
- Auto-detect encoding: UTF-8, Latin-1, Windows-1252
- Support double quotes for escaping

**Excel Files (.xlsx and .xls):**
- Single sheet: automatic import
- Multiple sheets: dropdown selector to choose which sheet to import
- No simultaneous multi-sheet import for MVP

### Upload Progress Feedback
Three-phase progress display on the same page:
- Phase 1: Upload progress bar showing 0-100%
- Phase 2: Spinner with "Analyzing..." message
- Phase 3: Smooth transition to preview display

### Data Preview
- Display first 20 rows only in scrollable table
- Horizontally scrollable for tables with many columns
- Footer text: "Preview of first 20 rows out of {total} detected rows"
- Column headers show detected types with visual indicators

### Type Detection Display
Visual indicators using icons and color badges:
- 📝 Text (text badge)
- 🔢 Number (number badge)
- 📅 Date (date badge)
- ❓ Unknown (unknown badge)
- Display is informational only (no override capability at this stage)

### Post-Upload Actions
- "Confirm and analyze" button after preview
- Saves dataset to catalog
- Launches cleaning pipeline
- Redirects to `/dashboard/datasets/{id}` (dataset detail page)
- Status indicator transitions: "Analysis in progress..." → "Complete"

### Error Handling
Generic error messages with retry capability:
- "Unable to read file" as primary message
- Sub-messages for context:
  - "Unsupported format"
  - "Corrupted file"
  - "Unrecognized encoding"
- "Try again with another file" button
- No detailed diagnostic information (specific row/line errors) for MVP

## Visual Design
No mockups provided. Implementation will follow Shadcn UI design system with modern, clean interface:
- Dropzone-inspired upload area with dashed border and hover state
- Progress bar using Shadcn Progress component
- Data table using Shadcn Table component with TanStack React Table
- Type badges using Shadcn Badge component with color variants
- Loading states using existing Button spinner pattern (Loader2 from lucide-react)

## Reusable Components

### Existing Code to Leverage

**Frontend Patterns:**
- Form handling pattern from LoginForm: React Hook Form + Zod validation
- Button loading state pattern: `<Button loading={isLoading}>` with Loader2 spinner
- API client pattern: Centralized axios instance in `lib/api/client.ts`
- React Query integration for server state management
- Shadcn UI components: Button, Input, Label, Form, Skeleton

**Backend Patterns:**
- Service layer pattern: `app/services/users_service.ts` demonstrates clean business logic separation
- Controller pattern: `app/controllers/auth_controller.ts` shows route handling structure
- Model pattern: `app/models/user.ts` using Lucid ORM with @column decorators
- Migration pattern: `database/migrations/` with up/down methods
- Validation pattern: `app/validators/login_validator.ts` for request validation
- Multi-tenancy pattern: User model includes `tenantId` field for tenant isolation
- Authentication middleware: `middleware.auth()` for protecting routes

**Database Patterns:**
- Lucid ORM BaseModel extension with relationships (@hasMany, @belongsTo)
- DateTime handling using Luxon
- Column decorators: @column, @column.dateTime
- Index creation in migrations
- Foreign key constraints

### New Components Required

**Why New Code is Needed:**
This is the first file upload feature in Varlor and the first data table implementation. No existing patterns for:
- File upload handling with drag-and-drop
- Large file processing and parsing
- Data preview tables
- Dataset storage architecture
- CSV/Excel parsing with auto-detection

**New Frontend Components:**
- `components/datasets/file-upload-zone.tsx` - Drag-and-drop using react-dropzone
- `components/datasets/upload-progress.tsx` - Three-phase progress indicator
- `components/datasets/dataset-preview-table.tsx` - Data table with type indicators
- `components/datasets/excel-sheet-selector.tsx` - Sheet selection dropdown
- `components/ui/table.tsx` - Shadcn Table component (new)
- `components/ui/progress.tsx` - Shadcn Progress component (new)
- `components/ui/badge.tsx` - Shadcn Badge component (new)

**New Backend Services:**
- `app/services/file_storage_service.ts` - File storage management
- `app/services/dataset_parser_service.ts` - CSV/Excel parsing with auto-detection
- `app/services/datasets_service.ts` - Dataset lifecycle management

**New Backend Models:**
- `app/models/dataset.ts` - Dataset catalog model
- `app/models/dataset_column.ts` - Column metadata model

**New Backend Infrastructure:**
- `app/controllers/datasets_controller.ts` - Upload and preview endpoints
- `app/validators/dataset_validator.ts` - File upload validation
- Database migrations for datasets and dataset_columns tables
- File storage structure: `/storage/datasets/{tenant_id}/{dataset_id}/raw/{filename}`

## Technical Approach

### Database Schema Design

**Datasets Table:**
```typescript
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

**Dataset Columns Table:**
```typescript
{
  id: serial (primary key)
  dataset_id: integer (foreign key to datasets.id, cascade delete)
  column_name: string
  column_index: integer
  detected_type: enum('TEXT', 'NUMBER', 'DATE', 'UNKNOWN')
  sample_values: jsonb (array of first 20 sample values)
  created_at: timestamp
  updated_at: timestamp
}
```

### API Endpoints

**POST /datasets/upload**
- Multipart form-data upload
- Request: File upload + optional sheetIndex (for multi-sheet Excel)
- Response: Dataset ID, preview data, column metadata
- Authentication required
- Validates file size, row count, format

**GET /datasets/:id**
- Returns dataset details and metadata
- Includes status, row count, column count, processing state
- Authentication required
- Tenant-scoped query

**GET /datasets/:id/preview**
- Returns first 20 rows of data with column metadata
- Authentication required
- Tenant-scoped query

### File Processing Pipeline
1. **Upload Phase**: Receive file via multipart upload, validate size and format
2. **Storage Phase**: Save raw file to local filesystem with tenant isolation
3. **Parsing Phase**:
   - Detect file format (CSV vs Excel)
   - For CSV: Auto-detect delimiter and encoding
   - For Excel: Enumerate sheets, use selected sheet
   - Extract first 20 rows for preview
   - Count total rows (up to 500k limit)
   - Infer column types (text, number, date, unknown)
4. **Catalog Phase**: Save dataset and column metadata to database
5. **Response Phase**: Return preview data to frontend

### Type Detection Algorithm
- **Number**: Regex match for numeric patterns (integers, floats, scientific notation)
- **Date**: Regex match for common date formats (ISO, US, EU), validate with date parser
- **Text**: Default if not number or date
- **Unknown**: Unable to determine type (mixed types, all nulls/empty)

### Multi-Tenancy Implementation
- All dataset records include `tenant_id` field
- Storage paths include tenant ID: `/storage/datasets/{tenant_id}/{dataset_id}/`
- Database queries filtered by authenticated user's tenant
- Foreign key relationship: `datasets.tenant_id → users.tenant_id`
- Follow existing User model pattern for consistency

### File Storage Strategy
**MVP Approach (Local Filesystem):**
- Storage root: `/storage/datasets/`
- Path pattern: `{tenant_id}/{dataset_id}/raw/{filename}`
- Filename sanitization to prevent path traversal attacks
- File deletion on dataset removal

**Future Enhancement (Deferred):**
- MinIO/S3 integration using @adonisjs/drive
- Distributed storage for scalability
- Versioning support

### Security Considerations
- File type validation: Check MIME type AND file extension
- File size limits enforced on both frontend and backend
- Tenant isolation: Users can only access their tenant's datasets
- Authentication middleware required for all dataset endpoints
- Filename sanitization to prevent directory traversal
- Rate limiting on upload endpoint (prevent abuse)
- Validate file contents during parsing (detect malicious code/macros)

### Performance Considerations
- Stream-based CSV parsing to avoid loading entire file in memory
- Excel parsing optimized for large files using streaming readers
- Preview limited to first 20 rows for fast response
- Row counting optimized (don't parse entire file if possible)
- Database indexes on tenant_id and user_id for fast queries
- Consider background job processing for files >10MB (future enhancement)

## Out of Scope

**Explicitly Excluded from MVP:**
- Inline data editing in preview
- Import from URL or APIs
- Saved upload templates
- Cloud storage connections (S3, Google Drive, Dropbox)
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
- Advanced cleaning pipeline (separate roadmap item)
- Data quality metrics dashboard

**Future Enhancements (Alpha/Beta):**
- Batch import feature for multiple files
- Advanced preview options (configurable row count, sampling)
- Column type override during import
- Data transformation capabilities
- S3/MinIO storage backend
- Import from external sources (URLs, APIs, databases)
- Scheduled imports with cron expressions
- Advanced data validation rules engine
- Template saving for repeated imports

## Success Criteria

**Functional Success:**
- Users can successfully upload CSV files up to 100MB and 500k rows
- Users can successfully upload Excel files (.xlsx and .xls)
- Multi-sheet Excel files display sheet selector
- Preview displays first 20 rows with correct data
- Column types are detected with >90% accuracy for common formats
- Upload progress displays correctly through all three phases
- Datasets are saved to catalog and accessible via detail page
- Error messages are clear and actionable

**Performance Success:**
- File upload completes within 30 seconds for 100MB files
- Preview generation completes within 5 seconds for 500k row files
- Page remains responsive during upload and parsing
- No memory leaks during large file processing

**User Experience Success:**
- Upload interface is intuitive and requires no documentation
- Users understand the state of their upload at all times
- Errors provide clear next steps (retry, try different file)
- Preview table is readable and scrollable on all screen sizes
- Mobile-responsive design (though desktop is primary use case)

**Technical Success:**
- Multi-tenant data isolation is enforced
- All endpoints are properly authenticated
- Database schema supports future enhancements
- Code follows existing Varlor patterns and conventions
- Test coverage >80% for parser service
- No security vulnerabilities in file upload

## Dependencies and Libraries

### Frontend Dependencies to Add
- `react-dropzone` - Drag-and-drop file upload interface
- `@tanstack/react-table` - Advanced table functionality (if needed beyond basic Shadcn Table)

### Backend Dependencies to Add
- `papaparse` - CSV parsing with auto-detection (preferred over csv-parser)
- `xlsx` - Excel file parsing (.xlsx and .xls formats)
- `file-type` - Reliable file format detection via magic numbers
- `iconv-lite` - Character encoding detection and conversion

### Shadcn UI Components to Add
- Table component: `npx shadcn@latest add table`
- Progress component: `npx shadcn@latest add progress`
- Badge component: `npx shadcn@latest add badge`

### Existing Dependencies to Use
- **Frontend**: Next.js, React Query, Zustand, Zod, React Hook Form, Axios, Lucide React
- **Backend**: AdonisJS, Lucid ORM, PostgreSQL, Luxon

## Implementation Phases

### Phase 1: Backend Foundation (Priority: High)
**Scope**: Database schema, models, file storage service

**Tasks**:
- Create datasets table migration
- Create dataset_columns table migration
- Implement Dataset model with Lucid ORM
- Implement DatasetColumn model with Lucid ORM
- Implement FileStorageService for local filesystem storage
- Add file storage configuration
- Unit tests for file storage service

**Success Criteria**: Database schema created, models functional, file storage works

### Phase 2: File Parsing Service (Priority: High)
**Scope**: CSV and Excel parsing with auto-detection

**Tasks**:
- Implement DatasetParserService
- CSV parser with delimiter auto-detection
- CSV encoder detection (UTF-8, Latin-1, Windows-1252)
- Excel parser with sheet enumeration
- Column type detection algorithm
- Row counting for large files
- Preview generation (first 20 rows)
- Comprehensive unit tests for various file formats
- Edge case handling (empty files, malformed data)

**Success Criteria**: Parser handles CSV and Excel correctly, type detection accurate

### Phase 3: Backend API (Priority: High)
**Scope**: Controllers, validators, routes

**Tasks**:
- Implement DatasetsService for orchestration
- Implement DatasetsController with upload, show, preview methods
- Create DatasetValidator for file upload validation
- Configure routes with authentication middleware
- Error handling and response formatting
- Integration tests for upload flow
- API documentation

**Success Criteria**: API endpoints functional, properly authenticated, error handling works

### Phase 4: Frontend Components (Priority: High)
**Scope**: UI components for upload and preview

**Tasks**:
- Add Shadcn UI components (Table, Progress, Badge)
- Implement FileUploadZone component with react-dropzone
- Implement UploadProgress component (three phases)
- Implement DatasetPreviewTable component
- Implement ExcelSheetSelector component
- Create dataset.schema.ts with Zod validation
- Component unit tests with Jest and React Testing Library

**Success Criteria**: All UI components render correctly, responsive, accessible

### Phase 5: Frontend Pages and Integration (Priority: High)
**Scope**: Import page and dataset detail page

**Tasks**:
- Create datasets API client (lib/api/datasets.ts)
- Implement `/dashboard/datasets/import` page
- Implement `/dashboard/datasets/[id]` page (basic version)
- Integrate components with React Query for state management
- Error handling and user feedback
- Loading states and transitions
- Navigation and routing
- End-to-end tests with Playwright

**Success Criteria**: Complete user flow works from upload to preview to catalog

### Phase 6: Testing and Polish (Priority: Medium)
**Scope**: Edge cases, performance, UX improvements

**Tasks**:
- Test with various CSV formats (different delimiters, encodings)
- Test with various Excel files (single/multi-sheet, .xls/.xlsx)
- Test with edge cases (empty files, huge files, malformed data)
- Performance testing with large files
- Mobile responsiveness verification
- Accessibility audit (keyboard navigation, screen readers)
- Error message refinement based on testing
- Documentation updates

**Success Criteria**: All edge cases handled, performance meets targets, accessible

### Phase 7: Deployment Preparation (Priority: Low)
**Scope**: Production readiness

**Tasks**:
- Environment configuration (storage paths, file limits)
- Database migration scripts for production
- Logging and monitoring setup
- Rate limiting configuration
- Security audit (file upload vulnerabilities)
- Backup strategy for uploaded files
- Deployment documentation
- User documentation

**Success Criteria**: Ready for production deployment, monitored, documented
