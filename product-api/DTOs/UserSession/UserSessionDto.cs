using System;

namespace product_api.DTOs.UserSession;

public sealed record class UserSessionDto
{
    public Guid Id { get; init; }
    public Guid UserId { get; init; }
    public string TokenId { get; init; } = string.Empty;
    public string IpAddress { get; init; } = string.Empty;
    public string UserAgent { get; init; } = string.Empty;
    public DateTime CreatedAt { get; init; }
    public DateTime ExpiresAt { get; init; }
    public DateTime? RevokedAt { get; init; }
    public string? ReplacedByTokenId { get; init; }
    public string? RevocationReason { get; init; }
}
