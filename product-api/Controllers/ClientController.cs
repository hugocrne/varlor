using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using product_api.Data;
using product_api.DTOs.Client;
using product_api.Models;

namespace product_api.Controllers;

/// <summary>
/// Expose les opérations REST pour l’entité Client avec gestion du soft delete.
/// </summary>
[ApiController]
[Authorize(Roles = "OWNER,ADMIN,SERVICE")]
[Route("api/[controller]")]
public class ClientController : ControllerBase
{
    private readonly VarlorDbContext _context;

    /// <summary>
    /// Initialise une nouvelle instance de <see cref="ClientController"/>.
    /// </summary>
    public ClientController(VarlorDbContext context)
    {
        _context = context;
    }

    /// <summary>
    /// Retourne la liste des clients actifs (non supprimés).
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<ClientDto>>> GetClients(CancellationToken cancellationToken)
    {
        var clients = await _context.Clients
            .AsNoTracking()
            .Where(client => client.DeletedAt == null)
            .Select(client => new ClientDto
            {
                Id = client.Id,
                Name = client.Name,
                Type = client.Type,
                Status = client.Status,
                CreatedAt = client.CreatedAt,
                UpdatedAt = client.UpdatedAt,
                DeletedAt = client.DeletedAt
            })
            .ToListAsync(cancellationToken);

        return Ok(clients);
    }

    /// <summary>
    /// Retourne un client par identifiant lorsque celui-ci n’est pas supprimé.
    /// </summary>
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<ClientDto>> GetClient(Guid id, CancellationToken cancellationToken)
    {
        var client = await _context.Clients
            .AsNoTracking()
            .Where(entity => entity.Id == id && entity.DeletedAt == null)
            .Select(entity => new ClientDto
            {
                Id = entity.Id,
                Name = entity.Name,
                Type = entity.Type,
                Status = entity.Status,
                CreatedAt = entity.CreatedAt,
                UpdatedAt = entity.UpdatedAt,
                DeletedAt = entity.DeletedAt
            })
            .FirstOrDefaultAsync(cancellationToken);

        if (client is null)
        {
            return NotFound();
        }

        return Ok(client);
    }

    /// <summary>
    /// Crée un nouveau client et retourne sa représentation.
    /// </summary>
    [HttpPost]
    public async Task<ActionResult<ClientDto>> CreateClient(CreateClientDto dto, CancellationToken cancellationToken)
    {
        var now = DateTime.UtcNow;

        var client = new Client
        {
            Id = Guid.NewGuid(),
            Name = dto.Name,
            Type = dto.Type,
            Status = dto.Status,
            CreatedAt = now,
            UpdatedAt = now,
            DeletedAt = null
        };

        _context.Clients.Add(client);
        await _context.SaveChangesAsync(cancellationToken);

        return CreatedAtAction(nameof(GetClient), new { id = client.Id }, MapToDto(client));
    }

    /// <summary>
    /// Met à jour partiellement un client existant lorsqu’il est actif.
    /// </summary>
    [HttpPatch("{id:guid}")]
    public async Task<ActionResult<ClientDto>> UpdateClient(Guid id, UpdateClientDto dto, CancellationToken cancellationToken)
    {
        if (dto.Name is null && !dto.Type.HasValue && !dto.Status.HasValue)
        {
            return BadRequest("Aucune donnée de mise à jour fournie.");
        }

        var client = await _context.Clients
            .FirstOrDefaultAsync(entity => entity.Id == id && entity.DeletedAt == null, cancellationToken);

        if (client is null)
        {
            return NotFound();
        }

        if (dto.Name is not null)
        {
            client.Name = dto.Name;
        }

        if (dto.Type.HasValue)
        {
            client.Type = dto.Type.Value;
        }

        if (dto.Status.HasValue)
        {
            client.Status = dto.Status.Value;
        }

        client.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        return Ok(MapToDto(client));
    }

    /// <summary>
    /// Marque un client comme inactif et enregistre la date de suppression logique.
    /// </summary>
    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteClient(Guid id, CancellationToken cancellationToken)
    {
        var client = await _context.Clients
            .FirstOrDefaultAsync(entity => entity.Id == id && entity.DeletedAt == null, cancellationToken);

        if (client is null)
        {
            return NotFound();
        }

        client.Status = ClientStatus.INACTIVE;
        client.DeletedAt = DateTime.UtcNow;
        client.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        return NoContent();
    }

    private static ClientDto MapToDto(Client client)
    {
        return new ClientDto
        {
            Id = client.Id,
            Name = client.Name,
            Type = client.Type,
            Status = client.Status,
            CreatedAt = client.CreatedAt,
            UpdatedAt = client.UpdatedAt,
            DeletedAt = client.DeletedAt
        };
    }
}
