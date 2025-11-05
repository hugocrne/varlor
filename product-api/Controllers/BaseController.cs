using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using product_api.Data;
using product_api.Exceptions;
using System.Net;

namespace product_api.Controllers;

[ApiController]
[Route("api/[controller]")]
public abstract class BaseController : ControllerBase
{
    protected readonly VarlorDbContext _context;
    protected readonly ILogger<BaseController> _logger;

    protected BaseController(VarlorDbContext context, ILogger<BaseController> logger)
    {
        _context = context;
        _logger = logger;
    }

    /// <summary>
    /// Handles pagination for queries with skip and take parameters
    /// </summary>
    public static int[] GetPaginationParameters(int? skip, int? take)
    {
        // Default pagination parameters
        var skipValue = Math.Max(0, skip ?? 0);
        var takeValue = Math.Min(100, Math.Max(1, take ?? 10)); // Limit max page size to 100

        return new[] { skipValue, takeValue };
    }

    /// <summary>
    /// Handles not found scenarios with consistent error response
    /// </summary>
    protected virtual IActionResult NotFound(string resource, Guid id)
    {
        throw new NotFoundException($"{resource} with ID '{id}' not found.");
    }

    /// <summary>
    /// Handles validation errors with consistent error response
    /// </summary>
    protected virtual IActionResult ValidationError(string message)
    {
        throw new ValidationException(message);
    }

    /// <summary>
    /// Handles database errors with consistent error response
    /// </summary>
    protected virtual IActionResult DatabaseError(Exception ex)
    {
        throw new DatabaseException("Database operation failed", ex);
    }

    /// <summary>
    /// Handles duplicate resource errors with consistent error response
    /// </summary>
    protected virtual IActionResult DuplicateError(string message)
    {
        throw new DuplicateResourceException(message);
    }

    /// <summary>
    /// Generic async operation handler with error handling
    /// </summary>
    protected async Task<IActionResult> ExecuteAsync<T>(Func<Task<T>> operation, string successMessage = "")
    {
        try
        {
            var result = await operation();
            return Ok(result);
        }
        catch (NotFoundException)
        {
            throw; // Re-throw to be handled by middleware
        }
        catch (ValidationException)
        {
            throw; // Re-throw to be handled by middleware
        }
        catch (DuplicateResourceException)
        {
            throw; // Re-throw to be handled by middleware
        }
        catch (DbUpdateException ex)
        {
            _logger.LogError(ex, "Database update error occurred");
            throw new DatabaseException("Failed to update database", ex);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unexpected error occurred");
            throw;
        }
    }

    /// <summary>
    /// Generic entity finder with not found handling
    /// </summary>
    protected async Task<T?> FindEntityAsync<T>(Guid id, CancellationToken cancellationToken = default) where T : class
    {
        var entity = await _context.FindAsync<T>([id], cancellationToken);
        if (entity == null)
        {
            throw new NotFoundException($"{typeof(T).Name} with ID '{id}' not found.");
        }
        return entity;
    }

    /// <summary>
    /// Validates model state and throws validation exception if invalid
    /// </summary>
    protected void ValidateModelState()
    {
        if (!ModelState.IsValid)
        {
            var errors = ModelState.Values
                .SelectMany(v => v.Errors)
                .Select(e => e.ErrorMessage)
                .ToList();

            throw new ValidationException($"Validation failed: {string.Join(", ", errors)}");
        }
    }

    /// <summary>
    /// Returns 201 Created response with location header
    /// </summary>
    protected virtual IActionResult CreatedResponse<T>(string controllerName, Guid id, T response)
    {
        return CreatedAtAction(
            actionName: nameof(GetById),
            controllerName: controllerName,
            routeValues: new { id },
            value: response);
    }

    /// <summary>
    /// Abstract method for getting entity by ID - must be implemented by derived controllers
    /// </summary>
    [HttpGet("{id}")]
    public abstract Task<IActionResult> GetById(Guid id);

    /// <summary>
    /// Saves changes to database with error handling
    /// </summary>
    protected async Task SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        try
        {
            await _context.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateConcurrencyException ex)
        {
            _logger.LogError(ex, "Concurrency error occurred during save");
            throw new DatabaseException("A concurrency error occurred. The record has been modified by another user.", ex);
        }
        catch (DbUpdateException ex)
        {
            _logger.LogError(ex, "Database update error occurred during save");
            throw new DatabaseException("Failed to save changes to database", ex);
        }
    }
}