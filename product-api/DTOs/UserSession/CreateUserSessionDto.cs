using System;
using System.ComponentModel.DataAnnotations;

namespace product_api.DTOs.UserSession;

public sealed record class CreateUserSessionDto
{
    [Required]
    public Guid UserId { get; init; }

    [Required]
    [MaxLength(255)]
    public string TokenId { get; init; } = string.Empty;

    [Required]
    [MaxLength(128)]
    public string TokenHash { get; init; } = string.Empty;

    [Required]
    [MaxLength(255)]
    public string IpAddress { get; init; } = string.Empty;

    [Required]
    [MaxLength(255)]
    public string UserAgent { get; init; } = string.Empty;

    [Required]
    public DateTime ExpiresAt { get; init; }
}
