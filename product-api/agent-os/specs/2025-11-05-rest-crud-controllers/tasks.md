# Task Breakdown: REST CRUD Controllers Implementation

## Overview
Total Tasks: 12 Task Groups (47 total sub-tasks)

## Task List

### Infrastructure and Foundation

#### Task Group 1: Project Structure and Base Components
**Dependencies:** None

- [x] 1.0 Complete project structure and base components
  - [x] 1.1 Write 2-8 focused tests for base components
    - Test BaseController patterns and error handling
    - Test middleware functionality
    - Test service registration
  - [x] 1.2 Create Controllers directory structure
    - Create `/Controllers` folder if not exists
    - Create subfolders for organization
  - [x] 1.3 Create DTOs directory structure
    - Create `/DTOs/Requests` folder
    - Create `/DTOs/Responses` folder
    - Create entity-specific subfolders
  - [x] 1.4 Create Middleware directory
    - Create `/Middleware` folder
  - [x] 1.5 Create Extensions directory
    - Create `/Extensions` folder for service registration
  - [x] 1.6 Implement BaseController class
    - Common CRUD patterns
    - Pagination helpers
    - Error handling methods
    - Async/await patterns
  - [x] 1.7 Create custom exception classes
    - ValidationException
    - NotFoundException
    - DuplicateResourceException
    - DatabaseException
    - UnauthorizedException
  - [x] 1.8 Ensure infrastructure tests pass
    - Run BaseController tests
    - Verify folder structure
    - Test exception classes

**Acceptance Criteria:**
- Project structure follows specified layout
- BaseController implements common patterns
- Custom exceptions properly defined
- Base component tests pass

#### Task Group 2: Error Handling Middleware
**Dependencies:** Task Group 1

- [ ] 2.0 Complete error handling middleware
  - [ ] 2.1 Write 2-8 focused tests for error middleware
    - Test exception interception
    - Test error response formatting
    - Test status code mapping
    - Test trace ID generation
  - [ ] 2.2 Implement ErrorHandlingMiddleware class
    - Exception interception logic
    - Error response formatting
    - HTTP status code mapping
    - Trace ID generation
  - [ ] 2.3 Create error response models
    - ErrorResponse class
    - ErrorDetail class
    - Consistent JSON structure
  - [ ] 2.4 Register middleware in Program.cs
    - Add to request pipeline
    - Configure order properly
  - [ ] 2.5 Add logging integration
    - Structured logging for errors
    - Trace correlation
  - [ ] 2.6 Ensure error middleware tests pass
    - Run middleware tests
    - Verify error responses
    - Test exception scenarios

**Acceptance Criteria:**
- Error middleware intercepts all exceptions
- Error responses follow specified format
- Proper HTTP status codes returned
- Trace IDs generated for correlation
- Middleware tests pass

### Data Transfer Objects (DTOs)

#### Task Group 3: Client DTOs
**Dependencies:** Task Group 1

- [ ] 3.0 Complete Client DTOs
  - [ ] 3.1 Write 2-8 focused tests for Client DTOs
    - Test validation rules
    - Test model binding
    - Test property mapping
  - [ ] 3.2 Create CreateClientRequest DTO
    - Fields: Name (required), Type (required), Status (required)
    - Validation attributes
    - String length constraints
    - Enum validation
  - [ ] 3.3 Create UpdateClientRequest DTO
    - Fields: Name (optional), Type (optional), Status (optional)
    - Partial update support
    - Validation for provided fields
  - [ ] 3.4 Create ClientResponse DTO
    - Fields: Id, Name, Type, Status, CreatedAt, UpdatedAt
    - Exclude sensitive fields
    - Proper date formatting
  - [ ] 3.5 Add mapping extensions
    - Entity to DTO mapping
    - DTO to entity mapping
    - Null handling
  - [ ] 3.6 Ensure Client DTO tests pass
    - Run DTO validation tests
    - Verify mapping logic
    - Test model binding

**Acceptance Criteria:**
- Client DTOs properly defined
- Validation rules enforced
- Mapping works correctly
- DTO tests pass

#### Task Group 4: User DTOs
**Dependencies:** Task Group 1

- [ ] 4.0 Complete User DTOs
  - [ ] 4.1 Write 2-8 focused tests for User DTOs
    - Test email validation
    - Test password requirements
    - Test model binding
  - [ ] 4.2 Create CreateUserRequest DTO
    - Fields: ClientId, Email, Password, FirstName, LastName, Role
    - Email format validation
    - Password requirements (min 8 chars, 1 letter, 1 number)
    - GUID validation for ClientId
  - [ ] 4.3 Create UpdateUserRequest DTO
    - Fields: Email, FirstName, LastName, Role, Status (all optional)
    - Email format validation
    - Enum validation
  - [ ] 4.4 CreateUserResponse DTO
    - Fields: Id, ClientId, Email, FirstName, LastName, Role, Status, LastLoginAt, CreatedAt, UpdatedAt
    - Exclude password hash
    - Proper date formatting
  - [ ] 4.5 Add mapping extensions
    - Entity to DTO mapping
    - DTO to entity mapping
    - Password handling (exclude from responses)
  - [ ] 4.6 Ensure User DTO tests pass
    - Run DTO validation tests
    - Verify email validation
    - Test password requirements

**Acceptance Criteria:**
- User DTOs properly defined
- Email validation enforced
- Password requirements enforced
- Password excluded from responses
- DTO tests pass

#### Task Group 5: UserPreference DTOs
**Dependencies:** Task Group 1

- [ ] 5.0 Complete UserPreference DTOs
  - [ ] 5.1 Write 2-8 focused tests for UserPreference DTOs
    - Test enum validation
    - Test model binding
    - Test GUID validation
  - [ ] 5.2 Create CreateUserPreferenceRequest DTO
    - Fields: UserId, Theme, Language, NotificationsEnabled
    - GUID validation for UserId
    - Enum validation for Theme
    - Boolean validation
  - [ ] 5.3 Create UpdateUserPreferenceRequest DTO
    - Fields: Theme, Language, NotificationsEnabled (all optional)
    - Enum validation
    - Boolean validation
  - [ ] 5.4 CreateUserPreferenceResponse DTO
    - Fields: Id, UserId, Theme, Language, NotificationsEnabled, CreatedAt, UpdatedAt
    - Proper date formatting
  - [ ] 5.5 Add mapping extensions
    - Entity to DTO mapping
    - DTO to entity mapping
    - Null handling
  - [ ] 5.6 Ensure UserPreference DTO tests pass
    - Run DTO validation tests
    - Verify enum validation
    - Test GUID validation

**Acceptance Criteria:**
- UserPreference DTOs properly defined
- Enum validation enforced
- GUID validation enforced
- DTO tests pass

#### Task Group 6: UserSession DTOs
**Dependencies:** Task Group 1

- [ ] 6.0 Complete UserSession DTOs
  - [ ] 6.1 Write 2-8 focused tests for UserSession DTOs
    - Test GUID validation
    - Test date validation
    - Test model binding
  - [ ] 6.2 Create CreateUserSessionRequest DTO
    - Fields: UserId, TokenId, IpAddress, UserAgent, ExpiresAt
    - GUID validation for UserId and TokenId
    - Date validation for ExpiresAt
  - [ ] 6.3 Create UpdateUserSessionRequest DTO
    - Fields: ExpiresAt (optional)
    - Date validation
  - [ ] 6.4 CreateUserSessionResponse DTO
    - Fields: Id, UserId, TokenId, IpAddress, UserAgent, CreatedAt, ExpiresAt
    - Proper date formatting
  - [ ] 6.5 Add mapping extensions
    - Entity to DTO mapping
    - DTO to entity mapping
    - Null handling
  - [ ] 6.6 Ensure UserSession DTO tests pass
    - Run DTO validation tests
    - Verify GUID validation
    - Test date validation

**Acceptance Criteria:**
- UserSession DTOs properly defined
- GUID validation enforced
- Date validation enforced
- DTO tests pass

### Controller Implementation

#### Task Group 7: Client Controller
**Dependencies:** Task Groups 1, 2, 3

- [ ] 7.0 Complete Client controller
  - [ ] 7.1 Write 2-8 focused tests for Client controller
    - Test GET all with pagination
    - Test GET by ID
    - Test POST creation
    - Test PATCH update
    - Test DELETE soft delete
  - [ ] 7.2 Create ClientsController class
    - Inherit from BaseController
    - Inject VarlorDbContext
    - Route configuration
  - [ ] 7.3 Implement GET /api/clients endpoint
    - Pagination with skip/take parameters
    - Async database query
    - Proper DTO mapping
    - 200 status code
  - [ ] 7.4 Implement GET /api/clients/{id} endpoint
    - Find entity by ID
    - Handle not found (404)
    - DTO mapping
    - 200 status code
  - [ ] 7.5 Implement POST /api/clients endpoint
    - Model validation
    - Entity creation
    - Database save
    - DTO mapping
    - 201 status code
  - [ ] 7.6 Implement PATCH /api/clients/{id} endpoint
    - Partial update support
    - Entity tracking
    - Property validation
    - Database save
    - 200 status code
  - [ ] 7.7 Implement DELETE /api/clients/{id} endpoint
    - Soft delete (update status to INACTIVE)
    - Entity existence check
    - Database save
    - 200 status code
  - [ ] 7.8 Add Swagger documentation
    - Operation summaries
    - Parameter descriptions
    - Response examples
  - [ ] 7.9 Ensure Client controller tests pass
    - Run controller tests
    - Verify all endpoints
    - Test pagination

**Acceptance Criteria:**
- All CRUD endpoints implemented
- Pagination works correctly
- Soft delete implemented
- Proper HTTP status codes
- Controller tests pass

#### Task Group 8: User Controller
**Dependencies:** Task Groups 1, 2, 4

- [ ] 8.0 Complete User controller
  - [ ] 8.1 Write 2-8 focused tests for User controller
    - Test GET all with pagination
    - Test GET by ID
    - Test POST creation with password hashing
    - Test auto-creation of preferences/sessions
    - Test PATCH update
    - Test DELETE soft delete
  - [ ] 8.2 Create UsersController class
    - Inherit from BaseController
    - Inject VarlorDbContext and PasswordHasher
    - Route configuration
  - [ ] 8.3 Implement GET /api/users endpoint
    - Pagination with skip/take parameters
    - Async database query
    - Proper DTO mapping
    - 200 status code
  - [ ] 8.4 Implement GET /api/users/{id} endpoint
    - Find entity by ID
    - Handle not found (404)
    - DTO mapping
    - 200 status code
  - [ ] 8.5 Implement POST /api/users endpoint
    - Model validation
    - Unique email validation
    - Password hashing
    - User creation
    - Auto-create UserPreference
    - Auto-create UserSession
    - Transaction handling
    - DTO mapping
    - 201 status code
  - [ ] 8.6 Implement PATCH /api/users/{id} endpoint
    - Partial update support
    - Email uniqueness check
    - Entity tracking
    - Database save
    - 200 status code
  - [ ] 8.7 Implement DELETE /api/users/{id} endpoint
    - Soft delete (update status to INACTIVE)
    - Entity existence check
    - Database save
    - 200 status code
  - [ ] 8.8 Add Swagger documentation
    - Operation summaries
    - Parameter descriptions
    - Response examples
    - Security considerations
  - [ ] 8.9 Ensure User controller tests pass
    - Run controller tests
    - Verify password hashing
    - Test auto-creation logic

**Acceptance Criteria:**
- All CRUD endpoints implemented
- Password hashing works correctly
- Auto-creation of preferences/sessions
- Unique email validation
- Soft delete implemented
- Controller tests pass

#### Task Group 9: UserPreference Controller
**Dependencies:** Task Groups 1, 2, 5

- [ ] 9.0 Complete UserPreference controller
  - [ ] 9.1 Write 2-8 focused tests for UserPreference controller
    - Test GET all with pagination
    - Test GET by ID
    - Test POST creation
    - Test PATCH update
    - Test DELETE hard delete
  - [ ] 9.2 CreateUserPreferencesController class
    - Inherit from BaseController
    - Inject VarlorDbContext
    - Route configuration
  - [ ] 9.3 Implement GET /api/userpreferences endpoint
    - Pagination with skip/take parameters
    - Async database query
    - Proper DTO mapping
    - 200 status code
  - [ ] 9.4 Implement GET /api/userpreferences/{id} endpoint
    - Find entity by ID
    - Handle not found (404)
    - DTO mapping
    - 200 status code
  - [ ] 9.5 Implement POST /api/userpreferences endpoint
    - Model validation
    - Valid UserId check
    - Entity creation
    - Database save
    - DTO mapping
    - 201 status code
  - [ ] 9.6 Implement PATCH /api/userpreferences/{id} endpoint
    - Partial update support
    - Entity tracking
    - Database save
    - 200 status code
  - [ ] 9.7 Implement DELETE /api/userpreferences/{id} endpoint
    - Hard delete from database
    - Entity existence check
    - Database save
    - 200 status code
  - [ ] 9.8 Add Swagger documentation
    - Operation summaries
    - Parameter descriptions
    - Response examples
  - [ ] 9.9 Ensure UserPreference controller tests pass
    - Run controller tests
    - Verify all endpoints
    - Test hard delete

**Acceptance Criteria:**
- All CRUD endpoints implemented
- Hard delete implemented
- UserId validation enforced
- Proper HTTP status codes
- Controller tests pass

#### Task Group 10: UserSession Controller
**Dependencies:** Task Groups 1, 2, 6

- [ ] 10.0 Complete UserSession controller
  - [ ] 10.1 Write 2-8 focused tests for UserSession controller
    - Test GET all with pagination
    - Test GET by ID
    - Test POST creation
    - Test PATCH update
    - Test DELETE hard delete
  - [ ] 10.2 CreateUserSessionsController class
    - Inherit from BaseController
    - Inject VarlorDbContext
    - Route configuration
  - [ ] 10.3 Implement GET /api/usersessions endpoint
    - Pagination with skip/take parameters
    - Async database query
    - Proper DTO mapping
    - 200 status code
  - [ ] 10.4 Implement GET /api/usersessions/{id} endpoint
    - Find entity by ID
    - Handle not found (404)
    - DTO mapping
    - 200 status code
  - [ ] 10.5 Implement POST /api/usersessions endpoint
    - Model validation
    - Valid UserId check
    - Entity creation
    - Database save
    - DTO mapping
    - 201 status code
  - [ ] 10.6 Implement PATCH /api/usersessions/{id} endpoint
    - Partial update support
    - Entity tracking
    - Database save
    - 200 status code
  - [ ] 10.7 Implement DELETE /api/usersessions/{id} endpoint
    - Hard delete from database
    - Entity existence check
    - Database save
    - 200 status code
  - [ ] 10.8 Add Swagger documentation
    - Operation summaries
    - Parameter descriptions
    - Response examples
  - [ ] 10.9 Ensure UserSession controller tests pass
    - Run controller tests
    - Verify all endpoints
    - Test hard delete

**Acceptance Criteria:**
- All CRUD endpoints implemented
- Hard delete implemented
- UserId validation enforced
- Proper HTTP status codes
- Controller tests pass

### Service Registration and Configuration

#### Task Group 11: Service Registration
**Dependencies:** Task Groups 1-10

- [ ] 11.0 Complete service registration
  - [ ] 11.1 Write 2-8 focused tests for service registration
    - Test controller registration
    - Test middleware registration
    - Test DbContext configuration
  - [ ] 11.2 Create ServiceCollectionExtensions
    - AddControllers registration
    - AddErrorHandling registration
    - AddSwaggerGen configuration
  - [ ] 11.3 Register PasswordHasher service
    - AddIdentity for password hashing
    - Configure password options
  - [ ] 11.4 Configure Swagger in Program.cs
    - Include XML comments
    - Configure security definitions
    - Set API info
  - [ ] 11.5 Update Program.cs with service registrations
    - Call service registration methods
    - Configure middleware pipeline
    - Set up development exceptions
  - [ ] 11.6 Ensure service registration tests pass
    - Run integration tests
    - Verify service resolution
    - Test middleware pipeline

**Acceptance Criteria:**
- All services properly registered
- Middleware pipeline configured
- Swagger documentation generated
- Service registration tests pass

### Integration and Testing

#### Task Group 12: Integration Testing and Final Verification
**Dependencies:** Task Groups 1-11

- [ ] 12.0 Complete integration testing and verification
  - [ ] 12.1 Review tests from Task Groups 1-11
    - Review infrastructure tests (Task Groups 1-2)
    - Review DTO tests (Task Groups 3-6)
    - Review controller tests (Task Groups 7-10)
    - Review service registration tests (Task Group 11)
    - Total existing tests: approximately 24-64 tests
  - [ ] 12.2 Analyze test coverage gaps for THIS feature only
    - Identify critical user workflows lacking coverage
    - Focus on end-to-end API workflows
    - Test error scenarios comprehensively
    - Verify business logic implementation
  - [ ] 12.3 Write up to 10 additional strategic tests maximum
    - Add integration tests for complete workflows
    - Test password hashing end-to-end
    - Test auto-creation of related entities
    - Test error middleware with various exception types
    - Add performance tests for pagination
    - Maximum of 10 new tests total
  - [ ] 12.4 Run comprehensive feature-specific test suite
    - Run ALL tests related to this spec (approximately 34-74 tests maximum)
    - Verify critical workflows pass
    - Test error handling scenarios
    - Validate business rules implementation
  - [ ] 12.5 Perform manual API verification
    - Test all endpoints with Swagger UI
    - Verify HTTP status codes
    - Test pagination functionality
    - Validate error responses
  - [ ] 12.6 Validate business requirements
    - Verify password hashing works
    - Confirm auto-creation logic
    - Test soft delete behavior
    - Test hard delete behavior
  - [ ] 12.7 Performance validation
    - Test pagination with large datasets
    - Verify async/await implementation
    - Test concurrent request handling
  - [ ] 12.8 Final documentation and cleanup
    - Update API documentation
    - Clean up test files
    - Verify code quality standards

**Acceptance Criteria:**
- All feature-specific tests pass (approximately 34-74 tests total)
- Critical workflows covered
- Business requirements verified
- Performance meets requirements
- API endpoints fully functional
- Error handling works correctly

## Execution Order

Recommended implementation sequence:
1. **Infrastructure Setup** (Task Groups 1-2)
2. **DTO Development** (Task Groups 3-6) - Can be done in parallel
3. **Controller Implementation** (Task Groups 7-10) - Sequential, starting with simplest (Client)
4. **Service Registration** (Task Group 11)
5. **Integration Testing** (Task Group 12)

## Testing Strategy

- **Limited Test Writing**: Each task group writes 2-8 focused tests maximum
- **Test Verification**: Run only newly written tests, not entire suite
- **Gap Analysis**: Final task group adds maximum 10 strategic tests
- **Focus**: Critical workflows and integration points over exhaustive coverage
- **Total Tests**: Approximately 34-74 tests maximum for entire feature

## Quality Gates

- Each task group must pass acceptance criteria before proceeding
- All async operations properly implemented
- Proper HTTP status codes returned
- Error handling middleware functions correctly
- Business logic requirements satisfied
- Performance requirements met