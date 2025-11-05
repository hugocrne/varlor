# Spec Requirements: REST CRUD Controllers Implementation

## Initial Description
Generate REST controllers in `/Controllers` for each existing model (`Client`, `User`, `UserPreference`, `UserSession`) following clean patterns with proper async/await, HTTP status codes, and Entity Framework Core best practices.

## Requirements Discussion

### First Round Questions

**Q1:** For the API models, should we expose entities directly or use DTOs to separate the API contract from domain entities?
**Answer:** Use DTOs to separate API models from domain entities

**Q2:** What validation strategy should we implement? Basic model validation, custom validation attributes, or FluentValidation?
**Answer:** Basic model validation + custom validation (unique email)

**Q3:** Should we implement any security/authorization? Open endpoints, role-based access control, or JWT authentication?
**Answer:** Open endpoints for now (no role-based access control initially)

**Q4:** For GET all endpoints, do you want pagination, filtering, or just return all records?
**Answer:** Simple pagination with skip/take parameters only

**Q5:** How should we handle errors? Simple exceptions, custom error responses, or centralized error handling middleware?
**Answer:** Centralized error handling middleware with structured error responses

**Q6:** For User creation (POST), should we hash passwords client-side or server-side?
**Answer:** Hash passwords server-side in the POST endpoint

**Q7:** For DELETE operations, should we implement soft delete (mark as deleted) or hard delete (remove from database)?
**Answer:** Soft delete for User/Client entities, hard delete for UserPreference and UserSession

**Q8:** Are there any specific business rules we should implement? For example, should creating a User automatically create associated UserPreference or UserSession records?
**Answer:** Auto-create UserPreference and UserSession when a User is created

### Existing Code to Reference

**Similar Features Identified:**
No existing controllers in the codebase - reference official Microsoft documentation for .NET 8 Web API best practices.

### Follow-up Questions
No follow-up questions were needed.

## Visual Assets

### Files Provided:
No visual assets provided.

### Visual Insights:
No visual files were found in the planning/visuals directory.

## Requirements Summary

### Functional Requirements
- Generate REST controllers for Client, User, UserPreference, and UserSession entities
- Implement full CRUD operations: GET all, GET by id, POST, PATCH, DELETE
- Use async/await patterns with Entity Framework Core
- Implement proper HTTP status codes (200, 201, 400, 404, 500)
- Use DTOs for API model separation
- Implement server-side password hashing for User creation
- Auto-create UserPreference and UserSession when creating Users
- Apply soft delete for User/Client entities
- Apply hard delete for UserPreference and UserSession entities

### Reusability Opportunities
- Follow Microsoft .NET 8 Web API documentation patterns
- Implement centralized error handling middleware for all controllers
- Create base controller class with common CRUD patterns
- Reuse validation patterns across DTOs

### Scope Boundaries

**In Scope:**
- REST API controllers for all 4 entities
- DTO classes for API contracts
- Basic validation (including unique email)
- Simple pagination (skip/take)
- Centralized error handling middleware
- Server-side password hashing
- Auto-creation of related entities

**Out of Scope:**
- Authentication and authorization (future enhancement)
- Complex filtering and sorting
- Advanced business logic beyond specified rules
- Database schema modifications
- UI components

### Technical Considerations
- Entity Framework Core with PostgreSQL already configured
- Existing VarlorDbContext with 4 entities
- .NET 8 Web API project structure
- Async/await patterns required
- HTTP status code compliance
- Centralized error handling architecture
- DTO pattern implementation