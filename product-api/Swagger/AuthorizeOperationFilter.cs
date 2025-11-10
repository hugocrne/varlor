using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using Microsoft.AspNetCore.Authorization;
using Microsoft.OpenApi.Models;
using Swashbuckle.AspNetCore.SwaggerGen;

namespace product_api.Swagger;

public sealed class AuthorizeOperationFilter : IOperationFilter
{
    public void Apply(OpenApiOperation operation, OperationFilterContext context)
    {
        var authorizeAttributes = GetAuthorizeAttributes(context);
        if (!authorizeAttributes.Any())
        {
            return;
        }

        AppendSecurityMetadata(operation);
        AppendRoleDescription(operation, authorizeAttributes);
        EnsureUnauthorizedResponses(operation);
    }

    private static IReadOnlyCollection<AuthorizeAttribute> GetAuthorizeAttributes(OperationFilterContext context)
    {
        var controllerAttributes = context.MethodInfo.DeclaringType?
            .GetCustomAttributes(true)
            .OfType<AuthorizeAttribute>()
            ?? Enumerable.Empty<AuthorizeAttribute>();

        var actionAttributes = context.MethodInfo
            .GetCustomAttributes(true)
            .OfType<AuthorizeAttribute>();

        return controllerAttributes.Concat(actionAttributes).ToArray();
    }

    private static void AppendSecurityMetadata(OpenApiOperation operation)
    {
        var bearerScheme = new OpenApiSecurityScheme
        {
            Reference = new OpenApiReference
            {
                Type = ReferenceType.SecurityScheme,
                Id = "Bearer"
            }
        };

        operation.Security ??= new List<OpenApiSecurityRequirement>();

        var requirementAlreadyAdded = operation.Security
            .Any(requirement => requirement.ContainsKey(bearerScheme));

        if (!requirementAlreadyAdded)
        {
            operation.Security.Add(new OpenApiSecurityRequirement
            {
                [bearerScheme] = Array.Empty<string>()
            });
        }
    }

    private static void AppendRoleDescription(OpenApiOperation operation, IReadOnlyCollection<AuthorizeAttribute> attributes)
    {
        var roles = attributes
            .Select(attribute => attribute.Roles)
            .Where(roleList => !string.IsNullOrWhiteSpace(roleList))
            .SelectMany(roleList => roleList!.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .OrderBy(role => role, StringComparer.OrdinalIgnoreCase)
            .ToArray();

        var descriptionBuilder = new StringBuilder();
        descriptionBuilder.Append("🔒 Accès protégé par JWT.");

        if (roles.Length > 0)
        {
            descriptionBuilder.Append(" Rôles requis : ");
            descriptionBuilder.Append(string.Join(", ", roles));
            descriptionBuilder.Append('.');
        }
        else
        {
            descriptionBuilder.Append(" Rôle conforme à la politique d'autorisation configurée.");
        }

        var note = descriptionBuilder.ToString();

        if (string.IsNullOrWhiteSpace(operation.Description))
        {
            operation.Description = note;
        }
        else if (!operation.Description.Contains(note, StringComparison.OrdinalIgnoreCase))
        {
            operation.Description = $"{operation.Description}\n\n{note}";
        }
    }

    private static void EnsureUnauthorizedResponses(OpenApiOperation operation)
    {
        operation.Responses ??= new OpenApiResponses();

        if (!operation.Responses.ContainsKey("401"))
        {
            operation.Responses.Add("401", new OpenApiResponse
            {
                Description = "Non authentifié. Fournir un jeton Bearer valide."
            });
        }

        if (!operation.Responses.ContainsKey("403"))
        {
            operation.Responses.Add("403", new OpenApiResponse
            {
                Description = "Accès interdit. Le rôle de l'utilisateur ne satisfait pas aux exigences."
            });
        }
    }
}

