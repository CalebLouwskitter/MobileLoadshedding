// Entity Framework Core implementation for database access (Microsoft, 2025)
// DbContext pattern for managing database connections and entities (Microsoft, 2025)
using IdleLoadsheddingAPI.Models.AppEntities;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Data;

public class GameDbContext(DbContextOptions<GameDbContext> options) : DbContext(options)
{
    public DbSet<User> Users => Set<User>();
    public DbSet<Neighbourhood> Neighbourhoods => Set<Neighbourhood>();
    public DbSet<Section> Sections => Set<Section>();
    public DbSet<Upgrade> Upgrades => Set<Upgrade>();
    public DbSet<Friend> Friends => Set<Friend>();
    public DbSet<LeaderboardEntry> LeaderboardEntries => Set<LeaderboardEntry>();
    public DbSet<StatsSnapshot> StatsSnapshots => Set<StatsSnapshot>();
    public DbSet<SaveSnapshot> SaveSnapshots => Set<SaveSnapshot>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(entity =>
        {
            entity.HasIndex(e => e.FirebaseUid).IsUnique();
        });

        modelBuilder.Entity<Neighbourhood>(entity =>
        {
            entity.HasOne(n => n.User)
                  .WithMany(u => u.Neighbourhoods)
                  .HasForeignKey(n => n.UserId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<Section>(entity =>
        {
            entity.HasOne(s => s.Neighbourhood)
                  .WithMany(n => n.Sections)
                  .HasForeignKey(s => s.NeighbourhoodId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<Friend>(entity =>
        {
            entity.HasOne(f => f.User)
                  .WithMany(u => u.Friends)
                  .HasForeignKey(f => f.UserId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(f => f.FriendUser)
                  .WithMany()
                  .HasForeignKey(f => f.FriendUserId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(f => new { f.UserId, f.FriendUserId }).IsUnique();
        });

        modelBuilder.Entity<LeaderboardEntry>(entity =>
        {
            entity.HasOne(entry => entry.User)
                  .WithMany(user => user.LeaderboardEntries)
                  .HasForeignKey(entry => entry.UserId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

      modelBuilder.Entity<StatsSnapshot>(entity =>
      {
        entity.HasOne(snapshot => snapshot.User)
            .WithMany(user => user.StatsSnapshots)
            .HasForeignKey(snapshot => snapshot.UserId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(snapshot => new { snapshot.UserId, snapshot.RecordedAtUtc });
        });

        modelBuilder.Entity<SaveSnapshot>(entity =>
        {
        entity.HasOne(snapshot => snapshot.User)
            .WithMany(user => user.SaveSnapshots)
            .HasForeignKey(snapshot => snapshot.UserId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasIndex(snapshot => new { snapshot.UserId, snapshot.RecordedAtUtc });
        });
    }
}
