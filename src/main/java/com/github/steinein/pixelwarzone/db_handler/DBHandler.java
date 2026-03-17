package com.github.steinein.pixelwarzone.db_handler;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DBHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBHandler.class);
    private final HikariDataSource dataSource;

    public DBHandler(DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName(config.getPoolName());
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getIp() + ":" + config.getPort()
                + "/" + config.getDatabase() + "?useSSL=false&allowPublicKeyRetrieval=true");
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS pixelwarzone_players (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(64) DEFAULT ''," +
                "wins INT DEFAULT 0," +
                "losses INT DEFAULT 0" +
                ");";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Add name column if upgrading from an older version without it
            try { stmt.execute("ALTER TABLE pixelwarzone_players ADD COLUMN name VARCHAR(64) DEFAULT ''"); }
            catch (SQLException ignored) {}
        } catch (SQLException e) {
            LOGGER.error("Error creating tables: {}", e.getMessage());
        }
    }

    public void savePlayer(WarzonePlayer warzonePlayer) {
        String sql = "INSERT INTO pixelwarzone_players (id, name, wins, losses) VALUES(?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), wins = VALUES(wins), losses = VALUES(losses)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warzonePlayer.getPlayer().getUUID().toString());
            pstmt.setString(2, warzonePlayer.getPlayer().getName().getString());
            pstmt.setInt(3, warzonePlayer.getWins());
            pstmt.setInt(4, warzonePlayer.getLosses());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error saving player: {}", e.getMessage());
        }
    }

    public void loadPlayer(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT wins, losses FROM pixelwarzone_players WHERE id = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            WarzonePlayer wp = PixelWarzone.getInstance().playerDataMap.get(uuid);
            if (wp != null && rs.next()) {
                wp.setWins(rs.getInt("wins"));
                wp.setLosses(rs.getInt("losses"));
            }
        } catch (SQLException e) {
            LOGGER.error("Error loading player {}: {}", uuid, e.getMessage());
        }
    }

    public List<WarzonePlayer> getTopPlayers() {
        List<WarzonePlayer> pixelwarzone_players = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, name, wins, losses FROM pixelwarzone_players ORDER BY wins DESC LIMIT 10")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("id"));
                String name = rs.getString("name");
                WarzonePlayer wp = new WarzonePlayer(uuid, name, rs.getInt("wins"), rs.getInt("losses"));
                pixelwarzone_players.add(wp);
            }
        } catch (SQLException e) {
            LOGGER.error("Error getting top pixelwarzone_players: {}", e.getMessage());
        }
        return pixelwarzone_players;
    }
}
