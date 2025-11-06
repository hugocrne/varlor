using System;
using System.ComponentModel.DataAnnotations;

namespace product_api.DTOs.UserSession;

public sealed record class UpdateUserSessionDto
{
    public Guid? UserId { get; init; }

    [MaxLength(255)]
    public string? TokenId { get; init; }

    [MaxLength(255)]
    public string? IpAddress { get; init; }

    [MaxLength(255)]
    public string? UserAgent { get; init; }

    public DateTime? ExpiresAt { get; init; }
}
