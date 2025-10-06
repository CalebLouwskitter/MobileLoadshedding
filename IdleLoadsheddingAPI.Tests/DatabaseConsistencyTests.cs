using System;
using System.Linq;
using System.Threading.Tasks;
using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.AppEntities;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace IdleLoadsheddingAPI.Tests;

public class DatabaseConsistencyTests
{
    private static string? GetConnStr()
    {
        // Allow overriding via environment variable
        var cs = Environment.GetEnvironmentVariable("TEST_SQL_CONNSTR");
        if (!string.IsNullOrWhiteSpace(cs)) return cs;

        // Default to local compose SQL
        return "Server=localhost,1433;Database=IdleLoadshedding;User ID=sa;Password=Password1!;TrustServerCertificate=True;";
    }

    private static bool CanReachSql(string connStr)
    {
        try
        {
            using var conn = new SqlConnection(connStr);
            conn.Open();
            conn.Close();
            return true;
        }
        catch
        {
            return false;
        }
    }

    private static GameDbContext CreateContext(string connStr)
    {
        var builder = new DbContextOptionsBuilder<GameDbContext>();
        builder.UseSqlServer(connStr);
        return new GameDbContext(builder.Options);
    }

    [Fact]
    public void SqlServerConnection_OpenClose_Succeeds()
    {
        var connStr = GetConnStr()!;
        Assert.True(CanReachSql(connStr), "Unable to connect to SQL Server. Ensure docker compose is running or set TEST_SQL_CONNSTR.");
    }

    [Fact]
    public async Task Crud_UserEntity_RoundTrip_Succeeds()
    {
        var connStr = GetConnStr()!;
        if (!CanReachSql(connStr))
        {
            return; // skip if SQL not available
        }

        await using var ctx = CreateContext(connStr);

        // Create
        var uid = $"test-crud-{Guid.NewGuid():N}";
        var user = new User
        {
            FirebaseUid = uid,
            Username = "CrudTester",
            Email = $"{uid}@test.local",
            Money = 1.23m,
            Electricity = 4.56m,
            PrestigeLevel = 0,
            CreatedAt = DateTime.UtcNow,
            LastSyncAt = DateTime.UtcNow
        };
        ctx.Users.Add(user);
        await ctx.SaveChangesAsync();

        // Read
        var fromDb = await ctx.Users.Where(u => u.FirebaseUid == uid).FirstOrDefaultAsync();
        Assert.NotNull(fromDb);
        Assert.Equal(1.23m, fromDb!.Money);

        // Update
        fromDb.Money = 9.87m;
        await ctx.SaveChangesAsync();

        var updated = await ctx.Users.Where(u => u.FirebaseUid == uid).Select(u => u.Money).FirstAsync();
        Assert.Equal(9.87m, updated);

        // Delete
        ctx.Users.Remove(fromDb);
        await ctx.SaveChangesAsync();

        var exists = await ctx.Users.AnyAsync(u => u.FirebaseUid == uid);
        Assert.False(exists);
    }

    [Fact]
    public async Task SeedData_IsPresent_And_Conforms()
    {
        var connStr = GetConnStr()!;
        if (!CanReachSql(connStr))
        {
            return; // skip if SQL not available
        }

        await using var ctx = CreateContext(connStr);

        // At least the 10 seed users if seed ran
        var userCount = await ctx.Users.CountAsync();
        Assert.True(userCount >= 10, $"Expected >= 10 users after seed, found {userCount}.");

        // Leaderboard entries should match users
        var leaderboardCount = await ctx.LeaderboardEntries.CountAsync();
        Assert.True(leaderboardCount >= 10, $"Expected >= 10 leaderboard entries, found {leaderboardCount}.");
    }
}
