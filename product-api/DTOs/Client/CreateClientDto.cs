using System.ComponentModel.DataAnnotations;
using product_api.Models;

namespace product_api.DTOs.Client;

public sealed record class CreateClientDto
{
    [Required]
    [MaxLength(100)]
    public string Name { get; init; } = string.Empty;

    [Required]
    [EnumDataType(typeof(ClientType))]
    public ClientType Type { get; init; }

    [Required]
    [EnumDataType(typeof(ClientStatus))]
    public ClientStatus Status { get; init; }
}
