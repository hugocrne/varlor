using System;

namespace product_api.Models;

public class Client
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public ClientType Type { get; set; }
    public ClientStatus Status { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    // Navigation property for relationship with Users
    public ICollection<User> Users { get; set; } = new List<User>();
}