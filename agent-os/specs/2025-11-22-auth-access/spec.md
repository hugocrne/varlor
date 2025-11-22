# Specification: Auth + Access (MVP)

## Goal

Implement a secure, JWT-based authentication system for Varlor MVP that establishes foundational patterns for all future development. This includes login/logout functionality, session management, admin account creation, and the core architectural patterns that will guide subsequent features.

## User Stories

### As an admin user
- I want to log in with my email and password so that I can access the Varlor platform securely
- I want my session to remain active for a reasonable time so that I don't have to log in repeatedly during active use
- I want to log out securely so that my session is terminated and my account remains protected
- I want clear feedback when login fails so that I understand what went wrong without compromising security

### As a system administrator (deployment)
- I want to create the initial admin account via environment variables so that I can bootstrap the system securely
- I want the system to enforce strong password requirements so that accounts remain secure
- I want failed login attempts to be rate-limited so that brute force attacks are prevented

## Core Requirements

### Authentication Flow
- Email and password login form with client-side and server-side validation
- JWT-based session management using httpOnly cookies for XSS protection
- Access tokens with 15-minute lifetime for active sessions
- Refresh tokens with 7-day lifetime stored in PostgreSQL
- Automatic token refresh mechanism before access token expiration
- Secure logout that invalidates refresh tokens server-side and clears client cookies
- Generic error messages ("Invalid email or password") to prevent user enumeration
- Rate limiting: 5 failed login attempts result in 15-minute account lockout

### Admin Account Management
- Database seed script (npm run seed:admin) for initial admin account creation
- Environment variable configuration: ADMIN_EMAIL and ADMIN_PASSWORD
- Secure password hashing with bcrypt (cost factor 12)

### Password Security
- Minimum 12 characters required
- Must contain at least one uppercase letter, lowercase letter, number, and special character
- Passwords hashed using bcrypt with cost factor 12
- No password expiration in MVP

### User Roles & Permissions
- Single "admin" role for MVP with full system access
- Database schema prepared for future role expansion (analyst, viewer in Alpha)
- Role-based access control enforced at API level using guards

### Session Management
- httpOnly cookies prevent client-side JavaScript access to tokens
- Secure flag enabled for production (HTTPS)
- SameSite=Lax for CSRF protection
- Refresh token rotation on use for enhanced security
- Redirect to /login page after logout

### Multi-Tenancy Foundation
- Users table includes tenant_id column (set to "default" for MVP)
- Database schema supports future multi-tenant expansion
- Manual configuration per deployment in MVP

## Visual Design

No visual mockups provided. Implementation follows standard authentication UI best practices:

### Login Page Layout
- Centered card-based design with maximum width constraint
- Clean, minimal form with Varlor branding
- Email input field with appropriate validation states
- Password input field with show/hide toggle
- Submit button with loading state during authentication
- Error message display area above or below form
- Responsive design: mobile-first approach with breakpoints at 640px, 768px, 1024px

### Design System
- Shadcn UI components as foundation
- CSS variables for theming:
  - Primary colors (brand)
  - Neutral grays for backgrounds and text
  - Semantic colors (error, success, warning)
  - Spacing scale (4px base unit)
  - Typography scale (heading and body sizes)
- Consistent border radius, shadows, and transitions
- Accessibility: WCAG 2.1 AA compliance minimum

### Component States
- Default state with proper focus indicators
- Loading state with spinner or skeleton
- Error state with red borders and error text
- Success state (for future features)
- Disabled state with reduced opacity

## Reusable Components

This is the FIRST feature implementation for Varlor. All patterns established here will be reused throughout the application.

### Existing Code to Leverage

**Frontend:**
- Basic Next.js App Router structure exists at /Users/hugo/Perso/Projets/varlor/client/web
- Dependencies already installed: Next.js 16, React 19, TypeScript, Tailwind CSS 4, Shadcn UI components
- No existing authentication or form components to reference

**Backend:**
- No backend exists yet - this will be the first NestJS implementation
- Backend will be created at /Users/hugo/Perso/Projets/varlor/server

**Key Insight:** This feature establishes the foundational architecture that all future features will follow.

### New Components Required

#### Frontend Components (First Implementation)

**Layout Components:**
- `AuthLayout` - Container for authentication pages (login, future signup/reset)
  - Centered card design
  - Varlor branding/logo placement
  - Responsive container
  - Background styling

- `DashboardLayout` - Main application layout for authenticated users
  - Header with navigation and user menu
  - Sidebar navigation (prepared for future features)
  - Main content area
  - Footer (optional)

**Form Components:**
- `Input` - Text input with validation states
  - Error, focused, disabled states
  - Label integration
  - Icon support (prefix/suffix)
  - Type-specific variants (email, password, text)

- `Button` - Interactive button component
  - Primary, secondary, ghost, destructive variants
  - Loading state with spinner
  - Disabled state
  - Size variants (sm, md, lg)

- `Label` - Form label with error handling
  - Required indicator
  - Helper text support
  - Error message display

- `Form` - Form wrapper with React Hook Form
  - Validation schema integration (Zod)
  - Error handling
  - Submit handling
  - Field state management

**Authentication Components:**
- `LoginForm` - Complete login form component
  - Email and password fields
  - Submit button with loading state
  - Error display
  - Form validation
  - API integration

**State Management:**
- `useAuthStore` (Zustand) - Authentication state
  - User object
  - Authentication status
  - Token management
  - Login/logout actions

**API Client:**
- `authApi` (TanStack Query) - Authentication API calls
  - Login mutation
  - Refresh mutation
  - Logout mutation
  - Error handling
  - Retry logic

#### Backend Components (First Implementation)

**Modules:**
- `AuthModule` - Authentication feature module
  - AuthController
  - AuthService
  - JWT strategies
  - Guards

- `UsersModule` - User management module
  - UsersController
  - UsersService
  - User repository
  - User entity

- `DatabaseModule` - Database configuration
  - TypeORM setup
  - PostgreSQL connection
  - Migration configuration

**Services:**
- `AuthService` - Authentication business logic
  - Login validation
  - Token generation (access + refresh)
  - Token validation
  - Password verification
  - Rate limiting enforcement

- `UsersService` - User management
  - User creation
  - User retrieval
  - Password hashing
  - User validation

- `TokenService` - Token management
  - Refresh token creation
  - Refresh token validation
  - Refresh token rotation
  - Token cleanup (expired tokens)

**Controllers:**
- `AuthController` - Authentication HTTP endpoints
  - POST /auth/login
  - POST /auth/refresh
  - POST /auth/logout
  - DTO validation
  - Response formatting

**Guards & Decorators:**
- `JwtAuthGuard` - Protects routes requiring authentication
- `@CurrentUser()` - Decorator to extract authenticated user from request
- `@Public()` - Decorator to mark public endpoints (bypass auth)
- `RolesGuard` - Role-based access control (prepared for future roles)

**Database Entities:**
- `User` entity - User account data
- `RefreshToken` entity - Refresh token storage

**DTOs (Data Transfer Objects):**
- `LoginDto` - Login request validation
- `LoginResponseDto` - Login response structure
- `RefreshDto` - Refresh token request
- `UserDto` - User data response

**Middleware:**
- `RateLimitMiddleware` - Login attempt rate limiting
- `RequestLoggerMiddleware` - Request logging
- `ErrorLoggingMiddleware` - Centralized error logging

**Exception Filters:**
- `HttpExceptionFilter` - Global HTTP exception handling
- Custom exceptions: `UnauthorizedException`, `TooManyRequestsException`

**Validation Pipes:**
- `ValidationPipe` - Global DTO validation using class-validator
- Custom validators for password complexity

## Technical Approach

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Client (Next.js)                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Pages: /login, /dashboard                          │  │
│  │  State: Zustand (auth state)                        │  │
│  │  API Client: TanStack Query                         │  │
│  │  Components: Shadcn UI + Custom                     │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTPS + httpOnly Cookies
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Server (NestJS)                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AuthModule                                          │  │
│  │    ├─ AuthController (POST /auth/login, etc)        │  │
│  │    ├─ AuthService (business logic)                  │  │
│  │    ├─ JwtStrategy (Passport)                        │  │
│  │    └─ Guards (JwtAuthGuard, RolesGuard)             │  │
│  │                                                      │  │
│  │  UsersModule                                         │  │
│  │    ├─ UsersService                                   │  │
│  │    └─ User Entity (TypeORM)                         │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │ TypeORM
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                   Database (PostgreSQL)                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Tables:                                             │  │
│  │    - users (id, email, password_hash, role, ...)    │  │
│  │    - refresh_tokens (id, user_id, token_hash, ...)  │  │
│  │    - roles (id, name) [prepared for future]         │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Frontend Architecture

**Technology Stack:**
- Next.js 16 (App Router)
- React 19 with TypeScript
- Shadcn UI for base components
- React Hook Form + Zod for form validation
- TanStack Query for API state management
- Zustand for client state management
- Tailwind CSS 4 for styling

**Directory Structure:**
```
client/web/
├── app/
│   ├── (auth)/
│   │   ├── login/
│   │   │   └── page.tsx              # Login page
│   │   └── layout.tsx                # Auth layout wrapper
│   ├── (dashboard)/
│   │   ├── dashboard/
│   │   │   └── page.tsx              # Main dashboard (protected)
│   │   └── layout.tsx                # Dashboard layout wrapper
│   ├── layout.tsx                    # Root layout
│   └── globals.css                   # Global styles + design tokens
├── components/
│   ├── ui/                           # Shadcn UI components
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── label.tsx
│   │   └── form.tsx
│   ├── auth/
│   │   └── login-form.tsx            # Login form component
│   └── layouts/
│       ├── auth-layout.tsx           # Auth pages layout
│       └── dashboard-layout.tsx      # Dashboard layout
├── lib/
│   ├── api/
│   │   ├── client.ts                 # Axios/fetch client config
│   │   └── auth.ts                   # Auth API functions
│   ├── stores/
│   │   └── auth.store.ts             # Zustand auth store
│   ├── hooks/
│   │   ├── useAuth.ts                # Auth hook
│   │   └── useRequireAuth.ts         # Protected route hook
│   ├── schemas/
│   │   └── auth.schema.ts            # Zod validation schemas
│   └── utils.ts                      # Utility functions
└── types/
    └── auth.types.ts                 # TypeScript types
```

**Key Frontend Patterns:**

1. **Form Validation:** React Hook Form + Zod schemas
2. **API Calls:** TanStack Query mutations and queries
3. **State Management:** Zustand for auth state, TanStack Query for server state
4. **Protected Routes:** Middleware in Next.js App Router
5. **Error Handling:** Error boundaries + toast notifications
6. **Loading States:** Suspense boundaries + skeleton loaders

### Backend Architecture

**Technology Stack:**
- NestJS 10+
- Passport.js with JWT strategy
- TypeORM for PostgreSQL
- class-validator for DTO validation
- bcrypt for password hashing
- @nestjs/jwt for token management

**Directory Structure:**
```
server/
├── src/
│   ├── main.ts                       # Application entry point
│   ├── app.module.ts                 # Root module
│   ├── config/
│   │   ├── database.config.ts        # Database configuration
│   │   ├── jwt.config.ts             # JWT configuration
│   │   └── validation.config.ts      # Validation pipe config
│   ├── common/
│   │   ├── decorators/
│   │   │   ├── current-user.decorator.ts
│   │   │   ├── public.decorator.ts
│   │   │   └── roles.decorator.ts
│   │   ├── guards/
│   │   │   ├── jwt-auth.guard.ts
│   │   │   └── roles.guard.ts
│   │   ├── filters/
│   │   │   └── http-exception.filter.ts
│   │   ├── interceptors/
│   │   │   └── logging.interceptor.ts
│   │   └── middleware/
│   │       └── rate-limit.middleware.ts
│   ├── modules/
│   │   ├── auth/
│   │   │   ├── auth.module.ts
│   │   │   ├── auth.controller.ts
│   │   │   ├── auth.service.ts
│   │   │   ├── strategies/
│   │   │   │   └── jwt.strategy.ts
│   │   │   └── dto/
│   │   │       ├── login.dto.ts
│   │   │       ├── login-response.dto.ts
│   │   │       └── refresh.dto.ts
│   │   ├── users/
│   │   │   ├── users.module.ts
│   │   │   ├── users.service.ts
│   │   │   ├── users.controller.ts
│   │   │   ├── entities/
│   │   │   │   └── user.entity.ts
│   │   │   └── dto/
│   │   │       └── user.dto.ts
│   │   ├── tokens/
│   │   │   ├── tokens.module.ts
│   │   │   ├── tokens.service.ts
│   │   │   └── entities/
│   │   │       └── refresh-token.entity.ts
│   │   └── database/
│   │       └── database.module.ts
│   └── migrations/
│       ├── 1700000000000-CreateUsersTable.ts
│       ├── 1700000001000-CreateRefreshTokensTable.ts
│       └── 1700000002000-CreateRolesTable.ts
├── scripts/
│   └── seed-admin.ts                 # Admin account seeding
├── .env.example                      # Environment variables template
├── nest-cli.json
├── package.json
└── tsconfig.json
```

**Key Backend Patterns:**

1. **Module Organization:** Feature-based modules with clear boundaries
2. **Service Layer:** Business logic isolated in services
3. **Controller Layer:** HTTP handling with DTO validation
4. **Repository Pattern:** TypeORM repositories for database access
5. **Dependency Injection:** Constructor-based DI throughout
6. **Exception Handling:** Global exception filter with structured responses
7. **Middleware Pipeline:** Rate limiting → Logging → Auth → Route handler

### Database Schema

**Users Table:**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'admin',
    tenant_id VARCHAR(100) NOT NULL DEFAULT 'default',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,

    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
```

**Refresh Tokens Table:**
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token UUID REFERENCES refresh_tokens(id),

    CONSTRAINT valid_expiration CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

**Roles Table (prepared for future):**
```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    permissions JSONB DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Seed initial admin role
INSERT INTO roles (name, description, permissions) VALUES
('admin', 'System administrator with full access', '["*"]');
```

**Migration Strategy:**
- Sequential numbered migrations with timestamps
- Up/down migrations for rollback capability
- Data migrations separate from schema migrations
- Migration testing in development environment before production

## API Specifications

### POST /auth/login

**Description:** Authenticate user with email and password, returning access and refresh tokens.

**Request:**
```typescript
POST /auth/login
Content-Type: application/json

{
  "email": "admin@varlor.com",
  "password": "SecurePassword123!"
}
```

**Success Response (200):**
```typescript
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "admin@varlor.com",
    "role": "admin",
    "tenantId": "default"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900 // 15 minutes in seconds
}

// Refresh token set in httpOnly cookie:
Set-Cookie: refreshToken=<token>; HttpOnly; Secure; SameSite=Lax; Path=/auth/refresh; Max-Age=604800
```

**Error Responses:**

*Invalid Credentials (401):*
```typescript
{
  "statusCode": 401,
  "message": "Invalid email or password",
  "error": "Unauthorized"
}
```

*Rate Limit Exceeded (429):*
```typescript
{
  "statusCode": 429,
  "message": "Too many login attempts. Please try again in 15 minutes.",
  "error": "Too Many Requests",
  "retryAfter": 900 // seconds
}
```

*Validation Error (400):*
```typescript
{
  "statusCode": 400,
  "message": [
    "email must be a valid email address",
    "password must be at least 12 characters"
  ],
  "error": "Bad Request"
}
```

**Rate Limiting:**
- 5 failed attempts per email address within 15-minute window
- Lockout period: 15 minutes after 5th failed attempt
- Tracked in database (users.failed_login_attempts, users.locked_until)

**Security Notes:**
- Password validated server-side, never logged
- Generic error message prevents user enumeration
- Refresh token in httpOnly cookie prevents XSS
- Access token in response body for Authorization header

---

### POST /auth/refresh

**Description:** Exchange refresh token for new access token.

**Request:**
```typescript
POST /auth/refresh
Cookie: refreshToken=<refresh_token>
```

**Success Response (200):**
```typescript
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900
}

// New refresh token rotated in cookie:
Set-Cookie: refreshToken=<new_token>; HttpOnly; Secure; SameSite=Lax; Path=/auth/refresh; Max-Age=604800
```

**Error Responses:**

*Invalid/Expired Token (401):*
```typescript
{
  "statusCode": 401,
  "message": "Invalid or expired refresh token",
  "error": "Unauthorized"
}
```

**Security Notes:**
- Refresh token rotation: old token invalidated, new token issued
- Revoked tokens tracked in database with replaced_by_token reference
- Detection of token reuse indicates potential compromise

---

### POST /auth/logout

**Description:** Invalidate refresh token and clear session.

**Request:**
```typescript
POST /auth/logout
Authorization: Bearer <access_token>
Cookie: refreshToken=<refresh_token>
```

**Success Response (200):**
```typescript
{
  "message": "Logged out successfully"
}

// Cookie cleared:
Set-Cookie: refreshToken=; HttpOnly; Secure; SameSite=Lax; Path=/auth/refresh; Max-Age=0
```

**Error Responses:**

*Unauthorized (401):*
```typescript
{
  "statusCode": 401,
  "message": "Unauthorized",
  "error": "Unauthorized"
}
```

**Security Notes:**
- Refresh token marked as revoked in database
- Access token remains valid until expiration (stateless JWT)
- Client responsible for clearing access token from memory

---

### GET /users/me (Protected)

**Description:** Get current authenticated user profile.

**Request:**
```typescript
GET /users/me
Authorization: Bearer <access_token>
```

**Success Response (200):**
```typescript
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "admin@varlor.com",
  "role": "admin",
  "tenantId": "default",
  "createdAt": "2025-11-22T10:00:00.000Z",
  "lastLoginAt": "2025-11-22T16:30:00.000Z"
}
```

**Error Responses:**

*Unauthorized (401):*
```typescript
{
  "statusCode": 401,
  "message": "Unauthorized",
  "error": "Unauthorized"
}
```

## Security Considerations

### Password Security
- **Hashing:** bcrypt with cost factor 12 (adjustable via environment variable)
- **Validation:** Minimum 12 characters, mixed case, numbers, special characters
- **Storage:** Never store plaintext passwords, only hashes
- **Transmission:** Only accept passwords over HTTPS

### Token Security
- **Access Tokens:**
  - Short-lived (15 minutes) to minimize exposure window
  - Stateless JWT with HS256 algorithm
  - Include minimal claims: userId, email, role, tenantId
  - Stored in memory (not localStorage) to prevent XSS

- **Refresh Tokens:**
  - Long-lived (7 days) but revocable
  - Stored in httpOnly cookies to prevent XSS
  - Hashed before database storage
  - Rotation on use prevents replay attacks
  - Server-side revocation for logout/compromise

### Authentication Patterns
- **Rate Limiting:**
  - 5 failed login attempts per email
  - 15-minute lockout period
  - IP-based tracking as secondary measure

- **Session Management:**
  - Automatic token refresh before expiration
  - Graceful handling of expired sessions
  - Server-side refresh token revocation

- **Error Messages:**
  - Generic "Invalid email or password" prevents user enumeration
  - Detailed logging server-side for debugging
  - No stack traces in production responses

### HTTPS & Cookies
- **Production Requirements:**
  - Secure flag on all cookies (HTTPS only)
  - SameSite=Lax for CSRF protection
  - httpOnly to prevent JavaScript access
  - Strict transport security headers

### Database Security
- **Connection:**
  - SSL/TLS for database connections
  - Connection pooling with max limits
  - Credentials in environment variables (never committed)

- **Queries:**
  - Parameterized queries via TypeORM (prevents SQL injection)
  - Input validation before database operations
  - Principle of least privilege for database user

### Future Migration to Keycloak
- **Token Compatibility:**
  - JWT claims structure follows OIDC standards
  - Role claim format compatible with Keycloak
  - Token expiration patterns align with Keycloak defaults

- **User Data:**
  - User schema compatible with Keycloak user federation
  - Password hashes use bcrypt (Keycloak-compatible)
  - Unique email constraint for SSO transition

## Implementation Guidance

### Phase 1: Database & Backend Setup (Week 1)

**1.1 Project Initialization**
- Create /server directory at project root
- Initialize NestJS project: `nest new server`
- Install dependencies: TypeORM, PostgreSQL driver, Passport, JWT, bcrypt
- Configure TypeScript, ESLint, Prettier

**1.2 Database Configuration**
- Set up PostgreSQL locally (Docker recommended)
- Create database: `varlor_dev`
- Configure TypeORM in DatabaseModule
- Create environment variable configuration (.env)

**1.3 Entity Creation**
- Create User entity with all fields
- Create RefreshToken entity
- Create Role entity (prepared for future)
- Set up entity relationships

**1.4 Migration Creation**
- Generate migrations from entities
- Review and adjust migration SQL
- Test migrations: up and down
- Document migration order

**1.5 Seed Script**
- Create admin seeding script in /scripts
- Read ADMIN_EMAIL and ADMIN_PASSWORD from env
- Validate password complexity
- Hash password with bcrypt
- Insert admin user with role "admin"
- Test seeding in development

### Phase 2: Authentication Module (Week 1-2)

**2.1 Auth Module Setup**
- Create AuthModule with imports/exports
- Create AuthController with placeholder routes
- Create AuthService with placeholder methods
- Set up dependency injection

**2.2 JWT Strategy**
- Configure @nestjs/jwt module
- Create JwtStrategy for Passport
- Set up JWT configuration (secret, expiration)
- Create JWT payload interface

**2.3 Login Implementation**
- Create LoginDto with validation decorators
- Implement AuthService.validateUser (email/password check)
- Implement AuthService.login (token generation)
- Add rate limiting logic (track attempts in DB)
- Implement AuthController.login endpoint
- Add error handling and logging

**2.4 Token Management**
- Create TokenService for refresh token handling
- Implement refresh token generation
- Implement refresh token validation
- Implement token rotation logic
- Add cleanup job for expired tokens (optional cron)

**2.5 Refresh & Logout**
- Implement AuthController.refresh endpoint
- Implement token rotation on refresh
- Implement AuthController.logout endpoint
- Add refresh token revocation logic

**2.6 Guards & Decorators**
- Create JwtAuthGuard extending Passport AuthGuard
- Create @Public() decorator for public routes
- Create @CurrentUser() decorator for user extraction
- Apply guards globally with @Public exceptions

### Phase 3: Frontend Setup (Week 2)

**3.1 Project Structure**
- Organize directory structure as specified
- Set up path aliases in tsconfig.json
- Configure environment variables (.env.local)

**3.2 Design System**
- Install Shadcn UI components (button, input, label, form)
- Create globals.css with CSS variables
- Define color palette, typography, spacing
- Create utility functions (cn, etc.)

**3.3 Base Components**
- Customize Shadcn components for Varlor branding
- Create form field wrapper component
- Create error message component
- Test component variants

**3.4 API Client Setup**
- Configure Axios or fetch client
- Create API base URL configuration
- Set up request/response interceptors
- Add token attachment logic
- Add error handling interceptor

**3.5 TanStack Query Setup**
- Configure QueryClientProvider in root layout
- Create query client with default options
- Set up devtools (development only)

**3.6 Zustand Store**
- Create auth store with state and actions
- Implement setUser, clearUser actions
- Add token management logic
- Create selectors for derived state

### Phase 4: Login Flow (Week 2-3)

**4.1 Login Form Component**
- Create LoginForm component with React Hook Form
- Create Zod validation schema
- Wire up form submission
- Add loading state
- Add error display

**4.2 Login Page**
- Create /app/(auth)/login/page.tsx
- Use AuthLayout wrapper
- Integrate LoginForm component
- Add redirect logic after successful login

**4.3 Auth Layout**
- Create AuthLayout component
- Add Varlor branding/logo
- Create responsive centered card design
- Add background styling

**4.4 API Integration**
- Create login mutation with TanStack Query
- Handle success: store user in Zustand, redirect to dashboard
- Handle errors: display error message, clear password field
- Implement rate limit error handling

**4.5 Token Management**
- Implement access token storage (memory/state)
- Implement automatic refresh before expiration
- Create token refresh mutation
- Add token refresh interval logic

### Phase 5: Protected Routes (Week 3)

**5.1 Dashboard Layout**
- Create DashboardLayout component
- Add header with user menu
- Add logout button
- Create basic navigation structure

**5.2 Dashboard Page**
- Create /app/(dashboard)/dashboard/page.tsx
- Add placeholder content
- Verify authentication required

**5.3 Auth Middleware**
- Create middleware.ts in app directory
- Check authentication status
- Redirect to /login if unauthenticated
- Redirect to /dashboard if authenticated user visits /login

**5.4 useAuth Hook**
- Create useAuth hook for accessing auth state
- Expose user, isAuthenticated, login, logout
- Add loading and error states

**5.5 Logout Implementation**
- Create logout mutation
- Clear Zustand store on logout
- Clear tokens
- Redirect to /login
- Handle errors gracefully

### Phase 6: Testing & Polish (Week 3-4)

**6.1 Backend Testing**
- Write unit tests for AuthService
- Write unit tests for UsersService
- Write integration tests for auth endpoints
- Test rate limiting behavior
- Test token refresh and rotation

**6.2 Frontend Testing**
- Write component tests for LoginForm
- Test form validation
- Test API integration (mocked)
- Test auth store behavior

**6.3 E2E Testing**
- Write E2E test for login flow
- Test protected route access
- Test logout flow
- Test token refresh behavior

**6.4 Error Handling**
- Review all error scenarios
- Ensure user-friendly error messages
- Add error logging
- Test network failure scenarios

**6.5 Accessibility**
- Audit form accessibility (ARIA labels, keyboard navigation)
- Test with screen reader
- Ensure proper focus management
- Add skip links if needed

**6.6 Performance**
- Optimize bundle size
- Implement code splitting
- Add loading skeletons
- Test on slow networks

### Environment Variables

**Backend (.env):**
```
# Database
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=varlor_dev
DATABASE_USER=varlor_user
DATABASE_PASSWORD=<secure_password>
DATABASE_SSL=false

# JWT
JWT_SECRET=<generate_secure_random_secret>
JWT_ACCESS_TOKEN_EXPIRATION=15m
JWT_REFRESH_TOKEN_EXPIRATION=7d

# Admin Seed
ADMIN_EMAIL=admin@varlor.com
ADMIN_PASSWORD=<SecureAdminPassword123!>

# Security
BCRYPT_ROUNDS=12
RATE_LIMIT_MAX_ATTEMPTS=5
RATE_LIMIT_WINDOW_MINUTES=15

# Application
NODE_ENV=development
PORT=3001
CORS_ORIGIN=http://localhost:3000
```

**Frontend (.env.local):**
```
NEXT_PUBLIC_API_URL=http://localhost:3001
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

## Testing Requirements

### Backend Unit Tests

**AuthService Tests:**
- Login with valid credentials returns tokens
- Login with invalid password returns error
- Login with non-existent email returns error
- Rate limiting blocks after 5 failed attempts
- Account lockout expires after 15 minutes
- Successful login resets failed attempt counter

**TokenService Tests:**
- Generate refresh token creates database entry
- Validate refresh token succeeds with valid token
- Validate refresh token fails with expired token
- Token rotation invalidates old token
- Token revocation marks token as revoked

**UsersService Tests:**
- Create user hashes password correctly
- Find user by email returns correct user
- Find user by email returns null for non-existent user
- Password validation succeeds with correct password
- Password validation fails with incorrect password

### Backend Integration Tests

**POST /auth/login:**
- Returns 200 with valid credentials
- Returns 401 with invalid credentials
- Returns 429 after 5 failed attempts
- Returns 400 with invalid email format
- Returns 400 with weak password
- Sets httpOnly cookie with refresh token

**POST /auth/refresh:**
- Returns 200 with valid refresh token
- Returns 401 with invalid refresh token
- Returns 401 with expired refresh token
- Rotates refresh token on success
- Invalidates old refresh token

**POST /auth/logout:**
- Returns 200 when authenticated
- Returns 401 when not authenticated
- Revokes refresh token in database
- Clears refresh token cookie

### Frontend Unit Tests

**LoginForm Component:**
- Renders form fields correctly
- Validates email format
- Validates password length
- Displays validation errors
- Submits form with valid data
- Shows loading state during submission
- Displays API error messages

**useAuth Hook:**
- Returns null user when not authenticated
- Returns user object when authenticated
- Login action updates state
- Logout action clears state
- Token refresh updates access token

**Auth Store:**
- setUser action updates user state
- clearUser action resets state
- isAuthenticated selector returns correct value

### E2E Tests

**Login Flow:**
1. Navigate to /login
2. Submit form with invalid credentials
3. Verify error message displayed
4. Submit form with valid credentials
5. Verify redirect to /dashboard
6. Verify user data displayed in header

**Protected Route Access:**
1. Navigate to /dashboard without authentication
2. Verify redirect to /login
3. Login with valid credentials
4. Verify redirect to /dashboard
5. Verify dashboard content loads

**Logout Flow:**
1. Login successfully
2. Navigate to /dashboard
3. Click logout button
4. Verify redirect to /login
5. Attempt to navigate to /dashboard
6. Verify redirect to /login

**Token Refresh:**
1. Login successfully
2. Wait for access token to approach expiration
3. Verify automatic token refresh
4. Verify continued access to protected routes

### Performance Tests

- Login endpoint responds within 500ms (p95)
- Dashboard page loads within 2s on 3G
- Token refresh occurs seamlessly without UI disruption
- Form validation responds immediately (< 100ms)

### Security Tests

- SQL injection attempts blocked by parameterized queries
- XSS attempts sanitized in error messages
- CSRF protection via SameSite cookies
- Rate limiting enforced correctly
- Token reuse detected and blocked
- Expired tokens rejected

## Future Considerations

### Alpha Phase (Post-MVP)

**Password Management:**
- Forgot password flow with email verification
- Password reset with time-limited tokens
- Password change in user profile
- Password history tracking (prevent reuse)

**Enhanced Roles:**
- Add "analyst" role with dataset access
- Add "viewer" role with read-only access
- Implement role-based UI rendering
- Create roles management interface for admins

**User Management:**
- Admin interface to create/edit/delete users
- User invitation flow with email
- User profile management
- Account activation/deactivation

**Audit Logging:**
- UI for viewing authentication logs
- Login history per user
- Failed login attempt tracking
- Session management view

### Beta Phase

**Keycloak Migration:**
- Replace custom auth with Keycloak OIDC
- Migrate user data to Keycloak
- Configure Keycloak realms and clients
- Implement social login (Google, Azure AD)
- Add SAML support for enterprise customers

**Multi-Factor Authentication:**
- TOTP (Google Authenticator, Authy)
- SMS verification
- Email verification
- Backup codes

**Multi-Tenancy:**
- Tenant creation and management
- Tenant-specific branding
- Tenant isolation enforcement
- Cross-tenant user access (admin only)

**Advanced Security:**
- IP whitelisting per tenant
- Session management (force logout)
- Device tracking and management
- Anomaly detection (unusual login patterns)
- Password expiration policies

### Production Phase

**Scalability:**
- Horizontal scaling of auth service
- Redis for session caching
- Database read replicas
- CDN for static assets

**Monitoring:**
- Authentication metrics dashboard
- Failed login alerting
- Token refresh rate monitoring
- Rate limit effectiveness tracking

**Compliance:**
- GDPR user data export
- Account deletion workflow
- Consent management
- Audit trail for compliance

## Out of Scope

### Explicitly Excluded from MVP
- Social login (Google, Apple, Microsoft, etc.)
- Email verification for new accounts
- Password reset flow
- Account lockout policies beyond rate limiting
- User profile management UI
- Keycloak integration
- SAML/OAuth2 flows
- Password expiration policies
- Audit logs UI (server-side logging exists, no UI)
- Multiple roles beyond "admin"
- "Remember me" functionality
- Public user registration
- User invitation system
- Account suspension/activation
- Two-factor authentication
- Session management UI
- Device management
- IP whitelisting
- Tenant management

### Deferred to Future Phases
- All items listed in "Future Considerations" section
- Advanced security features (anomaly detection, device tracking)
- Compliance features (GDPR export, data retention)
- Performance optimizations (Redis caching, read replicas)
- Monitoring dashboards and alerting

## Success Criteria

### Functional Success
- Admin can log in with email and password
- Admin can log out and session is terminated
- Admin can access protected dashboard after login
- Unauthenticated users redirected to login page
- Failed login attempts are rate-limited correctly
- Access tokens refresh automatically before expiration
- Generic error messages prevent user enumeration

### Technical Success
- All API endpoints respond within acceptable latency (< 500ms p95)
- Database migrations run successfully
- Admin seed script creates account correctly
- All unit tests pass with > 80% coverage
- All integration tests pass
- All E2E tests pass
- No security vulnerabilities in dependencies (npm audit)

### Security Success
- Passwords hashed with bcrypt cost factor 12
- JWT tokens use secure algorithm (HS256 minimum)
- Refresh tokens stored in httpOnly cookies
- Rate limiting blocks brute force attempts
- Token rotation prevents replay attacks
- SQL injection prevented by parameterized queries
- XSS prevented by input sanitization and httpOnly cookies

### User Experience Success
- Login form responsive on mobile and desktop
- Form validation provides clear, immediate feedback
- Loading states indicate processing
- Error messages are clear and actionable
- Dashboard loads within 2 seconds on 3G
- Keyboard navigation works throughout
- Screen reader accessibility verified

### Documentation Success
- API endpoints documented with request/response examples
- Environment variables documented with examples
- Database schema documented with relationships
- Setup instructions complete and tested
- Deployment guide created
- Security considerations documented

---

**Note:** This specification establishes the foundational patterns for all future Varlor development. All architectural decisions, coding conventions, component patterns, and API designs defined here should be followed consistently in subsequent features.