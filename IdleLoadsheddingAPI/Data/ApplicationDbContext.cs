
using IdleLoadsheddingAPI.Models.Entities;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Data;

public class ApplicationDbContext : DbContext
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options) : base(options)
    {
    }

    public DbSet<Player> Players => Set<Player>();
    public DbSet<PlayerSaveState> PlayerSaveStates => Set<PlayerSaveState>();
    public DbSet<PlayerSaveGenerator> PlayerSaveGenerators => Set<PlayerSaveGenerator>();
    public DbSet<Neighbourhood> Neighbourhoods => Set<Neighbourhood>();
    public DbSet<PlayerNeighbourhood> PlayerNeighbourhoods => Set<PlayerNeighbourhood>();
    public DbSet<Upgrade> Upgrades => Set<Upgrade>();
    public DbSet<PlayerUpgrade> PlayerUpgrades => Set<PlayerUpgrade>();
    public DbSet<LeaderboardEntry> LeaderboardEntries => Set<LeaderboardEntry>();
    public DbSet<PlayerFriend> PlayerFriends => Set<PlayerFriend>();
    public DbSet<GameEvent> GameEvents => Set<GameEvent>();
    public DbSet<GameEventDefinition> GameEventDefinitions => Set<GameEventDefinition>();
    public DbSet<PlayerStatsSnapshot> PlayerStatsSnapshots => Set<PlayerStatsSnapshot>();
    public DbSet<SyncMetadata> SyncMetadata => Set<SyncMetadata>();
    public DbSet<BackendStatsRollup> BackendStatsRollups => Set<BackendStatsRollup>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Player>()
            .HasIndex(p => p.FriendCode)
            .IsUnique()
            .HasFilter("[FriendCode] IS NOT NULL");

        modelBuilder.Entity<Player>()
            .Property(p => p.RowVersion)
            .IsRowVersion();

        modelBuilder.Entity<Player>()
            .HasOne(p => p.SaveState)
            .WithOne(ps => ps.Player)
            .HasForeignKey<PlayerSaveState>(ps => ps.PlayerId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<PlayerNeighbourhood>()
            .HasIndex(pn => new { pn.PlayerId, pn.NeighbourhoodId })
            .IsUnique();

        modelBuilder.Entity<PlayerNeighbourhood>()
            .HasOne(pn => pn.Player)
            .WithMany(p => p.Neighbourhoods)
            .HasForeignKey(pn => pn.PlayerId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<PlayerNeighbourhood>()
            .HasOne(pn => pn.Neighbourhood)
            .WithMany(n => n.PlayerNeighbourhoods)
            .HasForeignKey(pn => pn.NeighbourhoodId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<PlayerUpgrade>()
            .HasIndex(pu => new { pu.PlayerId, pu.UpgradeId })
            .IsUnique();

        modelBuilder.Entity<PlayerUpgrade>()
            .HasOne(pu => pu.Player)
            .WithMany(p => p.Upgrades)
            .HasForeignKey(pu => pu.PlayerId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<PlayerUpgrade>()
            .HasOne(pu => pu.Upgrade)
            .WithMany(u => u.PlayerUpgrades)
            .HasForeignKey(pu => pu.UpgradeId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<PlayerFriend>()
            .HasKey(pf => new { pf.PlayerId, pf.FriendId });

        modelBuilder.Entity<PlayerFriend>()
            .HasOne(pf => pf.Player)
            .WithMany(p => p.Friends)
            .HasForeignKey(pf => pf.PlayerId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<PlayerFriend>()
            .HasOne(pf => pf.Friend)
            .WithMany()
            .HasForeignKey(pf => pf.FriendId)
            .OnDelete(DeleteBehavior.NoAction);

        modelBuilder.Entity<LeaderboardEntry>()
            .HasIndex(le => le.PlayerId)
            .IsUnique();

        modelBuilder.Entity<PlayerStatsSnapshot>()
            .HasOne(ps => ps.Player)
            .WithMany()
            .HasForeignKey(ps => ps.PlayerId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<PlayerSaveGenerator>()
            .HasKey(pg => new { pg.SaveId, pg.GeneratorId });

        modelBuilder.Entity<PlayerSaveGenerator>()
            .HasOne(pg => pg.SaveState)
            .WithMany(ps => ps.Generators)
            .HasForeignKey(pg => pg.SaveId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<GameEventDefinition>()
            .HasIndex(def => def.Code)
            .IsUnique();
    }
}
