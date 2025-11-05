using product_api.Exceptions;
using System;
using Xunit;

namespace product_api.Tests.Exceptions;

public class ValidationExceptionTests
{
    [Fact]
    public void ValidationException_WithMessage_SetsMessageCorrectly()
    {
        // Arrange
        var message = "Validation failed";

        // Act
        var exception = new ValidationException(message);

        // Assert
        Assert.Equal(message, exception.Message);
    }

    [Fact]
    public void ValidationException_WithInnerException_SetsPropertiesCorrectly()
    {
        // Arrange
        var message = "Validation failed";
        var innerException = new InvalidOperationException("Inner error");

        // Act
        var exception = new ValidationException(message, innerException);

        // Assert
        Assert.Equal(message, exception.Message);
        Assert.Equal(innerException, exception.InnerException);
    }

    [Fact]
    public void ValidationException_DefaultConstructor_CreatesEmptyException()
    {
        // Act
        var exception = new ValidationException();

        // Assert
        Assert.NotNull(exception);
    }
}

public class NotFoundExceptionTests
{
    [Fact]
    public void NotFoundException_WithMessage_SetsMessageCorrectly()
    {
        // Arrange
        var message = "Resource not found";

        // Act
        var exception = new NotFoundException(message);

        // Assert
        Assert.Equal(message, exception.Message);
    }

    [Fact]
    public void NotFoundException_WithInnerException_SetsPropertiesCorrectly()
    {
        // Arrange
        var message = "Resource not found";
        var innerException = new InvalidOperationException("Inner error");

        // Act
        var exception = new NotFoundException(message, innerException);

        // Assert
        Assert.Equal(message, exception.Message);
        Assert.Equal(innerException, exception.InnerException);
    }

    [Fact]
    public void NotFoundException_DefaultConstructor_CreatesEmptyException()
    {
        // Act
        var exception = new NotFoundException();

        // Assert
        Assert.NotNull(exception);
    }
}

public class DuplicateResourceExceptionTests
{
    [Fact]
    public void DuplicateResourceException_WithMessage_SetsMessageCorrectly()
    {
        // Arrange
        var message = "Duplicate resource";

        // Act
        var exception = new DuplicateResourceException(message);

        // Assert
        Assert.Equal(message, exception.Message);
    }

    [Fact]
    public void DuplicateResourceException_WithInnerException_SetsPropertiesCorrectly()
    {
        // Arrange
        var message = "Duplicate resource";
        var innerException = new InvalidOperationException("Inner error");

        // Act
        var exception = new DuplicateResourceException(message, innerException);

        // Assert
        Assert.Equal(message, exception.Message);
        Assert.Equal(innerException, exception.InnerException);
    }

    [Fact]
    public void DuplicateResourceException_DefaultConstructor_CreatesEmptyException()
    {
        // Act
        var exception = new DuplicateResourceException();

        // Assert
        Assert.NotNull(exception);
    }
}

public class DatabaseExceptionTests
{
    [Fact]
    public void DatabaseException_WithMessage_SetsMessageCorrectly()
    {
        // Arrange
        var message = "Database error";

        // Act
        var exception = new DatabaseException(message);

        // Assert
        Assert.Equal(message, exception.Message);
    }

    [Fact]
    public void DatabaseException_WithInnerException_SetsPropertiesCorrectly()
    {
        // Arrange
        var message = "Database error";
        var innerException = new InvalidOperationException("Inner error");

        // Act
        var exception = new DatabaseException(message, innerException);

        // Assert
        Assert.Equal(message, exception.Message);
        Assert.Equal(innerException, exception.InnerException);
    }

    [Fact]
    public void DatabaseException_DefaultConstructor_CreatesEmptyException()
    {
        // Act
        var exception = new DatabaseException();

        // Assert
        Assert.NotNull(exception);
    }
}

public class UnauthorizedExceptionTests
{
    [Fact]
    public void UnauthorizedException_WithMessage_SetsMessageCorrectly()
    {
        // Arrange
        var message = "Unauthorized access";

        // Act
        var exception = new UnauthorizedException(message);

        // Assert
        Assert.Equal(message, exception.Message);
    }

    [Fact]
    public void UnauthorizedException_WithInnerException_SetsPropertiesCorrectly()
    {
        // Arrange
        var message = "Unauthorized access";
        var innerException = new InvalidOperationException("Inner error");

        // Act
        var exception = new UnauthorizedException(message, innerException);

        // Assert
        Assert.Equal(message, exception.Message);
        Assert.Equal(innerException, exception.InnerException);
    }

    [Fact]
    public void UnauthorizedException_DefaultConstructor_CreatesEmptyException()
    {
        // Act
        var exception = new UnauthorizedException();

        // Assert
        Assert.NotNull(exception);
    }
}