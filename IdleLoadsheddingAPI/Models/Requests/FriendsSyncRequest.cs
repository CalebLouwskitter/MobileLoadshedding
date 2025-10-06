using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Requests;

public class FriendsSyncRequest
{
    [Required]
    public string PlayerId { get; set; } = string.Empty;

    public List<FriendEntry> Friends { get; set; } = new();

    public class FriendEntry
    {
        [Required]
        public string Id { get; set; } = string.Empty;

        [Required]
        public string DisplayName { get; set; } = string.Empty;

        public bool IsOnline { get; set; }

        public double ContributionPerHour { get; set; }

        public string? LastInteractionUtc { get; set; }
    }
}
