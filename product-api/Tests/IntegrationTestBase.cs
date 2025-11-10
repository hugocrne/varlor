using System.Collections.Generic;
using System.IdentityModel.Tokens.Jwt;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Claims;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.IdentityModel.Tokens;
using Npgsql;
using product_api.Data;
using product_api.Models;
using Respawn;
using Respawn.Graph;
using Xunit;
using Xunit.Sdk;

namespace product_api.Tests;

public abstract class IntegrationTestBase : IAsyncLifetime
{
    private readonly JwtSecurityTokenHandler _jwtTokenHandler = new();
    private readonly AsyncServiceScope _scope;
    private readonly NpgsqlConnection _connection;
    private Respawner? _respawner;
    private readonly string _jwtKey;
    private readonly string _jwtIssuer;
    private readonly string _jwtAudience;

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

        _jwtKey = configuration["Jwt:Key"] ?? throw new InvalidOperationException("La clé secrète JWT est introuvable.");
        _jwtIssuer = configuration["Jwt:Issuer"] ?? throw new InvalidOperationException("L'issuer JWT est introuvable.");
        _jwtAudience = configuration["Jwt:Audience"] ?? throw new InvalidOperationException("L'audience JWT est introuvable.");

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

        ClearAuthentication();
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

    protected void AuthenticateAs(UserRole role, DateTime? expiresAtUtc = null, Guid? userId = null, Guid? clientId = null, string? email = null, IEnumerable<Claim>? additionalClaims = null)
    {
        var token = IssueJwtToken(role, expiresAtUtc, userId, clientId, email, additionalClaims);
        Client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    protected void ClearAuthentication()
    {
        Client.DefaultRequestHeaders.Authorization = null;
    }

    protected string IssueJwtToken(
        UserRole role,
        DateTime? expiresAtUtc = null,
        Guid? userId = null,
        Guid? clientId = null,
        string? email = null,
        IEnumerable<Claim>? additionalClaims = null)
    {
        var subject = userId ?? Guid.NewGuid();
        var associatedClientId = clientId ?? Guid.NewGuid();
        var now = DateTime.UtcNow;
        var expires = expiresAtUtc ?? now.AddHours(1);
        var notBefore = expiresAtUtc.HasValue
            ? expiresAtUtc.Value.AddMinutes(-5)
            : now.AddMinutes(-5);

        if (expires <= notBefore)
        {
            throw new ArgumentException("L'expiration du token doit être postérieure à sa date de validité initiale.", nameof(expiresAtUtc));
        }

        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, subject.ToString()),
            new("client_id", associatedClientId.ToString()),
            new("role", role.ToString()),
            new(ClaimTypes.Role, role.ToString()),
            new(JwtRegisteredClaimNames.Email, email ?? $"{role.ToString().ToLowerInvariant()}@example.com")
        };

        if (additionalClaims is not null)
        {
            claims.AddRange(additionalClaims);
        }

        var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_jwtKey));
        var signingCredentials = new SigningCredentials(signingKey, SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: _jwtIssuer,
            audience: _jwtAudience,
            claims: claims,
            notBefore: notBefore,
            expires: expires,
            signingCredentials: signingCredentials);

        return _jwtTokenHandler.WriteToken(token);
    }
}

