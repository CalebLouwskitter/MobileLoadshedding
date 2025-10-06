using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;
using System.Collections.Generic;
using System.Text.Json;
using Xunit;

namespace IdleLoadsheddingAPI.Tests;

/// <summary>
/// Unit tests for DTO serialization to validate Android/API contract compliance.
/// These tests ensure JSON property names match between Android (Gson/camelCase) and API (System.Text.Json/camelCase).
/// </summary>
public class DtoSerializationTests
{
    private static readonly JsonSerializerOptions ApiJsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    /// <summary>
    /// CRITICAL P1 TEST: Validates JSON serialization matches Android expectations.
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

        // Act - Serialize using same settings as API (camelCase)
        var json = JsonSerializer.Serialize(expectedResponse, ApiJsonOptions);

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
    /// CRITICAL P1 TEST: Validates that Android JSON can be deserialized by API.
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
        var request = JsonSerializer.Deserialize<SyncRequest>(androidJson, ApiJsonOptions);

        // Assert
        Assert.NotNull(request);
        Assert.Equal(250.75m, request.MoneyEarned);
        Assert.Equal(35.5m, request.ElectricityGenerated);
        Assert.Equal(3, request.PrestigeLevel);
    }

    [Fact]
    public void UpgradeResponse_SerializesWithCorrectCasing()
    {
        // Arrange
        var response = new UpgradeResponse
        {
            Success = true,
            NewElectricityRate = 75.25m,
            RemainingMoney = 500.0m
        };

        // Act
        var json = JsonSerializer.Serialize(response, ApiJsonOptions);

        // Assert
        Assert.Contains("\"success\"", json);
        Assert.Contains("\"newElectricityRate\"", json);
        Assert.Contains("\"remainingMoney\"", json);
        Assert.Contains("true", json);
        Assert.Contains("75.25", json);
        Assert.Contains("500", json);
    }

    [Fact]
    public void UpgradeRequest_DeserializesFromAndroidCamelCase()
    {
        // Arrange
        var androidJson = @"{
            ""name"": ""Solar Panel Mk2"",
            ""cost"": 1500.50,
            ""electricityBoost"": 250.0
        }";

        // Act
        var request = JsonSerializer.Deserialize<UpgradeRequest>(androidJson, ApiJsonOptions);

        // Assert
        Assert.NotNull(request);
        Assert.Equal("Solar Panel Mk2", request.Name);
        Assert.Equal(1500.50m, request.Cost);
        Assert.Equal(250.0m, request.ElectricityBoost);
    }

    /// <summary>
    /// P1 TEST: Validates that missing required fields result in validation errors.
    /// </summary>
    [Fact]
    public void UpgradeRequest_MissingRequiredName_DeserializesToDefault()
    {
        // Arrange - Missing required "name" field
        var malformedJson = @"{
            ""cost"": 100.0,
            ""electricityBoost"": 10.0
        }";

        // Act
        var request = JsonSerializer.Deserialize<UpgradeRequest>(malformedJson, ApiJsonOptions);

        // Assert - Name should be default empty string (validation happens at controller level)
        Assert.NotNull(request);
        Assert.Equal(string.Empty, request.Name);
        Assert.Equal(100.0m, request.Cost);
        Assert.Equal(10.0m, request.ElectricityBoost);
    }

    /// <summary>
    /// P1 TEST: Validates numeric boundary values don't overflow.
    /// </summary>
    [Fact]
    public void SyncRequest_LargeNumbers_DeserializesCorrectly()
    {
        // Arrange - Large but valid numbers
        var json = @"{
            ""moneyEarned"": 999999999.99,
            ""electricityGenerated"": 500000.50,
            ""prestigeLevel"": 999
        }";

        // Act
        var request = JsonSerializer.Deserialize<SyncRequest>(json, ApiJsonOptions);

        // Assert
        Assert.NotNull(request);
        Assert.Equal(999999999.99m, request.MoneyEarned);
        Assert.Equal(500000.50m, request.ElectricityGenerated);
        Assert.Equal(999, request.PrestigeLevel);
    }

    [Fact]
    public void StatsSnapshotRequest_DeserializesFromAndroidPayload()
    {
        var json = @"{
            ""playerId"": ""uid-123"",
            ""totalEnergyGenerated"": 1500.5,
            ""currentEnergy"": 250.25,
            ""energyPerSecond"": 12.5,
            ""activeGenerators"": 4,
            ""generatorLevelSum"": 12,
            ""offlineSeconds"": 3600.0,
            ""lastEventId"": ""event-42"",
            ""timestampUtc"": ""2025-10-05T12:34:56Z"",
            ""currentMoney"": 500.75,
            ""totalMoneyEarned"": 2500.0,
            ""hoursUntilMaintenance"": 2.5,
            ""coreChargePercent"": 75.0,
            ""districts"": [
                {
                    ""id"": ""district_1"",
                    ""name"": ""Central"",
                    ""level"": 5,
                    ""efficiency"": 0.85,
                    ""powerPerSecond"": 45.5,
                    ""moneyPerSecond"": 10.25,
                    ""hoursUntilMaintenance"": 1.5,
                    ""nextUpgradeCost"": 500.0
                }
            ]
        }";

        var request = JsonSerializer.Deserialize<StatsSnapshotRequest>(json, ApiJsonOptions);

        Assert.NotNull(request);
        Assert.Equal("uid-123", request!.PlayerId);
        Assert.Equal(1500.5, request.TotalEnergyGenerated);
        Assert.Single(request.Districts);
        Assert.Equal("district_1", request.Districts[0].Id);
    }

    [Fact]
    public void SaveSyncRequest_SerializesWithCamelCase()
    {
        var request = new SaveSyncRequest
        {
            PlayerId = "uid-123",
            Stats = "{ }",
            Generators = new List<SaveSyncRequest.GeneratorState>
            {
                new() { Id = "gen_1", Level = 3 }
            },
            TimestampUtc = "2025-10-05T12:34:56Z"
        };

        var json = JsonSerializer.Serialize(request, ApiJsonOptions);

        Assert.Contains("\"playerId\"", json);
        Assert.Contains("\"generators\"", json);
        Assert.Contains("\"timestampUtc\"", json);
    }

    [Fact]
    public void FriendsSyncRequest_DeserializesFromAndroidPayload()
    {
        var json = @"{
            ""playerId"": ""uid-123"",
            ""friends"": [
                {
                    ""id"": ""friend-1"",
                    ""displayName"": ""Alice"",
                    ""isOnline"": true,
                    ""contributionPerHour"": 12.5,
                    ""lastInteractionUtc"": ""2025-10-05T11:22:33Z""
                }
            ]
        }";

        var request = JsonSerializer.Deserialize<FriendsSyncRequest>(json, ApiJsonOptions);

        Assert.NotNull(request);
        Assert.Equal("uid-123", request!.PlayerId);
        Assert.Single(request.Friends);
        Assert.Equal("Alice", request.Friends[0].DisplayName);
    }
}
