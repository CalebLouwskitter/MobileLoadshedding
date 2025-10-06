using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Requests;

public class SaveSyncRequest
{
    [Required]
    public string PlayerId { get; set; } = string.Empty;

    [Required]
    public string Stats { get; set; } = string.Empty;

    public List<GeneratorState> Generators { get; set; } = new();

    public List<DistrictState> Districts { get; set; } = new();

    public CentralState? Central { get; set; }

    [Required]
    public string TimestampUtc { get; set; } = string.Empty;

    public class GeneratorState
    {
        [Required]
        public string Id { get; set; } = string.Empty;

        [Range(0, int.MaxValue)]
        public int Level { get; set; }
    }

    public class DistrictState
    {
        [Required]
        public string Id { get; set; } = string.Empty;

        [Range(0, int.MaxValue)]
        public int Level { get; set; }

        [Range(0, double.MaxValue)]
        public double Efficiency { get; set; }
    }

    public class CentralState
    {
        [Range(0, double.MaxValue)]
        public double Charge { get; set; }
    }
}
