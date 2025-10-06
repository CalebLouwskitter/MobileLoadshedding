
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.Entities;

public class Player
{
    [Key]
    [MaxLength(64)]
    public string PlayerId { get; set; } = default!;

    [MaxLength(100)]
    public string DisplayName { get; set; } = string.Empty;

    [MaxLength(256)]
    public string? Email { get; set; }

    [Required]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? LastSyncAt { get; set; }

    public int PrestigeCurrency { get; set; }

    [MaxLength(10)]
    public string? FriendCode { get; set; }

    public long TotalMoneyEarned { get; set; }

    public int PrestigeLevel { get; set; }

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public PlayerSaveState? SaveState { get; set; }

    public ICollection<PlayerNeighbourhood> Neighbourhoods { get; set; } = new List<PlayerNeighbourhood>();

    public ICollection<PlayerUpgrade> Upgrades { get; set; } = new List<PlayerUpgrade>();

    public ICollection<PlayerFriend> Friends { get; set; } = new List<PlayerFriend>();

    public ICollection<LeaderboardEntry> LeaderboardEntries { get; set; } = new List<LeaderboardEntry>();
}
