using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Entities;

public class SyncMetadata
{
    [Key]
    [MaxLength(50)]
    public string PayloadType { get; set; } = string.Empty;

    public DateTime LastSyncedUtc { get; set; } = DateTime.UtcNow;

    [MaxLength(64)]
    public string? LastPayloadHash { get; set; }

    public string? LastPayloadJson { get; set; }

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();
}
