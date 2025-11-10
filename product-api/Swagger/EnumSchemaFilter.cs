using System;
using Microsoft.OpenApi.Any;
using Microsoft.OpenApi.Models;
using Swashbuckle.AspNetCore.SwaggerGen;

namespace product_api.Swagger;

public sealed class EnumSchemaFilter : ISchemaFilter
{
    public void Apply(OpenApiSchema schema, SchemaFilterContext context)
    {
        if (schema.Enum is not { Count: > 0 } || !context.Type.IsEnum)
        {
            return;
        }

        schema.Enum.Clear();

        foreach (var enumName in Enum.GetNames(context.Type))
        {
            schema.Enum.Add(new OpenApiString(enumName));
        }

        schema.Type = "string";
        schema.Format = null;
    }
}

