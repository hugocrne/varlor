# MVP Report - Requirements

## 1. Report Page Location
- New tab in dataset detail view at `/dashboard/datasets/{id}`
- Tab order: Apercu -> Qualite -> Analyse -> **Rapport** (new)
- Not a separate page in main navigation
- "Export PDF" button visible in the Report tab
- User stays in dataset context

## 2. Dataset Summary Content
**Include:**
- Dataset name
- Original source filename
- Upload date
- Row count
- Column count
- File size
- Format (CSV/Excel)
- List of columns with detected types

**Exclude (MVP):**
- User description (no description field in MVP)
- Tags or categories
- Version history

## 3. Quality Synthesis Content
**Part 1 - Global Summary:**
- Global quality score (e.g., 92%)
- Total corrections applied
- Remaining issues count

**Part 2 - Condensed Metrics:**
- Simple table with problematic columns only
- Not full breakdown of all columns
- Focus on problems (>5% missing or invalid)

## 4. Key Charts Selection
**Rules:**
- Maximum 4-6 charts in report
- Not all generated charts

**Auto-selection criteria:**
- Histograms (max 2): Numeric columns with highest variance or detected outliers
- Bar charts (max 2): Categorical columns with good diversity (5-20 categories)
- Time curves (max 1): Include if date column detected

**Logic:**
- If outliers detected -> include histogram for that column
- Otherwise -> numeric column with highest variance

## 5. Explanatory Text Format
- Flowing narrative paragraphs (not bullet points)
- 2-3 paragraphs maximum
- Coherent, professional narrative text
- Transform existing AI insights into readable prose

## 6. PDF Generation Method
**Method:** Server-side generation
- Use Puppeteer or WeasyPrint
- More reliable than client-side
- Full control over rendering

**PDF Layout:**
- Print-optimized (not identical to screen)
- A4 portrait format
- Professional margins
- Header with Varlor logo
- Footer with generation date + page number
- Print-adapted typography

**Branding:**
- Varlor logo at top
- Brand colors
- Footer: "Genere par Varlor - [date]"

## 7. Preview Before Export
**User flow:**
1. Click on "Rapport" tab
2. See full report rendered on screen
3. Visible "Telecharger PDF" button
4. Click -> generation + automatic download

**Why preview:**
- User sees what they'll export
- Can verify before sharing
- Consistent with modern UX

## 8. MVP Exclusions
**Out of scope:**
- Scheduled/automated reports
- Multiple formats (Word, Excel, HTML)
- Customizable report templates
- Report sharing/collaboration
- Report history/versioning
- User annotations/comments
- Customizable sections
- Multi-dataset comparison reports
- Recurring email reports
- Batch generation
- Custom watermark
- PDF password protection
- Configurable language

**In scope (MVP):**
- Single format: PDF only
- Fixed content (summary + quality + charts + insights)
- Manual on-demand export
- French only
- Standard Varlor branding

## Visual Assets
No visual mockups provided.
