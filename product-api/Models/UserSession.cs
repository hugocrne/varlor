using System;

namespace product_api.Models;

public class UserSession
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public string TokenId { get; set; } = string.Empty;
    public string TokenHash { get; set; } = string.Empty;
    public string IpAddress { get; set; } = string.Empty;
    public string UserAgent { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime ExpiresAt { get; set; }
    public DateTime? RevokedAt { get; set; }
    public string? ReplacedByTokenId { get; set; }
    public string? RevocationReason { get; set; }

    // Navigation property
    public User User { get; set; } = null!;
}