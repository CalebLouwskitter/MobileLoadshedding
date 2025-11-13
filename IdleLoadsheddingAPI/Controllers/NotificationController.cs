using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;
using IdleLoadsheddingAPI.Services.Interfaces;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace IdleLoadsheddingAPI.Controllers;

/// <summary>
/// Controller for managing push notifications and notification preferences.
/// Handles FCM token registration and notification settings.
/// </summary>
[ApiController]
[Authorize]
[Route("api/[controller]")]
public class NotificationController(INotificationService notificationService) : ControllerBase
{
    private readonly INotificationService _notificationService = notificationService;

    /// <summary>
    /// Registers or updates an FCM token for push notifications.
    /// POST /api/notification/register-token
    /// </summary>
    [HttpPost("register-token")]
    public async Task<ActionResult<FcmTokenResponse>> RegisterToken([FromBody] FcmTokenRequest request)
    {
        var firebaseUid = ResolveFirebaseUid();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        // Ensure the request matches the authenticated user
        if (!string.IsNullOrEmpty(request.FirebaseUid) && 
            !string.Equals(request.FirebaseUid, firebaseUid, StringComparison.Ordinal))
        {
            return Forbid("Cannot register token for another user.");
        }

        request.FirebaseUid = firebaseUid;
        var result = await _notificationService.RegisterFcmTokenAsync(request);
        
        if (!result.Success)
        {
            return BadRequest(result);
        }

        return Ok(result);
    }

    /// <summary>
    /// Tracks a notification action (opened, dismissed, etc.) for analytics.
    /// POST /api/notification/action
    /// </summary>
    [HttpPost("action")]
    public async Task<IActionResult> TrackAction([FromBody] NotificationActionRequest request)
    {
        var firebaseUid = ResolveFirebaseUid();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        await _notificationService.TrackNotificationActionAsync(firebaseUid, request);
        return NoContent();
    }

    /// <summary>
    /// Retrieves notification preferences for the authenticated user.
    /// GET /api/notification/preferences
    /// </summary>
    [HttpGet("preferences")]
    public async Task<ActionResult<NotificationPreferencesResponse>> GetPreferences()
    {
        var firebaseUid = ResolveFirebaseUid();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        var result = await _notificationService.GetNotificationPreferencesAsync(firebaseUid);
        return Ok(result);
    }

    /// <summary>
    /// Updates notification preferences for the authenticated user.
    /// PUT /api/notification/preferences
    /// </summary>
    [HttpPut("preferences")]
    public async Task<IActionResult> UpdatePreferences([FromBody] UpdateNotificationPreferencesRequest request)
    {
        var firebaseUid = ResolveFirebaseUid();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        // Ensure the request matches the authenticated user
        if (!string.IsNullOrEmpty(request.Preferences.FirebaseUid) && 
            !string.Equals(request.Preferences.FirebaseUid, firebaseUid, StringComparison.Ordinal))
        {
            return Forbid("Cannot update preferences for another user.");
        }

        request.Preferences.FirebaseUid = firebaseUid;
        await _notificationService.UpdateNotificationPreferencesAsync(firebaseUid, request);
        return NoContent();
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
