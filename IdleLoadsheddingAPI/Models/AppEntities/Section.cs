using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.AppEntities;

public class Section
{
    [Key]
    public int SectionId { get; set; }

    [Required]
    public int NeighbourhoodId { get; set; }

    [Required]
    [MaxLength(50)]
    public string Name { get; set; } = string.Empty;

    [Column(TypeName = "decimal(10,2)")]
    public decimal PowerOutput { get; set; }

    public int UpgradeLevel { get; set; }

    public Neighbourhood? Neighbourhood { get; set; }
}
