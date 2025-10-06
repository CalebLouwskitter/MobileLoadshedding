using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace IdleLoadsheddingAPI.Migrations
{
    /// <inheritdoc />
    public partial class AddSnapshotAndFriendSyncTables : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "SaveSnapshots",
                columns: table => new
                {
                    SaveSnapshotId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    UserId = table.Column<int>(type: "int", nullable: false),
                    PayloadJson = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    RecordedAtUtc = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SaveSnapshots", x => x.SaveSnapshotId);
                    table.ForeignKey(
                        name: "FK_SaveSnapshots_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "UserId",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "StatsSnapshots",
                columns: table => new
                {
                    StatsSnapshotId = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    UserId = table.Column<int>(type: "int", nullable: false),
                    PayloadJson = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    RecordedAtUtc = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_StatsSnapshots", x => x.StatsSnapshotId);
                    table.ForeignKey(
                        name: "FK_StatsSnapshots_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "UserId",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_SaveSnapshots_UserId_RecordedAtUtc",
                table: "SaveSnapshots",
                columns: new[] { "UserId", "RecordedAtUtc" });

            migrationBuilder.CreateIndex(
                name: "IX_StatsSnapshots_UserId_RecordedAtUtc",
                table: "StatsSnapshots",
                columns: new[] { "UserId", "RecordedAtUtc" });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "SaveSnapshots");

            migrationBuilder.DropTable(
                name: "StatsSnapshots");
        }
    }
}
