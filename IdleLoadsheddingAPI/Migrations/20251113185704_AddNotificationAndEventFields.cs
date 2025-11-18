using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace IdleLoadsheddingAPI.Migrations
{
    /// <inheritdoc />
    public partial class AddNotificationAndEventFields : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "FcmDeviceId",
                table: "Users",
                type: "nvarchar(128)",
                maxLength: 128,
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "FcmPlatform",
                table: "Users",
                type: "nvarchar(32)",
                maxLength: 32,
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "FcmToken",
                table: "Users",
                type: "nvarchar(512)",
                maxLength: 512,
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "FcmTokenUpdatedAt",
                table: "Users",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "FriendCode",
                table: "Users",
                type: "nvarchar(10)",
                maxLength: 10,
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "LastNotificationActionAt",
                table: "Users",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<bool>(
                name: "NotifEnableEvents",
                table: "Users",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<bool>(
                name: "NotifEnableFriends",
                table: "Users",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<bool>(
                name: "NotifEnableLeaderboard",
                table: "Users",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<bool>(
                name: "NotifEnableMaintenance",
                table: "Users",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<bool>(
                name: "NotifEnableMilestones",
                table: "Users",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<bool>(
                name: "NotifEnableOfflineEarnings",
                table: "Users",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<bool>(
                name: "NotifEnableSystemAnnouncements",
                table: "Users",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<DateTime>(
                name: "NotificationPreferencesUpdatedAt",
                table: "Users",
                type: "datetime2",
                nullable: true);

            migrationBuilder.CreateTable(
                name: "GameEventDefinitions",
                columns: table => new
                {
                    EventDefinitionId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    Code = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    EventCode = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    Title = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: false),
                    Description = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    EffectJson = table.Column<string>(type: "nvarchar(max)", nullable: true),
                    TriggerWeight = table.Column<int>(type: "int", nullable: false),
                    DurationSeconds = table.Column<int>(type: "int", nullable: false),
                    ProductionMultiplier = table.Column<double>(type: "float", nullable: false),
                    IsActive = table.Column<bool>(type: "bit", nullable: false),
                    StartTimeUtc = table.Column<DateTime>(type: "datetime2", nullable: false),
                    EndTimeUtc = table.Column<DateTime>(type: "datetime2", nullable: false),
                    CreatedAtUtc = table.Column<DateTime>(type: "datetime2", nullable: false),
                    RowVersion = table.Column<byte[]>(type: "rowversion", rowVersion: true, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_GameEventDefinitions", x => x.EventDefinitionId);
                });

            migrationBuilder.CreateTable(
                name: "GameEvents",
                columns: table => new
                {
                    EventId = table.Column<string>(type: "nvarchar(64)", maxLength: 64, nullable: false),
                    Title = table.Column<string>(type: "nvarchar(200)", maxLength: 200, nullable: false),
                    Description = table.Column<string>(type: "nvarchar(1000)", maxLength: 1000, nullable: true),
                    IsStarting = table.Column<bool>(type: "bit", nullable: false),
                    DurationSeconds = table.Column<double>(type: "float", nullable: false),
                    ProductionMultiplier = table.Column<double>(type: "float", nullable: false),
                    EventTimestampUtc = table.Column<DateTime>(type: "datetime2", nullable: false),
                    RecordedAt = table.Column<DateTime>(type: "datetime2", nullable: false),
                    RowVersion = table.Column<byte[]>(type: "rowversion", rowVersion: true, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_GameEvents", x => x.EventId);
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "GameEventDefinitions");

            migrationBuilder.DropTable(
                name: "GameEvents");

            migrationBuilder.DropColumn(
                name: "FcmDeviceId",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "FcmPlatform",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "FcmToken",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "FcmTokenUpdatedAt",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "FriendCode",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "LastNotificationActionAt",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "NotifEnableEvents",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "NotifEnableFriends",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "NotifEnableLeaderboard",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "NotifEnableMaintenance",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "NotifEnableMilestones",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "NotifEnableOfflineEarnings",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "NotifEnableSystemAnnouncements",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "NotificationPreferencesUpdatedAt",
                table: "Users");
        }
    }
}
