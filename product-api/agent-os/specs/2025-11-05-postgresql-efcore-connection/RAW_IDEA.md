# PostgreSQL EF Core Connection for Varlor Project

Initialize a new spec for connecting the Varlor .NET project to PostgreSQL using Entity Framework Core.

Context: The project is called Varlor and needs to properly link to an existing PostgreSQL database (accessible via psql -U hugo -d varlor) using Entity Framework Core 9 with Npgsql provider. The objective is to establish a clean, idiomatic, and best-practice ORM connection without modifying the existing database schema.

Tasks to accomplish:
1. Check official documentation for current best practices (.NET 9 / EF Core 9 / Npgsql)
2. Install and configure Npgsql.EntityFrameworkCore.PostgreSQL provider
3. Add proper connection string in appsettings.json
4. Register DbContext in Program.cs using AddDbContext
5. Organize project with /Data for DbContext and /Models for entities
6. Provide minimal, clean, compilable example following official documentation

Constraints: .NET 9, EF Core 9, Npgsql provider, no database schema modifications allowed.