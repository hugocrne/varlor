using System;
using System.IdentityModel.Tokens.Jwt;
using System.Linq;
using System.Net;
using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using product_api.DTOs.Auth;
using product_api.Models;
using Xunit;

namespace product_api.Tests.Auth;

public class AuthControllerTests : IntegrationTestBase
{
    [Fact]
    public async Task Login_IdentifiantsValides_RetourneJetonEtClaims()
    {
        const string password = "MotDePasse!123";
        var user = await CreateActiveUserAsync(password);

        var payload = new LoginRequestDto
        {
            Email = user.Email,
            Password = password
        };

        var response = await Client.PostAsJsonAsync("/api/Auth/login", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.OK);

        var dto = await response.Content.ReadFromJsonAsync<LoginResponseDto>(SerializerOptions);
        Assert.NotNull(dto);
        Assert.False(string.IsNullOrWhiteSpace(dto!.AccessToken));
        Assert.False(string.IsNullOrWhiteSpace(dto.RefreshToken));
        Assert.True(dto.ExpiresAt > DateTimeOffset.UtcNow);
        Assert.True(dto.RefreshExpiresAt > dto.ExpiresAt);

        var handler = new JwtSecurityTokenHandler();
        Assert.True(handler.CanReadToken(dto.AccessToken));
        var token = handler.ReadJwtToken(dto.AccessToken);

        AssertClaim(token, JwtRegisteredClaimNames.Sub, user.Id.ToString());
        AssertClaim(token, "client_id", user.ClientId.ToString());
        AssertClaim(token, "role", user.Role.ToString());
        AssertClaim(token, JwtRegisteredClaimNames.Email, user.Email);

        var session = await DbContext.UserSessions.AsNoTracking().SingleAsync();
        Assert.Equal(user.Id, session.UserId);
        Assert.Null(session.RevokedAt);
        Assert.Equal(HashRefreshToken(dto.RefreshToken), session.TokenHash);
        Assert.True(session.ExpiresAt > DateTime.UtcNow);
    }

    [Fact]
    public async Task Login_MotDePasseIncorrect_Retourne401()
    {
        const string password = "MotDePasse!123";
        var user = await CreateActiveUserAsync(password);

        var payload = new LoginRequestDto
        {
            Email = user.Email,
            Password = "MauvaisMotDePasse!321"
        };

        var response = await Client.PostAsJsonAsync("/api/Auth/login", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.Unauthorized);
    }

    [Fact]
    public async Task Login_UtilisateurInexistant_Retourne404()
    {
        var payload = new LoginRequestDto
        {
            Email = "absent@example.com",
            Password = "Pwd!12345"
        };

        var response = await Client.PostAsJsonAsync("/api/Auth/login", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.NotFound);
    }

    [Fact]
    public async Task Login_EmailManquant_Retourne400()
    {
        var payload = JsonContent.Create(new
        {
            password = "SansEmail!123"
        }, options: SerializerOptions);

        var response = await Client.PostAsync("/api/Auth/login", payload);
        await AssertStatusCodeAsync(response, HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task Refresh_TokenValide_RetourneNouveauxTokensEtRevoqueAncienneSession()
    {
        const string password = "MotDePasse!123";
        var user = await CreateActiveUserAsync(password);
        var loginDto = await LoginAsync(user.Email, password);

        var payload = new RefreshTokenRequestDto
        {
            RefreshToken = loginDto.RefreshToken
        };

        var response = await Client.PostAsJsonAsync("/api/Auth/refresh", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.OK);

        var refreshDto = await response.Content.ReadFromJsonAsync<TokenPairResponseDto>(SerializerOptions);
        Assert.NotNull(refreshDto);
        Assert.False(string.IsNullOrWhiteSpace(refreshDto!.AccessToken));
        Assert.False(string.IsNullOrWhiteSpace(refreshDto.RefreshToken));
        Assert.NotEqual(loginDto.RefreshToken, refreshDto.RefreshToken);

        var sessions = await DbContext.UserSessions.AsNoTracking().OrderBy(session => session.CreatedAt).ToListAsync();
        Assert.Equal(2, sessions.Count);

        var previousSession = sessions.Single(session => session.TokenHash == HashRefreshToken(loginDto.RefreshToken));
        var newSession = sessions.Single(session => session.TokenHash == HashRefreshToken(refreshDto.RefreshToken));

        Assert.NotNull(previousSession.RevokedAt);
        Assert.Equal("ROTATED", previousSession.RevocationReason);
        Assert.Equal(newSession.TokenId, previousSession.ReplacedByTokenId);
        Assert.Null(newSession.RevokedAt);
    }

    [Fact]
    public async Task Refresh_TokenInvalide_Retourne401()
    {
        var payload = new RefreshTokenRequestDto
        {
            RefreshToken = Convert.ToBase64String(RandomNumberGenerator.GetBytes(64))
        };

        var response = await Client.PostAsJsonAsync("/api/Auth/refresh", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.Unauthorized);
    }

    [Fact]
    public async Task Logout_TokenValide_RevoqueSession()
    {
        const string password = "MotDePasse!123";
        var user = await CreateActiveUserAsync(password);
        var loginDto = await LoginAsync(user.Email, password);

        var payload = new LogoutRequestDto
        {
            RefreshToken = loginDto.RefreshToken
        };

        var response = await Client.PostAsJsonAsync("/api/Auth/logout", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.NoContent);

        var session = await DbContext.UserSessions.AsNoTracking().SingleAsync();
        Assert.NotNull(session.RevokedAt);
        Assert.Equal("LOGOUT", session.RevocationReason);
        Assert.Null(session.ReplacedByTokenId);
    }

    [Fact]
    public async Task Logout_RevokeAllSessions_RevoqueToutesLesSessionsActives()
    {
        const string password = "MotDePasse!123";
        var user = await CreateActiveUserAsync(password);
        var firstLogin = await LoginAsync(user.Email, password);
        var secondLogin = await LoginAsync(user.Email, password);

        var payload = new LogoutRequestDto
        {
            RefreshToken = secondLogin.RefreshToken,
            RevokeAllSessions = true
        };

        var response = await Client.PostAsJsonAsync("/api/Auth/logout", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.NoContent);

        var sessions = await DbContext.UserSessions.AsNoTracking().ToListAsync();
        Assert.Equal(2, sessions.Count);
        Assert.All(sessions, session =>
        {
            Assert.NotNull(session.RevokedAt);
            Assert.Equal("LOGOUT", session.RevocationReason);
        });
    }

    private async Task<User> CreateActiveUserAsync(string password)
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Client Auth",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        var user = new User
        {
            Id = Guid.NewGuid(),
            ClientId = client.Id,
            Email = $"utilisateur_{Guid.NewGuid():N}@example.com",
            FirstName = "Jean",
            LastName = "Auth",
            Role = UserRole.ADMIN,
            Status = UserStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        using var scope = Factory.Services.CreateScope();
        var passwordHasher = scope.ServiceProvider.GetRequiredService<IPasswordHasher<User>>();
        user.PasswordHash = passwordHasher.HashPassword(user, password);

        DbContext.Clients.Add(client);
        DbContext.Users.Add(user);
        await DbContext.SaveChangesAsync();

        ClearChangeTracker();
        return user;
    }

    private async Task<LoginResponseDto> LoginAsync(string email, string password)
    {
        var payload = new LoginRequestDto
        {
            Email = email,
            Password = password
        };

        var response = await Client.PostAsJsonAsync("/api/Auth/login", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.OK);
        var dto = await response.Content.ReadFromJsonAsync<LoginResponseDto>(SerializerOptions);
        Assert.NotNull(dto);
        return dto!;
    }

    private static void AssertClaim(JwtSecurityToken token, string type, string expectedValue)
    {
        var claim = token.Claims.FirstOrDefault(c => c.Type == type);
        Assert.NotNull(claim);
        Assert.Equal(expectedValue, claim!.Value);
    }

    private static string HashRefreshToken(string refreshToken)
    {
        var bytes = Encoding.UTF8.GetBytes(refreshToken);
        var hash = SHA256.HashData(bytes);
        return Convert.ToBase64String(hash);
    }
}

