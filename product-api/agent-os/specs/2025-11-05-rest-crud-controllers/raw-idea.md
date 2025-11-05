# REST CRUD Controllers Implementation

**Requirements:**
1. Generate REST controllers in `/Controllers` for each existing model (`Client`, `User`, `UserPreference`, `UserSession`)
2. Follow a clean pattern:
   - `[ApiController]` + `[Route("api/[controller]")]`
   - Inject the DbContext via the constructor
   - Methods: **GET all**, **GET by id**, **POST**, **PATCH**, **DELETE**
   - Proper handling of HTTP status codes (200, 201, 400, 404, 500)
   - Use of **async/await** (`ToListAsync`, `FindAsync`, etc.)
3. Do not mention table creation or modification — focus solely on API logic
4. Expected result: Fully implemented CRUD controllers connected to the existing DbContext, compliant with Microsoft and EF Core best practices

**Context:**
- Project already has Entity Framework Core configured with PostgreSQL
- VarlorDbContext with 4 entities already implemented
- Models: Client, User, UserPreference, UserSession with proper relationships
- Project structure follows .NET 8 Web API patterns
- Existing test project structure available