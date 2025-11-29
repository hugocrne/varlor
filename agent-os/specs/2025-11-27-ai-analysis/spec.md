# Specification: AI Analysis (MVP)

## Goal

Add an AI-powered insights section to the Analysis page that automatically generates natural language interpretations of dataset statistics after algorithmic analysis completes. The AI interprets already-calculated statistics to provide users with clear, quantitative insights in French.

## User Stories

- As a Data Manager, I want to see AI-generated insights about my data quality so that I can quickly understand dataset health without reading technical statistics.
- As an Operations Analyst, I want concise summaries highlighting data completeness and distribution issues so that I can prioritize data cleaning efforts.
- As a User, I want insights to appear automatically after upload so that I do not need to trigger them manually.
- As a User, I want to continue using the dataset even if AI insights fail so that my workflow is not blocked by LLM issues.

## Core Requirements

- Display "Insights IA" section at the top of the Analysis tab
- Generate 3-6 insights automatically after algorithmic analysis completes
- Use Anthropic Claude API for LLM generation
- Insights must be in French with quantitative values
- Graceful degradation if LLM fails (show error message, rest of page remains usable)
- No regeneration button for MVP (automatic only)

## Visual Design

No mockups provided. The "Insights IA" section should:
- Appear as the first section in the Analysis tab (before "Statistics by Column")
- Use a card-based layout similar to existing sections
- Include an icon (lightbulb or sparkles) to indicate AI-generated content
- Display insights as a list of bullet points
- Show loading state during generation ("Generation des insights...")
- Show error state if LLM fails with a non-blocking message

## Reusable Components

### Existing Code to Leverage

**Frontend:**
- `AnalysisTab` component (`/Users/hugo/Perso/Projets/varlor/client/web/components/datasets/analysis-tab.tsx`) - Main container for analysis results, will add new section here
- `SectionHeader` component (defined in AnalysisTab) - Reuse for "Insights IA" header
- `Alert` component (`/Users/hugo/Perso/Projets/varlor/client/web/components/ui/alert.tsx`) - Use for error states
- `Skeleton` component - Use for loading states
- Analysis hooks pattern (`use-analysis.ts`) - Follow same pattern for new hooks

**Backend:**
- `AnalysisService` (`/Users/hugo/Perso/Projets/varlor/server/app/services/analysis_service.ts`) - Contains all statistical data needed for prompt
- `AnalysisController` (`/Users/hugo/Perso/Projets/varlor/server/app/controllers/analysis_controller.ts`) - Follow same pattern for new endpoints
- `Dataset` model - Already has analysis status fields
- `DatasetColumnStats` model - Contains all stats needed for LLM prompt

**Schemas/Types:**
- `analysis.schema.ts` - Extend with AI insights types
- API client pattern in `lib/api/` - Follow for new API functions

### New Components Required

**Frontend:**
- `AIInsightsSection` component - Display AI-generated insights
- `useAIInsights` hook - Fetch and cache AI insights
- AI insights API functions - Call backend endpoints

**Backend:**
- `AIInsightsService` - New service for LLM interaction
- `AIInsightsController` - New controller (or extend AnalysisController)
- AI insights model/storage - Store generated insights in database

**Why new code is needed:**
- No existing LLM integration in the codebase
- AI insights require new data model to persist generated content
- Need new UI component for insight display format (bullet list with icons)

## Technical Approach

### LLM Integration

**Provider:** Anthropic Claude API (as specified in STACK.md)

**Configuration:**
- API key via environment variable `ANTHROPIC_API_KEY`
- Default model: `claude-3-haiku-20240307` (fast, cost-effective for structured analysis)
- Max tokens: 1024 (sufficient for 6 insights)
- Temperature: 0.3 (low for consistent, factual outputs)

**Prompt Design:**
The prompt should include:
1. Dataset metadata (name, row count, column count)
2. Aggregated statistics summary (no raw data)
3. Quality score from cleaning
4. Column types and their key statistics
5. Outlier information

**Prompt Structure (French):**
```
Tu es un analyste de donnees expert. Genere entre 3 et 6 insights en francais
sur ce dataset en utilisant les statistiques fournies.

Regles:
- Langue: francais uniquement
- Style: quantitatif avec valeurs precises
- Phrases: courtes et directes
- Format: liste numerotee

Categories d'insights (par priorite):
1. Qualite globale
2. Completude des colonnes
3. Problemes de qualite
4. Distribution
5. Valeurs extremes
6. Temporalite (si dates presentes)

Dataset: [name]
Lignes: [rowCount]
Colonnes: [columnCount]
Score qualite: [qualityScore]%

Statistiques par colonne:
[formatted column stats]
```

### Data Flow

```
Analysis Completed
       |
       v
Frontend detects analysis completed
       |
       v
Frontend calls GET /datasets/:id/ai-insights
       |
       v
Backend checks if insights already exist
       |
       +--> Yes: Return cached insights
       |
       +--> No: Generate new insights
              |
              v
         Build prompt from stats
              |
              v
         Call Claude API
              |
              v
         Parse response
              |
              v
         Store in database
              |
              v
         Return insights
```

### Pipeline Integration

The AI insights generation should be triggered automatically as part of the analysis pipeline:

```
Upload -> Parse -> Clean -> Analyze (stats) -> Generate AI Insights -> Ready
```

This can be implemented by:
1. Adding a `setImmediate` call in the analysis completion handler to trigger AI insights generation
2. Or creating a separate status field `aiInsightsStatus` for independent tracking

**Recommended:** Use separate status tracking (`aiInsightsStatus: 'pending' | 'processing' | 'completed' | 'failed'`) so that:
- Analysis results are available immediately
- AI insights appear when ready
- Failures do not block the main workflow

### Database Schema

**Option 1: Extend Dataset model** (Recommended for MVP)
```sql
ALTER TABLE datasets ADD COLUMN ai_insights_status VARCHAR(20) DEFAULT 'pending';
ALTER TABLE datasets ADD COLUMN ai_insights_json JSONB;
ALTER TABLE datasets ADD COLUMN ai_insights_generated_at TIMESTAMP;
ALTER TABLE datasets ADD COLUMN ai_insights_error TEXT;
```

**Insights JSON structure:**
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

### API Design

**GET /datasets/:id/ai-insights**
- Returns AI insights if available
- Returns status if still processing
- Returns error if generation failed

**Response (completed):**
```json
{
  "datasetId": 123,
  "status": "completed",
  "insights": [
    {
      "category": "quality",
      "text": "Ce dataset contient 15,000 lignes avec une qualite globale de 92%."
    },
    {
      "category": "completeness",
      "text": "Les colonnes les plus completes sont 'ID_Client' et 'Date_Commande' avec 100% de donnees renseignees."
    }
  ],
  "generatedAt": "2025-11-27T12:00:00Z"
}
```

**Response (processing):**
```json
{
  "datasetId": 123,
  "status": "processing"
}
```

**Response (failed):**
```json
{
  "datasetId": 123,
  "status": "failed",
  "error": "Insights temporairement indisponibles"
}
```

**POST /datasets/:id/ai-insights/generate** (Internal use only)
- Triggers generation if not already processing
- Called automatically after analysis completes
- Returns 202 Accepted

### Frontend Implementation

**AIInsightsSection Component:**
```typescript
interface AIInsightsSectionProps {
  datasetId: string
  analysisStatus: AnalysisStatus
}
```

**States to handle:**
1. **Loading** - Analysis completed, insights being fetched/generated
2. **Completed** - Show insights list
3. **Failed** - Show non-blocking error alert
4. **Pending** - Analysis not yet complete (section not shown)

**Polling Strategy:**
- Initial fetch when analysis completes
- If status is 'processing', poll every 3 seconds
- Stop polling on 'completed' or 'failed'
- Maximum 10 retries before showing timeout error

### Error Handling

**Backend Errors:**
| Error | Handling |
|-------|----------|
| API key missing | Log error, set status to 'failed', return user-friendly message |
| API timeout (30s) | Retry once, then fail gracefully |
| API rate limit | Exponential backoff, max 3 retries |
| Invalid response | Log full response, set status to 'failed' |
| Database error | Standard error handling, status unchanged |

**Frontend Errors:**
- Network errors: Show "Impossible de charger les insights" with retry option
- Timeout: Show "Generation des insights en cours..." then timeout message after 60s
- Failed status: Show "Insights temporairement indisponibles" (non-blocking)

### Security Considerations

**Data Privacy:**
- Only send aggregated statistics to LLM, never raw data
- No personally identifiable information in prompts
- Column names may be included (assumed non-sensitive for MVP)

**API Security:**
- API key stored in environment variable (not committed)
- API calls made server-side only
- Rate limiting on insight generation endpoint

## Out of Scope

- Predictive insights ("this trend will continue...")
- Cross-column correlations ("X and Y are correlated")
- Actionable recommendations ("you should clean...")
- Comparison with other datasets
- Complex pattern detection
- Advanced time series analysis
- Segmentation/clustering insights
- Business anomalies (only statistical anomalies)
- Conversational Q&A about the data
- On-demand insight regeneration
- Insight style customization
- Insight export
- Multi-language support (French only for MVP)
- Self-hosted LLM (vLLM) - planned for future

## Success Criteria

- Insights are generated automatically within 30 seconds of analysis completion
- Insights contain precise numerical values (percentages, counts, etc.)
- Users can view analysis results even if AI insights fail
- Error messages are displayed in French and non-technical
- At least 3 relevant insights are generated for any valid dataset
- No raw data is sent to the LLM (only aggregated statistics)

## Testing Strategy

**Unit Tests:**
- Prompt builder function (correct formatting of stats)
- Response parser (handles various LLM output formats)
- Insights storage/retrieval

**Integration Tests:**
- Full flow from analysis completion to insights display
- Error handling (mock API failures)
- Concurrent requests handling

**E2E Tests:**
- Upload file -> Wait for analysis -> See insights
- Insights section shows loading state
- Insights section handles failure gracefully

**Manual Testing:**
- Verify French language quality
- Verify insights relevance for different dataset types
- Verify numeric accuracy in insights
