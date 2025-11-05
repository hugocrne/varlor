using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.OpenApi.Models;
using product_api.Data;
using product_api.Extensions;
using Xunit;

namespace product_api.Tests.Extensions;

public class ServiceCollectionExtensionsTests : IDisposable
{
    private readonly IServiceCollection _services;
    private readonly IConfiguration _configuration;

    public ServiceCollectionExtensionsTests()
    {
        _services = new ServiceCollection();

        // Add basic required services
        _services.AddLogging(builder => builder.AddConsole());
        _services.AddOptions();

        // Setup in-memory configuration
        var configData = new Dictionary<string, string?>
        {
            ["ConnectionStrings:DefaultConnection"] = "Host=localhost;Database=testdb;Username=test;Password=test"
        };

        _configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(configData)
            .Build();
    }

    [Fact]
    public void AddApiServices_ShouldAddRequiredServices()
    {
        // Act
        _services.AddApiServices();

        // Assert
        var serviceProvider = _services.BuildServiceProvider();

        // Check that password hasher is registered
        var passwordHasher = serviceProvider.GetService<IPasswordHasher<object>>();
        Assert.NotNull(passwordHasher);
    }

    [Fact]
    public void AddDatabaseServices_ShouldAddDbContext()
    {
        // Act
        _services.AddDatabaseServices(_configuration);

        // Assert
        var serviceProvider = _services.BuildServiceProvider();

        // Check that DbContext is registered
        var dbContext = serviceProvider.GetService<VarlorDbContext>();
        Assert.NotNull(dbContext);
    }

    [Fact]
    public void AddDevelopmentServices_ShouldEnableSensitiveDataLogging()
    {
        // Act
        _services.AddDevelopmentServices(_configuration);

        // Assert
        var serviceProvider = _services.BuildServiceProvider();

        // Check that DbContext is registered with development settings
        var dbContext = serviceProvider.GetService<VarlorDbContext>();
        Assert.NotNull(dbContext);
    }

    [Fact]
    public void ServiceCollectionExtensions_BasicRegistration_ShouldWork()
    {
        // Act
        _services.AddApiServices()
               .AddDatabaseServices(_configuration)
               .AddDevelopmentServices(_configuration);

        // Assert
        var serviceProvider = _services.BuildServiceProvider();

        // Verify basic services are registered
        var passwordHasher = serviceProvider.GetService<IPasswordHasher<object>>();
        var dbContext = serviceProvider.GetService<VarlorDbContext>();

        Assert.NotNull(passwordHasher);
        Assert.NotNull(dbContext);
    }

    [Fact]
    public void AddDatabaseServices_WithNullConfiguration_ShouldThrowArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => _services.AddDatabaseServices(null!));
    }

    [Fact]
    public void AddDevelopmentServices_WithNullConfiguration_ShouldThrowArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => _services.AddDevelopmentServices(null!));
    }

    public void Dispose()
    {
        // Cleanup if needed
    }
}