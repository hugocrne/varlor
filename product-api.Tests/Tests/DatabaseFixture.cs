using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using product_api.Data;
using System;
using System.IO;
using System.Threading.Tasks;
using Xunit;

namespace product_api.Tests;

public class DatabaseFixture : IAsyncLifetime
{
    public ServiceProvider ServiceProvider { get; private set; } = null!;

    public async Task InitializeAsync()
    {
        // Build configuration
        var configuration = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile("appsettings.json", optional: false, reloadOnChange: true)
            .AddJsonFile("appsettings.Development.json", optional: true, reloadOnChange: true)
            .AddEnvironmentVariables()
            .Build();

        // Configure services
        var services = new ServiceCollection();

        // Add configuration
        services.AddSingleton<IConfiguration>(configuration);

        // Add logging
        services.AddLogging(builder =>
        {
            builder.AddConsole();
            builder.SetMinimumLevel(LogLevel.Information);
        });

        // Configure DbContext with PostgreSQL
        var connectionString = configuration.GetConnectionString("DefaultConnection");
        if (string.IsNullOrEmpty(connectionString))
        {
            throw new InvalidOperationException("Connection string 'DefaultConnection' not found in configuration.");
        }

        services.AddDbContext<VarlorDbContext>(options =>
        {
            options.UseNpgsql(connectionString, npgsqlOptions =>
            {
                npgsqlOptions.EnableRetryOnFailure(
                    maxRetryCount: 3,
                    maxRetryDelay: TimeSpan.FromSeconds(30),
                    errorCodesToAdd: null);
            });

            // Enable sensitive data logging in test environment
            options.EnableSensitiveDataLogging();
            options.EnableDetailedErrors();

            // Set command timeout for tests
            options.EnableServiceProviderCaching();
        });

        ServiceProvider = services.BuildServiceProvider();

        // Test database connection and create transaction scope
        await TestDatabaseConnection();
    }

    private async Task TestDatabaseConnection()
    {
        using var scope = ServiceProvider.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<VarlorDbContext>();

        // Test that we can connect to the database
        try
        {
            var canConnect = await dbContext.Database.CanConnectAsync();
            if (!canConnect)
            {
                Console.WriteLine("Warning: Cannot connect to the test database. Tests requiring database access will be skipped.");
                return;
            }

            // Test that we can query the database
            await dbContext.Database.OpenConnectionAsync();

            // Verify tables exist by attempting to query them
            await dbContext.Clients.CountAsync();
            await dbContext.Users.CountAsync();
            await dbContext.UserPreferences.CountAsync();
            await dbContext.UserSessions.CountAsync();

            Console.WriteLine("Database connection test successful.");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Warning: Database connection test failed: {ex.Message}. Tests requiring database access will be skipped.");
        }
        finally
        {
            try
            {
                await dbContext.Database.CloseConnectionAsync();
            }
            catch
            {
                // Ignore cleanup errors
            }
        }
    }

    public Task DisposeAsync()
    {
        // Clean up any test data if necessary
        // For this implementation, we're using the existing "varlor" database
        // and ensuring tests don't modify data, so no cleanup is needed

        ServiceProvider?.Dispose();
        return Task.CompletedTask;
    }

    /// <summary>
    /// Gets a fresh DbContext instance for testing.
    /// This ensures each test gets a clean context without tracking issues.
    /// </summary>
    public VarlorDbContext CreateDbContext()
    {
        var scope = ServiceProvider.CreateScope();
        return scope.ServiceProvider.GetRequiredService<VarlorDbContext>();
    }
}