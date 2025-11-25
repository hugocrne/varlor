# Specification: Simple Automatic Cleaning

## Goal
Implement Simple Automatic Cleaning functionality that automatically detects and corrects common data quality issues in uploaded datasets, providing transparent correction logging, quality scoring, and seamless integration into the existing upload pipeline.

## User Stories
- As a data analyst, I want my uploaded files to be automatically checked for data quality issues so that I can trust the data integrity without manual inspection
- As a user, I want to see quality metrics and corrections applied to my dataset so that I understand what changes were made and why
- As a system user, I want both original and cleaned versions of my data preserved so that I can revert changes if needed

## Core Requirements
- Automatic data quality processing after file parsing in the upload pipeline
- Detection and correction of missing values, duplicate rows, invalid formats, outliers, and inconsistent categorical data
- Transparent logging of all corrections with detailed metrics
- Quality scoring system with severity badges and global quality score
- Dual storage system maintaining both original (raw/) and cleaned (cleaned/) versions with metadata/
- Expandable UI section in dataset preview page showing cleaning results above data table
- File size limits: <100MB, <500k rows, <10sec processing time for synchronous processing
- Loading states during processing with user feedback

## Visual Design
No mockups provided for this feature. UI should integrate seamlessly into existing dataset preview page with:
- Expandable section above preview table (default collapsed)
- Quality score badge prominently displayed
- Column-level quality metrics in expandable details
- Consistent with existing design system

## Reusable Components
### Existing Code to Leverage
- **Backend Patterns**: Auth module's service/controller/model/migration structure in AdonisJS
- **Frontend Patterns**: Auth module's Next.js API calls, Zustand state management, and TanStack Query data fetching
- **File Storage**: Dataset storage patterns for organizing raw/, cleaned/, metadata/ folders
- **Data Processing**: DatasetParserService patterns for file handling and data extraction
- **API Layer**: Existing datasets API structure for extending with cleaning endpoints
- **UI Components**: FileUploadZone component patterns for processing states and error handling
- **Database Patterns**: Dataset and DatasetColumn models for extending with cleaning metadata

### New Components Required
- **CleaningService**: Backend service for data quality detection and correction algorithms
- **DatasetCleaningLog**: New model for storing detailed correction logs
- **CleaningResultsSection**: Frontend component for displaying cleaning metrics and corrections
- **QualityScoringUtils**: Utility functions for calculating quality scores and severity badges
- **File organization**: Enhanced file storage system with raw/, cleaned/, metadata/ subfolder structure

## Technical Approach
- **Pipeline Integration**: Extend existing upload flow: Upload → Parsing → Cleaning → Ready
- **Synchronous Processing**: Handle files under 100MB synchronously with loading states
- **Storage Organization**: Maintain raw/original files, generate cleaned versions, store metadata separately
- **Database Schema**: Hybrid approach - extend existing tables with cleaning columns + new cleaning logs table
- **Quality Scoring**: Implement multi-level severity system (Excellent/Good/Medium/Poor) with global scoring
- **Correction Strategy**: Automatic correction for most issues, flagging-only for duplicates (no deletion)

## Out of Scope
- Background job processing (future enhancement for larger files)
- Automatic duplicate removal (detection and flagging only)
- Separate cleaning results page or modal overlay
- Manual cleaning triggers by users
- Advanced cleaning algorithms beyond basic data quality corrections
- User-configurable cleaning rules or thresholds

## Success Criteria
- Processing time under 10 seconds for files <100MB
- Quality score accuracy with proper severity classification
- Zero data loss with dual storage system preserving original files
- Transparent correction logging with detailed metrics per column
- Seamless integration maintaining existing upload flow performance
- User-friendly display of cleaning results without cluttering preview interface