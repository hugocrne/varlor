using System.ComponentModel.DataAnnotations;
using product_api.Models;

namespace product_api.DTOs.UserPreference;

public sealed record class UpdateUserPreferenceDto
{
    [EnumDataType(typeof(Theme))]
    public Theme? Theme { get; init; }

    [MaxLength(255)]
    public string? Language { get; init; }

    public bool? NotificationsEnabled { get; init; }
}
