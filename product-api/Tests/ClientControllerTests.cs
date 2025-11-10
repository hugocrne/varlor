using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http.Json;
using product_api.DTOs.Client;
using product_api.Models;
using Xunit;

namespace product_api.Tests;

public class ClientControllerTests : IntegrationTestBase
{
    [Fact]
    public async Task GetClients_RetourneClientsActifs()
    {
        var activeClient = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Active Corp",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        var deletedClient = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Deleted Corp",
            Type = ClientType.INDIVIDUAL,
            Status = ClientStatus.INACTIVE,
            CreatedAt = DateTime.UtcNow.AddDays(-2),
            UpdatedAt = DateTime.UtcNow.AddDays(-1),
            DeletedAt = DateTime.UtcNow.AddHours(-12)
        };

        DbContext.Clients.AddRange(activeClient, deletedClient);
        await DbContext.SaveChangesAsync();

        var response = await Client.GetAsync("/api/Client");

        await AssertStatusCodeAsync(response, HttpStatusCode.OK);

        var clients = await response.Content.ReadFromJsonAsync<List<ClientDto>>(SerializerOptions);
        Assert.NotNull(clients);
        Assert.Single(clients!);
        var client = clients[0];
        Assert.Equal(activeClient.Id, client.Id);
        Assert.Equal(activeClient.Name, client.Name);
        Assert.Equal(activeClient.Type, client.Type);
        Assert.Equal(activeClient.Status, client.Status);
        Assert.Null(client.DeletedAt);
    }

    [Fact]
    public async Task CreateClient_RetourneClientCree()
    {
        var payload = new CreateClientDto
        {
            Name = "Nouvelle Société",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE
        };

        var response = await Client.PostAsJsonAsync("/api/Client", payload, SerializerOptions);

        await AssertStatusCodeAsync(response, HttpStatusCode.Created);

        var dto = await response.Content.ReadFromJsonAsync<ClientDto>(SerializerOptions);
        Assert.NotNull(dto);
        Assert.Equal(payload.Name, dto!.Name);
        Assert.Equal(payload.Type, dto.Type);
        Assert.Equal(payload.Status, dto.Status);

        ClearChangeTracker();
        var client = await DbContext.Clients.FindAsync(dto.Id);
        Assert.NotNull(client);
        Assert.Equal(ClientStatus.ACTIVE, client!.Status);
        Assert.Null(client.DeletedAt);
    }

    [Fact]
    public async Task UpdateClient_SansPayload_RetourneBadRequest()
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Client Modifiable",
            Type = ClientType.COMPANY,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        DbContext.Clients.Add(client);
        await DbContext.SaveChangesAsync();

        var response = await Client.PatchAsync($"/api/Client/{client.Id}", JsonContent.Create(new { }, options: SerializerOptions));

        await AssertStatusCodeAsync(response, HttpStatusCode.BadRequest);
    }

    [Fact]
    public async Task DeleteClient_Active_SoftDelete()
    {
        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = "Client à supprimer",
            Type = ClientType.INDIVIDUAL,
            Status = ClientStatus.ACTIVE,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        DbContext.Clients.Add(client);
        await DbContext.SaveChangesAsync();

        var response = await Client.DeleteAsync($"/api/Client/{client.Id}");

        await AssertStatusCodeAsync(response, HttpStatusCode.NoContent);

        ClearChangeTracker();
        var refreshed = await DbContext.Clients.FindAsync(client.Id);
        Assert.NotNull(refreshed);
        Assert.Equal(ClientStatus.INACTIVE, refreshed!.Status);
        Assert.NotNull(refreshed.DeletedAt);
    }

    [Fact]
    public async Task GetClient_Inexistant_RetourneNotFound()
    {
        var response = await Client.GetAsync($"/api/Client/{Guid.NewGuid()}");
        await AssertStatusCodeAsync(response, HttpStatusCode.NotFound);
    }
}

