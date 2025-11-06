using System;
using product_api.Models;

namespace product_api.DTOs.User;

public sealed record class UserDto
{
    public Guid Id { get; init; }
    public Guid ClientId { get; init; }
    public string Email { get; init; } = string.Empty;
    public string FirstName { get; init; } = string.Empty;
    public string LastName { get; init; } = string.Empty;
    public UserRole Role { get; init; }
    public UserStatus Status { get; init; }
    public DateTime? LastLoginAt { get; init; }
    public DateTime CreatedAt { get; init; }
    public DateTime UpdatedAt { get; init; }
}
