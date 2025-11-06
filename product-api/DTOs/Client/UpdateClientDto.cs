using System.ComponentModel.DataAnnotations;
using product_api.Models;

namespace product_api.DTOs.Client;

public sealed record class UpdateClientDto
{
    [MaxLength(100)]
    public string? Name { get; init; }

    [EnumDataType(typeof(ClientType))]
    public ClientType? Type { get; init; }

    [EnumDataType(typeof(ClientStatus))]
    public ClientStatus? Status { get; init; }
}
