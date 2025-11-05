using System;

namespace product_api.Models;

public class UserPreference
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public Theme Theme { get; set; }
    public string Language { get; set; } = string.Empty;
    public bool NotificationsEnabled { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    // Navigation property
    public User User { get; set; } = null!;
}