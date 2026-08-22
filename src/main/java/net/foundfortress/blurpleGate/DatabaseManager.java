package net.foundfortress.blurpleGate;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public class DatabaseManager {
    Connection conn;

    public DatabaseManager connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException _) {
            BlurpleGate.getPlugin().getComponentLogger().error("PaperMC did not provide org.sqlite.JDBC. This error " +
                "definitely shouldn't occur under normal circumstances.");
        }

        conn = DriverManager.getConnection("jdbc:sqlite:plugins/BlurpleGate/database.db"); // todo: perhaps support "real" databases idk sqlites probably fine actually
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
        return this;
    }

    public void insertTokens(Tokens tokens) throws SQLException {
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

    public Tokens getTokensFromMcUuid(UUID mcUuid) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM tokens WHERE mc_uuid=?;")) {
            pstmt.setString(1, mcUuid.toString());
            return getTokens(pstmt);
        }
    }

    public Tokens getTokensFromDiscordId(String discordId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM tokens WHERE discord_id=?;")) {
            pstmt.setString(1, discordId);
            return getTokens(pstmt);
        }
    }

    public void disconnect() throws SQLException {
        conn.close();
    }

    private Tokens getTokens(PreparedStatement pstmt) throws SQLException {
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
        public static Tokens fromDiscordTokenResponse(UUID mcUuid, long discordId,
                                                      LinkingManager.DiscordTokenResponse discordTokenResponse) {
            long unixTimestamp = Instant.now().getEpochSecond();
            return new Tokens(mcUuid, discordId, discordTokenResponse.accessToken(),
                discordTokenResponse.refreshToken(), unixTimestamp + discordTokenResponse.expiresIn());
        }
    }
}
