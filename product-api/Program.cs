using System.IO;
using System.Reflection;
using System.Text.Json.Serialization;
using Microsoft.EntityFrameworkCore;
using Microsoft.OpenApi.Models;
using product_api.Data;
using product_api.Middlewares;
using product_api.Swagger;

var builder = WebApplication.CreateBuilder(args);

const string CorsPolicy = "FrontendAllowlist";

builder.Services.AddDbContext<VarlorDbContext>(options =>
{
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection"));

    if (builder.Environment.IsDevelopment())
    {
        options.EnableDetailedErrors();
        options.EnableSensitiveDataLogging();
    }
});

builder.Services
    .AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = null;
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
    });

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc(
        "v1",
        new OpenApiInfo
        {
            Title = "Varlor API",
            Version = "v1",
            Description = "Documentation OpenAPI pour l'API Varlor."
        });

    var xmlFileName = $"{Assembly.GetExecutingAssembly().GetName().Name}.xml";
    var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFileName);
    if (File.Exists(xmlPath))
    {
        options.IncludeXmlComments(xmlPath, includeControllerXmlComments: true);
    }

    options.CustomOperationIds(apiDesc =>
    {
        var controller = apiDesc.ActionDescriptor.RouteValues.TryGetValue("controller", out var controllerName)
            ? controllerName
            : null;
        var action = apiDesc.ActionDescriptor.RouteValues.TryGetValue("action", out var actionName)
            ? actionName
            : null;

        return controller is null || action is null ? null : $"{controller}_{action}";
    });

    options.TagActionsBy(apiDesc =>
    {
        if (!string.IsNullOrEmpty(apiDesc.GroupName))
        {
            return new[] { apiDesc.GroupName };
        }

        if (apiDesc.ActionDescriptor.RouteValues.TryGetValue("controller", out var controller))
        {
            return new[] { controller };
        }

        return new[] { "Endpoints" };
    });
    options.DocInclusionPredicate((_, _) => true);

    options.SchemaFilter<EnumSchemaFilter>();

    var securityScheme = new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Authentification JWT. Saisissez: Bearer {votre token}.",
        Reference = new OpenApiReference
        {
            Type = ReferenceType.SecurityScheme,
            Id = "Bearer"
        }
    };

    options.AddSecurityDefinition("Bearer", securityScheme);
    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            securityScheme,
            Array.Empty<string>()
        }
    });
});

builder.Services.AddCors(options =>
{
    options.AddPolicy(
        CorsPolicy,
        policy => policy
            .WithOrigins("http://localhost:4200", "http://localhost:3000")
            .AllowAnyHeader()
            .AllowAnyMethod());
});

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseMiddleware<ExceptionHandlingMiddleware>();

app.UseHttpsRedirection();

app.UseCors(CorsPolicy);

app.UseAuthorization();

app.MapControllers();

app.Run();

public partial class Program;