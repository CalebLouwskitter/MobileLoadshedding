using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

// Add logging (Microsoft, 2025)
builder.Logging.ClearProviders();
builder.Logging.AddConsole();
builder.Logging.AddDebug();

// (Microsoft, 2025) - Controller-based API configuration
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        // (Microsoft, 2025) - Use camelCase to match Android client expectations (Gson/Retrofit)
        // Without this, API returns PascalCase JSON that Android cannot deserialize
        options.JsonSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
    });

// Database configuration with test-friendly fallbacks (Microsoft, 2025)
builder.Services.AddDbContext<GameDbContext>((serviceProvider, options) =>
{
    var configuration = serviceProvider.GetRequiredService<IConfiguration>();
    var connectionSetting = configuration.GetConnectionString("Default");

    if (string.IsNullOrWhiteSpace(connectionSetting))
    {
        Console.WriteLine("No SQL connection string detected. Using in-memory database 'IdleLoadshedding_TestDb'.");
        // (Microsoft, 2025) - In-memory database for testing
        options.UseInMemoryDatabase("IdleLoadshedding_TestDb");
        return;
    }

    const string inMemoryPrefix = "InMemory:";
    if (connectionSetting.StartsWith(inMemoryPrefix, StringComparison.OrdinalIgnoreCase))
    {
        var dbName = connectionSetting[inMemoryPrefix.Length..];
        if (string.IsNullOrWhiteSpace(dbName))
        {
            dbName = "IdleLoadshedding_TestDb";
        }

        Console.WriteLine($"Using in-memory database '{dbName}' as configured.");
        // (Microsoft, 2025) - In-memory database provider
        options.UseInMemoryDatabase(dbName);
        return;
    }

    Console.WriteLine($"Using SQL Server connection string: {connectionSetting.Replace("Password=", "Password=***")}");
    // (Microsoft, 2025) - SQL Server database provider
    options.UseSqlServer(connectionSetting);
});

// Register services (Microsoft, 2025) - Dependency injection pattern
builder.Services.AddScoped<GameService>();
builder.Services.AddScoped<LeaderboardService>();
builder.Services.AddScoped<SyncService>();

// Firebase Authentication (Google, 2025; Microsoft, 2025)
var firebaseProjectId = builder.Configuration["Firebase:ProjectId"] ?? string.Empty;
Console.WriteLine($"Firebase Project ID: {firebaseProjectId}");

// (Microsoft, 2025) - JWT Bearer authentication configuration
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        // (Google, 2025) - Firebase token validation parameters
        options.Authority = $"https://securetoken.google.com/{firebaseProjectId}";
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = $"https://securetoken.google.com/{firebaseProjectId}",
            ValidateAudience = true,
            ValidAudience = firebaseProjectId,
            ValidateLifetime = true
        };
    });

// (Microsoft, 2025) - Authorization middleware
builder.Services.AddAuthorization();

// CORS configuration (Microsoft, 2025)
builder.Services.AddCors(options =>
    options.AddDefaultPolicy(policy =>
        policy.AllowAnyHeader()
              .AllowAnyMethod()
              .AllowCredentials()
              .WithOrigins(builder.Configuration.GetSection("Cors:Origins").Get<string[]>() ??
                           new[] { "http://localhost:4200", "http://localhost:5173" })));

var app = builder.Build();

// Database initialization
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<GameDbContext>();
    try
    {
        // If using SQL Server, apply pending EF Core migrations to ensure schema is current
        if (db.Database.IsSqlServer())
        {
            Console.WriteLine("Applying EF Core migrations...");
            await db.Database.MigrateAsync();
            Console.WriteLine("Migrations applied successfully.");
        }

        Console.WriteLine("Testing database connection...");
        await db.Database.CanConnectAsync();
        Console.WriteLine("Database connection successful!");

        var userCount = await db.Users.CountAsync();
        Console.WriteLine($"Current user count: {userCount}");
    }
    catch (Exception ex)
    {
        Console.WriteLine($"Database connection failed: {ex.Message}");
    }
}

// Global error handling
app.Use(async (context, next) =>
{
    try
    {
        await next();
    }
    catch (InvalidOperationException ex)
    {
        context.Response.StatusCode = StatusCodes.Status400BadRequest;
        await context.Response.WriteAsJsonAsync(new { error = ex.Message });
    }
    catch (Exception ex)
    {
        Console.WriteLine($"Unhandled error: {ex}");
        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        await context.Response.WriteAsJsonAsync(new { error = "An internal error occurred" });
    }
});

app.UseCors();
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

var urls = Environment.GetEnvironmentVariable("ASPNETCORE_URLS") ?? "http://+:8080";
Console.WriteLine($"Starting server on: {urls}");
app.Run();

// Make Program accessible to test projects
public partial class Program { }


// - Microsoft, 2025. .NET 9 Documentation. [online] Microsoft Learn. Available at:
//   https://learn.microsoft.com/en-us/dotnet/core/whats-new/dotnet-9/overview
//   [Accessed 5 Oct. 2025].
//
// - Microsoft, 2025. ASP.NET Core Documentation. [online] Microsoft Learn.
//   Available at: https://learn.microsoft.com/en-us/aspnet/core/ [Accessed 5 Oct. 2025].
//
// - Microsoft, 2025. C# Language Documentation. [online] Microsoft Learn. Available at:
//   https://learn.microsoft.com/en-us/dotnet/csharp/ [Accessed 5 Oct. 2025].
//
// - Microsoft, 2025. Entity Framework Core Documentation.
//   [online] Microsoft Learn. Available at: https://learn.microsoft.com/en-us/ef/core/
//   [Accessed 5 Oct. 2025].
//
// - Google, 2025. Firebase Authentication Documentation. [online] Google Firebase.
//   Available at: https://firebase.google.com/docs/auth [Accessed 5 Oct. 2025].
//
// - Microsoft, 2025. ASP.NET Core Authentication - JWT Bearer. [online] Microsoft Learn.
//   Available at: https://learn.microsoft.com/en-us/aspnet/core/security/authentication/
//   [Accessed 5 Oct. 2025].
//
// - Docker Inc., 2025. Docker Documentation - Get Started. [online] Available at:
//   https://docs.docker.com/get-started/ [Accessed 5 Oct. 2025].
//