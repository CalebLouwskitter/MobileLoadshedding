using IdleLoadsheddingAPI.Models.DTOs;
using IdleLoadsheddingAPI.Services.Interfaces;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace IdleLoadsheddingAPI.Controllers;

/// <summary>
/// Controller for managing game events.
/// Handles event recording and retrieval of active events.
/// </summary>
[ApiController]
[Authorize]
[Route("api/[controller]")]
public class EventsController(IEventService eventService) : ControllerBase
{
    private readonly IEventService _eventService = eventService;

    /// <summary>
    /// Records a game event from Unity client.
    /// POST /api/events
    /// </summary>
    [HttpPost]
    public async Task<IActionResult> RecordEvent([FromBody] EventRequestDto request)
    {
        var firebaseUid = ResolveFirebaseUid();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        await _eventService.RecordEventAsync(firebaseUid, request);
        return NoContent();
    }

    /// <summary>
    /// Retrieves active/available game events.
    /// GET /api/events
    /// </summary>
    [HttpGet]
    [AllowAnonymous]
    public async Task<ActionResult<IEnumerable<EventResponseDto>>> GetActiveEvents()
    {
        var events = await _eventService.GetActiveEventsAsync();
        return Ok(events);
    }

    /// <summary>
    /// Extracts Firebase UID from JWT claims.
    /// </summary>
    private string? ResolveFirebaseUid()
    {
        return User.FindFirstValue(JwtRegisteredClaimNames.Sub) 
               ?? User.FindFirstValue("user_id");
    }
}
