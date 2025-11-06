using System;
using System.ComponentModel.DataAnnotations;
using product_api.Models;

namespace product_api.DTOs.User;

public sealed record class CreateUserDto
{
    [Required]
    public Guid ClientId { get; init; }

    [Required]
    [EmailAddress]
    [MaxLength(255)]
    public string Email { get; init; } = string.Empty;

    [Required]
    [MaxLength(255)]
    public string PasswordHash { get; init; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string FirstName { get; init; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string LastName { get; init; } = string.Empty;

    [Required]
    [EnumDataType(typeof(UserRole))]
    public UserRole Role { get; init; }

    [Required]
    [EnumDataType(typeof(UserStatus))]
    public UserStatus Status { get; init; }
}
