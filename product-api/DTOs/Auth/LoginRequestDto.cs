using System.ComponentModel.DataAnnotations;

namespace product_api.DTOs.Auth;

public sealed record class LoginRequestDto
{
    [Required]
    [EmailAddress]
    [MaxLength(255)]
    public string Email { get; init; } = string.Empty;

    [Required]
    [MaxLength(255)]
    public string Password { get; init; } = string.Empty;
}

