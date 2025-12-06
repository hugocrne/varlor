# Story 1.2: API Connector Framework

Status: Ready for Development

## Story

As a technical user,
I want to connect to REST APIs with various authentication methods,
So that I can pull data from external services into Varlor.

## Acceptance Criteria

1. **Given** I am on the dataset creation page
   **When** I click "Connect API"
   **Then** I see a modal with API connector options (REST API, GraphQL, Webhook)

2. **Given** I selected REST API connector
   **When** the authentication configuration appears
   **Then** I can choose between Bearer Token, OAuth2 with PKCE, and API Key authentication methods
   **And** I can configure base URL, headers, and rate limits

3. **Given** I selected OAuth2 with PKCE authentication
   **When** I configure authorization URL, token URL, client ID
   **Then** the system generates a PKCE code challenge and verifier
   **And** redirects me through the OAuth2 flow
   **And** securely stores the access and refresh tokens in Redis

4. **Given** I have configured an API connector with Bearer token
   **When** I test the connection
   **Then** the system validates the token against the API endpoint
   **And** shows response status and sample data preview
   **And** stores the token encrypted with tenant-specific key

5. **Given** I have successfully configured an API connector
   **When** I use it to create a dataset
   **Then** I can select from detected API endpoints or enter custom paths
   **And** the system handles pagination automatically
   **And** applies rate limiting per connector configuration

6. **Given** my API connector uses OAuth2
   **When** the access token expires
   **Then** the system automatically refreshes the token using the refresh token
   **And** retries failed requests with the new token
   **And** logs refresh events for audit

## Tasks / Subtasks

- [ ] **Backend: API Connector Models and Services** (AC: 1, 2, 4, 6)
  - [ ] Extend `database_connections` table for API connectors
  - [ ] Create `APIConnectorService` base class
  - [ ] Implement OAuth2 service with PKCE flow
  - [ ] Implement Bearer token authentication handler
  - [ ] Implement API key authentication handler
  - [ ] Add token refresh mechanism with background jobs

- [ ] **Backend: API Endpoints** (AC: 1, 2, 3, 4, 5, 6)
  - [ ] Extend `ConnectorsController` with API-specific endpoints:
    - POST `/api/v2/connectors/test-api` - Test API connection
    - POST `/api/v2/connectors/oauth/start` - Start OAuth2 flow
    - GET `/api/v2/connectors/oauth/callback` - OAuth2 callback
    - GET `/api/v2/connectors/:id/endpoints` - Discover API endpoints
    - POST `/api/v2/connectors/:id/fetch` - Fetch data from API
  - [ ] Implement request validation with VineJS

- [ ] **Backend: Token Management** (AC: 3, 6)
  - [ ] Create `TokenManagerService` for secure token storage
  - [ ] Implement OAuth2 PKCE flow generation
  - [ ] Add automatic token refresh with Bull queues
  - [ ] Store tokens encrypted in Redis with tenant isolation
  - [ ] Add token revocation on connector deletion

- [ ] **Backend: Rate Limiting and Monitoring** (AC: 5, 6)
  - [ ] Implement per-connector rate limiting
  - [ ] Add API request monitoring and logging
  - [ ] Create retry logic with exponential backoff
  - [ ] Add connector health check endpoint

- [ ] **Frontend: API Connector Components** (AC: 1, 2, 3, 4, 5)
  - [ ] Create APIConnectorModal component
  - [ ] Create APIConnectorForm with authentication options
  - [ ] Create OAuth2Flow component for PKCE authentication
  - [ ] Create ConnectionTestButton for API validation
  - [ ] Create EndpointSelector for API discovery
  - [ ] Create ConnectorConfigList component

- [ ] **Frontend: State Management** (AC: 3, 5, 6)
  - [ ] Extend connectors store with API connector state
  - [ ] Implement OAuth2 flow handling
  - [ ] Add real-time connection status updates
  - [ ] Create API data preview functionality

- [ ] **Security Implementation** (AC: 3, 4, 6)
  - [ ] Extend EncryptionService for token encryption
  - [ ] Implement PKCE code verifier/challenge generation
  - [ ] Ensure all tokens are stored with tenant-specific keys
  - [ ] Add audit logging for token operations
  - [ ] Implement CSRF protection for OAuth2 flow

- [ ] **Testing** (All ACs)
  - [ ] Unit tests for API connector services
  - [ ] Integration tests for OAuth2 flow
  - [ ] Frontend component tests for authentication forms
  - [ ] End-to-end test for complete API connector workflow

## Dev Notes

### Technical Requirements

#### Backend Implementation Details

**1. Database Model Extension**
```typescript
// app/models/database_connection.ts (extended)
export default class DatabaseConnection extends BaseModel {
  // ... existing fields

  @column() // New for API connectors
  public connectorType: 'database' | 'api' | 'storage'

  @column() // API-specific fields
  public baseUrl?: string

  @column()
  public authType: 'bearer' | 'oauth2' | 'apikey'

  @column() // OAuth2 fields
  public oauthConfig?: {
    authorizationUrl: string
    tokenUrl: string
    clientId: string
    scopes?: string[]
    pkceVerifier?: string
  }

  @column() // API key fields
  public apiKeyConfig?: {
    keyName: string
    location: 'header' | 'query'
    prefix?: string
  }

  @column() // Rate limiting
  public rateLimitConfig?: {
    requestsPerMinute: number
    burstLimit: number
  }
}
```

**2. API Connector Service Pattern**
```typescript
// app/services/api_connector_service.ts
export default class APIConnectorService {
  static async testConnection(config: APIConnectorConfig): Promise<TestResult> {
    // Implementation based on auth type
    switch (config.authType) {
      case 'oauth2':
        return this.testOAuth2Connection(config)
      case 'bearer':
        return this.testBearerConnection(config)
      case 'apikey':
        return this.testApiKeyConnection(config)
    }
  }

  static async discoverEndpoints(baseUrl: string): Promise<EndpointInfo[]> {
    // Try common API discovery patterns
    // - /openapi.json
    // - /.well-known/openid-configuration
    // - /api/docs
  }

  static async fetchData(config: APIConnectorConfig, endpoint: string): Promise<any[]> {
    // Handle pagination, rate limiting, retries
  }
}
```

**3. OAuth2 Service with PKCE**
```typescript
// app/services/oauth2_service.ts
export default class OAuth2Service {
  static async generatePKCE(): Promise<{ verifier: string; challenge: string }> {
    const verifier = base64url(crypto.randomBytes(32))
    const digest = crypto.createHash('sha256').update(verifier).digest()
    const challenge = base64url(digest)
    return { verifier, challenge }
  }

  static async startOAuthFlow(config: OAuth2Config): Promise<string> {
    const { verifier, challenge } = await this.generatePKCE()

    // Store verifier temporarily in Redis
    await Redis.setex(`oauth:verifier:${config.connectorId}`, 600, verifier)

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: config.clientId,
      redirect_uri: this.getRedirectUri(),
      code_challenge: challenge,
      code_challenge_method: 'S256',
      scope: config.scopes?.join(' ') || ''
    })

    return `${config.authorizationUrl}?${params}`
  }

  static async handleCallback(code: string, verifier: string): Promise<TokenResponse> {
    // Exchange code for tokens
  }

  static async refreshToken(refreshToken: string, config: OAuth2Config): Promise<TokenResponse> {
    // Refresh expired access token
  }
}
```

**4. Token Management Service**
```typescript
// app/services/token_manager_service.ts
export default class TokenManagerService {
  static async storeTokens(
    tenantId: string,
    connectorId: number,
    tokens: OAuthTokens
  ): Promise<void> {
    const encrypted = await EncryptionService.encrypt(tenantId, tokens)

    // Store in Redis with automatic refresh
    await Redis.setex(
      `connector:tokens:${tenantId}:${connectorId}`,
      tokens.expires_in || 3600,
      encrypted
    )
  }

  static async getValidToken(
    tenantId: string,
    connectorId: number
  ): Promise<string> {
    const encrypted = await Redis.get(`connector:tokens:${tenantId}:${connectorId}`)

    if (!encrypted) {
      throw new Exception('No tokens found')
    }

    const tokens = await EncryptionService.decrypt(tenantId, encrypted)

    // Check if access token is expired
    if (this.isTokenExpired(tokens.access_token)) {
      const refreshed = await this.refreshToken(tenantId, connectorId, tokens.refresh_token)
      return refreshed.access_token
    }

    return tokens.access_token
  }
}
```

#### Frontend Implementation Details

**1. Component Structure**
```typescript
// app/components/api/APIConnectorModal.tsx
export function APIConnectorModal() {
  // Modal for API connector creation
  // Type selection (REST, GraphQL, Webhook)
  // Authentication method selection
  // Configuration forms based on auth type
}
```

**2. OAuth2 Flow Component**
```typescript
// app/components/api/OAuth2Flow.tsx
export function OAuth2Flow({ config, onConnect }: OAuth2FlowProps) {
  const [isConnecting, setIsConnecting] = useState(false)

  const handleConnect = async () => {
    setIsConnecting(true)
    try {
      const authUrl = await api.startOAuthFlow(config)
      window.location.href = authUrl
    } catch (error) {
      // Handle error
    }
  }

  // Handle OAuth2 callback
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search)
    const code = urlParams.get('code')
    const state = urlParams.get('state')

    if (code && state) {
      handleOAuthCallback(code, state)
    }
  }, [])
}
```

### Architecture Compliance

1. **Service Layer Pattern**: Follow existing service structure in `app/services/`
2. **Controller Pattern**: Extend existing `ConnectorsController` with API endpoints
3. **Model Layer**: Extend existing `DatabaseConnection` model with API-specific fields
4. **API Versioning**: Use `/api/v2` prefix as defined in integration architecture
5. **Authentication**: Extend existing JWT middleware with OAuth2 flows
6. **Multi-tenancy**: Ensure all connector data is tenant-scoped

### File Structure Requirements

```
app/
├── controllers/
│   └── connectors_controller.ts          # EXTENDED (API endpoints)
├── models/
│   └── database_connection.ts            # EXTENDED (API fields)
├── services/
│   ├── api_connector_service.ts          # NEW
│   ├── oauth2_service.ts                 # NEW
│   ├── token_manager_service.ts          # NEW
│   └── encryption_service.ts             # EXTENDED
├── validators/
│   └── connectors_validator.ts           # EXTENDED (API validation)
└── jobs/
    └── token_refresh_job.ts              # NEW

database/migrations/
└── xxx_extend_database_connections_for_api.ts  # NEW

frontend/
├── components/
│   └── api/                              # NEW
│       ├── APIConnectorModal.tsx
│       ├── APIConnectorForm.tsx
│       ├── OAuth2Flow.tsx
│       ├── BearerTokenForm.tsx
│       ├── APIKeyForm.tsx
│       ├── EndpointSelector.tsx
│       └── ConnectionTestButton.tsx
├── lib/
│   ├── api/
│   │   └── connectors.ts                 # EXTENDED (API endpoints)
│   └── hooks/
│       └── useConnectors.ts              # EXTENDED (API state)
└── stores/
    └── connectors.store.ts               # EXTENDED (API connectors)
```

### Library/Framework Requirements

#### Backend Dependencies
```json
{
  "node-fetch": "^3.3.0",        // HTTP client for API calls
  "openid-client": "^5.6.0",      // OAuth2/OIDC client library
  "crypto": "^1.0.1",            // Built-in Node.js crypto
  "base64url": "^8.0.0"          // PKCE encoding
}
```

#### Frontend Dependencies (none new, using existing)
- React Hook Form for form validation
- TanStack Query for API calls
- Zustand for state management

### Security Considerations

1. **Token Storage**: Use existing EncryptionService with tenant-specific keys
2. **PKCE Implementation**: Generate cryptographically secure code verifiers
3. **OAuth2 State**: Store and verify state parameter to prevent CSRF
4. **Token Refresh**: Secure background token refresh with proper error handling
5. **Rate Limiting**: Apply rate limiting to OAuth2 endpoints
6. **Audit Logging**: Log all token operations for compliance

### Testing Requirements

1. **Backend Tests**:
   - Unit tests for OAuth2 PKCE flow
   - Integration tests with mock OAuth2 servers
   - Token refresh automation tests
   - Rate limiting validation tests

2. **Frontend Tests**:
   - OAuth2 flow component tests
   - Form validation tests
   - API integration tests

3. **E2E Tests**:
   - Complete OAuth2 connector setup
   - Token refresh scenarios
   - Error handling for expired tokens

### Performance Requirements

1. **Connection Testing**: 30s timeout for API connection tests
2. **Token Refresh**: Background refresh before token expiration
3. **Rate Limiting**: Configurable per connector (default: 60 requests/minute)
4. **Caching**: Cache API discovery endpoints for 1 hour

## Context Reference

### Epic Context
From Epic 1: Universal Data Connectors
- Story 1.2 extends connector framework to support REST APIs
- Builds on authentication patterns from Story 1.1
- Enables real-time data sync for Alpha phase

### Previous Work
Story 1.1 established:
- Database connector patterns and service structure
- Encryption service for credential storage
- Multi-tenant connector isolation
- API v2 routing structure

### Project Context
- Current system only supports file uploads (CSV, Excel)
- Need to extend to live API connections
- Must integrate with existing dataset processing pipeline
- Maintain multi-tenant isolation

## Git Intelligence

Based on recent commits, the system has:
- Stable connector infrastructure from Story 1.1
- Encryption service with tenant-specific key derivation
- Existing rate limiting with Redis
- Background job processing with Bull queues

## Latest Tech Information

### OAuth2 with PKCE (2025 Best Practices)
- Use SHA256 for code challenge method
- Store PKCE verifier temporarily in Redis with TTL
- Implement secure state parameter for CSRF protection
- Use short-lived access tokens (15 minutes) with refresh tokens

### Token Storage Patterns
- Encrypt tokens with tenant-specific keys using existing EncryptionService
- Store in Redis with automatic expiration
- Implement background token refresh before expiration
- Log token operations for audit trails

### API Rate Limiting
- Implement per-connector rate limiting (not per-user)
- Use sliding window algorithm with Redis
- Allow connector-specific configuration
- Add burst handling for batch operations

## Dev Agent Record

### Agent Model Used
Claude-4 (anthropic-claude-4-20241101)

### Implementation Notes
This story extends the connector framework established in Story 1.1 to support REST APIs with multiple authentication methods. The implementation focuses on security (OAuth2 PKCE), automatic token management, and seamless integration with existing patterns.

### Key Architectural Decisions
1. **Extend Existing Models**: Use `database_connections` table for API connectors
2. **Leverage Encryption Service**: Reuse existing tenant-specific encryption
3. **Background Token Refresh**: Use existing Bull queue system
4. **Per-Connector Rate Limiting**: Implement at connector level, not user level
5. **OAuth2 PKCE**: Use industry standard for SPA security

### Security Implementation
- PKCE (Proof Key for Code Exchange) for OAuth2 security
- Tenant-specific token encryption using existing EncryptionService
- CSRF protection with state parameters
- Audit logging for all token operations
- Secure token refresh with proper error handling