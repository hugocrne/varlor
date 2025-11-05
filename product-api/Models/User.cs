using System;

namespace product_api.Models;

public class User
{
    public Guid Id { get; set; }
    public Guid ClientId { get; set; }
    public string Email { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public UserRole Role { get; set; }
    public UserStatus Status { get; set; }
    public DateTime? LastLoginAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    // Navigation properties
    public Client Client { get; set; } = null!;
    public UserPreference? UserPreference { get; set; }
    public ICollection<UserSession> UserSessions { get; set; } = new List<UserSession>();
}