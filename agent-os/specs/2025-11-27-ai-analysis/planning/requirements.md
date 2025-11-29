# AI Analysis - Requirements

## 1. UI Integration

**Decision:** Dedicated section in existing Analysis page

**Placement:**
- In the existing "Analysis" tab
- New section "Insights IA" at the top of the page
- Above statistics and charts
- Visible immediately (not collapsed)

**Rationale:** AI insights are the "intelligent summary" - should be seen first before technical details

**Updated Analysis Page Structure:**
```
Analysis Tab
├── Insights IA (NEW - first)
├── Statistiques par Colonne
├── Graphiques
└── Outliers Détectés
```

## 2. Trigger Mode

**Decision:** Automatic after algorithmic analysis

**Complete Flow:**
```
Upload → Parse → Clean → Analyze (stats) → Generate AI Insights → Ready
```

- No "Generate insights" button
- Part of the automatic pipeline
- Status: "Generating insights..." → "Complete"
- If LLM fails → display error message, rest of dataset remains usable

**Rationale:** MVP philosophy = fully automatic, user observes

## 3. LLM Provider

**Decision:** Cloud LLM API for MVP (Anthropic Claude)

**Recommended Choice:** Anthropic Claude API
- Already mentioned in STACK.md
- Better for structured analysis
- Good quality/price ratio

**Configuration:**
- API key in environment variable
- Fallback if API unavailable → message "Insights temporarily unavailable"
- No sensitive data sent (only aggregated stats, not raw data)

**Future (V1+):** vLLM self-hosted for sovereignty

## 4. Number of Insights

**Decision:** Dynamic, between 3 and 6 insights

**Rules:**
- Minimum 3 insights (always something to say)
- Maximum 6 insights (don't overload user)
- Prioritize by relevance/impact

**Insight Categories (by priority order):**
1. **Overall Quality** - "This dataset contains 15,000 rows with 92% overall quality"
2. **Completeness** - "The most complete columns are..."
3. **Quality Issues** - "Column X has the most missing values (12%)"
4. **Distribution** - "The distribution of Y is highly asymmetric"
5. **Outliers** - "23 extreme values detected in column Z"
6. **Temporality** - "Data covers a 6-month period" (if date present)

**Logic:** AI decides which insights are relevant based on data

## 5. Insight Text Style

**Decision:** Quantitative with precise values

**Desired Style:**
- Clear, non-technical language
- Include numbers and percentages
- Short, direct sentences
- Adapted to personas (Data Manager, Operations Analyst)

**Good Examples:**
```
✅ "La colonne 'Montant' présente 23 valeurs extrêmes (2.3%),
    dont la plus élevée atteint 99,999€."

✅ "Les colonnes les plus complètes sont 'ID_Client' et 'Date_Commande'
    avec 100% de données renseignées."

✅ "La colonne 'Pays' a 12.3% de valeurs manquantes,
    le taux le plus élevé du dataset."
```

**Bad Examples:**
```
❌ "Certaines colonnes ont des valeurs manquantes." (too vague)
❌ "La distribution présente une skewness de 2.3." (too technical)
```

## 6. Language

**Decision:** French only for MVP

**Justification:**
- Varlor project = European/French platform
- Initial target = French market
- Simplify MVP

**Implementation:**
- LLM prompt in French
- Insights generated in French
- No language detection

**Future (Alpha):**
- UI language detection
- Multilingual generation (FR/EN)

## 7. MVP Exclusions

### Out of Scope for MVP:
- ❌ Predictive insights ("this trend will continue...")
- ❌ Cross-column correlations ("X and Y are correlated")
- ❌ Actionable recommendations ("you should clean...")
- ❌ Comparison with other datasets
- ❌ Complex pattern detection
- ❌ Advanced time series analysis
- ❌ Segmentation/clustering insights
- ❌ Business anomalies (only statistical anomalies)
- ❌ Conversational Q&A
- ❌ On-demand insight regeneration
- ❌ Insight style customization
- ❌ Insight export

### What AI Does (MVP):
- ✅ Interpret calculated stats
- ✅ Summarize data quality
- ✅ Highlight problems
- ✅ Describe distributions
- ✅ Flag outliers

**Philosophy:** AI only interprets in natural language what algorithms have already calculated
