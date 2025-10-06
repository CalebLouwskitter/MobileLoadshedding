using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Entities;

public class Neighbourhood
{
    [Key]
    public int NeighbourhoodId { get; set; }

    [Required]
    public int UserId { get; set; }

    [Required]
    [MaxLength(50)]
    public string Name { get; set; } = string.Empty;

    public bool IsActive { get; set; } = true;

    [Column(TypeName = "decimal(10,2)")]
    public decimal PowerConsumption { get; set; }

    [Column(TypeName = "decimal(10,2)")]
    public decimal IncomeRate { get; set; }

    public int Level { get; set; } = 1;

    public User? User { get; set; }

    public ICollection<Section> Sections { get; set; } = new List<Section>();
}
