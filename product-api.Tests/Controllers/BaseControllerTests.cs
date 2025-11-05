using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Moq;
using product_api.Controllers;
using product_api.Data;
using product_api.Exceptions;
using product_api.Models;
using System;
using System.Threading;
using System.Threading.Tasks;
using Xunit;

namespace product_api.Tests.Controllers;

public class TestableBaseController : BaseController
{
    public TestableBaseController(VarlorDbContext context, ILogger<BaseController> logger)
        : base(context, logger)
    {
    }

    public override Task<IActionResult> GetById(Guid id)
    {
        return Task.FromResult<IActionResult>(Ok($"Entity {id}"));
    }

    // Expose protected methods for testing
    public static int[] ExposeGetPaginationParameters(int? skip, int? take)
        => BaseController.GetPaginationParameters(skip, take);

    public IActionResult ExposeNotFound(string resource, Guid id)
        => base.NotFound(resource, id);

    public IActionResult ExposeValidationError(string message)
        => base.ValidationError(message);

    public IActionResult ExposeDatabaseError(Exception ex)
        => base.DatabaseError(ex);

    public IActionResult ExposeDuplicateError(string message)
        => base.DuplicateError(message);

    public void ExposeValidateModelState()
        => base.ValidateModelState();

    public async Task<IActionResult> TestExecuteAsync<T>(Func<Task<T>> operation)
        => await base.ExecuteAsync(operation);

    public async Task<T?> TestFindEntityAsync<T>(Guid id, CancellationToken cancellationToken = default) where T : class
        => await base.FindEntityAsync<T>(id, cancellationToken);
}

public class BaseControllerTests : IDisposable
{
    private readonly VarlorDbContext _context;
    private readonly Mock<ILogger<BaseController>> _mockLogger;
    private readonly TestableBaseController _controller;

    public BaseControllerTests()
    {
        var options = new DbContextOptionsBuilder<VarlorDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new VarlorDbContext(options);
        _mockLogger = new Mock<ILogger<BaseController>>();
        _controller = new TestableBaseController(_context, _mockLogger.Object);
    }

    [Fact]
    public void GetPaginationParameters_WithNullValues_ReturnsDefaults()
    {
        // Act
        var result = TestableBaseController.ExposeGetPaginationParameters(null, null);

        // Assert
        Assert.Equal(0, result[0]); // skip
        Assert.Equal(10, result[1]); // take
    }

    [Fact]
    public void GetPaginationParameters_WithCustomValues_ReturnsCustomValues()
    {
        // Act
        var result = TestableBaseController.ExposeGetPaginationParameters(20, 50);

        // Assert
        Assert.Equal(20, result[0]); // skip
        Assert.Equal(50, result[1]); // take
    }

    [Fact]
    public void GetPaginationParameters_WithNegativeValues_ClampsToValidRange()
    {
        // Act
        var result = TestableBaseController.ExposeGetPaginationParameters(-10, -5);

        // Assert
        Assert.Equal(0, result[0]); // skip clamped to 0
        Assert.Equal(1, result[1]); // take clamped to 1
    }

    [Fact]
    public void GetPaginationParameters_WithLargeTakeValue_ClampsToMaximum()
    {
        // Act
        var result = TestableBaseController.ExposeGetPaginationParameters(0, 200);

        // Assert
        Assert.Equal(0, result[0]); // skip
        Assert.Equal(100, result[1]); // take clamped to 100
    }

    [Fact]
    public void NotFound_ThrowsNotFoundException()
    {
        // Arrange
        var id = Guid.NewGuid();

        // Act & Assert
        var exception = Assert.Throws<NotFoundException>(() => _controller.ExposeNotFound("Client", id));
        Assert.Contains("Client with ID", exception.Message);
        Assert.Contains(id.ToString(), exception.Message);
        Assert.Contains("not found", exception.Message);
    }

    [Fact]
    public void ValidationError_ThrowsValidationException()
    {
        // Arrange
        var message = "Test validation error";

        // Act & Assert
        var exception = Assert.Throws<ValidationException>(() => _controller.ExposeValidationError(message));
        Assert.Equal(message, exception.Message);
    }

    [Fact]
    public void DatabaseError_ThrowsDatabaseException()
    {
        // Arrange
        var innerException = new Exception("Inner error");
        var message = "Database operation failed";

        // Act & Assert
        var exception = Assert.Throws<DatabaseException>(() => _controller.ExposeDatabaseError(innerException));
        Assert.Equal(message, exception.Message);
        Assert.Equal(innerException, exception.InnerException);
    }

    [Fact]
    public void DuplicateError_ThrowsDuplicateResourceException()
    {
        // Arrange
        var message = "Email already exists";

        // Act & Assert
        var exception = Assert.Throws<DuplicateResourceException>(() => _controller.ExposeDuplicateError(message));
        Assert.Equal(message, exception.Message);
    }

    [Fact]
    public async Task ExecuteAsync_WithValidOperation_ReturnsOkResult()
    {
        // Arrange
        var operation = Task.FromResult("Test Result");

        // Act
        var result = await _controller.TestExecuteAsync(() => operation);

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.Equal("Test Result", okResult.Value);
    }

    [Fact]
    public async Task ExecuteAsync_WithNotFoundException_ThrowsOriginalException()
    {
        // Arrange
        var operation = Task.FromException<string>(new NotFoundException("Not found"));

        // Act & Assert
        await Assert.ThrowsAsync<NotFoundException>(() => _controller.TestExecuteAsync(() => operation));
    }

    [Fact]
    public async Task ExecuteAsync_WithValidationException_ThrowsOriginalException()
    {
        // Arrange
        var operation = Task.FromException<string>(new ValidationException("Validation failed"));

        // Act & Assert
        await Assert.ThrowsAsync<ValidationException>(() => _controller.TestExecuteAsync(() => operation));
    }

    [Fact]
    public async Task ExecuteAsync_WithDuplicateResourceException_ThrowsOriginalException()
    {
        // Arrange
        var operation = Task.FromException<string>(new DuplicateResourceException("Duplicate"));

        // Act & Assert
        await Assert.ThrowsAsync<DuplicateResourceException>(() => _controller.TestExecuteAsync(() => operation));
    }

    [Fact]
    public async Task ExecuteAsync_WithDbUpdateException_ThrowsDatabaseException()
    {
        // Arrange
        var dbException = new DbUpdateException("Database error", new Exception("Inner"));
        var operation = Task.FromException<string>(dbException);

        // Act & Assert
        await Assert.ThrowsAsync<DatabaseException>(() => _controller.TestExecuteAsync(() => operation));
    }

    public void Dispose()
    {
        _context?.Dispose();
    }
}