
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.Entities;

public class PlayerFriend
{
    [MaxLength(64)]
    public string PlayerId { get; set; } = default!;

    [MaxLength(64)]
    public string FriendId { get; set; } = default!;

    [MaxLength(100)]
    public string FriendDisplayName { get; set; } = string.Empty;

    public double ContributionPerHour { get; set; }

    public bool IsOnline { get; set; }

    public DateTime LastInteractionUtc { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public Player Player { get; set; } = default!;

    [ForeignKey(nameof(FriendId))]
    public Player? Friend { get; set; }
}
