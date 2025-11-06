using System;
using System.ComponentModel.DataAnnotations;
using product_api.Models;

namespace product_api.DTOs.UserPreference;

public sealed record class CreateUserPreferenceDto
{
    [Required]
    public Guid UserId { get; init; }

    [Required]
    [EnumDataType(typeof(Theme))]
    public Theme Theme { get; init; }

    [Required]
    [MaxLength(255)]
    public string Language { get; init; } = string.Empty;

    [Required]
    public bool NotificationsEnabled { get; init; }
}
