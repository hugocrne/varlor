using System.IO;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using product_api.Models;

namespace product_api.Tests;

public class TestWebApplicationFactory : WebApplicationFactory<Program>
{
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        var contentRoot = Directory.GetCurrentDirectory();

        AppContext.SetSwitch("Npgsql.EnableLegacyTimestampBehavior", true);

        builder
            .UseEnvironment("Development")
            .UseContentRoot(contentRoot)
            .ConfigureServices(services =>
            {
                services.AddScoped<IPasswordHasher<User>, PasswordHasher<User>>();
            });
    }
}

