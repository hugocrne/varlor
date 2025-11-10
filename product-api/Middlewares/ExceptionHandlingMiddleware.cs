using System.Net.Mime;
using System.Text.Json;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using product_api.Exceptions;

namespace product_api.Middlewares;

public sealed class ExceptionHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ExceptionHandlingMiddleware> _logger;
    private readonly JsonSerializerOptions _serializerOptions;

    public ExceptionHandlingMiddleware(
        RequestDelegate next,
        ILogger<ExceptionHandlingMiddleware> logger,
        IOptions<JsonOptions> jsonOptions)
    {
        _next = next ?? throw new ArgumentNullException(nameof(next));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _serializerOptions = jsonOptions?.Value.JsonSerializerOptions
                              ?? throw new ArgumentNullException(nameof(jsonOptions));
    }

    public async Task InvokeAsync(HttpContext context)
    {
        ArgumentNullException.ThrowIfNull(context);

        try
        {
            await _next(context);
        }
        catch (Exception exception)
        {
            await HandleExceptionAsync(context, exception);
        }
    }

    private async Task HandleExceptionAsync(HttpContext context, Exception exception)
    {
        var (statusCode, title) = GetErrorDetails(exception);

        if (statusCode >= StatusCodes.Status500InternalServerError)
        {
            _logger.LogError(exception, "Erreur inattendue lors du traitement de {Path}", context.Request.Path);
        }
        else
        {
            _logger.LogWarning(exception, "Erreur fonctionnelle {ExceptionType} sur {Path}", exception.GetType().Name, context.Request.Path);
        }

        var problemDetails = new ProblemDetails
        {
            Status = statusCode,
            Title = title,
            Detail = string.IsNullOrWhiteSpace(exception.Message) ? title : exception.Message,
            Instance = context.Request.Path,
            Type = $"https://httpstatuses.io/{statusCode}"
        };

        context.Response.ContentType = MediaTypeNames.Application.Json;
        context.Response.StatusCode = statusCode;

        await context.Response.WriteAsJsonAsync(problemDetails, _serializerOptions);
    }

    private static (int StatusCode, string Title) GetErrorDetails(Exception exception) =>
        exception switch
        {
            ValidationException => (StatusCodes.Status400BadRequest, "Erreur de validation"),
            DuplicateResourceException => (StatusCodes.Status409Conflict, "Conflit de ressource"),
            NotFoundException => (StatusCodes.Status404NotFound, "Ressource introuvable"),
            UnauthorizedException => (StatusCodes.Status401Unauthorized, "Accès non autorisé"),
            DatabaseException => (StatusCodes.Status500InternalServerError, "Erreur de base de données"),
            _ => (StatusCodes.Status500InternalServerError, "Erreur interne du serveur")
        };
}

