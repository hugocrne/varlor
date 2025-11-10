using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http.Json;
using product_api.DTOs.UserSession;
using product_api.Models;
using Xunit;

namespace product_api.Tests;

public class UserSessionControllerTests : IntegrationTestBase
{
    [Fact]
    public async Task GetUserSessions_RetourneListe()
    {
        var userId = await CreateClientAndUserAsync();

        var session = new UserSession
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            TokenId = Guid.NewGuid().ToString(),
            IpAddress = "127.0.0.1",
            UserAgent = "TestAgent",
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = DateTime.UtcNow.AddHours(1)
        };

        DbContext.UserSessions.Add(session);
        await DbContext.SaveChangesAsync();

        var response = await Client.GetAsync("/api/UserSession");
        await AssertStatusCodeAsync(response, HttpStatusCode.OK);

        var sessions = await response.Content.ReadFromJsonAsync<List<UserSessionDto>>(SerializerOptions);
        Assert.NotNull(sessions);
        Assert.Single(sessions!);
        Assert.Equal(session.TokenId, sessions[0].TokenId);
    }

    [Fact]
    public async Task CreateUserSession_Valide_Retourne201()
    {
        var userId = await CreateClientAndUserAsync();

        var payload = new CreateUserSessionDto
        {
            UserId = userId,
            TokenId = Guid.NewGuid().ToString(),
            IpAddress = "127.0.0.1",
            UserAgent = "TestAgent",
            ExpiresAt = DateTime.UtcNow.AddHours(2)
        };

        var response = await Client.PostAsJsonAsync("/api/UserSession", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.Created);

        var dto = await response.Content.ReadFromJsonAsync<UserSessionDto>(SerializerOptions);
        Assert.NotNull(dto);
        Assert.Equal(payload.TokenId, dto!.TokenId);
        Assert.Equal(payload.UserId, dto.UserId);
    }

    [Fact]
    public async Task CreateUserSession_DateExpirée_Retourne400()
    {
        var userId = await CreateClientAndUserAsync();

        var payload = new CreateUserSessionDto
        {
            UserId = userId,
            TokenId = Guid.NewGuid().ToString(),
            IpAddress = "127.0.0.1",
            UserAgent = "TestAgent",
            ExpiresAt = DateTime.UtcNow.AddMinutes(-5)
        };

        var response = await Client.PostAsJsonAsync("/api/UserSession", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task CreateUserSession_TokenDuplique_Retourne409()
    {
        var userId = await CreateClientAndUserAsync();
        var tokenId = Guid.NewGuid().ToString();

        DbContext.UserSessions.Add(new UserSession
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            TokenId = tokenId,
            IpAddress = "127.0.0.1",
            UserAgent = "Agent",
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = DateTime.UtcNow.AddHours(1)
        });
        await DbContext.SaveChangesAsync();

        var payload = new CreateUserSessionDto
        {
            UserId = userId,
            TokenId = tokenId,
            IpAddress = "127.0.0.2",
            UserAgent = "Agent2",
            ExpiresAt = DateTime.UtcNow.AddHours(2)
        };

        var response = await Client.PostAsJsonAsync("/api/UserSession", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.Conflict);
    }

    [Fact]
    public async Task DeleteUserSession_Retourne204()
    {
        var userId = await CreateClientAndUserAsync();
        var session = new UserSession
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            TokenId = Guid.NewGuid().ToString(),
            IpAddress = "127.0.0.10",
            UserAgent = "DeleteAgent",
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = DateTime.UtcNow.AddHours(1)
        };

        DbContext.UserSessions.Add(session);
        await DbContext.SaveChangesAsync();

        var response = await Client.DeleteAsync($"/api/UserSession/{session.Id}");
        await AssertStatusCodeAsync(response, HttpStatusCode.NoContent);

        ClearChangeTracker();
        var exists = await DbContext.UserSessions.FindAsync(session.Id);
        Assert.Null(exists);
    }

    [Fact]
    public async Task GetUserSession_Inexistant_Retourne404()
    {
        var response = await Client.GetAsync($"/api/UserSession/{Guid.NewGuid()}");
        await AssertStatusCodeAsync(response, HttpStatusCode.NotFound);
    }

    private async Task<Guid> CreateClientAndUserAsync()
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Client Session",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        var user = new User
        {
            Id = Guid.NewGuid(),
            ClientId = client.Id,
            Email = "session@example.com",
            FirstName = "Session",
            LastName = "User",
            Role = UserRole.MEMBER,
            Status = UserStatus.ACTIVE,
            PasswordHash = "hash",
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        DbContext.Clients.Add(client);
        DbContext.Users.Add(user);
        await DbContext.SaveChangesAsync();

        return user.Id;
    }
}

