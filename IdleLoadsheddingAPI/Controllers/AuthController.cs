// ASP.NET Core API Controller implementation (Microsoft, 2025)
// JWT Bearer authentication pattern (Google, 2025; Microsoft, 2025)
using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;
using IdleLoadsheddingAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace IdleLoadsheddingAPI.Controllers;

[ApiController]
[AllowAnonymous]
[Route("api/[controller]")]
public class AuthController(SyncService syncService) : ControllerBase
{
    private readonly SyncService _syncService = syncService;

    [HttpPost("register")]
    public async Task<ActionResult<UserProfileResponse>> Register([FromBody] RegisterRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var profile = await _syncService.EnsureUserProfileAsync(request);
        return Ok(profile);
    }

    [HttpGet("ping")]
    public ActionResult<HealthCheckResponse> Ping()
    {
        return Ok(new HealthCheckResponse("Idle Loadshedding API ready"));
    }
}
