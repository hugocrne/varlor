using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using product_api.Data;
using product_api.DTOs.User;
using product_api.Models;

namespace product_api.Controllers;

/// <summary>
/// Expose les opérations REST pour l’entité User avec gestion du soft delete et hachage des mots de passe.
/// </summary>
[ApiController]
[Authorize(Roles = "OWNER,ADMIN,MEMBER")]
[Route("api/[controller]")]
public class UserController : ControllerBase
{
    private readonly VarlorDbContext _context;
    private readonly IPasswordHasher<User> _passwordHasher;

    /// <summary>
    /// Initialise une nouvelle instance de <see cref="UserController"/>.
    /// </summary>
    public UserController(VarlorDbContext context, IPasswordHasher<User> passwordHasher)
    {
        _context = context;
        _passwordHasher = passwordHasher;
    }

    /// <summary>
    /// Retourne la liste des utilisateurs actifs (non supprimés).
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<UserDto>>> GetUsers(CancellationToken cancellationToken)
    {
        var users = await _context.Users
            .AsNoTracking()
            .Where(user => user.DeletedAt == null)
            .Select(user => new UserDto
            {
                Id = user.Id,
                ClientId = user.ClientId,
                Email = user.Email,
                FirstName = user.FirstName,
                LastName = user.LastName,
                Role = user.Role,
                Status = user.Status,
                LastLoginAt = user.LastLoginAt,
                CreatedAt = user.CreatedAt,
                UpdatedAt = user.UpdatedAt,
                DeletedAt = user.DeletedAt
            })
            .ToListAsync(cancellationToken);

        return Ok(users);
    }

    /// <summary>
    /// Retourne un utilisateur par identifiant lorsqu’il n’est pas supprimé.
    /// </summary>
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<UserDto>> GetUser(Guid id, CancellationToken cancellationToken)
    {
        var user = await _context.Users
            .AsNoTracking()
            .Where(entity => entity.Id == id && entity.DeletedAt == null)
            .Select(entity => new UserDto
            {
                Id = entity.Id,
                ClientId = entity.ClientId,
                Email = entity.Email,
                FirstName = entity.FirstName,
                LastName = entity.LastName,
                Role = entity.Role,
                Status = entity.Status,
                LastLoginAt = entity.LastLoginAt,
                CreatedAt = entity.CreatedAt,
                UpdatedAt = entity.UpdatedAt,
                DeletedAt = entity.DeletedAt
            })
            .FirstOrDefaultAsync(cancellationToken);

        if (user is null)
        {
            return NotFound();
        }

        return Ok(user);
    }

    /// <summary>
    /// Crée un nouvel utilisateur et retourne sa représentation.
    /// </summary>
    [HttpPost]
    public async Task<ActionResult<UserDto>> CreateUser(CreateUserDto dto, CancellationToken cancellationToken)
    {
        var clientExists = await _context.Clients
            .AnyAsync(client => client.Id == dto.ClientId && client.DeletedAt == null, cancellationToken);

        if (!clientExists)
        {
            return BadRequest("Client associé introuvable ou supprimé.");
        }

        var user = new User
        {
            Id = Guid.NewGuid(),
            ClientId = dto.ClientId,
            Email = dto.Email,
            FirstName = dto.FirstName,
            LastName = dto.LastName,
            Role = dto.Role,
            Status = dto.Status,
            LastLoginAt = null,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow,
            DeletedAt = null
        };

        user.PasswordHash = _passwordHasher.HashPassword(user, dto.PasswordHash);

        _context.Users.Add(user);
        await _context.SaveChangesAsync(cancellationToken);

        return CreatedAtAction(nameof(GetUser), new { id = user.Id }, MapToDto(user));
    }

    /// <summary>
    /// Met à jour partiellement un utilisateur existant lorsqu’il est actif.
    /// </summary>
    [HttpPatch("{id:guid}")]
    public async Task<ActionResult<UserDto>> UpdateUser(Guid id, UpdateUserDto dto, CancellationToken cancellationToken)
    {
        if (dto.ClientId is null &&
            dto.Email is null &&
            dto.PasswordHash is null &&
            dto.FirstName is null &&
            dto.LastName is null &&
            !dto.Role.HasValue &&
            !dto.Status.HasValue &&
            dto.LastLoginAt is null &&
            dto.DeletedAt is null)
        {
            return BadRequest("Aucune donnée de mise à jour fournie.");
        }

        var user = await _context.Users
            .FirstOrDefaultAsync(entity => entity.Id == id && entity.DeletedAt == null, cancellationToken);

        if (user is null)
        {
            return NotFound();
        }

        if (dto.ClientId.HasValue)
        {
            var clientExists = await _context.Clients
                .AnyAsync(client => client.Id == dto.ClientId.Value && client.DeletedAt == null, cancellationToken);

            if (!clientExists)
            {
                return BadRequest("Client associé introuvable ou supprimé.");
            }

            user.ClientId = dto.ClientId.Value;
        }

        if (dto.Email is not null)
        {
            user.Email = dto.Email;
        }

        if (dto.PasswordHash is not null)
        {
            user.PasswordHash = _passwordHasher.HashPassword(user, dto.PasswordHash);
        }

        if (dto.FirstName is not null)
        {
            user.FirstName = dto.FirstName;
        }

        if (dto.LastName is not null)
        {
            user.LastName = dto.LastName;
        }

        if (dto.Role.HasValue)
        {
            user.Role = dto.Role.Value;
        }

        if (dto.Status.HasValue)
        {
            user.Status = dto.Status.Value;
        }

        if (dto.LastLoginAt.HasValue)
        {
            user.LastLoginAt = dto.LastLoginAt.Value;
        }

        user.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        return Ok(MapToDto(user));
    }

    /// <summary>
    /// Marque un utilisateur comme inactif et enregistre la date de suppression logique.
    /// </summary>
    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteUser(Guid id, CancellationToken cancellationToken)
    {
        var user = await _context.Users
            .FirstOrDefaultAsync(entity => entity.Id == id && entity.DeletedAt == null, cancellationToken);

        if (user is null)
        {
            return NotFound();
        }

        user.Status = UserStatus.INACTIVE;
        user.DeletedAt = DateTime.UtcNow;
        user.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        return NoContent();
    }

    private static UserDto MapToDto(User user)
    {
        return new UserDto
        {
            Id = user.Id,
            ClientId = user.ClientId,
            Email = user.Email,
            FirstName = user.FirstName,
            LastName = user.LastName,
            Role = user.Role,
            Status = user.Status,
            LastLoginAt = user.LastLoginAt,
            CreatedAt = user.CreatedAt,
            UpdatedAt = user.UpdatedAt,
            DeletedAt = user.DeletedAt
        };
    }
}
