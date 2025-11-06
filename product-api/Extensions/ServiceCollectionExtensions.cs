using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using product_api.Data;

namespace product_api.Extensions;

public static class ServiceCollectionExtensions
{
    /// <summary>
    /// Adds all API services to the service collection
    /// </summary>
    public static IServiceCollection AddApiServices(this IServiceCollection services)
    {
        // Add controllers
        services.AddControllers();

        // Add password hasher for user management
        services.AddSingleton<IPasswordHasher<object>, PasswordHasher<object>>();

        return services;
    }

    /// <summary>
    /// Adds API documentation services
    /// </summary>
    public static IServiceCollection AddApiDocumentation(this IServiceCollection services)
    {
        // Add Swagger/OpenAPI
        services.AddEndpointsApiExplorer();
        services.AddSwaggerGen(c =>
        {
            c.SwaggerDoc("v1", new()
            {
                Title = "Product API",
                Version = "v1",
                Description = "REST API for managing clients, users, preferences, and sessions"
            });

            // Include XML comments if available
            var xmlFile = $"{System.Reflection.Assembly.GetExecutingAssembly().GetName().Name}.xml";
            var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFile);
            if (File.Exists(xmlPath))
            {
                c.IncludeXmlComments(xmlPath);
            }
        });

        return services;
    }

    /// <summary>
    /// Adds database services
    /// </summary>
    public static IServiceCollection AddDatabaseServices(this IServiceCollection services, IConfiguration configuration)
    {
        if (configuration == null)
            throw new ArgumentNullException(nameof(configuration));

        // Add Entity Framework DbContext
        services.AddDbContext<VarlorDbContext>(options =>
            options.UseNpgsql(configuration.GetConnectionString("DefaultConnection")));

        return services;
    }

    /// <summary>
    /// Configures development services
    /// </summary>
    public static IServiceCollection AddDevelopmentServices(this IServiceCollection services, IConfiguration configuration)
    {
        if (configuration == null)
            throw new ArgumentNullException(nameof(configuration));

        // Enable sensitive data logging in development
        services.AddDbContext<VarlorDbContext>(options =>
            options.UseNpgsql(configuration.GetConnectionString("DefaultConnection"))
                .EnableSensitiveDataLogging()
                .EnableDetailedErrors());

        return services;
    }
}