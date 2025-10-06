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
public class GameController(GameService gameService, LeaderboardService leaderboardService) : ControllerBase
{
    private readonly GameService _gameService = gameService;
    private readonly LeaderboardService _leaderboardService = leaderboardService;

    [HttpPost("sync")]
    public async Task<ActionResult<SyncResponse>> SyncGameState([FromBody] SyncRequest request)
    {
        var (firebaseUid, email, displayName) = ResolveUserIdentity();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        var result = await _gameService.SyncPlayerStateAsync(firebaseUid, request, email, displayName);
        return Ok(result);
    }

    [HttpPost("upgrade")]
    public async Task<ActionResult<UpgradeResponse>> PurchaseUpgrade([FromBody] UpgradeRequest request)
    {
        var (firebaseUid, email, displayName) = ResolveUserIdentity();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        var result = await _gameService.ApplyUpgradeAsync(firebaseUid, request, email, displayName);
        return Ok(result);
    }

    [HttpGet("leaderboard")]
    [AllowAnonymous]
    public async Task<ActionResult<IEnumerable<LeaderboardEntryResponse>>> GetLeaderboard([FromQuery] int take = 10)
    {
        take = Math.Clamp(take, 1, 50);
        var entries = await _leaderboardService.GetTopEntriesAsync(take);
        return Ok(entries);
    }

    [HttpPost("leaderboard")]
    public async Task<IActionResult> SubmitScore([FromBody] LeaderboardScoreRequest request)
    {
        var (firebaseUid, _, displayName) = ResolveUserIdentity(request.DisplayName);
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        await _gameService.RecordLeaderboardScoreAsync(firebaseUid, request.Score, displayName);
        return NoContent();
    }

    [HttpPost("stats")]
    public async Task<IActionResult> RecordStatsSnapshot([FromBody] StatsSnapshotRequest request)
    {
        var (firebaseUid, email, displayName) = ResolveUserIdentity();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        if (!string.IsNullOrEmpty(request.PlayerId) && !string.Equals(request.PlayerId, firebaseUid, StringComparison.Ordinal))
        {
            return Forbid();
        }

        await _gameService.RecordStatsSnapshotAsync(firebaseUid, request, email, displayName);
        return NoContent();
    }

    [HttpPost("save")]
    public async Task<IActionResult> RecordSaveSnapshot([FromBody] SaveSyncRequest request)
    {
        var (firebaseUid, email, displayName) = ResolveUserIdentity();
        if (firebaseUid is null)
        {
            return Unauthorized("Missing Firebase user identifier.");
        }

        if (!string.IsNullOrEmpty(request.PlayerId) && !string.Equals(request.PlayerId, firebaseUid, StringComparison.Ordinal))
        {
            return Forbid();
        }

        await _gameService.RecordSaveSnapshotAsync(firebaseUid, request, email, displayName);
        return NoContent();
    }

    private (string? FirebaseUid, string? Email, string DisplayName) ResolveUserIdentity(string? defaultDisplayName = null)
    {
        var firebaseUid = User.FindFirstValue(JwtRegisteredClaimNames.Sub)
                          ?? User.FindFirstValue("user_id");
        var email = User.FindFirstValue(ClaimTypes.Email)
                ?? User.FindFirstValue("email");
        var displayName = User.FindFirstValue(ClaimTypes.Name)
                ?? User.FindFirstValue("name")
                ?? User.FindFirstValue("nickname")
                ?? defaultDisplayName
                ?? string.Empty;

        return (firebaseUid, email, displayName);
    }
}
