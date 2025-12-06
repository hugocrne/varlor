# Product Roadmap

**Objective:** Build Varlor in 4 major stages:
1. **MVP** — Proof of value
2. **Alpha/Beta** — Birth of the platform
3. **Release V1** — Enterprise Ready
4. **V2** — Advanced platform, configurable, Palantir-like

---

## 1️⃣ MVP — "Import, clean, analyze, show that it's worth it"

🕒 Target duration: 2 to 4 months
🎯 Objective: Prove that Varlor provides **immediate value** to a client with simple use cases.

---

### 1.1. User-Facing Features

#### A. Auth + Access
- [x] Login page with:
  - [x] Email + password
  - [x] Basic session management (login / logout)
- [x] Admin account creation page (manual at first, not public)
- [x] No advanced multi-tenant yet → 1 environment = 1 client (or multiple clients managed "manually")

#### B. Data Import (files only)
- [x] "Import Dataset" page:
  - [x] CSV or Excel file upload
  - [x] Reasonable max size (e.g., a few hundred thousand rows initially)
- [x] After upload, user sees:
  - [x] Preview of first rows
  - [x] Detected columns with assumed type: text, number, date

#### C. Simple Automatic Cleaning
- [x] For each column:
  - [x] Detection of missing values
  - [x] Detection of values non-conforming to type (e.g., text in a date column)
  - [x] Row duplicate count

- [x] Simple automatic corrections:
  - [x] Whitespace trimming
  - [x] Basic date normalization (DD/MM/YYYY ↔ YYYY-MM-DD if possible)
  - [x] Number conversion attempt (e.g., `1,23` → `1.23`)

- [x] User sees:
  - [x] Quality summary (e.g., "Column X: 5% missing values, 3% invalid values")
  - [x] What was automatically corrected
  - [x] List of uncorrected issues

#### D. Algorithmic Analysis (MVP level)
- [x] Basic statistics per column:
  - [x] Min, max, mean, median, standard deviation
  - [x] Distribution (histogram)
  - [x] Top N most frequent values

- [x] Simple outlier detection:
  - [x] On numeric columns
  - [x] Via simple rule (e.g., ±3 standard deviations)

- [x] Auto-generated charts:
  - [x] Histogram per numeric column
  - [x] Bar chart for categories
  - [x] Basic time curve if a date column is detected

#### E. AI Analysis (MVP level)
- [x] Auto-generated text such as:
  - [x] "The most complete columns are..."
  - [x] "The columns with the most missing values are..."
  - [x] "The distribution of X is highly asymmetric..."
  - [x] "Column Y contains several extreme values, including..."

👉 Here AI only **interprets** what algorithms output (no magic, just clear text).

#### F. MVP Report
- [x] "Report" page + simple PDF export with:
  - [x] Dataset summary
  - [x] Quality synthesis
  - [x] Key charts
  - [x] Generated explanatory text

---

### 1.2. Backend / Data Infrastructure

- [x] Simple pipeline:
  1. [x] Uploaded file → stored in internal storage
  2. [x] Parsing → schema detection
  3. [x] "Raw" storage + dataset version
  4. [x] Launch job for:
     - [x] Profiling
     - [x] Basic cleaning
     - [x] Stats calculation
     - [x] "Dataset summary" object generation

- [x] No ontology at this stage
- [x] No API/DB connectors yet
- [x] No user-configurable analysis axes (everything is automatic)

---

### 1.3. Assumed MVP Limitations

- Supported formats: CSV/Excel only
- Single implicit "analysis axis": **by column**
- No advanced user configuration: they view, but don't configure yet
- Basic or non-formalized multi-tenant (pre-production)

---

## 2️⃣ Alpha / Beta — "From tool to platform beginning"

🕒 Target duration: 4 to 8 months after MVP
🎯 Objective: Move from a useful prototype to a **real structured platform**.

---

### 2.1. Alpha/Beta Main Objectives

1. **No longer limited to files**: Start connecting to other sources
2. **Introduce catalog concept**: Know which datasets exist, where they come from, how they're used
3. **Improve cleaning and quality**
4. **First ontology elements** (very simple) for generic business cases
5. **Start letting user choose certain analysis axes**

---

### 2.2. Alpha Features

#### A. Advanced Ingestion (v1)

- [ ] Still files, but with better management:
  - [ ] Multi-sheet Excel
  - [ ] JSON and XML (with auto-detection of tabular structure)
  - [ ] Richer previews

- [ ] First basic database connector:
  - [ ] Connection to relational database (e.g., generic SQL)
  - [ ] Query configuration or table selection
  - [ ] Import of extract/complete dataset

- [ ] First generic API connector (simplified):
  - [ ] URL
  - [ ] Header/API key
  - [ ] Basic pagination (page, limit)

#### B. Data Catalog (v1)
- [ ] "Catalog" page listing all datasets:
  - [ ] Name
  - [ ] Source type (file, DB, API)
  - [ ] Approximate size
  - [ ] Last import date
  - [ ] Status (OK, errors, processing)

- [ ] Dataset detail:
  - [ ] Columns
  - [ ] Types
  - [ ] Main statistics
  - [ ] Import/version history

#### C. Cleaning / Quality (v1 advanced)
- [ ] Improved profiling:
  - [ ] More metrics
  - [ ] Constant column detection
  - [ ] Near-duplicate column detection
  - [ ] Statistics by subgroup (e.g., by category)

- [ ] Improved anomaly analyses:
  - [ ] Smarter outliers
  - [ ] Simple rules (e.g., negative value where it makes no sense)
  - [ ] Inconsistent format detection within same column

- [ ] User can:
  - [ ] See "problematic" columns
  - [ ] Mark certain columns as "ignore"
  - [ ] Accept or reject certain proposed corrections

#### D. Extended (Algo) Analyses
- [ ] Addition of:
  - [ ] Variable correlation
  - [ ] First simple clusters (e.g., group similar customers)
  - [ ] Subgroup comparisons

- [ ] User can choose:
  - [ ] Which column(s) to analyze
  - [ ] Whether to see more details on anomaly or cluster

👉 Starting to approach **analysis axis configuration**: user doesn't define very complex axes yet, but already chooses **what/how** to analyze.

---

### 2.3. Beta Features

#### A. Ontology (version 0.5)

Objective: Introduce **business object** concept without going too far.

- [ ] Ability to declare some internal objects:
  - [ ] "Customer"
  - [ ] "Order"
  - [ ] "Product"
  - [ ] "Event"

- [ ] Semi-automatic mapping:
  - [ ] Varlor suggests "the `customer_id` column could be linked to the Customer object"
  - [ ] Varlor proposes considering `order_date` as date of an "Order" object

- [ ] User can:
  - [ ] Accept/refuse mappings
  - [ ] Rename certain objects

#### B. Analysis Axes (v1)

Analysis axes start being **configurable**:

Examples:
- Temporal axis (by day, week, month)
- Geographic axis (if corresponding columns exist)
- "Customer" axis
- "Product" axis

- [ ] User can:
  - [ ] Choose a main axis (e.g., time)
  - [ ] Possibly cross with a second axis (e.g., product)
  - [ ] Launch analyses (aggregations, trends, anomalies) according to these axes

> 🔹 This is the first **clear appearance** of "user configures their analysis axes".

#### C. Enriched Reports

- [ ] More structured report:
  - [ ] "Data quality" sections
  - [ ] "Key trends" sections
  - [ ] "Structural anomalies" section
  - [ ] "AI insights" section

- [ ] AI explains:
  - [ ] What clusters mean
  - [ ] Why certain anomalies are critical
  - [ ] Which dimensions seem important (e.g., region, product type)

---

### 2.4. Alpha/Beta Infrastructure / Security

- [ ] Basic multi-tenant:
  - [ ] Each organization has its datasets
  - [ ] Strict data separation
  - [ ] First roles (admin / analyst / viewer)

- [ ] Logging:
  - [ ] Who imported what
  - [ ] When
  - [ ] Which reports consulted

---

## 3️⃣ Release V1 — "Enterprise-Ready Platform"

🕒 Target duration: Around 12–18 months from start
🎯 Objective: First version truly **enterprise-deployable**, with security, solid ontology, powerful analyses, integrated AI, and seriously configurable analysis axes.

---

### 3.1. V1 Main Objectives

1. **Truly universal ingestion** (files + API + DB)
2. **Business ontology** usable in practice, not just a demo
3. **Quality + analysis + AI pipeline** robust, traceable, reliable
4. **Analysis axes configurable by user in advanced way**
5. **Security and multi-tenant** suitable for real clients
6. **Sovereignty and enterprise deployment** (private cloud / on-prem)

---

### 3.2. V1 User-Side Features

#### A. Universal Ingestion (v2)

- [ ] Connectors:
  - [ ] Files (all standard formats)
  - [ ] Relational databases
  - [ ] Some NoSQL databases if priority
  - [ ] REST APIs with advanced pagination
  - [ ] Scheduled sources (e.g., automatic pull every X days)

- [ ] Ingestion dashboard:
  - [ ] Each source status (OK / error)
  - [ ] Last update time
  - [ ] Detailed logs

#### B. Ontology (v1 complete)

- [ ] Visual business object editor:
  - [ ] User can define "Customer", "Order", "Machine", "Incident" etc.
  - [ ] Define properties (attributes) of each object
  - [ ] Define relationships (a customer has multiple orders, etc.)

- [ ] Varlor proposes:
  - [ ] Automatic mappings
  - [ ] Business object suggestions
  - [ ] Links between datasets

- [ ] Ontology becomes the **main view** to understand data:
  - [ ] No longer just looking at tables
  - [ ] Looking at business entities and their relationships

#### C. Configurable Analysis Axes (v2)

Here, we really go into your objective:
👉 **Users define their own analysis axes.**

- [ ] They can:
  - [ ] Choose business dimensions (e.g., time, product, customer, region)
  - [ ] Define filters (e.g., country = FR, amount > X)
  - [ ] Choose metrics (e.g., total, average, variance, error rate)
  - [ ] Save reusable "analysis views"

Concrete examples:
- "Analyze order delay rate by region and product type, over last 6 months"
- "Analyze anomaly rate in sensors by factory and production line"

#### D. Advanced Analyses (algos)

- [ ] Advanced multi-variable correlations
- [ ] Predictive models (on certain chosen axes)
- [ ] Time series anomaly detection
- [ ] Recurring pattern detection

- [ ] User chooses:
  - [ ] Which axes to apply these analyses to
  - [ ] Which algos to activate (simple mode: "check desired analyses")

#### E. AI (v1 advanced)

- [ ] AI:
  - [ ] Interprets analysis results
  - [ ] Highlights most interesting axis combinations ("strongest differences observed when cutting by X and Y")
  - [ ] Synthesizes **business insights**:
    - "Delays are particularly concentrated on certain product for certain regions"
    - "Sensor anomalies appear mainly during certain event type"

- [ ] User can ask questions like:
  - [ ] "What should I focus on first?"
  - [ ] "What are the 3 most atypical segments?"

#### F. Intelligent Reports (v1)

- [ ] Reports versioned by:
  - [ ] Dataset
  - [ ] Date
  - [ ] Chosen analysis axes

- [ ] Export:
  - [ ] PDF
  - [ ] Possibly data formats (JSON/CSV) for reuse

---

### 3.3. V1 Security / Deployment

- [ ] Serious multi-tenant:
  - [ ] Organization separation
  - [ ] Multiple roles (admin, data engineer, data analyst, viewer, etc.)

- [ ] Access policy:
  - [ ] Control by dataset
  - [ ] By report type
  - [ ] By action (import, analyze, view)

- [ ] Deployment:
  - [ ] **Private cloud** or on-prem support
  - [ ] Installation documentation
  - [ ] Backup/restore mechanisms
  - [ ] Basic monitoring

---

## 4️⃣ V2 — "Advanced platform, Palantir-like"

🕒 Target duration: 18 to 30 months
🎯 Objective: Transform Varlor into **data OS**, with workflows, real-time, collaboration, internal marketplace, highly integrated AI.

---

### 4.1. V2 Main Objectives

1. Introduce **business workflows** (actions, not just analyses)
2. Handle **real-time** if necessary (streaming)
3. Add **collaboration and scenarios** (what-if)
4. Allow **complex connectors (SAP, ERP, IoT...)**
5. Make AI a **proactive assistant** for modeling and analysis
6. Make platform **extensible (plugins, business packs)**
7. Varlor is no longer "a data analysis tool"
- It's a **platform for orchestrating data-driven decisions**
- Clients can:
  - [ ] Define their ontology
  - [ ] Build their analysis axes
  - [ ] Launch advanced analyses
  - [ ] Automate reactions
  - [ ] Collaborate around insights
  - [ ] Adapt solution to each business/country/factory/etc.

---

# 🧠 Final Synthesis

1. **MVP**
   → Prove that Varlor knows how to:
   - Import a file
   - Clean it
   - Analyze it
   - Produce a clear and useful report

2. **Alpha/Beta**
   → Move from tool to **platform**:
   - Broader ingestion
   - Catalog
   - Advanced cleaning
   - First configurable analysis axes
   - Beginning of ontology

3. **Release V1**
   → Become **enterprise-ready**:
   - Universal ingestion
   - Strong ontology
   - Advanced analyses
   - AI that seriously interprets
   - User-configurable analysis axes
   - Security, enterprise deployment

4. **V2**
   → Data OS:
   - Business workflows
   - Real-time
   - Collaboration
   - Business packs
   - AI copilot for modeling and analysis