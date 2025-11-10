namespace product_api.DTOs.Auth;

public sealed record class LoginResponseDto
{
    public string Token { get; init; } = string.Empty;
    public DateTimeOffset ExpiresAt { get; init; }
}

