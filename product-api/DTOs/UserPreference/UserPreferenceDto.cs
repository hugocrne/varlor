using System;
using product_api.Models;

namespace product_api.DTOs.UserPreference;

public sealed record class UserPreferenceDto
{
    public Guid Id { get; init; }
    public Guid UserId { get; init; }
    public Theme Theme { get; init; }
    public string Language { get; init; } = string.Empty;
    public bool NotificationsEnabled { get; init; }
    public DateTime CreatedAt { get; init; }
    public DateTime UpdatedAt { get; init; }
}
