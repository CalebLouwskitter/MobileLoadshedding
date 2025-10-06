using System;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;
using IdleLoadsheddingAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace IdleLoadsheddingAPI.Controllers;

[ApiController]
[Authorize]
[Route("api/[controller]")]
public class SocialController(SyncService syncService) : ControllerBase
{
    private readonly SyncService _syncService = syncService;

    [HttpGet("friends")]
    public async Task<ActionResult<IEnumerable<FriendResponse>>> GetFriends()
    {
        var firebaseUid = User.FindFirstValue(JwtRegisteredClaimNames.Sub)
                          ?? User.FindFirstValue("user_id");

        if (string.IsNullOrWhiteSpace(firebaseUid))
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        var friends = await _syncService.GetFriendListAsync(firebaseUid);
        return Ok(friends);
    }

    [HttpPost("friends")]
    public async Task<IActionResult> AddFriend([FromBody] AddFriendRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var firebaseUid = User.FindFirstValue(JwtRegisteredClaimNames.Sub)
                          ?? User.FindFirstValue("user_id");

        if (string.IsNullOrWhiteSpace(firebaseUid))
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        await _syncService.AddFriendAsync(firebaseUid, request.FirebaseUid);
        return NoContent();
    }

    [HttpPost("friends/sync")]
    public async Task<IActionResult> SyncFriends([FromBody] FriendsSyncRequest request)
    {
        var firebaseUid = User.FindFirstValue(JwtRegisteredClaimNames.Sub)
                          ?? User.FindFirstValue("user_id");

        if (string.IsNullOrWhiteSpace(firebaseUid))
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        if (!string.Equals(request.PlayerId, firebaseUid, StringComparison.Ordinal))
        {
            return Forbid();
        }

        await _syncService.SyncFriendsAsync(firebaseUid, request);
        return NoContent();
    }
}
