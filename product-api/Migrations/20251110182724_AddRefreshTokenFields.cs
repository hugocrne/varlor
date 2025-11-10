using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace product_api.Migrations
{
    /// <inheritdoc />
    public partial class AddRefreshTokenFields : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "replaced_by_token_id",
                table: "user_sessions",
                type: "character varying(255)",
                maxLength: 255,
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "revocation_reason",
                table: "user_sessions",
                type: "character varying(255)",
                maxLength: 255,
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "revoked_at",
                table: "user_sessions",
                type: "timestamp",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "token_hash",
                table: "user_sessions",
                type: "character varying(128)",
                maxLength: 128,
                nullable: false,
                defaultValue: "");

            migrationBuilder.CreateIndex(
                name: "idx_user_sessions_token_hash",
                table: "user_sessions",
                column: "token_hash",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "idx_user_sessions_token_hash",
                table: "user_sessions");

            migrationBuilder.DropColumn(
                name: "replaced_by_token_id",
                table: "user_sessions");

            migrationBuilder.DropColumn(
                name: "revocation_reason",
                table: "user_sessions");

            migrationBuilder.DropColumn(
                name: "revoked_at",
                table: "user_sessions");

            migrationBuilder.DropColumn(
                name: "token_hash",
                table: "user_sessions");
        }
    }
}
