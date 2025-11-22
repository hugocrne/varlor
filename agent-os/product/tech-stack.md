# Tech Stack

## Frontend

### Core Framework
- **Framework**: React 18+ with TypeScript
- **Meta-Framework**: Next.js (App Router for SSR/SSG capabilities)
- **Build Tool**: Turbopack (Next.js default)
- **Package Manager**: npm or pnpm

### UI & Styling
- **Component Library**: Shadcn UI (accessible, customizable components)
- **Styling**: Tailwind CSS with CSS Modules for complex cases
- **Icons**: Lucide React
- **Forms**: React Hook Form with Zod validation

### Data Visualization
- **Primary**: Apache ECharts (rich charting library)
- **Advanced**: D3.js (for custom, complex visualizations)
- **Tables**: TanStack Table (formerly React Table)

### State Management
- **Client State**: Zustand (lightweight, TypeScript-first)
- **Server State**: TanStack Query (React Query) for data fetching/caching
- **Form State**: React Hook Form

### Authentication (Client)
- **Protocol**: OIDC/OAuth2
- **Library**: Keycloak JS Adapter or NextAuth.js with Keycloak provider

## API Layer

### API Gateway
- **Gateway**: Kong or Envoy (for routing, rate limiting, TLS termination)
- **Custom Plugins**: Go (for specialized auth/routing logic)
- **Features**: JWT verification, rate limiting, request routing, service mesh capabilities

### Backend for Frontend (BFF)
- **Language**: TypeScript
- **Framework**: NestJS (enterprise-grade Node.js framework)
- **Transport**: REST APIs for external clients, gRPC for internal service communication
- **Validation**: class-validator and class-transformer

## Authentication & Authorization

### Identity Provider (Self-Hosted)
- **Solution**: Keycloak (open-source OIDC/OAuth2/SAML provider)
- **Capabilities**:
  - User/role management
  - Multi-factor authentication (MFA)
  - Password policies and rotation
  - Audit event logging
  - Admin console
  - Completely air-gap capable

### Authorization Service (Business Logic)
- **Language**: Go or Kotlin
- **Database**: PostgreSQL (for permissions, roles, policies)
- **Cache**: Redis (for permission lookups)
- **Model**: RBAC (Role-Based Access Control) + ABAC (Attribute-Based Access Control)

## Backend Microservices

### Data Ingestion Service
- **Language**: Python 3.11+
- **Framework**: FastAPI (async, high-performance)
- **Message Queue**: Apache Kafka (for ingestion pipeline events)
- **Task Queue**: Celery with Redis broker or Faust (stream processing)
- **Storage**: S3-compatible object storage (MinIO for self-hosted)
- **Libraries**: pandas, pyarrow (Parquet handling), openpyxl (Excel), xmltodict

### Data Catalog & Metadata Service
- **Language**: Kotlin
- **Framework**: Spring Boot 3
- **Database**: PostgreSQL (schema metadata, lineage, versioning)
- **Optional**: Integration with DataHub or OpenMetadata for advanced cataloging

### Data Quality & Cleaning Service
- **Language**: Python
- **Compute Engine**: Apache Spark (distributed processing via Kubernetes Spark Operator)
- **Libraries**:
  - PySpark for distributed operations
  - pandas for in-memory operations
  - Great Expectations for data validation rules
  - scikit-learn for outlier detection

### Analytics & Machine Learning Service
- **Language**: Python
- **ML Libraries**:
  - scikit-learn (clustering, regression, classification)
  - XGBoost (gradient boosting)
  - Spark MLlib (distributed ML)
- **Orchestration**: Apache Airflow or Dagster (pipeline scheduling)
- **Feature Store**: Feast (optional, for feature management)

### Report Generation Service
- **Language**: Node.js (NestJS) or Go
- **PDF Generation**:
  - WeasyPrint (HTML to PDF with CSS support)
  - Puppeteer (Chrome headless for complex layouts)
- **Data Source**: Direct SQL queries to data warehouse
- **Storage**: S3/MinIO for generated reports

### AI Insights / LLM Service
- **Language**: Python or Node.js
- **LLM Providers**:
  - OpenAI API (GPT-4)
  - Anthropic API (Claude)
  - Groq (high-speed inference)
  - Future: vLLM for self-hosted LLM deployment
- **Vector Database**:
  - PostgreSQL with pgvector extension
  - Alternatives: Qdrant or Weaviate
- **Framework**: LangChain or LlamaIndex for LLM orchestration

## Data Layer

### Data Lake (Raw Storage)
- **Storage**: S3-compatible object storage (AWS S3 or self-hosted MinIO)
- **Formats**: Parquet (columnar), ORC, Avro
- **Organization**: Partitioned by dataset ID, date, version
- **Access**: Via Spark, Trino, or direct S3 API

### Data Warehouse / Lakehouse
- **Cloud Option**: Snowflake or Google BigQuery
- **Self-Hosted Option**: Trino or Presto with Hive MetaStore
- **Query Engine**: SQL-based, ANSI SQL compliant
- **Use Case**: Cleaned, transformed data ready for analysis and reporting

### Application Databases
- **Primary Database**: PostgreSQL 15+
- **Use Cases**:
  - User management and authentication metadata
  - Tenant configuration and isolation
  - Business permissions and roles
  - Data catalog metadata
  - Ontology definitions
- **High Availability**: PostgreSQL replication (streaming replication or Patroni)

### Caching Layer
- **Cache**: Redis 7+
- **Use Cases**:
  - Session storage
  - Permission cache
  - API response cache
  - Real-time metrics
  - Celery task broker

## Data Pipeline Orchestration

### Workflow Orchestrator
- **Primary**: Dagster (modern, code-first, Python-native)
- **Alternative**: Apache Airflow (mature, large ecosystem)
- **Capabilities**:
  - Schedule data ingestion pipelines
  - Orchestrate cleaning → analysis → report generation
  - Integrate with Spark, Kafka, databases
  - Track data lineage
  - Manage pipeline versioning

## Infrastructure & DevOps

### Container Orchestration
- **Platform**: Kubernetes
- **Managed Options**: EKS (AWS), GKE (Google Cloud), AKS (Azure)
- **Self-Hosted**: K3s (lightweight) or vanilla Kubernetes on bare metal/VMs
- **Service Mesh**: Istio or Linkerd (for advanced traffic management)

### Observability

#### Logging
- **Log Aggregation**: OpenSearch or Elasticsearch
- **Log Shipper**: Fluentbit or Fluent
- **Use Cases**: Application logs, audit trails, security events

#### Metrics
- **Metrics Collection**: Prometheus
- **Visualization**: Grafana
- **Exporters**: Node exporter, PostgreSQL exporter, custom app metrics

#### Tracing
- **Standard**: OpenTelemetry (vendor-neutral instrumentation)
- **Backend**: Jaeger or Tempo
- **Use Cases**: Distributed request tracing across microservices

### Secrets Management
- **Solution**: HashiCorp Vault
- **Capabilities**:
  - Dynamic secrets generation
  - Secret rotation
  - Encryption as a service
  - PKI management
  - Audit logging

### CI/CD
- **Version Control**: Git (GitHub, GitLab, or self-hosted Gitea)
- **CI/CD Platform**: GitHub Actions, GitLab CI, or ArgoCD (GitOps)
- **Container Registry**: Docker Hub, GitHub Container Registry, or self-hosted Harbor
- **Deployment**: Helm charts for Kubernetes deployments

## Development Tools

### Code Quality
- **Linting**: ESLint (TypeScript/JavaScript), Pylint/Ruff (Python), ktlint (Kotlin)
- **Formatting**: Prettier (frontend), Black (Python), ktfmt (Kotlin)
- **Type Checking**: TypeScript strict mode, mypy (Python)

### Testing
- **Frontend**: Vitest or Jest + React Testing Library + Playwright (E2E)
- **Backend Python**: pytest + pytest-asyncio
- **Backend JVM**: JUnit 5 + Mockk
- **Backend Node**: Jest or Vitest
- **Load Testing**: k6 or Locust

### API Documentation
- **REST**: OpenAPI/Swagger (auto-generated from FastAPI, NestJS)
- **GraphQL**: GraphQL Playground or Apollo Studio (if using GraphQL)

## Security & Compliance

### Network Security
- **TLS**: Let's Encrypt or internal CA for certificate management
- **Firewall**: Kubernetes Network Policies
- **DDoS Protection**: Cloudflare (if public) or on-premise WAF

### Data Security
- **Encryption at Rest**: S3/MinIO server-side encryption, PostgreSQL transparent data encryption
- **Encryption in Transit**: TLS 1.3 for all service communication
- **Data Masking**: PostgreSQL row-level security for sensitive data

### Compliance
- **GDPR**: Data residency controls, right to erasure, audit logs
- **SOC2**: Access controls, audit trails, encryption
- **Air-Gap Support**: All components can operate without internet connectivity

## Summary Architecture

**Varlor Platform Stack:**
- **Frontend**: Next.js + React + TypeScript + Shadcn UI + ECharts
- **API Gateway**: Kong/Envoy + NestJS BFF
- **Auth**: Self-hosted Keycloak (OIDC/OAuth2) + custom authorization service
- **Microservices**: Python (FastAPI), Kotlin (Spring Boot), Node.js (NestJS)
- **Data Processing**: Apache Spark on Kubernetes
- **Data Storage**: S3/MinIO (lake) + Trino/Snowflake (warehouse) + PostgreSQL (app DB)
- **Orchestration**: Dagster or Airflow
- **AI/ML**: Python + scikit-learn + LLM APIs + vector DB
- **Infrastructure**: Kubernetes + Prometheus/Grafana + OpenTelemetry + Vault
- **Deployment**: Fully self-hosted capable, air-gap compatible, sovereign by design
