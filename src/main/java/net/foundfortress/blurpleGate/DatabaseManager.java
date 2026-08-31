/*
 * net.foundfortress.blurpleGate.DatabaseManager
 * Copyright (C) 2026 FoundFortress
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package net.foundfortress.blurpleGate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public class DatabaseManager {
    Connection conn;

    public void connect() throws SQLException {
        conn = DriverManager.getConnection(Constants.DB_URL); // todo: perhaps support "real" databases idk sqlites probably fine actually
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tokens(
                  mc_uuid TEXT PRIMARY KEY,
                  discord_id INTEGER UNIQUE NOT NULL,
                  access_token TEXT NOT NULL,
                  refresh_token TEXT NOT NULL,
                  access_token_expires DATETIME NOT NULL
                );""");
        }
    }

    public void insertTokens(@NotNull Tokens tokens) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("""
                INSERT INTO tokens(mc_uuid, discord_id, access_token, refresh_token, access_token_expires)
                VALUES (?, ?, ?, ?, ?);""")) {
            pstmt.setString(1, tokens.mcUuid.toString());
            pstmt.setLong(2, tokens.discordId);
            pstmt.setString(3, tokens.accessToken);
            pstmt.setString(4, tokens.refreshToken);
            pstmt.setLong(5, tokens.accessTokenExpires);
            pstmt.execute();
        }
    }

    public @Nullable Tokens getTokensFromMcUuid(@NotNull UUID mcUuid) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM tokens WHERE mc_uuid=?;")) {
            pstmt.setString(1, mcUuid.toString());
            return getTokens(pstmt);
        }
    }

    public @Nullable Tokens getTokensFromDiscordId(@NotNull String discordId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM tokens WHERE discord_id=?;")) {
            pstmt.setString(1, discordId);
            return getTokens(pstmt);
        }
    }

    public void disconnect() throws SQLException {
        conn.close();
    }

    private @Nullable Tokens getTokens(PreparedStatement pstmt) throws SQLException {
        try (ResultSet resultSet = pstmt.executeQuery()) {
            if (!resultSet.next()) return null;
            return new Tokens(
                UUID.fromString(resultSet.getString("mc_uuid")),
                resultSet.getLong("discord_id"),
                resultSet.getString("access_token"),
                resultSet.getString("refresh_token"),
                resultSet.getLong("access_token_expires")
            );
        }
    }

    public record Tokens(
        UUID mcUuid,
        long discordId,
        String accessToken,
        String refreshToken,
        long accessTokenExpires // Unix Timestamp
    ) {
        public static @NotNull Tokens fromDiscordTokenResponse(@NotNull UUID mcUuid, long discordId,
                                                               @NotNull LinkingManager.DiscordTokenResponse
                                                                   discordTokenResponse) {
            long unixTimestamp = Instant.now().getEpochSecond();
            return new Tokens(mcUuid, discordId, discordTokenResponse.accessToken(),
                discordTokenResponse.refreshToken(), unixTimestamp + discordTokenResponse.expiresIn());
        }
    }
}
