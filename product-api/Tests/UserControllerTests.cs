using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http.Json;
using Microsoft.EntityFrameworkCore;
using product_api.DTOs.User;
using product_api.Models;
using Xunit;

namespace product_api.Tests;

public class UserControllerTests : IntegrationTestBase
{
    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        AuthenticateAs(UserRole.ADMIN);
    }

    [Fact]
    public async Task GetUsers_RetourneUtilisateursActifs()
    {
        var clientId = Guid.NewGuid();
        DbContext.Clients.Add(new Client
        {
            Id = clientId,
            Name = "Client Racine",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        });

        var activeUser = new User
        {
            Id = Guid.NewGuid(),
            ClientId = clientId,
            Email = "active@example.com",
            FirstName = "Active",
            LastName = "User",
            Role = UserRole.ADMIN,
            Status = UserStatus.ACTIVE,
            PasswordHash = "hashed",
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        var deletedUser = new User
        {
            Id = Guid.NewGuid(),
            ClientId = clientId,
            Email = "deleted@example.com",
            FirstName = "Deleted",
            LastName = "User",
            Role = UserRole.MEMBER,
            Status = UserStatus.INACTIVE,
            PasswordHash = "hashed",
            CreatedAt = DateTime.UtcNow.AddDays(-1),
            UpdatedAt = DateTime.UtcNow.AddHours(-12),
            DeletedAt = DateTime.UtcNow.AddHours(-6)
        };

        DbContext.Users.AddRange(activeUser, deletedUser);
        await DbContext.SaveChangesAsync();

        var response = await Client.GetAsync("/api/User");
        await AssertStatusCodeAsync(response, HttpStatusCode.OK);

        var users = await response.Content.ReadFromJsonAsync<List<UserDto>>(SerializerOptions);
        Assert.NotNull(users);
        Assert.Single(users!);
        var dto = users[0];
        Assert.Equal(activeUser.Id, dto.Id);
        Assert.Equal(activeUser.Email, dto.Email);
        Assert.Equal(activeUser.ClientId, dto.ClientId);
        Assert.Null(dto.DeletedAt);
    }

    [Fact]
    public async Task CreateUser_ClientValide_Retourne201()
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Société Support",
            Type = ClientType.INDIVIDUAL,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        DbContext.Clients.Add(client);
        await DbContext.SaveChangesAsync();

        var payload = new CreateUserDto
        {
            ClientId = client.Id,
            Email = "user@example.com",
            PasswordHash = "MotDePasse!123",
            FirstName = "Jean",
            LastName = "Dupont",
            Role = UserRole.MEMBER,
            Status = UserStatus.ACTIVE
        };

        var response = await Client.PostAsJsonAsync("/api/User", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.Created);

        var dto = await response.Content.ReadFromJsonAsync<UserDto>(SerializerOptions);
        Assert.NotNull(dto);
        Assert.Equal(payload.Email, dto!.Email);
        Assert.Equal(payload.ClientId, dto.ClientId);

        var user = await DbContext.Users.SingleAsync(u => u.Id == dto.Id);
        Assert.NotEqual(payload.PasswordHash, user.PasswordHash); // Vérifie le hachage
    }

    [Fact]
    public async Task CreateUser_ClientAbsent_RetourneBadRequest()
    {
        var payload = new CreateUserDto
        {
            ClientId = Guid.NewGuid(),
            Email = "nouveau@example.com",
            PasswordHash = "Pwd!12345",
            FirstName = "Marie",
            LastName = "Martin",
            Role = UserRole.SERVICE,
            Status = UserStatus.ACTIVE
        };

        var response = await Client.PostAsJsonAsync("/api/User", payload, SerializerOptions);
        await AssertStatusCodeAsync(response, HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task UpdateUser_ModifieEmailEtRole()
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Client Update",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
        DbContext.Clients.Add(client);

        var user = new User
        {
            Id = Guid.NewGuid(),
            ClientId = client.Id,
            Email = "ancien@example.com",
            FirstName = "Ancien",
            LastName = "Nom",
            Role = UserRole.MEMBER,
            Status = UserStatus.ACTIVE,
            PasswordHash = "hash",
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
        DbContext.Users.Add(user);
        await DbContext.SaveChangesAsync();

        var payload = new UpdateUserDto
        {
            Email = "nouveau@example.com",
            Role = UserRole.ADMIN
        };

        var response = await Client.PatchAsync($"/api/User/{user.Id}", JsonContent.Create(payload, options: SerializerOptions));
        await AssertStatusCodeAsync(response, HttpStatusCode.OK);

        var dto = await response.Content.ReadFromJsonAsync<UserDto>(SerializerOptions);
        Assert.NotNull(dto);
        Assert.Equal(payload.Email, dto!.Email);
        Assert.Equal(payload.Role, dto.Role);

        ClearChangeTracker();
        var refreshed = await DbContext.Users.FindAsync(user.Id);
        Assert.NotNull(refreshed);
        Assert.Equal(payload.Email, refreshed!.Email);
        Assert.Equal(payload.Role, refreshed.Role);
    }

    [Fact]
    public async Task DeleteUser_SoftDelete()
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Client Delete",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
        DbContext.Clients.Add(client);

        var user = new User
        {
            Id = Guid.NewGuid(),
            ClientId = client.Id,
            Email = "delete@example.com",
            FirstName = "Delete",
            LastName = "User",
            Role = UserRole.MEMBER,
            Status = UserStatus.ACTIVE,
            PasswordHash = "hash",
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
        DbContext.Users.Add(user);
        await DbContext.SaveChangesAsync();

        var response = await Client.DeleteAsync($"/api/User/{user.Id}");
        await AssertStatusCodeAsync(response, HttpStatusCode.NoContent);

        ClearChangeTracker();
        var refreshed = await DbContext.Users.FindAsync(user.Id);
        Assert.NotNull(refreshed);
        Assert.Equal(UserStatus.INACTIVE, refreshed!.Status);
        Assert.NotNull(refreshed.DeletedAt);
    }

    [Fact]
    public async Task GetUser_Inexistant_Retourne404()
    {
        var response = await Client.GetAsync($"/api/User/{Guid.NewGuid()}");
        await AssertStatusCodeAsync(response, HttpStatusCode.NotFound);
    }
}

