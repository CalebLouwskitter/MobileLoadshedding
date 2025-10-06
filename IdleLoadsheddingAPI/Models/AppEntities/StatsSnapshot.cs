using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.AppEntities;

public class StatsSnapshot
{
    [Key]
    public int StatsSnapshotId { get; set; }

    [Required]
    public int UserId { get; set; }

    [Required]
    [Column(TypeName = "nvarchar(max)")]
    public string PayloadJson { get; set; } = string.Empty;

    [Required]
    public DateTime RecordedAtUtc { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }
}
