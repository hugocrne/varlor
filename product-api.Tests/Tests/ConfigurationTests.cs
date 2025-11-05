using Microsoft.Extensions.Configuration;
using Microsoft.EntityFrameworkCore;
using product_api.Data;
using Xunit;

namespace product_api.Tests;

public class ConfigurationTests
{
    [Fact]
    public void ConnectionString_Should_Load_From_AppSettings()
    {
        // Arrange
        var builder = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile("appsettings.json", optional: false)
            .AddJsonFile("appsettings.Development.json", optional: true)
            .AddEnvironmentVariables();

        var configuration = builder.Build();

        // Act
        var connectionString = configuration.GetConnectionString("DefaultConnection");

        // Assert
        Assert.NotNull(connectionString);
        Assert.Contains("Host=localhost", connectionString);
        Assert.Contains("Database=varlor", connectionString);
        Assert.Contains("Username=hugo", connectionString);
    }

    [Fact]
    public void DbContext_Should_Be_Configurable_With_Connection_String()
    {
        // Arrange
        var connectionString = "Host=localhost;Database=varlor;Username=hugo;Password=test";
        var options = new DbContextOptionsBuilder<VarlorDbContext>()
            .UseNpgsql(connectionString)
            .Options;

        // Act & Assert
        Assert.NotNull(options);
        var extension = options.FindExtension<Microsoft.EntityFrameworkCore.Infrastructure.CoreOptionsExtension>();
        Assert.NotNull(extension);
    }

    [Fact]
    public void Environment_Specific_Configuration_Should_Load()
    {
        // Arrange
        var builder = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile("appsettings.json", optional: false)
            .AddJsonFile("appsettings.Development.json", optional: true)
            .AddEnvironmentVariables();

        var configuration = builder.Build();

        // Act
        var efLogLevel = configuration.GetValue<string>("Logging:LogLevel:Microsoft.EntityFrameworkCore.Database.Command");

        // Assert
        Assert.NotNull(efLogLevel);
        Assert.Equal("Information", efLogLevel);
    }
}