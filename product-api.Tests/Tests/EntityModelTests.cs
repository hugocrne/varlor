using product_api.Models;
using System;
using Xunit;

namespace product_api.Tests;

public class EntityModelTests
{
    [Fact]
    public void Client_Entity_Should_Have_Correct_Properties_And_Enums()
    {
        // Arrange & Act
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Test Client",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        // Assert
        Assert.NotEqual(Guid.Empty, client.Id);
        Assert.Equal("Test Client", client.Name);
        Assert.Equal(ClientType.COMPANY, client.Type);
        Assert.Equal(ClientStatus.ACTIVE, client.Status);
        Assert.True(client.CreatedAt <= DateTime.UtcNow);
        Assert.True(client.UpdatedAt <= DateTime.UtcNow);
        Assert.NotNull(client.Users);
        Assert.IsType<System.Collections.Generic.List<User>>(client.Users);
    }

    [Fact]
    public void User_Entity_Should_Have_Correct_Relationships_And_Enums()
    {
        // Arrange & Act
        var userId = Guid.NewGuid();
        var clientId = Guid.NewGuid();
        var user = new User
        {
            Id = userId,
            ClientId = clientId,
            Email = "test@example.com",
            PasswordHash = "hashedpassword",
            FirstName = "John",
            LastName = "Doe",
            Role = UserRole.ADMIN,
            Status = UserStatus.ACTIVE,
            LastLoginAt = DateTime.UtcNow,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        // Assert
        Assert.Equal(userId, user.Id);
        Assert.Equal(clientId, user.ClientId);
        Assert.Equal("test@example.com", user.Email);
        Assert.Equal("hashedpassword", user.PasswordHash);
        Assert.Equal("John", user.FirstName);
        Assert.Equal("Doe", user.LastName);
        Assert.Equal(UserRole.ADMIN, user.Role);
        Assert.Equal(UserStatus.ACTIVE, user.Status);
        Assert.True(user.LastLoginAt <= DateTime.UtcNow);
        Assert.Null(user.UserPreference); // UserPreference is nullable and null by default
        Assert.NotNull(user.UserSessions);
        Assert.IsType<System.Collections.Generic.List<UserSession>>(user.UserSessions);
    }

    [Fact]
    public void UserPreference_Entity_Should_Have_Correct_Properties()
    {
        // Arrange & Act
        var preferenceId = Guid.NewGuid();
        var userId = Guid.NewGuid();
        var preference = new UserPreference
        {
            Id = preferenceId,
            UserId = userId,
            Theme = Theme.DARK,
            Language = "en",
            NotificationsEnabled = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        // Assert
        Assert.Equal(preferenceId, preference.Id);
        Assert.Equal(userId, preference.UserId);
        Assert.Equal(Theme.DARK, preference.Theme);
        Assert.Equal("en", preference.Language);
        Assert.True(preference.NotificationsEnabled);
        Assert.True(preference.CreatedAt <= DateTime.UtcNow);
        Assert.True(preference.UpdatedAt <= DateTime.UtcNow);
    }

    [Fact]
    public void UserSession_Entity_Should_Have_Correct_Properties()
    {
        // Arrange & Act
        var sessionId = Guid.NewGuid();
        var userId = Guid.NewGuid();
        var tokenId = Guid.NewGuid();
        var session = new UserSession
        {
            Id = sessionId,
            UserId = userId,
            TokenId = tokenId.ToString(),
            IpAddress = "192.168.1.1",
            UserAgent = "Mozilla/5.0 (Test Browser)",
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = DateTime.UtcNow.AddHours(1)
        };

        // Assert
        Assert.Equal(sessionId, session.Id);
        Assert.Equal(userId, session.UserId);
        Assert.Equal(tokenId.ToString(), session.TokenId);
        Assert.Equal("192.168.1.1", session.IpAddress);
        Assert.Equal("Mozilla/5.0 (Test Browser)", session.UserAgent);
        Assert.True(session.CreatedAt <= DateTime.UtcNow);
        Assert.True(session.ExpiresAt > session.CreatedAt);
    }
}