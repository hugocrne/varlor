# Task Breakdown: AI Analysis (MVP)

## Overview
Total Tasks: 4 Task Groups

## Feature Summary
AI-powered insights section for the Analysis page that automatically generates natural language interpretations of dataset statistics in French using Anthropic Claude API.

## Task List

### Database Layer

#### Task Group 1: Database Schema Extension
**Dependencies:** None

- [x] 1.0 Complete database schema for AI insights storage
  - [x] 1.1 Write 4-6 focused tests for AI insights data model
    - Test ai_insights_status field values ('pending', 'processing', 'completed', 'failed')
    - Test ai_insights_json JSONB storage and retrieval
    - Test ai_insights_generated_at timestamp handling
    - Test ai_insights_error field storage
    - Test Dataset model with new AI insights fields
  - [x] 1.2 Create database migration for AI insights columns
    - Add `ai_insights_status` VARCHAR(20) DEFAULT 'pending'
    - Add `ai_insights_json` JSONB (nullable)
    - Add `ai_insights_generated_at` TIMESTAMP (nullable)
    - Add `ai_insights_error` TEXT (nullable)
    - Reference existing Dataset model at `/Users/hugo/Perso/Projets/varlor/server/app/models/`
  - [x] 1.3 Extend Dataset model with AI insights fields
    - Add type definitions for AIInsights interface:
      ```typescript
      interface AIInsights {
        insights: Array<{
          category: 'quality' | 'completeness' | 'issues' | 'distribution' | 'outliers' | 'temporality'
          text: string
        }>
        generatedAt: string
        modelUsed: string
        promptTokens: number
        completionTokens: number
      }
      ```
    - Add getter/setter methods for insights JSON
    - Add status transition methods
  - [x] 1.4 Ensure database layer tests pass
    - Run ONLY the 4-6 tests written in 1.1
    - Verify migration runs successfully
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- Migration creates all required columns on datasets table
- Dataset model correctly handles AI insights fields
- Status values are properly validated
- JSONB storage works for insights structure

---

### Backend Services Layer

#### Task Group 2: AI Insights Service and API
**Dependencies:** Task Group 1

- [x] 2.0 Complete AI insights backend service and API endpoints
  - [x] 2.1 Write 6-8 focused tests for AIInsightsService and API
    - Test prompt builder function with various dataset stats
    - Test response parser handles valid LLM output
    - Test response parser handles malformed output gracefully
    - Test API endpoint GET /datasets/:id/ai-insights returns correct status
    - Test API endpoint returns cached insights when available
    - Test API endpoint returns processing status during generation
    - Test error handling for API failures
    - Test authorization on endpoints (optional)
  - [x] 2.2 Create AIInsightsService
    - Location: `/Users/hugo/Perso/Projets/varlor/server/app/services/ai_insights_service.ts`
    - Reference existing AnalysisService pattern at `/Users/hugo/Perso/Projets/varlor/server/app/services/analysis_service.ts`
    - Implement methods:
      - `buildPrompt(dataset, stats)` - Format stats into French prompt
      - `generateInsights(datasetId)` - Main generation orchestrator
      - `callClaudeAPI(prompt)` - Anthropic API integration
      - `parseResponse(response)` - Extract insights from LLM response
      - `storeInsights(datasetId, insights)` - Persist to database
  - [x] 2.3 Implement prompt builder
    - Include dataset metadata (name, row count, column count)
    - Include aggregated statistics summary (no raw data)
    - Include quality score from cleaning
    - Include column types and key statistics
    - Include outlier information
    - Use French prompt template from spec
    - Ensure no PII or raw data is included
  - [x] 2.4 Implement Anthropic Claude API integration
    - Use `@anthropic-ai/sdk` package
    - Configuration: model `claude-3-haiku-20240307`, max_tokens 1024, temperature 0.3
    - Read API key from `ANTHROPIC_API_KEY` environment variable
    - Implement 30s timeout
    - Implement retry logic (max 3 retries with exponential backoff for rate limits)
  - [x] 2.5 Implement response parser
    - Parse numbered list format from LLM
    - Categorize insights (quality, completeness, issues, distribution, outliers, temporality)
    - Handle malformed responses gracefully
    - Ensure 3-6 insights are extracted
  - [x] 2.6 Create API controller and routes
    - Reference existing AnalysisController at `/Users/hugo/Perso/Projets/varlor/server/app/controllers/analysis_controller.ts`
    - GET `/datasets/:id/ai-insights` - Return insights or status
    - POST `/datasets/:id/ai-insights/generate` - Internal trigger endpoint
    - Response formats:
      - Completed: `{ datasetId, status: 'completed', insights: [...], generatedAt }`
      - Processing: `{ datasetId, status: 'processing' }`
      - Failed: `{ datasetId, status: 'failed', error: 'Insights temporairement indisponibles' }`
  - [x] 2.7 Integrate with analysis pipeline
    - Trigger AI insights generation after analysis completes
    - Add `setImmediate` call or similar async trigger in analysis completion handler
    - Ensure non-blocking execution (analysis results available immediately)
    - Update status to 'processing' before starting generation
  - [x] 2.8 Implement error handling
    - API key missing: Log error, set status 'failed', return user-friendly message
    - API timeout: Retry once, then fail gracefully
    - API rate limit: Exponential backoff, max 3 retries
    - Invalid response: Log full response, set status 'failed'
    - All error messages in French
  - [x] 2.9 Ensure backend service tests pass
    - Run ONLY the 6-8 tests written in 2.1
    - Verify service methods work correctly
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- AIInsightsService generates insights from dataset stats
- Prompt contains only aggregated statistics (no raw data)
- API endpoints return correct response formats
- Insights are generated automatically after analysis completes
- Error handling is graceful and non-blocking
- All error messages are in French

---

### Frontend Layer

#### Task Group 3: UI Components and Integration
**Dependencies:** Task Group 2

- [x] 3.0 Complete frontend components for AI insights display
  - [x] 3.1 Write 4-6 focused tests for UI components
    - Test AIInsightsSection renders loading state (skeleton)
    - Test AIInsightsSection renders insights list when completed
    - Test AIInsightsSection renders error alert when failed
    - Test useAIInsights hook fetches insights on mount
    - Test useAIInsights hook polls when status is 'processing'
    - Test polling stops after 'completed' or 'failed' status
  - [x] 3.2 Create AI insights API client functions
    - Location: `/Users/hugo/Perso/Projets/varlor/client/web/lib/api/`
    - Follow existing API client patterns
    - Function: `getAIInsights(datasetId)` - Fetch insights status/data
    - Add TypeScript types for API responses
  - [x] 3.3 Create useAIInsights hook
    - Location: `/Users/hugo/Perso/Projets/varlor/client/web/hooks/`
    - Reference existing hooks pattern at `use-analysis.ts`
    - Implement polling strategy:
      - Initial fetch when analysis status is 'completed'
      - Poll every 3 seconds if ai_insights_status is 'processing'
      - Stop polling on 'completed' or 'failed'
      - Maximum 10 retries before showing timeout error
    - Return: `{ insights, status, error, isLoading }`
  - [x] 3.4 Create AIInsightsSection component
    - Location: `/Users/hugo/Perso/Projets/varlor/client/web/components/datasets/`
    - Props: `{ datasetId: string, analysisStatus: AnalysisStatus }`
    - Use existing SectionHeader component (defined in AnalysisTab)
    - Header: "Insights IA" with lightbulb or sparkles icon
    - Card-based layout similar to existing sections
  - [x] 3.5 Implement component states
    - **Loading state:** Skeleton component with "Generation des insights..." message
    - **Completed state:** List of insights as bullet points with category indicators
    - **Failed state:** Alert component with "Insights temporairement indisponibles" (non-blocking)
    - **Pending state:** Section not shown (analysis not yet complete)
  - [x] 3.6 Style AIInsightsSection component
    - Follow existing design system
    - Use variables from existing style files
    - Ensure visual consistency with other Analysis tab sections
    - Add subtle AI/sparkle indicator for generated content
  - [x] 3.7 Integrate into AnalysisTab
    - Location: `/Users/hugo/Perso/Projets/varlor/client/web/components/datasets/analysis-tab.tsx`
    - Add AIInsightsSection as FIRST section (before "Statistiques par Colonne")
    - Pass datasetId and analysisStatus props
    - Only render when analysis is completed
  - [x] 3.8 Extend analysis schema types
    - Location: `/Users/hugo/Perso/Projets/varlor/client/web/lib/schemas/`
    - Add AI insights types matching backend response
    - Add ai_insights_status to dataset types if needed
  - [x] 3.9 Ensure frontend tests pass
    - Run ONLY the 4-6 tests written in 3.1
    - Verify component renders correctly in all states
    - Do NOT run the entire test suite at this stage

**Acceptance Criteria:**
- AIInsightsSection displays at top of Analysis tab
- Loading state shows skeleton while generating
- Completed state displays 3-6 insights as bullet points
- Failed state shows non-blocking error message
- Polling works correctly for processing status
- Rest of Analysis page remains usable if insights fail

---

### Testing and Integration

#### Task Group 4: Test Review and Integration Validation
**Dependencies:** Task Groups 1-3

- [x] 4.0 Review existing tests and validate full integration
  - [x] 4.1 Review tests from Task Groups 1-3
    - Review 4-6 tests from database layer (Task 1.1)
    - Review 6-8 tests from backend service (Task 2.1)
    - Review 4-6 tests from frontend components (Task 3.1)
    - Total existing tests: approximately 14-20 tests
  - [x] 4.2 Analyze test coverage gaps for AI Analysis feature
    - Identify critical user workflows lacking coverage
    - Focus ONLY on this feature's requirements
    - Prioritize end-to-end flow: Upload -> Analysis -> AI Insights display
  - [x] 4.3 Write up to 8 additional integration tests maximum
    - E2E test: Full flow from analysis completion to insights display
    - E2E test: Insights section shows loading state during generation
    - E2E test: Insights section handles failure gracefully (non-blocking)
    - Integration test: Pipeline triggers AI insights after analysis
    - Integration test: Concurrent requests handling
    - Integration test: Mock API failures return French error messages
    - Test: Verify no raw data in prompt (only aggregated stats)
    - Test: Verify insights contain numerical values
  - [x] 4.4 Validate success criteria
    - Insights generated within 30 seconds of analysis completion
    - Insights contain precise numerical values (percentages, counts)
    - Users can view analysis results even if AI insights fail
    - Error messages displayed in French and non-technical
    - At least 3 relevant insights generated for valid datasets
    - No raw data sent to LLM (only aggregated statistics)
  - [x] 4.5 Run feature-specific tests only
    - Run all tests from Task Groups 1-4 (approximately 22-28 tests)
    - Verify all critical workflows pass
    - Do NOT run the entire application test suite
    - Document any remaining issues

**Acceptance Criteria:**
- All feature-specific tests pass (approximately 22-28 tests total)
- Critical user workflows are covered
- Full integration from analysis to insights display works
- Error handling is graceful throughout the stack
- Success criteria from spec are validated

---

## Execution Order

Recommended implementation sequence:

1. **Database Layer** (Task Group 1)
   - Schema changes must be in place before services can store data
   - No external dependencies

2. **Backend Services** (Task Group 2)
   - Depends on database schema
   - Must be complete before frontend can fetch data
   - Includes pipeline integration

3. **Frontend Components** (Task Group 3)
   - Depends on API endpoints being available
   - Can be developed in parallel with backend if API contract is defined

4. **Integration Testing** (Task Group 4)
   - Requires all layers to be complete
   - Validates end-to-end functionality

## Configuration Requirements

Before starting implementation, ensure:
- `ANTHROPIC_API_KEY` environment variable is configured
- Anthropic SDK package is available (`@anthropic-ai/sdk`)

## Key Files Reference

**Backend:**
- `/Users/hugo/Perso/Projets/varlor/server/app/services/analysis_service.ts` - Pattern reference
- `/Users/hugo/Perso/Projets/varlor/server/app/controllers/analysis_controller.ts` - Pattern reference
- `/Users/hugo/Perso/Projets/varlor/server/app/models/` - Dataset model location

**Frontend:**
- `/Users/hugo/Perso/Projets/varlor/client/web/components/datasets/analysis-tab.tsx` - Integration point
- `/Users/hugo/Perso/Projets/varlor/client/web/components/ui/alert.tsx` - Error state component
- `/Users/hugo/Perso/Projets/varlor/client/web/hooks/` - Hooks location
- `/Users/hugo/Perso/Projets/varlor/client/web/lib/api/` - API client location
