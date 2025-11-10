using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Npgsql;
using product_api.Data;
using Respawn;
using Respawn.Graph;
using Xunit;
using Xunit.Sdk;

namespace product_api.Tests;

public abstract class IntegrationTestBase : IAsyncLifetime
{
    private readonly AsyncServiceScope _scope;
    private readonly NpgsqlConnection _connection;
    private Respawner? _respawner;

    protected IntegrationTestBase()
    {
        SerializerOptions = new JsonSerializerOptions(JsonSerializerDefaults.Web)
        {
            PropertyNamingPolicy = null
        };
        SerializerOptions.Converters.Add(new JsonStringEnumConverter());

        Factory = new TestWebApplicationFactory();
        Client = Factory.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });

        _scope = Factory.Services.CreateAsyncScope();
        var configuration = _scope.ServiceProvider.GetRequiredService<IConfiguration>();
        var connectionString = configuration.GetConnectionString("DefaultConnection")
            ?? throw new InvalidOperationException("La chaîne de connexion 'DefaultConnection' est introuvable.");

        _connection = new NpgsqlConnection(connectionString);
        DbContext = _scope.ServiceProvider.GetRequiredService<VarlorDbContext>();
    }

    protected TestWebApplicationFactory Factory { get; }

    protected HttpClient Client { get; }

    protected VarlorDbContext DbContext { get; }

    protected JsonSerializerOptions SerializerOptions { get; }

    public virtual async Task InitializeAsync()
    {
        await _connection.OpenAsync();

        _respawner = await Respawner.CreateAsync(_connection, new RespawnerOptions
        {
            DbAdapter = DbAdapter.Postgres,
            SchemasToInclude = new[] { "public" },
            TablesToIgnore = new[]
            {
                new Table("public", "__EFMigrationsHistory")
            }
        });

        await ResetDatabaseAsync();
    }

    public virtual async Task DisposeAsync()
    {
        await ResetDatabaseAsync();

        await _connection.DisposeAsync();
        await _scope.DisposeAsync();
        Client.Dispose();
        Factory.Dispose();
    }

    protected async Task ResetDatabaseAsync()
    {
        if (_respawner is not null)
        {
            await _respawner.ResetAsync(_connection);
        }
    }

    protected void ClearChangeTracker()
    {
        DbContext.ChangeTracker.Clear();
    }

    protected static async Task AssertStatusCodeAsync(HttpResponseMessage response, HttpStatusCode expectedStatus)
    {
        if (response.StatusCode == expectedStatus)
        {
            return;
        }

        string content = await response.Content.ReadAsStringAsync();
        throw new XunitException($"Statut attendu {(int)expectedStatus} {expectedStatus}, obtenu {(int)response.StatusCode} {response.StatusCode}. Corps: {content}");
    }
}

