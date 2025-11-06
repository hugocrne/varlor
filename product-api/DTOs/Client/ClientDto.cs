using System;
using product_api.Models;

namespace product_api.DTOs.Client;

public sealed record class ClientDto
{
    public Guid Id { get; init; }
    public string Name { get; init; } = string.Empty;
    public ClientType Type { get; init; }
    public ClientStatus Status { get; init; }
    public DateTime CreatedAt { get; init; }
    public DateTime UpdatedAt { get; init; }
}
