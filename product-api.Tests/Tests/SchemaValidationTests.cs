using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;
using product_api.Data;
using product_api.Models;
using System;
using System.Linq;
using Xunit;

namespace product_api.Tests;

public class SchemaValidationTests : IClassFixture<DatabaseFixture>
{
    private readonly DatabaseFixture _fixture;

    public SchemaValidationTests(DatabaseFixture fixture)
    {
        _fixture = fixture;
    }

    [Fact]
    public void Client_Entity_Mapping_Should_Match_Database_Schema()
    {
        // Arrange
        using var dbContext = _fixture.CreateDbContext();

        // Act
        var clientEntity = dbContext.Model.FindEntityType(typeof(Client));

        // Assert
        Assert.NotNull(clientEntity);
        Assert.Equal("clients", clientEntity.GetTableName());

        // Verify primary key
        var pk = clientEntity.FindPrimaryKey();
        Assert.NotNull(pk);
        Assert.Equal("id", pk.Properties.Single().GetColumnName());

        // Verify column mappings
        var properties = clientEntity.GetProperties().ToDictionary(p => p.GetColumnName(), p => p);

        Assert.True(properties.ContainsKey("id"));
        Assert.True(properties.ContainsKey("name"));
        Assert.True(properties.ContainsKey("type"));
        Assert.True(properties.ContainsKey("status"));
        Assert.True(properties.ContainsKey("created_at"));
        Assert.True(properties.ContainsKey("updated_at"));

        // Verify column types and constraints
        Assert.Equal("id", properties["id"].GetColumnName());
        Assert.Equal("name", properties["name"].GetColumnName());
        Assert.Equal("type", properties["type"].GetColumnName());
        Assert.Equal("status", properties["status"].GetColumnName());
        Assert.Equal("created_at", properties["created_at"].GetColumnName());
        Assert.Equal("updated_at", properties["updated_at"].GetColumnName());

        // Verify type conversions for enums
        Assert.True(properties["type"].GetProviderClrType() == typeof(string));
        Assert.True(properties["status"].GetProviderClrType() == typeof(string));
    }

    [Fact]
    public void User_Entity_Mapping_Should_Match_Database_Schema()
    {
        // Arrange
        using var dbContext = _fixture.CreateDbContext();

        // Act
        var userEntity = dbContext.Model.FindEntityType(typeof(User));

        // Assert
        Assert.NotNull(userEntity);
        Assert.Equal("users", userEntity.GetTableName());

        // Verify primary key
        var pk = userEntity.FindPrimaryKey();
        Assert.NotNull(pk);
        Assert.Equal("id", pk.Properties.Single().GetColumnName());

        // Verify column mappings
        var properties = userEntity.GetProperties().ToDictionary(p => p.GetColumnName(), p => p);

        Assert.True(properties.ContainsKey("id"));
        Assert.True(properties.ContainsKey("client_id"));
        Assert.True(properties.ContainsKey("email"));
        Assert.True(properties.ContainsKey("password_hash"));
        Assert.True(properties.ContainsKey("first_name"));
        Assert.True(properties.ContainsKey("last_name"));
        Assert.True(properties.ContainsKey("role"));
        Assert.True(properties.ContainsKey("status"));
        Assert.True(properties.ContainsKey("last_login_at"));
        Assert.True(properties.ContainsKey("created_at"));
        Assert.True(properties.ContainsKey("updated_at"));

        // Verify foreign key
        var clientFk = userEntity.GetForeignKeys().FirstOrDefault(fk => fk.PrincipalEntityType.Name == "Client");
        Assert.NotNull(clientFk);
        Assert.Equal("client_id", clientFk.Properties.Single().GetColumnName());

        // Verify type conversions for enums
        Assert.True(properties["role"].GetProviderClrType() == typeof(string));
        Assert.True(properties["status"].GetProviderClrType() == typeof(string));
    }

    [Fact]
    public void UserPreference_Entity_Mapping_Should_Match_Database_Schema()
    {
        // Arrange
        using var dbContext = _fixture.CreateDbContext();

        // Act
        var userPreferenceEntity = dbContext.Model.FindEntityType(typeof(UserPreference));

        // Assert
        Assert.NotNull(userPreferenceEntity);
        Assert.Equal("user_preferences", userPreferenceEntity.GetTableName());

        // Verify primary key
        var pk = userPreferenceEntity.FindPrimaryKey();
        Assert.NotNull(pk);
        Assert.Equal("id", pk.Properties.Single().GetColumnName());

        // Verify column mappings
        var properties = userPreferenceEntity.GetProperties().ToDictionary(p => p.GetColumnName(), p => p);

        Assert.True(properties.ContainsKey("id"));
        Assert.True(properties.ContainsKey("user_id"));
        Assert.True(properties.ContainsKey("theme"));
        Assert.True(properties.ContainsKey("language"));
        Assert.True(properties.ContainsKey("notifications_enabled"));
        Assert.True(properties.ContainsKey("created_at"));
        Assert.True(properties.ContainsKey("updated_at"));

        // Verify foreign key to User
        var userFk = userPreferenceEntity.GetForeignKeys().FirstOrDefault(fk => fk.PrincipalEntityType.Name == "User");
        Assert.NotNull(userFk);
        Assert.Equal("user_id", userFk.Properties.Single().GetColumnName());

        // Verify type conversion for enum
        Assert.True(properties["theme"].GetProviderClrType() == typeof(string));

        // Verify boolean type
        Assert.True(properties["notifications_enabled"].GetProviderClrType() == typeof(bool));
    }

    [Fact]
    public void UserSession_Entity_Mapping_Should_Match_Database_Schema()
    {
        // Arrange
        using var dbContext = _fixture.CreateDbContext();

        // Act
        var userSessionEntity = dbContext.Model.FindEntityType(typeof(UserSession));

        // Assert
        Assert.NotNull(userSessionEntity);
        Assert.Equal("user_sessions", userSessionEntity.GetTableName());

        // Verify primary key
        var pk = userSessionEntity.FindPrimaryKey();
        Assert.NotNull(pk);
        Assert.Equal("id", pk.Properties.Single().GetColumnName());

        // Verify column mappings
        var properties = userSessionEntity.GetProperties().ToDictionary(p => p.GetColumnName(), p => p);

        Assert.True(properties.ContainsKey("id"));
        Assert.True(properties.ContainsKey("user_id"));
        Assert.True(properties.ContainsKey("token_id"));
        Assert.True(properties.ContainsKey("ip_address"));
        Assert.True(properties.ContainsKey("user_agent"));
        Assert.True(properties.ContainsKey("created_at"));
        Assert.True(properties.ContainsKey("expires_at"));

        // Verify foreign key to User
        var userFk = userSessionEntity.GetForeignKeys().FirstOrDefault(fk => fk.PrincipalEntityType.Name == "User");
        Assert.NotNull(userFk);
        Assert.Equal("user_id", userFk.Properties.Single().GetColumnName());
    }

    [Fact]
    public void ENUM_Values_Should_Match_Database_Definitions()
    {
        // Verify ClientType enum values
        var clientTypeValues = Enum.GetValues(typeof(ClientType)).Cast<ClientType>();
        Assert.Contains(ClientType.INDIVIDUAL, clientTypeValues);
        Assert.Contains(ClientType.COMPANY, clientTypeValues);
        Assert.Equal(2, clientTypeValues.Count());

        // Verify ClientStatus enum values
        var clientStatusValues = Enum.GetValues(typeof(ClientStatus)).Cast<ClientStatus>();
        Assert.Contains(ClientStatus.ACTIVE, clientStatusValues);
        Assert.Contains(ClientStatus.INACTIVE, clientStatusValues);
        Assert.Contains(ClientStatus.SUSPENDED, clientStatusValues);
        Assert.Contains(ClientStatus.PENDING, clientStatusValues);
        Assert.Equal(4, clientStatusValues.Count());

        // Verify UserRole enum values
        var userRoleValues = Enum.GetValues(typeof(UserRole)).Cast<UserRole>();
        Assert.Contains(UserRole.OWNER, userRoleValues);
        Assert.Contains(UserRole.ADMIN, userRoleValues);
        Assert.Contains(UserRole.MEMBER, userRoleValues);
        Assert.Contains(UserRole.SERVICE, userRoleValues);
        Assert.Equal(4, userRoleValues.Count());

        // Verify UserStatus enum values
        var userStatusValues = Enum.GetValues(typeof(UserStatus)).Cast<UserStatus>();
        Assert.Contains(UserStatus.ACTIVE, userStatusValues);
        Assert.Contains(UserStatus.INACTIVE, userStatusValues);
        Assert.Contains(UserStatus.SUSPENDED, userStatusValues);
        Assert.Contains(UserStatus.PENDING, userStatusValues);
        Assert.Equal(4, userStatusValues.Count());

        // Verify Theme enum values
        var themeValues = Enum.GetValues(typeof(Theme)).Cast<Theme>();
        Assert.Contains(Theme.LIGHT, themeValues);
        Assert.Contains(Theme.DARK, themeValues);
        Assert.Contains(Theme.SYSTEM, themeValues);
        Assert.Equal(3, themeValues.Count());
    }

    [Fact]
    public void Timestamp_Fields_Should_Be_Properly_Configured()
    {
        // Arrange
        using var dbContext = _fixture.CreateDbContext();

        // Act & Assert - Client timestamps
        var clientEntity = dbContext.Model.FindEntityType(typeof(Client));
        var clientProperties = clientEntity!.GetProperties().ToDictionary(p => p.GetColumnName(), p => p);

        Assert.Equal("timestamp", clientProperties["created_at"].GetColumnType());
        Assert.Equal("timestamp", clientProperties["updated_at"].GetColumnType());

        // Act & Assert - User timestamps
        var userEntity = dbContext.Model.FindEntityType(typeof(User));
        var userProperties = userEntity!.GetProperties().ToDictionary(p => p.GetColumnName(), p => p);

        Assert.Equal("timestamp", userProperties["created_at"].GetColumnType());
        Assert.Equal("timestamp", userProperties["updated_at"].GetColumnType());
        Assert.Equal("timestamp", userProperties["last_login_at"].GetColumnType());

        // Act & Assert - UserPreference timestamps
        var userPrefEntity = dbContext.Model.FindEntityType(typeof(UserPreference));
        var userPrefProperties = userPrefEntity!.GetProperties().ToDictionary(p => p.GetColumnName(), p => p);

        Assert.Equal("timestamp", userPrefProperties["created_at"].GetColumnType());
        Assert.Equal("timestamp", userPrefProperties["updated_at"].GetColumnType());

        // Act & Assert - UserSession timestamps
        var userSessionEntity = dbContext.Model.FindEntityType(typeof(UserSession));
        var userSessionProperties = userSessionEntity!.GetProperties().ToDictionary(p => p.GetColumnName(), p => p);

        Assert.Equal("timestamp", userSessionProperties["created_at"].GetColumnType());
        Assert.Equal("timestamp", userSessionProperties["expires_at"].GetColumnType());
    }

    [Fact]
    public void Relationships_Should_Be_Configured_Correctly()
    {
        // Arrange
        using var dbContext = _fixture.CreateDbContext();

        // Act & Assert - Client to Users relationship
        var clientEntity = dbContext.Model.FindEntityType(typeof(Client));
        var clientToUsersFk = clientEntity!.GetForeignKeys().FirstOrDefault();
        Assert.Null(clientToUsersFk); // Client is the principal in the relationship

        var userEntity = dbContext.Model.FindEntityType(typeof(User));
        var userToClientFk = userEntity!.GetForeignKeys().FirstOrDefault(fk => fk.PrincipalEntityType.Name == "Client");
        Assert.NotNull(userToClientFk);
        Assert.Equal("client_id", userToClientFk.Properties.Single().GetColumnName());
        Assert.Equal(DeleteBehavior.Cascade, userToClientFk.DeleteBehavior);

        // Act & Assert - User to UserPreference relationship
        var userPreferenceEntity = dbContext.Model.FindEntityType(typeof(UserPreference));
        var prefToUserFk = userPreferenceEntity!.GetForeignKeys().FirstOrDefault(fk => fk.PrincipalEntityType.Name == "User");
        Assert.NotNull(prefToUserFk);
        Assert.Equal("user_id", prefToUserFk.Properties.Single().GetColumnName());
        Assert.Equal(DeleteBehavior.Cascade, prefToUserFk.DeleteBehavior);

        // Act & Assert - User to UserSessions relationship
        var userSessionEntity = dbContext.Model.FindEntityType(typeof(UserSession));
        var sessionToUserFk = userSessionEntity!.GetForeignKeys().FirstOrDefault(fk => fk.PrincipalEntityType.Name == "User");
        Assert.NotNull(sessionToUserFk);
        Assert.Equal("user_id", sessionToUserFk.Properties.Single().GetColumnName());
        Assert.Equal(DeleteBehavior.Cascade, sessionToUserFk.DeleteBehavior);
    }
}