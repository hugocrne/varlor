using System.Collections.Generic;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using System.Linq;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using product_api.Data;
using product_api.DTOs.Auth;
using product_api.Models;

namespace product_api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private static readonly TimeSpan AccessTokenLifetime = TimeSpan.FromMinutes(15);
    private static readonly TimeSpan RefreshTokenLifetime = TimeSpan.FromDays(14);
    private const int RefreshTokenByteLength = 64;
    private const string RevocationReasonUserLogout = "LOGOUT";
    private const string RevocationReasonRotation = "ROTATED";

    private readonly VarlorDbContext _context;
    private readonly IConfiguration _configuration;
    private readonly IPasswordHasher<User> _passwordHasher;
    private readonly ILogger<AuthController> _logger;

    public AuthController(
        VarlorDbContext context,
        IConfiguration configuration,
        IPasswordHasher<User> passwordHasher,
        ILogger<AuthController> logger)
    {
        _context = context ?? throw new ArgumentNullException(nameof(context));
        _configuration = configuration ?? throw new ArgumentNullException(nameof(configuration));
        _passwordHasher = passwordHasher ?? throw new ArgumentNullException(nameof(passwordHasher));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Authentifie un utilisateur et retourne un jeton JWT signé.
    /// </summary>
    /// <param name="request">Identifiants de connexion.</param>
    /// <param name="cancellationToken">Jeton d'annulation.</param>
    [HttpPost("login")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(LoginResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<ActionResult<LoginResponseDto>> Login(
        [FromBody] LoginRequestDto request,
        CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var user = await _context.Users
            .FirstOrDefaultAsync(
                entity => entity.Email == request.Email && entity.DeletedAt == null,
                cancellationToken);

        if (user is null)
        {
            return NotFound("Utilisateur introuvable.");
        }

        var verificationResult = _passwordHasher.VerifyHashedPassword(user, user.PasswordHash, request.Password);
        if (verificationResult == PasswordVerificationResult.Failed)
        {
            return Unauthorized("Identifiants invalides.");
        }

        if (verificationResult == PasswordVerificationResult.SuccessRehashNeeded)
        {
            user.PasswordHash = _passwordHasher.HashPassword(user, request.Password);
        }

        if (user.Status != UserStatus.ACTIVE)
        {
            return Unauthorized("Le compte utilisateur est inactif.");
        }

        await CleanupExpiredSessionsAsync(user.Id, cancellationToken);

        var now = DateTime.UtcNow;
        var ipAddress = HttpContext.Connection.RemoteIpAddress?.ToString() ?? string.Empty;
        var userAgent = Request.Headers["User-Agent"].ToString();

        var tokenPair = CreateTokenPair(user, ipAddress, userAgent, now);

        user.LastLoginAt = now;
        user.UpdatedAt = now;

        try
        {
            await _context.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException exception)
        {
            _logger.LogError(exception, "Échec lors de la persistance de la session pour l'utilisateur {UserId}", user.Id);
            return StatusCode(StatusCodes.Status500InternalServerError, "Échec de la création de la session.");
        }

        var response = new LoginResponseDto
        {
            AccessToken = tokenPair.AccessToken,
            RefreshToken = tokenPair.RefreshToken,
            ExpiresAt = tokenPair.ExpiresAt,
            RefreshExpiresAt = tokenPair.RefreshExpiresAt
        };

        return Ok(response);
    }

    /// <summary>
    /// Permet de régénérer un couple de jetons à partir d'un refresh token valide.
    /// </summary>
    /// <param name="request">Refresh token à échanger.</param>
    /// <param name="cancellationToken">Jeton d'annulation.</param>
    [HttpPost("refresh")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(TokenPairResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<ActionResult<TokenPairResponseDto>> Refresh(
        [FromBody] RefreshTokenRequestDto request,
        CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var tokenHash = HashRefreshToken(request.RefreshToken);

        var session = await _context.UserSessions
            .Include(entity => entity.User)
            .FirstOrDefaultAsync(
                entity => entity.TokenHash == tokenHash,
                cancellationToken);

        if (session is null)
        {
            return Unauthorized("Refresh token invalide.");
        }

        if (session.RevokedAt is not null || session.ExpiresAt <= DateTime.UtcNow)
        {
            return Unauthorized("Refresh token expiré ou révoqué.");
        }

        if (session.User.DeletedAt is not null || session.User.Status != UserStatus.ACTIVE)
        {
            return Unauthorized("Utilisateur invalide.");
        }

        await CleanupExpiredSessionsAsync(session.UserId, cancellationToken);

        var now = DateTime.UtcNow;
        var ipAddress = HttpContext.Connection.RemoteIpAddress?.ToString() ?? string.Empty;
        var userAgent = Request.Headers["User-Agent"].ToString();

        var tokenPair = CreateTokenPair(session.User, ipAddress, userAgent, now, session);

        try
        {
            await _context.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException exception)
        {
            _logger.LogError(exception, "Échec lors de la rotation du refresh token pour la session {SessionId}", session.Id);
            return StatusCode(StatusCodes.Status500InternalServerError, "Échec de la rotation du refresh token.");
        }

        return Ok(tokenPair);
    }

    /// <summary>
    /// Révoque le refresh token courant (et optionnellement toutes les sessions actives de l'utilisateur).
    /// </summary>
    /// <param name="request">Refresh token à révoquer.</param>
    /// <param name="cancellationToken">Jeton d'annulation.</param>
    [HttpPost("logout")]
    [AllowAnonymous]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Logout(
        [FromBody] LogoutRequestDto request,
        CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var tokenHash = HashRefreshToken(request.RefreshToken);

        var session = await _context.UserSessions
            .FirstOrDefaultAsync(
                entity => entity.TokenHash == tokenHash,
                cancellationToken);

        if (session is null)
        {
            return NoContent();
        }

        var now = DateTime.UtcNow;

        if (request.RevokeAllSessions)
        {
            var sessions = await _context.UserSessions
                .Where(entity => entity.UserId == session.UserId && entity.RevokedAt == null)
                .ToListAsync(cancellationToken);

            foreach (var userSession in sessions)
            {
                userSession.RevokedAt = now;
                userSession.RevocationReason = RevocationReasonUserLogout;
                userSession.ReplacedByTokenId = null;
            }
        }
        else
        {
            if (session.RevokedAt is null)
            {
                session.RevokedAt = now;
                session.RevocationReason = RevocationReasonUserLogout;
                session.ReplacedByTokenId = null;
            }
        }

        try
        {
            await _context.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException exception)
        {
            _logger.LogError(exception, "Échec lors de la révocation de la session {SessionId}", session.Id);
            return StatusCode(StatusCodes.Status500InternalServerError, "Échec de la révocation du refresh token.");
        }

        return NoContent();
    }

    private TokenPairResponseDto CreateTokenPair(
        User user,
        string ipAddress,
        string userAgent,
        DateTime utcNow,
        UserSession? sessionToRevoke = null)
    {
        var issuedAt = new DateTimeOffset(utcNow, TimeSpan.Zero);
        var accessTokenExpiresAt = issuedAt.Add(AccessTokenLifetime);
        var refreshTokenExpiresAt = issuedAt.Add(RefreshTokenLifetime);
        var refreshToken = GenerateRefreshToken();
        var refreshTokenHash = HashRefreshToken(refreshToken);
        var newTokenId = Guid.NewGuid().ToString("N");

        var session = new UserSession
        {
            Id = Guid.NewGuid(),
            UserId = user.Id,
            TokenId = newTokenId,
            TokenHash = refreshTokenHash,
            IpAddress = Truncate(ipAddress, 45),
            UserAgent = Truncate(string.IsNullOrWhiteSpace(userAgent) ? "unknown" : userAgent, 500),
            CreatedAt = utcNow,
            ExpiresAt = refreshTokenExpiresAt.UtcDateTime
        };

        if (sessionToRevoke is not null)
        {
            sessionToRevoke.RevokedAt = utcNow;
            sessionToRevoke.RevocationReason = RevocationReasonRotation;
            sessionToRevoke.ReplacedByTokenId = newTokenId;
        }

        _context.UserSessions.Add(session);

        var accessToken = GenerateAccessToken(user, accessTokenExpiresAt);

        return new TokenPairResponseDto
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken,
            ExpiresAt = accessTokenExpiresAt,
            RefreshExpiresAt = refreshTokenExpiresAt
        };
    }

    private string GenerateAccessToken(User user, DateTimeOffset expiresAt)
    {
        var key = _configuration["Jwt:Key"];
        var issuer = _configuration["Jwt:Issuer"];
        var audience = _configuration["Jwt:Audience"];

        if (string.IsNullOrWhiteSpace(key) || string.IsNullOrWhiteSpace(issuer) || string.IsNullOrWhiteSpace(audience))
        {
            throw new InvalidOperationException("La configuration JWT est incomplète.");
        }

        var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key));
        var signingCredentials = new SigningCredentials(signingKey, SecurityAlgorithms.HmacSha256);

        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            new("client_id", user.ClientId.ToString()),
            new("role", user.Role.ToString()),
            new(JwtRegisteredClaimNames.Email, user.Email)
        };

        var jwtToken = new JwtSecurityToken(
            issuer: issuer,
            audience: audience,
            claims: claims,
            notBefore: DateTime.UtcNow,
            expires: expiresAt.UtcDateTime,
            signingCredentials: signingCredentials);

        return new JwtSecurityTokenHandler().WriteToken(jwtToken);
    }

    private static string GenerateRefreshToken()
    {
        Span<byte> buffer = stackalloc byte[RefreshTokenByteLength];
        RandomNumberGenerator.Fill(buffer);
        return Convert.ToBase64String(buffer);
    }

    private static string HashRefreshToken(string refreshToken)
    {
        var refreshTokenBytes = Encoding.UTF8.GetBytes(refreshToken);
        var hash = SHA256.HashData(refreshTokenBytes);
        return Convert.ToBase64String(hash);
    }

    private async Task CleanupExpiredSessionsAsync(Guid userId, CancellationToken cancellationToken)
    {
        var now = DateTime.UtcNow;

        var expiredSessions = await _context.UserSessions
            .Where(session => session.UserId == userId && session.ExpiresAt <= now)
            .ToListAsync(cancellationToken);

        if (expiredSessions.Count > 0)
        {
            _context.UserSessions.RemoveRange(expiredSessions);
        }
    }

    private static string Truncate(string value, int maxLength)
    {
        if (string.IsNullOrEmpty(value) || value.Length <= maxLength)
        {
            return value;
        }

        return value[..maxLength];
    }
}

