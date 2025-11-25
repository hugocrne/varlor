# Spec Requirements: Simple Automatic Cleaning

## Initial Description
Implement Simple Automatic Cleaning functionality for datasets with automatic detection and correction of common data quality issues including missing values, duplicate rows, invalid formats, outliers, and inconsistent categorical data. The feature should provide transparent corrections with logging, quality scoring, and seamless integration into existing upload flow.

## Requirements Discussion

### First Round Questions

**Q1:** Should Simple Automatic Cleaning run automatically after every file upload, or should users trigger it manually? Also, should it be part of a batch processing pipeline or run synchronously during upload?

**Answer:** Automatic after parsing in same pipeline (Upload → Parsing → Cleaning → Ready), synchronous processing for MVP with files < 100MB

**Q2:** For data storage after cleaning, should we:
A) Replace the original file entirely, or B) maintain both versions with version history?
Also, where should we store cleaning metadata and correction logs - in the database, separate files, or both?

**Answer:** Maintain both versions with raw/ (original), cleaned/ (cleaned version), metadata/ (quality metrics + correction logs) folders

**Q3:** How should we structure the database schema for storing cleaning metadata?
A) Add columns to existing tables (datasets, dataset_columns), B) Create new dedicated tables for cleaning metadata, or C) Hybrid approach?

**Answer:** Hybrid approach - add columns to existing tables (datasets: cleaning_status, corrections_applied, quality_score; dataset_columns: missing_count, invalid_count, percentages) + new dataset_cleaning_logs table for detailed tracing

**Q4:** For UI integration, where should cleaning results be displayed?
A) As a separate cleaning results page, B) Within the existing dataset preview page as an expandable section, or C) As a modal overlay on the preview page?

**Answer:** Expandable section in preview page, above preview table, collapsible (default closed), quality score + badges visible, column details in expansion, no modal/separate page

**Q5:** For duplicate handling specifically, should we:
A) Remove duplicates automatically and keep only unique rows, B) Flag duplicates and let users decide, or C) Create a separate cleaned version while preserving original?

**Answer:** Flagging only - detect and report exact row duplicates, count and show percentage, preserve all duplicates in data, no automatic deletion

**Q6:** For quality thresholds and scoring, what severity levels should we use for the quality badges?
A) Simple (Good/Poor), B) Moderate (Excellent/Good/Fair/Poor), or C) Detailed (Excellent/Good/Warning/Bad/Critical)? Also, what should be the threshold values for these levels?

**Answer:** Show all metrics with severity badges (Excellent <5%, Medium 5-20%, Poor >20%), global quality score 90-100% (green), 70-89% (orange), <70% (red)

**Q7:** For processing architecture, should we use synchronous processing in the upload flow, or should cleaning be handled as background jobs to avoid blocking the user experience? Also, what's the target file size/row count for synchronous vs asynchronous processing?

**Answer:** Same synchronous pipeline for MVP (files <100MB, <500k rows, <10sec processing), loading state on page, future background jobs for larger files

**Q8:** Are there existing patterns in your codebase for similar features we should reference?
A) File processing and storage patterns, B) Data validation or transformation logic, C) Background job processing, D) UI components for similar data operations?

**Answer:** Reuse auth module patterns for both backend (AdonisJS services/controllers/models/migrations) and frontend (Next.js API calls/Zustand/TanStack Query), establish new patterns for file storage, data tables, and data pipelines

### Existing Code to Reference
**Similar Features Identified:**
- Feature: Auth module - Backend: AdonisJS services/controllers/models/migrations patterns to reuse
- Feature: Auth module - Frontend: Next.js API calls/Zustand/TanStack Query patterns to reuse
- Components to potentially reuse: API calling patterns, state management, data fetching
- Backend logic to reference: Service layer structure, controller patterns, model relationships, migration patterns
- New patterns to establish: File storage organization, data table handling, data pipeline processing

### Follow-up Questions
No follow-up questions needed - all requirements were comprehensively addressed.

## Visual Assets

### Files Provided:
No visual assets provided.

### Visual Insights:
No design mockups or wireframes were provided for this feature.

## Requirements Summary

### Functional Requirements
- Automatic data quality processing after file parsing in the upload pipeline
- Detection and correction of missing values, duplicates, invalid formats, outliers, and inconsistent categorical data
- Transparent logging of all corrections applied
- Quality scoring system with severity badges and global quality score
- Dual storage system maintaining both original and cleaned versions of data
- Seamless integration into existing dataset preview page with expandable cleaning results section
- File size limits for synchronous processing (<100MB, <500k rows, <10sec processing time)
- Loading states during processing

### Reusability Opportunities
- **Backend Patterns**: Auth module's service/controller/model/migration structure in AdonisJS
- **Frontend Patterns**: Auth module's Next.js API calls, Zustand state management, and TanStack Query data fetching
- **New Patterns to Establish**: File storage organization (raw/, cleaned/, metadata/ folders), data processing pipelines, quality metrics storage

### Scope Boundaries

**In Scope:**
- Automatic detection of common data quality issues
- Correction of detected issues with logging
- Quality scoring and badge system
- Integration into upload pipeline for files under 100MB
- Expandable UI section in preview page
- Dual file storage system
- Comprehensive database schema for cleaning metadata

**Out of Scope:**
- Background job processing (future enhancement for larger files)
- Automatic duplicate removal (flagging only)
- Separate cleaning results page or modal
- Manual cleaning triggers
- Advanced cleaning algorithms beyond basic corrections

### Technical Considerations

**Integration Points:**
- Existing file upload and parsing pipeline
- Current dataset preview page UI
- Database schema modifications to existing tables
- File storage system extensions

**Existing System Constraints:**
- File size limit of 100MB for synchronous processing
- Processing time target of <10 seconds for optimal user experience
- Row count limit of 500k for synchronous processing

**Technology Preferences:**
- Backend: AdonisJS with existing auth module patterns
- Frontend: Next.js with Zustand and TanStack Query patterns
- Database: Hybrid schema approach (existing table modifications + new tables)

**Similar Code Patterns to Follow:**
- Auth module structure for backend services, controllers, models, and migrations
- Auth module patterns for frontend API integration and state management
- File storage organization patterns for raw/cleaned/metadata folder structure