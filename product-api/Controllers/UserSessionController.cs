using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using product_api.Data;
using product_api.DTOs.UserSession;
using product_api.Models;

namespace product_api.Controllers;

[ApiController]
[Authorize(Roles = "OWNER,ADMIN,MEMBER")]
[Route("api/[controller]")]
public class UserSessionController : ControllerBase
{
    private readonly VarlorDbContext _context;

    public UserSessionController(VarlorDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<UserSessionDto>>> GetUserSessions(CancellationToken cancellationToken)
    {
        var sessions = await _context.UserSessions
            .AsNoTracking()
            .OrderByDescending(session => session.CreatedAt)
            .Select(session => new UserSessionDto
            {
                Id = session.Id,
                UserId = session.UserId,
                TokenId = session.TokenId,
                IpAddress = session.IpAddress,
                UserAgent = session.UserAgent,
                CreatedAt = session.CreatedAt,
                ExpiresAt = session.ExpiresAt
            })
            .ToListAsync(cancellationToken);

        return Ok(sessions);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<UserSessionDto>> GetUserSession(Guid id, CancellationToken cancellationToken)
    {
        var session = await _context.UserSessions
            .AsNoTracking()
            .FirstOrDefaultAsync(entity => entity.Id == id, cancellationToken);

        if (session is null)
        {
            return NotFound();
        }

        return Ok(MapToDto(session));
    }

    [HttpPost]
    public async Task<ActionResult<UserSessionDto>> CreateUserSession(CreateUserSessionDto dto, CancellationToken cancellationToken)
    {
        if (dto.ExpiresAt <= DateTime.UtcNow)
        {
            ModelState.AddModelError(nameof(dto.ExpiresAt), "La date d'expiration doit être dans le futur.");
            return ValidationProblem(ModelState);
        }

        var userExists = await _context.Users
            .AsNoTracking()
            .AnyAsync(user => user.Id == dto.UserId && user.DeletedAt == null, cancellationToken);

        if (!userExists)
        {
            return BadRequest("Utilisateur associé introuvable ou supprimé.");
        }

        var tokenAlreadyExists = await _context.UserSessions
            .AsNoTracking()
            .AnyAsync(session => session.TokenId == dto.TokenId, cancellationToken);

        if (tokenAlreadyExists)
        {
            return Conflict("Un jeton de session identique existe déjà.");
        }

        var session = new UserSession
        {
            Id = Guid.NewGuid(),
            UserId = dto.UserId,
            TokenId = dto.TokenId,
            IpAddress = dto.IpAddress,
            UserAgent = dto.UserAgent,
            CreatedAt = DateTime.UtcNow,
            ExpiresAt = dto.ExpiresAt
        };

        _context.UserSessions.Add(session);
        await _context.SaveChangesAsync(cancellationToken);

        return CreatedAtAction(nameof(GetUserSession), new { id = session.Id }, MapToDto(session));
    }

    [HttpPatch("{id:guid}")]
    public async Task<ActionResult<UserSessionDto>> UpdateUserSession(Guid id, UpdateUserSessionDto dto, CancellationToken cancellationToken)
    {
        if (dto.UserId is null && dto.TokenId is null && dto.IpAddress is null && dto.UserAgent is null && dto.ExpiresAt is null)
        {
            return BadRequest("Aucune donnée de mise à jour fournie.");
        }

        var session = await _context.UserSessions
            .FirstOrDefaultAsync(entity => entity.Id == id, cancellationToken);

        if (session is null)
        {
            return NotFound();
        }

        if (dto.UserId.HasValue)
        {
            var userExists = await _context.Users
                .AsNoTracking()
                .AnyAsync(user => user.Id == dto.UserId.Value && user.DeletedAt == null, cancellationToken);

            if (!userExists)
            {
                return BadRequest("Utilisateur associé introuvable ou supprimé.");
            }

            session.UserId = dto.UserId.Value;
        }

        if (dto.TokenId is not null)
        {
            var tokenAlreadyExists = await _context.UserSessions
                .AsNoTracking()
                .AnyAsync(entity => entity.TokenId == dto.TokenId && entity.Id != session.Id, cancellationToken);

            if (tokenAlreadyExists)
            {
                return Conflict("Un jeton de session identique existe déjà.");
            }

            session.TokenId = dto.TokenId;
        }

        if (dto.IpAddress is not null)
        {
            session.IpAddress = dto.IpAddress;
        }

        if (dto.UserAgent is not null)
        {
            session.UserAgent = dto.UserAgent;
        }

        if (dto.ExpiresAt.HasValue)
        {
            if (dto.ExpiresAt.Value <= DateTime.UtcNow)
            {
                ModelState.AddModelError(nameof(dto.ExpiresAt), "La date d'expiration doit être dans le futur.");
                return ValidationProblem(ModelState);
            }

            session.ExpiresAt = dto.ExpiresAt.Value;
        }

        await _context.SaveChangesAsync(cancellationToken);

        return Ok(MapToDto(session));
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteUserSession(Guid id, CancellationToken cancellationToken)
    {
        var session = await _context.UserSessions
            .FirstOrDefaultAsync(entity => entity.Id == id, cancellationToken);

        if (session is null)
        {
            return NotFound();
        }

        _context.UserSessions.Remove(session);
        await _context.SaveChangesAsync(cancellationToken);

        return NoContent();
    }

    private static UserSessionDto MapToDto(UserSession session)
    {
        return new UserSessionDto
        {
            Id = session.Id,
            UserId = session.UserId,
            TokenId = session.TokenId,
            IpAddress = session.IpAddress,
            UserAgent = session.UserAgent,
            CreatedAt = session.CreatedAt,
            ExpiresAt = session.ExpiresAt
        };
    }
}
