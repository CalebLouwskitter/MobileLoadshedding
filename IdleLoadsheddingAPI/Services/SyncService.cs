using System;
using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Entities;
using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Services;

public class SyncService(GameDbContext context)
{
    private readonly GameDbContext _context = context;

    public async Task<UserProfileResponse> EnsureUserProfileAsync(RegisterRequest request)
    {
        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.FirebaseUid == request.FirebaseUid);

        if (user is null)
        {
            user = new User
            {
                FirebaseUid = request.FirebaseUid,
                Username = request.Username,
                Email = request.Email,
                Money = 0,
                Electricity = 0,
                PrestigeLevel = 0,
                CreatedAt = DateTime.UtcNow,
                LastSyncAt = DateTime.UtcNow,
                LastLoginAt = DateTime.UtcNow
            };

            _context.Users.Add(user);
            await _context.SaveChangesAsync();
        }
        else
        {
            user.Username = request.Username;
            user.Email = request.Email;
            user.LastLoginAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }

        return new UserProfileResponse
        {
            UserId = user.UserId,
            FirebaseUid = user.FirebaseUid,
            Username = user.Username,
            Email = user.Email,
            Money = user.Money,
            Electricity = user.Electricity,
            PrestigeLevel = user.PrestigeLevel
        };
    }

    public async Task<IReadOnlyList<FriendResponse>> GetFriendListAsync(string firebaseUid)
    {
        var user = await _context.Users
            .AsNoTracking()
            .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);

        if (user is null)
        {
            return Array.Empty<FriendResponse>();
        }

        return await _context.Friends
            .AsNoTracking()
            .Where(f => f.UserId == user.UserId)
            .Join(
                _context.Users,
                friend => friend.FriendUserId,
                other => other.UserId,
                (friend, other) => new FriendResponse
                {
                    UserId = other.UserId,
                    Username = other.Username,
                    AddedAt = friend.CreatedAt
                })
            .OrderBy(friend => friend.Username)
            .ToListAsync();
    }

    public async Task AddFriendAsync(string firebaseUid, string friendFirebaseUid)
    {
        if (firebaseUid == friendFirebaseUid)
        {
            throw new InvalidOperationException("Cannot add yourself as a friend.");
        }

        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid)
            ?? throw new InvalidOperationException("User profile not found.");

        var friend = await _context.Users
            .FirstOrDefaultAsync(u => u.FirebaseUid == friendFirebaseUid)
            ?? throw new InvalidOperationException("Friend profile not found.");

        var alreadyFriends = await _context.Friends.AnyAsync(f =>
            (f.UserId == user.UserId && f.FriendUserId == friend.UserId) ||
            (f.UserId == friend.UserId && f.FriendUserId == user.UserId));

        if (alreadyFriends)
        {
            return;
        }

        var now = DateTime.UtcNow;

        _context.Friends.AddRange(
            new Friend { UserId = user.UserId, FriendUserId = friend.UserId, CreatedAt = now },
            new Friend { UserId = friend.UserId, FriendUserId = user.UserId, CreatedAt = now }
        );

        await _context.SaveChangesAsync();
    }

    public async Task SyncFriendsAsync(string firebaseUid, FriendsSyncRequest request)
    {
        if (!string.Equals(firebaseUid, request.PlayerId, StringComparison.Ordinal))
        {
            throw new InvalidOperationException("Player identity mismatch.");
        }

        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid)
            ?? throw new InvalidOperationException("User profile not found.");

        var existingFriends = await _context.Friends
            .Where(f => f.UserId == user.UserId)
            .ToListAsync();

        var desiredFriendUserIds = new HashSet<int>();
        var now = DateTime.UtcNow;

        foreach (var friend in request.Friends)
        {
            if (string.IsNullOrWhiteSpace(friend.Id))
            {
                continue;
            }

            User? friendUser = null;

            friendUser = await _context.Users
                .FirstOrDefaultAsync(u => u.FirebaseUid == friend.Id);

            if (friendUser is null && int.TryParse(friend.Id, out var friendUserId))
            {
                friendUser = await _context.Users.FirstOrDefaultAsync(u => u.UserId == friendUserId);
            }

            if (friendUser is null)
            {
                // Skip unknown friends to avoid creating phantom accounts
                continue;
            }

            desiredFriendUserIds.Add(friendUser.UserId);

            var alreadyLinked = existingFriends.Any(f => f.FriendUserId == friendUser.UserId);
            if (!alreadyLinked)
            {
                _context.Friends.AddRange(
                    new Friend { UserId = user.UserId, FriendUserId = friendUser.UserId, CreatedAt = now },
                    new Friend { UserId = friendUser.UserId, FriendUserId = user.UserId, CreatedAt = now }
                );
            }
        }

        // Remove friendships that are no longer present in payload
        var toRemove = existingFriends
            .Where(f => !desiredFriendUserIds.Contains(f.FriendUserId))
            .ToList();

        if (toRemove.Count > 0)
        {
            var mirrorIds = toRemove.Select(f => f.FriendUserId).ToHashSet();

            var reciprocal = await _context.Friends
                .Where(f => f.UserId != user.UserId && mirrorIds.Contains(f.UserId) && f.FriendUserId == user.UserId)
                .ToListAsync();

            _context.Friends.RemoveRange(toRemove);
            _context.Friends.RemoveRange(reciprocal);
        }

        await _context.SaveChangesAsync();
    }
}
