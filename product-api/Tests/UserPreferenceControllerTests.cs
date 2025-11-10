using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http.Json;
using product_api.DTOs.UserPreference;
using product_api.Models;
using Xunit;

namespace product_api.Tests;

public class UserPreferenceControllerTests : IntegrationTestBase
{
    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        AuthenticateAs(UserRole.ADMIN);
    }

    [Fact]
    public async Task GetUserPreferences_RetourneListe()
    {
        var (clientId, userId) = await CreateClientAndUserAsync();

        DbContext.UserPreferences.Add(new UserPreference
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            Theme = Theme.DARK,
            Language = "fr",
            NotificationsEnabled = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        });

        await DbContext.SaveChangesAsync();

        var response = await Client.GetAsync("/api/UserPreference");
        await AssertStatusCodeAsync(response, HttpStatusCode.OK);

        var preferences = await response.Content.ReadFromJsonAsync<List<UserPreferenceDto>>(SerializerOptions);
        Assert.NotNull(preferences);
        Assert.Single(preferences!);
        Assert.Equal(userId, preferences[0].UserId);
    }

    [Fact]
    public async Task CreateUserPreference_UtilisateurValide_Retourne201()
    {
        var (_, userId) = await CreateClientAndUserAsync();

        var payload = new CreateUserPreferenceDto
        {
            UserId = userId,
            Theme = Theme.LIGHT,
            Language = "en",
            NotificationsEnabled = false
        };

        var response = await Client.PostAsJsonAsync("/api/UserPreference", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.Created);

        var dto = await response.Content.ReadFromJsonAsync<UserPreferenceDto>(SerializerOptions);
        Assert.NotNull(dto);
        Assert.Equal(payload.UserId, dto!.UserId);
        Assert.Equal(payload.Theme, dto.Theme);
    }

    [Fact]
    public async Task CreateUserPreference_UtilisateurAbsent_Retourne400()
    {
        var payload = new CreateUserPreferenceDto
        {
            UserId = Guid.NewGuid(),
            Theme = Theme.DARK,
            Language = "fr",
            NotificationsEnabled = true
        };

        var response = await Client.PostAsJsonAsync("/api/UserPreference", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task CreateUserPreference_DejaExistante_Retourne409()
    {
        var (_, userId) = await CreateClientAndUserAsync();

        var preference = new UserPreference
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            Theme = Theme.DARK,
            Language = "fr",
            NotificationsEnabled = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        DbContext.UserPreferences.Add(preference);
        await DbContext.SaveChangesAsync();

        var payload = new CreateUserPreferenceDto
        {
            UserId = userId,
            Theme = Theme.LIGHT,
            Language = "en",
            NotificationsEnabled = false
        };

        var response = await Client.PostAsJsonAsync("/api/UserPreference", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.Conflict);
    }

    [Fact]
    public async Task UpdateUserPreference_ModifieLangue()
    {
        var (_, userId) = await CreateClientAndUserAsync();

        var preference = new UserPreference
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            Theme = Theme.DARK,
            Language = "fr",
            NotificationsEnabled = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        DbContext.UserPreferences.Add(preference);
        await DbContext.SaveChangesAsync();

        var payload = new UpdateUserPreferenceDto
        {
            Language = "es"
        };

        var response = await Client.PatchAsync($"/api/UserPreference/{preference.Id}", JsonContent.Create(payload, options: SerializerOptions));
        await AssertStatusCodeAsync(response, HttpStatusCode.OK);

        var dto = await response.Content.ReadFromJsonAsync<UserPreferenceDto>(SerializerOptions);
        Assert.NotNull(dto);
        Assert.Equal("es", dto!.Language);

        ClearChangeTracker();
        var refreshed = await DbContext.UserPreferences.FindAsync(preference.Id);
        Assert.NotNull(refreshed);
        Assert.Equal("es", refreshed!.Language);
    }

    [Fact]
    public async Task DeleteUserPreference_Retourne204()
    {
        var (_, userId) = await CreateClientAndUserAsync();

        var preference = new UserPreference
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            Theme = Theme.DARK,
            Language = "fr",
            NotificationsEnabled = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        DbContext.UserPreferences.Add(preference);
        await DbContext.SaveChangesAsync();

        var response = await Client.DeleteAsync($"/api/UserPreference/{preference.Id}");
        await AssertStatusCodeAsync(response, HttpStatusCode.NoContent);

        ClearChangeTracker();
        var count = DbContext.UserPreferences.Count();
        Assert.Equal(0, count);
    }

    [Fact]
    public async Task GetUserPreference_Inexistante_Retourne404()
    {
        var response = await Client.GetAsync($"/api/UserPreference/{Guid.NewGuid()}");
        await AssertStatusCodeAsync(response, HttpStatusCode.NotFound);
    }

    private async Task<(Guid clientId, Guid userId)> CreateClientAndUserAsync()
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Client Parent",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        var user = new User
        {
            Id = Guid.NewGuid(),
            ClientId = client.Id,
            Email = "pref@example.com",
            FirstName = "Pref",
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

        return (client.Id, user.Id);
    }
}

