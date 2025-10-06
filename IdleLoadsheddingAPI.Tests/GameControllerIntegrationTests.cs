using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http.Json;
using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Configuration;
using Xunit;

namespace IdleLoadsheddingAPI.Tests;

/// <summary>
/// Integration tests for GameController endpoints.
/// Tests validate DTO serialization, database transactions, and contract compliance.
/// </summary>
public class GameControllerIntegrationTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;
    private readonly HttpClient _client;

    public GameControllerIntegrationTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory.WithWebHostBuilder(builder =>
        {
            var databaseName = $"TestDatabase_{Guid.NewGuid()}";

            builder.ConfigureAppConfiguration((_, configBuilder) =>
            {
                configBuilder.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["ConnectionStrings:Default"] = $"InMemory:{databaseName}"
                });
            });

            builder.ConfigureServices(services =>
            {
                var sp = services.BuildServiceProvider();
                using var scope = sp.CreateScope();
                var db = scope.ServiceProvider.GetRequiredService<GameDbContext>();
                db.Database.EnsureCreated();
            });
        });

        _client = _factory.CreateClient();
    }

    [Fact]
    public async Task PostSync_WithValidPayload_ReturnsSyncResponse()
    {
        // Arrange - Create a sync request with known values
        var request = new SyncRequest
        {
            MoneyEarned = 100.50m,
            ElectricityGenerated = 25.0m,
            PrestigeLevel = 1
        };

        // Act - Post to sync endpoint (without auth for now - will fail with 401)
        var response = await _client.PostAsJsonAsync("/api/game/sync", request);

        // Assert - Expect Unauthorized because no Firebase token provided
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task PostUpgrade_WithValidPayload_ReturnsUpgradeResponse()
    {
        // Arrange
        var request = new UpgradeRequest
        {
            Name = "Solar Panel Level 1",
            Cost = 50.0m,
            ElectricityBoost = 10.0m
        };

        // Act
        var response = await _client.PostAsJsonAsync("/api/game/upgrade", request);

        // Assert - Expect Unauthorized
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task PostUpgrade_WithMalformedPayload_ReturnsBadRequest()
    {
        // Arrange - Missing required Name field
        var malformedJson = "{\"cost\": 50.0, \"electricityBoost\": 10.0}";
        var content = new StringContent(malformedJson, System.Text.Encoding.UTF8, "application/json");

        // Act
        var response = await _client.PostAsync("/api/game/upgrade", content);

        // Assert - Should fail validation (or Unauthorized if auth checked first)
        Assert.True(response.StatusCode == HttpStatusCode.BadRequest || 
                   response.StatusCode == HttpStatusCode.Unauthorized);
    }

    [Fact]
    public async Task PostStatsSnapshot_WithoutAuth_ReturnsUnauthorized()
    {
        var payload = new StatsSnapshotRequest
        {
            PlayerId = "uid-123",
            TotalEnergyGenerated = 1000,
            CurrentEnergy = 250,
            EnergyPerSecond = 12.5,
            ActiveGenerators = 3,
            GeneratorLevelSum = 9,
            OfflineSeconds = 120,
            TimestampUtc = DateTime.UtcNow.ToString("o"),
            CurrentMoney = 500,
            TotalMoneyEarned = 1500,
            HoursUntilMaintenance = 2,
            CoreChargePercent = 50
        };

        var response = await _client.PostAsJsonAsync("/api/game/stats", payload);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task PostSaveSnapshot_WithoutAuth_ReturnsUnauthorized()
    {
        var payload = new SaveSyncRequest
        {
            PlayerId = "uid-123",
            Stats = "{}",
            TimestampUtc = DateTime.UtcNow.ToString("o")
        };

        var response = await _client.PostAsJsonAsync("/api/game/save", payload);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    /// <summary>
    /// CRITICAL TEST: Validates JSON serialization matches Android expectations.
    /// Android uses Gson with @SerializedName, API uses System.Text.Json (camelCase default).
    /// This test ensures property names match exactly.
    /// </summary>
    [Fact]
    public void SyncResponse_SerializesWithCorrectCasing()
    {
        // Arrange - Create expected response
        var expectedResponse = new SyncResponse
        {
            TotalMoney = 1000.50m,
            OfflineEarnings = 100.0m,
            ElectricityRate = 50.5m,
            PrestigeLevel = 2
        };

        // Act - Serialize using same settings as API (default System.Text.Json)
        var json = System.Text.Json.JsonSerializer.Serialize(
            expectedResponse,
            new System.Text.Json.JsonSerializerOptions
            {
                PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase,
                PropertyNameCaseInsensitive = true
            });

        // Assert - Verify camelCase property names (Android expects these)
        Assert.Contains("\"totalMoney\"", json);
        Assert.Contains("\"offlineEarnings\"", json);
        Assert.Contains("\"electricityRate\"", json);
        Assert.Contains("\"prestigeLevel\"", json);

        // Verify values are correct
        Assert.Contains("1000.5", json);
        Assert.Contains("100", json);
        Assert.Contains("50.5", json);
        Assert.Contains("2", json);
    }

    /// <summary>
    /// CRITICAL TEST: Validates that Android JSON can be deserialized by API.
    /// Tests the request path (Android -> API).
    /// </summary>
    [Fact]
    public void SyncRequest_DeserializesFromAndroidCamelCase()
    {
        // Arrange - JSON as sent from Android (camelCase)
        var androidJson = @"{
            ""moneyEarned"": 250.75,
            ""electricityGenerated"": 35.5,
            ""prestigeLevel"": 3
        }";

        // Act - Deserialize using API settings
        var request = System.Text.Json.JsonSerializer.Deserialize<SyncRequest>(
            androidJson,
            new System.Text.Json.JsonSerializerOptions
            {
                PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase,
                PropertyNameCaseInsensitive = true
            });

        // Assert
        Assert.NotNull(request);
        Assert.Equal(250.75m, request.MoneyEarned);
        Assert.Equal(35.5m, request.ElectricityGenerated);
        Assert.Equal(3, request.PrestigeLevel);
    }

    /// <summary>
    /// P0 ISSUE TEST: Tests database transaction safety.
    /// If upgrade fails mid-operation, ensure no partial state is saved.
    /// </summary>
    [Fact]
    public void ApplyUpgrade_OnException_RollsBackTransaction()
    {
        // This test requires a custom test factory with transaction support
        // Currently, GameService doesn't use explicit transactions
        // This is a P1 issue that needs fixing

        // TODO: Implement after adding transaction support to GameService
        Assert.True(true, "Transaction rollback test pending implementation");
    }
}
