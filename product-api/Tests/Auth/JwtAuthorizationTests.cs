using System;
using System.Collections.Generic;
using System.IdentityModel.Tokens.Jwt;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Claims;
using System.Text;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.IdentityModel.Tokens;
using product_api.Models;
using Xunit;

namespace product_api.Tests.Auth;

public class JwtAuthorizationTests : IntegrationTestBase
{
    private readonly JwtSecurityTokenHandler _tokenHandler = new();
    private readonly string _jwtKey;
    private readonly string _jwtIssuer;
    private readonly string _jwtAudience;

    public JwtAuthorizationTests()
    {
        using var scope = Factory.Services.CreateScope();
        var configuration = scope.ServiceProvider.GetRequiredService<IConfiguration>();

        _jwtKey = configuration["Jwt:Key"] ?? throw new InvalidOperationException("Clé JWT introuvable dans la configuration.");
        _jwtIssuer = configuration["Jwt:Issuer"] ?? throw new InvalidOperationException("Issuer JWT introuvable dans la configuration.");
        _jwtAudience = configuration["Jwt:Audience"] ?? throw new InvalidOperationException("Audience JWT introuvable dans la configuration.");
    }

    [Fact]
    public async Task GetClients_SansToken_Retourne401()
    {
        var response = await Client.GetAsync("/api/Client");

        await AssertStatusCodeAsync(response, HttpStatusCode.Unauthorized);
    }

    [Fact]
    public async Task GetClients_TokenInvalide_Retourne401()
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, "/api/Client");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", "jeton-invalide");

        var response = await Client.SendAsync(request);

        await AssertStatusCodeAsync(response, HttpStatusCode.Unauthorized);
    }

    [Fact]
    public async Task GetClients_TokenExpiré_Retourne401()
    {
        var user = await CreateUserAsync(UserRole.ADMIN);
        var expiredToken = GenerateJwtToken(user, expiresAtUtc: DateTime.UtcNow.AddMinutes(-5));

        using var request = BuildAuthorizedRequest("/api/Client", expiredToken);
        var response = await Client.SendAsync(request);

        await AssertStatusCodeAsync(response, HttpStatusCode.Unauthorized);
    }

    [Fact]
    public async Task GetClients_TokenValide_Retourne200()
    {
        var user = await CreateUserAsync(UserRole.ADMIN);
        var token = GenerateJwtToken(user);

        using var request = BuildAuthorizedRequest("/api/Client", token);
        var response = await Client.SendAsync(request);

        await AssertStatusCodeAsync(response, HttpStatusCode.OK);
    }

    [Fact]
    public async Task GetClients_TokenRoleInsuffisant_Retourne403()
    {
        var user = await CreateUserAsync(UserRole.MEMBER);
        var token = GenerateJwtToken(user);

        using var request = BuildAuthorizedRequest("/api/Client", token);
        var response = await Client.SendAsync(request);

        await AssertStatusCodeAsync(response, HttpStatusCode.Forbidden);
    }

    [Fact]
    public async Task GetUserPreferences_TokenRoleAutorisé_Retourne200()
    {
        var user = await CreateUserAsync(UserRole.MEMBER, withPreference: true);
        var token = GenerateJwtToken(user);

        using var request = BuildAuthorizedRequest("/api/UserPreference", token);
        var response = await Client.SendAsync(request);

        await AssertStatusCodeAsync(response, HttpStatusCode.OK);
    }

    private string GenerateJwtToken(User user, DateTime? expiresAtUtc = null)
    {
        var expires = expiresAtUtc ?? DateTime.UtcNow.AddMinutes(30);
        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            new("client_id", user.ClientId.ToString()),
            new("role", user.Role.ToString()),
            new(JwtRegisteredClaimNames.Email, user.Email)
        };

        var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_jwtKey));
        var signingCredentials = new SigningCredentials(signingKey, SecurityAlgorithms.HmacSha256);

        var notBefore = DateTime.UtcNow.AddHours(-1);

        var token = new JwtSecurityToken(
            issuer: _jwtIssuer,
            audience: _jwtAudience,
            claims: claims,
            notBefore: notBefore,
            expires: expires,
            signingCredentials: signingCredentials);

        var encodedToken = _tokenHandler.WriteToken(token);
        var parsedToken = _tokenHandler.ReadJwtToken(encodedToken);

        AssertClaim(parsedToken, JwtRegisteredClaimNames.Sub, user.Id.ToString());
        AssertClaim(parsedToken, "role", user.Role.ToString());
        AssertClaim(parsedToken, "client_id", user.ClientId.ToString());
        AssertClaim(parsedToken, JwtRegisteredClaimNames.Email, user.Email);

        return encodedToken;
    }

    private async Task<User> CreateUserAsync(UserRole role, bool withPreference = false)
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = $"Client {role}",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        var user = new User
        {
            Id = Guid.NewGuid(),
            ClientId = client.Id,
            Email = $"{role.ToString().ToLowerInvariant()}_{Guid.NewGuid():N}@example.com",
            FirstName = role.ToString(),
            LastName = "Tester",
            Role = role,
            Status = UserStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        DbContext.Clients.Add(client);
        DbContext.Users.Add(user);

        if (withPreference)
        {
            DbContext.UserPreferences.Add(new UserPreference
            {
                Id = Guid.NewGuid(),
                UserId = user.Id,
                Theme = Theme.DARK,
                Language = "fr",
                NotificationsEnabled = true,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
        }

        await DbContext.SaveChangesAsync();
        ClearChangeTracker();

        return user;
    }

    private static HttpRequestMessage BuildAuthorizedRequest(string uri, string token)
    {
        var request = new HttpRequestMessage(HttpMethod.Get, uri);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return request;
    }

    private static void AssertClaim(JwtSecurityToken token, string type, string expectedValue)
    {
        var claim = token.Claims.FirstOrDefault(c => c.Type == type);
        Assert.NotNull(claim);
        Assert.Equal(expectedValue, claim!.Value);
    }
}

