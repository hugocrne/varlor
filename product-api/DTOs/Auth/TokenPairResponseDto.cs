namespace product_api.DTOs.Auth;

public record class TokenPairResponseDto
{
    public string AccessToken { get; init; } = string.Empty;
    public string RefreshToken { get; init; } = string.Empty;
    public DateTimeOffset ExpiresAt { get; init; }
    public DateTimeOffset RefreshExpiresAt { get; init; }
}

