using System;
using System.IdentityModel.Tokens.Jwt;
using System.Linq;
using System.Net;
using System.Net.Http.Json;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Identity;
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
        Assert.False(string.IsNullOrWhiteSpace(dto!.Token));
        Assert.True(dto.ExpiresAt > DateTimeOffset.UtcNow);

        var handler = new JwtSecurityTokenHandler();
        Assert.True(handler.CanReadToken(dto.Token));
        var token = handler.ReadJwtToken(dto.Token);

        AssertClaim(token, JwtRegisteredClaimNames.Sub, user.Id.ToString());
        AssertClaim(token, "client_id", user.ClientId.ToString());
        AssertClaim(token, "role", user.Role.ToString());
        AssertClaim(token, JwtRegisteredClaimNames.Email, user.Email);
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

    private static void AssertClaim(JwtSecurityToken token, string type, string expectedValue)
    {
        var claim = token.Claims.FirstOrDefault(c => c.Type == type);
        Assert.NotNull(claim);
        Assert.Equal(expectedValue, claim!.Value);
    }
}

