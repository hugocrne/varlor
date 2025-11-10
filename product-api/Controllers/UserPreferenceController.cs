using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using product_api.Data;
using product_api.DTOs.UserPreference;
using product_api.Models;

namespace product_api.Controllers;

[ApiController]
[Authorize(Roles = "OWNER,ADMIN,MEMBER")]
[Route("api/[controller]")]
public class UserPreferenceController : ControllerBase
{
    private readonly VarlorDbContext _context;

    public UserPreferenceController(VarlorDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<UserPreferenceDto>>> GetUserPreferences(CancellationToken cancellationToken)
    {
        var preferences = await _context.UserPreferences
            .AsNoTracking()
            .OrderBy(preference => preference.CreatedAt)
            .Select(preference => new UserPreferenceDto
            {
                Id = preference.Id,
                UserId = preference.UserId,
                Theme = preference.Theme,
                Language = preference.Language,
                NotificationsEnabled = preference.NotificationsEnabled,
                CreatedAt = preference.CreatedAt,
                UpdatedAt = preference.UpdatedAt
            })
            .ToListAsync(cancellationToken);

        return Ok(preferences);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<UserPreferenceDto>> GetUserPreference(Guid id, CancellationToken cancellationToken)
    {
        var preference = await _context.UserPreferences
            .AsNoTracking()
            .FirstOrDefaultAsync(entity => entity.Id == id, cancellationToken);

        if (preference is null)
        {
            return NotFound();
        }

        return Ok(MapToDto(preference));
    }

    [HttpPost]
    public async Task<ActionResult<UserPreferenceDto>> CreateUserPreference(CreateUserPreferenceDto dto, CancellationToken cancellationToken)
    {
        var userExists = await _context.Users
            .AsNoTracking()
            .AnyAsync(user => user.Id == dto.UserId && user.DeletedAt == null, cancellationToken);

        if (!userExists)
        {
            return BadRequest("Utilisateur associé introuvable ou supprimé.");
        }

        var preferenceAlreadyExists = await _context.UserPreferences
            .AsNoTracking()
            .AnyAsync(entity => entity.UserId == dto.UserId, cancellationToken);

        if (preferenceAlreadyExists)
        {
            return Conflict("Une préférence utilisateur existe déjà pour cet utilisateur.");
        }

        var preference = new UserPreference
        {
            Id = Guid.NewGuid(),
            UserId = dto.UserId,
            Theme = dto.Theme,
            Language = dto.Language,
            NotificationsEnabled = dto.NotificationsEnabled,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.UserPreferences.Add(preference);
        await _context.SaveChangesAsync(cancellationToken);

        return CreatedAtAction(nameof(GetUserPreference), new { id = preference.Id }, MapToDto(preference));
    }

    [HttpPatch("{id:guid}")]
    public async Task<ActionResult<UserPreferenceDto>> UpdateUserPreference(Guid id, UpdateUserPreferenceDto dto, CancellationToken cancellationToken)
    {
        if (dto.Theme is null && dto.Language is null && !dto.NotificationsEnabled.HasValue)
        {
            return BadRequest("Aucune donnée de mise à jour fournie.");
        }

        var preference = await _context.UserPreferences
            .FirstOrDefaultAsync(entity => entity.Id == id, cancellationToken);

        if (preference is null)
        {
            return NotFound();
        }

        if (dto.Theme.HasValue)
        {
            preference.Theme = dto.Theme.Value;
        }

        if (dto.Language is not null)
        {
            preference.Language = dto.Language;
        }

        if (dto.NotificationsEnabled.HasValue)
        {
            preference.NotificationsEnabled = dto.NotificationsEnabled.Value;
        }

        preference.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        return Ok(MapToDto(preference));
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteUserPreference(Guid id, CancellationToken cancellationToken)
    {
        var preference = await _context.UserPreferences
            .FirstOrDefaultAsync(entity => entity.Id == id, cancellationToken);

        if (preference is null)
        {
            return NotFound();
        }

        _context.UserPreferences.Remove(preference);
        await _context.SaveChangesAsync(cancellationToken);

        return NoContent();
    }

    private static UserPreferenceDto MapToDto(UserPreference preference)
    {
        return new UserPreferenceDto
        {
            Id = preference.Id,
            UserId = preference.UserId,
            Theme = preference.Theme,
            Language = preference.Language,
            NotificationsEnabled = preference.NotificationsEnabled,
            CreatedAt = preference.CreatedAt,
            UpdatedAt = preference.UpdatedAt
        };
    }
}
