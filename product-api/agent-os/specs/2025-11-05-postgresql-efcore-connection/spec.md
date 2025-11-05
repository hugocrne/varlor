# Specification: PostgreSQL EF Core Connection

## Goal
Establish a clean, idiomatic Entity Framework Core 9 connection between the Varlor .NET 9 Web API and the existing PostgreSQL database using the Npgsql provider, following modern .NET conventions without modifying the existing database schema.

## User Stories
- As a developer, I want to connect the .NET application to the existing PostgreSQL database using EF Core so that I can perform database operations using strongly-typed entities
- As a developer, I want proper configuration management through appsettings.json so that connection strings are environment-specific and secure
- As a developer, I want organized project structure with dedicated Data and Models folders so that the codebase follows .NET best practices
- As a developer, I want to leverage the already-installed Npgsql.EntityFrameworkCore.PostgreSQL package so that no additional dependencies are needed

## Core Requirements
- Connect to existing PostgreSQL database named "varlor" using Npgsql provider
- Configure DbContext with proper connection string management
- Map existing database schema (clients, users, user_preferences, user_sessions tables) to EF Core entities
- Register DbContext in Program.cs using modern .NET 9 dependency injection patterns
- Organize project with /Data for DbContext and /Models for entity classes
- Use the already installed Npgsql.EntityFrameworkCore.PostgreSQL v9.0.4 package
- Provide minimal, clean, compilable example code
- No database schema modifications allowed - only read connection and mapping

## Visual Design
No visual assets provided for this technical infrastructure specification.

## Reusable Components

### Existing Code to Leverage
- **Project Structure**: Standard .NET 9 Web API with Program.cs and appsettings.json
- **Package Dependencies**: Npgsql.EntityFrameworkCore.PostgreSQL v9.0.4 already installed
- **Configuration System**: Built-in .NET configuration with appsettings.json and appsettings.Development.json
- **Dependency Injection**: .NET 9 service container in Program.cs
- **Database Schema**: Well-documented existing schema in DATABASE.md with clients, users, user_preferences, user_sessions tables

### New Components Required
- **DbContext Class**: Custom VarlorDbContext inheriting from DbContext to manage database operations
- **Entity Models**: C# entity classes for each table (Client, User, UserPreference, UserSession) with proper mappings
- **Data Folder**: New project folder to contain the DbContext class
- **Models Folder**: New project folder to contain all entity classes
- **Connection String Configuration**: PostgreSQL connection string in appsettings.json

## Technical Approach
- Create Data/VarlorDbContext.cs with DbSet properties for each table and OnConfiguring method for PostgreSQL connection
- Map entities to existing database schema using Fluent API in OnModelCreating to avoid EF Core conventions that conflict with existing schema
- Configure connection string in appsettings.json with proper PostgreSQL format
- Register DbContext in Program.cs using AddDbContext<VarlorDbContext>() with configuration binding
- Use attributes and Fluent API for precise column mapping, especially for UUID primary keys, ENUM types, and timestamp columns
- Implement proper disposal patterns and async/await for database operations

## Out of Scope
- Database schema creation or modifications
- Database migrations (schema already exists)
- Authentication/authorization implementation
- Repository pattern or additional abstraction layers
- API endpoints or controller implementations
- Database seeding or initial data setup
- Connection pooling configuration beyond defaults
- Advanced EF Core features like value objects, complex types, or table splitting

## Success Criteria
- Application successfully connects to PostgreSQL database without errors
- Entity Framework Core can read from all existing tables (clients, users, user_preferences, user_sessions)
- DbContext is properly registered in dependency injection container
- Configuration is environment-aware (Development/Production)
- Code compiles without warnings or errors
- Project follows .NET 9 best practices and conventions
- Solution is minimal and focused only on connection configuration