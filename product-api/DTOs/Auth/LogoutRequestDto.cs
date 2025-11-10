using System.ComponentModel.DataAnnotations;

namespace product_api.DTOs.Auth;

public sealed record class LogoutRequestDto
{
    [Required]
    [MinLength(32)]
    public string RefreshToken { get; init; } = string.Empty;

    public bool RevokeAllSessions { get; init; }
}

