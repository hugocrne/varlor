using System;
using System.ComponentModel.DataAnnotations;
using product_api.Models;

namespace product_api.DTOs.User;

public sealed record class UpdateUserDto
{
    public Guid? ClientId { get; init; }

    [EmailAddress]
    [MaxLength(255)]
    public string? Email { get; init; }

    [MaxLength(255)]
    public string? PasswordHash { get; init; }

    [MaxLength(100)]
    public string? FirstName { get; init; }

    [MaxLength(100)]
    public string? LastName { get; init; }

    [EnumDataType(typeof(UserRole))]
    public UserRole? Role { get; init; }

    [EnumDataType(typeof(UserStatus))]
    public UserStatus? Status { get; init; }

    public DateTime? LastLoginAt { get; init; }
}
