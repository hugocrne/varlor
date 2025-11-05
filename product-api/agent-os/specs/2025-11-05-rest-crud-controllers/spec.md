# Specification: REST CRUD Controllers Implementation

## Goal
Create a comprehensive set of REST API controllers that provide full CRUD functionality for Client, User, UserPreference, and UserSession entities using async/await patterns, proper DTO separation, and enterprise-grade error handling.

## User Stories
- As a developer, I want to create, read, update, and delete client records through REST endpoints so that I can manage client data programmatically
- As a developer, I want to manage user accounts with password hashing and automatic preference/session creation so that user onboarding is seamless
- As a developer, I want paginated access to all entity collections so that I can handle large datasets efficiently
- As a developer, I want structured error responses and proper HTTP status codes so that API consumers can handle errors gracefully
- As a developer, I want separate DTOs from domain entities so that API contracts remain stable and database schemas can evolve independently

## Core Requirements
- Four REST controllers (Client, User, UserPreference, UserSession) with full CRUD operations
- Async/await patterns throughout using Entity Framework Core
- DTO Pattern implementation separating API models from domain entities
- Server-side password hashing for User creation using ASP.NET Core Identity
- Auto-creation of UserPreference and UserSession when creating Users
- Soft delete for User/Client entities (status field updates)
- Hard delete for UserPreference/UserSession entities
- Simple pagination with skip/take parameters for GET all endpoints
- Centralized error handling middleware with structured responses
- Open endpoints without authentication/authorization
- Proper HTTP status codes (200, 201, 400, 404, 500)

## Visual Design
No visual assets provided for this backend API specification.

## Reusable Components
### Existing Code to Leverage
- Models: Client, User, UserPreference, UserSession entities already defined
- DbContext: VarlorDbContext with full EF Core configuration
- Enums: ClientType, ClientStatus, UserRole, UserStatus, Theme already defined
- Project Structure: .NET 8 Web API with PostgreSQL already configured
- Program.cs: Controllers, Swagger, and DbContext services already registered

### New Components Required
- Controllers directory with 4 controller classes
- DTOs directory with request/response models for each entity
- Error handling middleware for centralized exception management
- Base controller class with common CRUD patterns to reduce duplication
- Validation attributes and custom validators (unique email constraint)

## Technical Approach

### Controller Structure
Each controller will implement the following endpoints:
- `GET /api/[entity]` - Retrieve all entities with pagination (skip/take)
- `GET /api/[entity]/{id}` - Retrieve specific entity by ID
- `POST /api/[entity]` - Create new entity
- `PATCH /api/[entity]/{id}` - Partial update of entity
- `DELETE /api/[entity]/{id}` - Delete entity (soft/hard based on requirements)

### DTO Pattern Implementation
- Request DTOs for POST/PATCH operations
- Response DTOs for all GET operations
- Separate mapping logic between entities and DTOs
- Exclusion of sensitive data (password hashes, internal fields)

### Special Business Logic
- User creation triggers automatic UserPreference and UserSession creation
- Password hashing using ASP.NET Core Identity PasswordHasher
- Soft delete implementation via status field updates for User/Client
- Unique email validation for User creation

### Error Handling Strategy
- Custom middleware for exception interception
- Structured error responses with consistent format
- Proper HTTP status code mapping
- Validation error handling with field-level details

## API Endpoint Specifications

### Client Controller (/api/clients)
- `GET /api/clients?skip=0&take=10` - Paginated client list
- `GET /api/clients/{id}` - Get client by ID
- `POST /api/clients` - Create new client
- `PATCH /api/clients/{id}` - Update client (name, type, status)
- `DELETE /api/clients/{id}` - Soft delete (update status to INACTIVE)

### User Controller (/api/users)
- `GET /api/users?skip=0&take=10` - Paginated user list
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user with password hashing, auto-create preferences/sessions
- `PATCH /api/users/{id}` - Update user (excluding password)
- `DELETE /api/users/{id}` - Soft delete (update status to INACTIVE)

### UserPreference Controller (/api/userpreferences)
- `GET /api/userpreferences?skip=0&take=10` - Paginated preferences list
- `GET /api/userpreferences/{id}` - Get preferences by ID
- `POST /api/userpreferences` - Create new preferences
- `PATCH /api/userpreferences/{id}` - Update preferences
- `DELETE /api/userpreferences/{id}` - Hard delete from database

### UserSession Controller (/api/usersessions)
- `GET /api/usersessions?skip=0&take=10` - Paginated session list
- `GET /api/usersessions/{id}` - Get session by ID
- `POST /api/usersessions` - Create new session
- `PATCH /api/usersessions/{id}` - Update session
- `DELETE /api/usersessions/{id}` - Hard delete from database

## DTO Structure Requirements

### Client DTOs
- `CreateClientRequest`: Name, Type, Status
- `UpdateClientRequest`: Optional Name, Type, Status
- `ClientResponse`: Id, Name, Type, Status, CreatedAt, UpdatedAt

### User DTOs
- `CreateUserRequest`: ClientId, Email, Password, FirstName, LastName, Role
- `UpdateUserRequest`: Optional Email, FirstName, LastName, Role, Status
- `UserResponse`: Id, ClientId, Email, FirstName, LastName, Role, Status, LastLoginAt, CreatedAt, UpdatedAt

### UserPreference DTOs
- `CreateUserPreferenceRequest`: UserId, Theme, Language, NotificationsEnabled
- `UpdateUserPreferenceRequest`: Optional Theme, Language, NotificationsEnabled
- `UserPreferenceResponse`: Id, UserId, Theme, Language, NotificationsEnabled, CreatedAt, UpdatedAt

### UserSession DTOs
- `CreateUserSessionRequest`: UserId, TokenId, IpAddress, UserAgent, ExpiresAt
- `UpdateUserSessionRequest`: Optional ExpiresAt
- `UserSessionResponse`: Id, UserId, TokenId, IpAddress, UserAgent, CreatedAt, ExpiresAt

## Validation Requirements

### Standard Validation
- Required field validation on all required properties
- String length validation matching database constraints
- Email format validation for User.Email field
- GUID validation for ID fields and foreign key references

### Custom Validation
- Unique email constraint for User creation
- Valid ClientId reference for User creation
- Valid UserId reference for UserPreference/UserSession creation
- Valid enum values for all enum properties

## Error Handling Specifications

### Error Response Format
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "Email",
        "message": "Email is already in use"
      }
    ],
    "timestamp": "2025-11-05T15:30:00Z",
    "traceId": "guid"
  }
}
```

### HTTP Status Code Mapping
- 200: Successful GET/PATCH operations
- 201: Successful POST operations
- 400: Validation errors, bad requests
- 404: Resource not found
- 500: Server errors, unhandled exceptions

### Exception Types
- ValidationException: Input validation failures
- NotFoundException: Resource not found
- DuplicateResourceException: Constraint violations (unique email)
- DatabaseException: EF Core operation failures
- UnauthorizedException: Future auth implementation

## Security Considerations

### Password Handling
- Server-side password hashing using ASP.NET Core Identity
- Never return password hashes in response DTOs
- Password requirements: minimum 8 characters, at least one letter and one number

### Data Exposure
- Exclude sensitive fields from all response DTOs
- Implement proper input validation to prevent injection attacks
- Use parameterized queries through EF Core

### Future Security Enhancements
- JWT authentication middleware (out of scope)
- Rate limiting for API endpoints
- HTTPS enforcement in production
- CORS configuration for frontend integration

## Implementation Guidelines

### Project Structure
```
Controllers/
├── BaseController.cs
├── ClientsController.cs
├── UsersController.cs
├── UserPreferencesController.cs
└── UserSessionsController.cs

DTOs/
├── Requests/
│   ├── Client/
│   ├── User/
│   ├── UserPreference/
│   └── UserSession/
└── Responses/
    ├── Client/
    ├── User/
    ├── UserPreference/
    └── UserSession/

Middleware/
└── ErrorHandlingMiddleware.cs

Extensions/
└── ServiceCollectionExtensions.cs
```

### Async/Await Patterns
- All database operations must use async methods
- Proper async/await chaining throughout controllers
- ConfigureAwait(false) for library code
- CancellationToken support where appropriate

### Entity Framework Core Best Practices
- Use async methods (ToListAsync, FindAsync, SaveChangesAsync)
- Implement proper change tracking for updates
- Handle concurrency conflicts where needed
- Use transactions for multi-entity operations

### Testing Considerations
- Unit tests for controller logic
- Integration tests for database operations
- Test validation rules and error scenarios
- Mock external dependencies for isolation

## Out of Scope
- Authentication and authorization (JWT, roles, permissions)
- Complex filtering, sorting, and searching capabilities
- Advanced business logic beyond specified requirements
- Database schema modifications or migrations
- Frontend UI components
- API versioning strategy
- Caching mechanisms
- Logging and monitoring infrastructure
- API documentation beyond auto-generated Swagger

## Success Criteria

### Functional Requirements
- All 4 controllers successfully implement full CRUD operations
- API responses conform to specified DTO structures
- Password hashing works correctly for user creation
- User creation automatically creates associated preferences and sessions
- Pagination functions correctly for all GET all endpoints
- Soft delete works for User/Client entities
- Hard delete works for UserPreference/UserSession entities
- Unique email validation prevents duplicate user creation
- Error middleware returns properly formatted error responses

### Performance Requirements
- All endpoints respond within 500ms for typical operations
- Pagination handles datasets up to 10,000 records efficiently
- Concurrent requests handled without database deadlocks
- Memory usage remains stable under load

### Quality Requirements
- All async operations properly implemented
- Code follows .NET 8 Web API conventions
- Swagger documentation generated accurately
- Error scenarios handled gracefully with proper HTTP status codes
- Input validation prevents invalid data persistence
- Database operations are transactional where needed