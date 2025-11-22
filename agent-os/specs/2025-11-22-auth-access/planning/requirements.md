# Spec Requirements: Auth + Access (MVP)

## Initial Description
Auth + Access Feature for Varlor MVP

### A. Auth + Access
- Login page with:
  - Email + password
  - Basic session management (login / logout)
- Admin account creation page (manual at first, not public)
- No advanced multi-tenant yet → 1 environment = 1 client (or multiple clients managed "manually")

## Requirements Discussion

### First Round Questions

**Q1: Forgot Password Functionality**
Should we include "forgot password" or password reset functionality in the MVP?
**Answer:** Out of scope for MVP. Add in Alpha phase.

**Q2: Session Management**
I assume we'll use JWT tokens for session management. Should the tokens be stored in httpOnly cookies for security? Also, will we use refresh tokens, and should these be stored server-side (e.g., in PostgreSQL)?
**Answer:**
- Use JWT tokens with httpOnly cookies
- NestJS handles token generation/validation (Passport.js + JWT strategy)
- Refresh tokens stored in PostgreSQL
- Migration note: Token format will be compatible with Keycloak for easy transition

**Q3: Admin Account Creation**
For the admin account creation page, I assume this will be a manual process (database seed script or environment variable) rather than a public signup form. Is that correct?
**Answer:**
- Database seed script (npm run seed:admin)
- Environment variable for initial admin credentials
- Format: ADMIN_EMAIL=admin@varlor.com ADMIN_PASSWORD=<secure-password>

**Q4: Password Requirements**
What password complexity requirements should we enforce? I'm thinking minimum 12 characters with at least one uppercase, lowercase, number, and special character. Should we also implement password expiration policies?
**Answer:**
- Minimum 12 characters
- At least one uppercase, lowercase, number, special character
- No expiration for MVP
- Hashed with bcrypt (cost factor 12)

**Q5: Multi-Factor Authentication (MFA)**
Should we plan for MFA (TOTP/SMS) in the MVP, or defer this to a later phase when Keycloak is integrated?
**Answer:** Defer to Alpha/Beta when Keycloak is integrated

**Q6: User Roles**
For the basic access control, should we start with just two roles (admin/user), or do you need more granular roles like admin, analyst, and viewer from the start?
**Answer:**
- Start with just "admin" role for MVP
- Database schema prepared for roles (for easy extension)
- Add "analyst" and "viewer" in Alpha phase

**Q7: Session Timeout**
What should the session timeout be? I'm thinking 15 minutes of inactivity for access tokens, with a 7-day refresh token. Should we include a "remember me" option?
**Answer:**
- Access token: 15 minutes
- Refresh token: 7 days
- Automatic refresh via refresh token
- No "remember me" for MVP

**Q8: Login Error Messages**
For security, should we use generic error messages like "Invalid email or password" instead of specific messages that could reveal whether an email exists in the system?
**Answer:**
- Generic "Invalid email or password" for security
- Rate limiting (5 failed attempts = 15 min lockout)
- Log detailed errors server-side for debugging

**Q9: Logout Behavior**
After logout, should we redirect to the login page? Should we invalidate the refresh token on the server side?
**Answer:**
- Redirect to /login page
- Invalidate refresh token in database
- Clear cookies client-side

**Q10: Environment Setup**
For the "1 environment = 1 client" approach, should we set up a single PostgreSQL database with a simple tenant_id column (always set to a default value for MVP), or keep it simpler with no tenant concept at all?
**Answer:**
- Single PostgreSQL database
- Basic tenant_id column in users table (always "default" for MVP)
- Manual configuration per deployment
- Ready for multi-tenant in Alpha

**Q11: Out of Scope Items**
Are there any specific features you want to explicitly exclude from the MVP (e.g., social login, email verification, password history, account lockout policies)?
**Answer:** Explicitly OUT of Scope:
- Social login (Google/Apple/etc)
- Email verification
- Password reset flow
- Account lockout after repeated failures (just rate limiting)
- User profile management UI
- Keycloak integration
- SAML/OAuth2 flows
- Password expiration policies
- Audit logs UI (logs exist, no UI)

### Existing Code to Reference

**Similar Features Identified:**
No similar existing features identified for reference. This is the first authentication implementation for Varlor MVP.

### Follow-up Questions

**Follow-up 1: UI Component Patterns**
Since this is the first authentication feature and we're building with Next.js on the frontend and NestJS on the backend, are there any existing UI components (forms, buttons, layouts) in the codebase we should reference for consistent styling, or should we start fresh using Shadcn UI components?

**Answer:** We're starting fresh!
We'll establish the foundational UI patterns using:
- Shadcn UI components (as specified in your stack)
- Design system that we'll create now and reuse throughout Varlor
- Consistent theming with CSS variables for colors, spacing, typography

Creating:
- Base layout components (AuthLayout, DashboardLayout)
- Form components (Input, Button, Label with validation states)
- Reusable patterns that all future features will follow

**Follow-up 2: Backend Service Patterns**
You mentioned NestJS will handle token generation/validation with Passport.js + JWT strategy. Do you have any existing NestJS services, controllers, or middleware patterns we should follow for consistency, or is this the first backend service being built?

**Answer:** This is the first backend service!
We'll establish the foundational NestJS patterns:
- Module structure: Feature-based modules (auth, users, datasets later)
- Service layer: Business logic isolation
- Controller layer: HTTP handling with proper DTOs
- Repository pattern: Database access via TypeORM
- Decorator patterns: Custom decorators for auth (@CurrentUser, @Public)
- Exception filters: Consistent error handling
- Validation pipes: Request validation with class-validator

## Visual Assets

### Files Provided:
No visual assets provided.

### Visual Insights:
No visual assets available for analysis. Implementation will follow standard authentication UI patterns:
- Clean, minimal login form
- Clear error states
- Responsive design
- Accessibility best practices

## Requirements Summary

### Functional Requirements

**Authentication:**
- Email + password login form
- JWT-based session management with httpOnly cookies
- Access tokens (15 min lifetime) + refresh tokens (7 day lifetime)
- Automatic token refresh mechanism
- Logout functionality with server-side token invalidation
- Generic error messages for security
- Rate limiting: 5 failed attempts = 15 min lockout

**Admin Account Management:**
- Database seed script for initial admin creation
- Environment variable configuration for admin credentials
- Format: ADMIN_EMAIL and ADMIN_PASSWORD env vars

**Password Security:**
- Minimum 12 characters
- Required: uppercase, lowercase, number, special character
- Bcrypt hashing with cost factor 12
- No password expiration in MVP

**User Roles:**
- Single "admin" role for MVP
- Database schema includes roles table for future extension
- Prepared for "analyst" and "viewer" roles in Alpha

**Session Management:**
- NestJS backend with Passport.js + JWT strategy
- Refresh tokens stored in PostgreSQL
- Token format compatible with future Keycloak migration
- Redirect to /login after logout

**Environment Setup:**
- Single PostgreSQL database
- Users table with tenant_id column (set to "default" for MVP)
- Manual configuration per deployment
- Foundation for multi-tenant architecture in Alpha

### Architectural Foundations

**Frontend Architecture (First Implementation):**
This feature establishes the foundational UI patterns for all future Varlor features:

- **Component Library:** Shadcn UI as the base component system
- **Design System:**
  - CSS variables for theming (colors, spacing, typography)
  - Consistent visual language across all features
  - Reusable component patterns

- **Layout Components:**
  - AuthLayout: Container for authentication pages (login, future signup)
  - DashboardLayout: Main application layout (for authenticated pages)

- **Form Components:**
  - Input component with validation states
  - Button component with loading states
  - Label component with error handling
  - Form wrapper with React Hook Form integration

- **Patterns to Establish:**
  - Form validation patterns with Zod schemas
  - Error display conventions
  - Loading state handling
  - Responsive breakpoints
  - Accessibility standards (ARIA labels, keyboard navigation)

**Backend Architecture (First Implementation):**
This feature establishes the foundational NestJS patterns for all future backend services:

- **Module Structure:**
  - Feature-based modules (auth module is first, followed by users, datasets)
  - Clear module boundaries with defined exports/imports
  - Shared module for cross-cutting concerns

- **Service Layer:**
  - Business logic isolation in service classes
  - Dependency injection for testability
  - Clear service responsibilities (AuthService, UserService, TokenService)

- **Controller Layer:**
  - HTTP request handling with proper status codes
  - DTO (Data Transfer Objects) for request/response validation
  - RESTful endpoint design conventions
  - Swagger/OpenAPI documentation

- **Repository Pattern:**
  - Database access via TypeORM repositories
  - Entity definitions with proper relationships
  - Migration files for schema versioning

- **Decorator Patterns:**
  - @CurrentUser decorator for extracting authenticated user
  - @Public decorator for marking public endpoints
  - Custom validation decorators for common patterns

- **Exception Filters:**
  - Global exception filter for consistent error responses
  - Custom exception classes (UnauthorizedException, ValidationException)
  - Error logging and monitoring integration

- **Validation Pipes:**
  - Global validation pipe using class-validator
  - DTO validation for all incoming requests
  - Transform pipes for data normalization

- **Middleware Patterns:**
  - JWT authentication guard
  - Rate limiting middleware
  - Request logging middleware
  - CORS configuration

### Reusability Opportunities

This is the first authentication implementation and will establish foundational patterns for:

**Frontend Patterns:**
- Form components (React Hook Form + Zod validation)
- Layout structures (AuthLayout, DashboardLayout)
- Shadcn UI component integration
- Error handling UI patterns
- Loading states and feedback
- Responsive design system
- Accessibility patterns

**Backend Patterns:**
- NestJS module organization
- Service and controller structure
- TypeORM entity and repository patterns
- DTO validation patterns
- Custom decorators (@CurrentUser, @Public)
- Exception filters and error handling
- JWT authentication guards
- Middleware structure

**Cross-Cutting Patterns:**
- API client configuration (TanStack Query)
- Environment variable management
- Database migration workflow
- Testing patterns (unit and integration)
- CI/CD pipeline structure

### Scope Boundaries

**In Scope:**
- Login page with email/password form
- JWT token generation and validation
- Refresh token mechanism
- Admin account seeding via environment variables
- Password complexity validation
- Bcrypt password hashing
- Basic rate limiting
- Session timeout and logout
- Generic error messages
- Single "admin" role
- Database schema with tenant_id for future multi-tenancy
- Foundational UI component library setup
- Foundational NestJS backend architecture setup
- Design system with CSS variables
- Base layout components (AuthLayout, DashboardLayout)
- Form component patterns
- NestJS module/service/controller patterns
- Custom decorators and middleware
- Exception filters and validation pipes

**Out of Scope:**
- Forgot password / password reset flow (Alpha phase)
- Multi-factor authentication (Alpha/Beta phase)
- Social login (Google, Apple, etc)
- Email verification
- Account lockout policies beyond rate limiting
- User profile management UI
- Keycloak integration (Alpha/Beta phase)
- SAML/OAuth2 flows
- Password expiration policies
- Audit logs UI (server-side logging only)
- Multiple roles beyond "admin" (Alpha phase)
- "Remember me" functionality
- Public user registration

### Technical Considerations

**Frontend Stack:**
- Next.js (App Router)
- React 18+ with TypeScript
- Shadcn UI components for forms and UI elements
- React Hook Form with Zod validation
- TanStack Query for API calls and caching
- Zustand for client-side state management

**Backend Stack:**
- NestJS framework
- Passport.js for authentication
- JWT strategy for token handling
- PostgreSQL for user and refresh token storage
- TypeORM for database access
- Bcrypt for password hashing (cost factor 12)
- class-validator for DTO validation

**API Design:**
- REST endpoints for authentication
- POST /auth/login (email, password → access token + refresh token)
- POST /auth/refresh (refresh token → new access token)
- POST /auth/logout (invalidate refresh token)
- httpOnly cookies for token storage

**Database Schema:**
- Users table: id, email, password_hash, role, tenant_id, created_at, updated_at
- Refresh_tokens table: id, user_id, token_hash, expires_at, created_at
- Roles table: id, name (prepared for future use)

**Security Patterns:**
- httpOnly cookies to prevent XSS attacks
- Bcrypt with cost factor 12 for password hashing
- Generic error messages to prevent user enumeration
- Rate limiting to prevent brute force attacks
- Server-side token invalidation on logout
- JWT expiration and refresh token rotation

**Future Migration Path:**
- Token format designed for Keycloak compatibility
- Database schema includes tenant_id for multi-tenancy
- Roles table prepared for expansion
- Clean separation of concerns for easier Keycloak integration
