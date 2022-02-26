package com.github.steinein.pixelwarzone.db_handler;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

public class DBHandler {

    public void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS players (\n"
                + " id varchar(64) PRIMARY KEY,\n"
                + " wins INT DEFAULT 0,\n"
                + " losses INT DEFAULT 0,\n"
                + ");";

        executeSQLStatement(sql);
    }

    public void savePlayer(WarzonePlayer warzonePlayer) {
        String sql = "INSERT INTO players (id, wins, losses) VALUES(?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "wins = ?," +
                "losses = ?";

        try (Connection conn = PixelWarzone.getInstance().getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            // set parameter
            pstmt.setString(1, warzonePlayer.getSpongePlayer().getUniqueId().toString());
            pstmt.setLong(2, warzonePlayer.getWins());
            pstmt.setLong(3, warzonePlayer.getLosses());

            pstmt.setLong(4, warzonePlayer.getWins());
            pstmt.setLong(5, warzonePlayer.getLosses());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            PixelWarzone.getInstance().getLogger().warn(e.getMessage());
        }
    }

    public void loadPlayer(User user) {
        WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(PixelWarzone.getInstance(), (Player) user);

        try (Connection conn = PixelWarzone.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players where id='" + user.getUniqueId().toString() + "'");
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                warzonePlayer.setWins(results.getInt("wins"));
                warzonePlayer.setLosses(results.getInt("losses"));
            }
            PixelWarzone.getInstance().playerDataMap.put(user.getUniqueId(), warzonePlayer);
        } catch (SQLException e) {
            PixelWarzone.getInstance().getLogger().warn(e.getMessage());
        }
    }

    public List<WarzonePlayer> getTopPlayers() {
        List<WarzonePlayer> players = new ArrayList<>();
        Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);

        if (!userStorage.isPresent()) {
            return players;
        }

        try (Connection conn = PixelWarzone.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players ORDER by wins DESC LIMIT 10");
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                UUID uuid = UUID.fromString(results.getString("id"));
                User user = userStorage.get().get(uuid).orElse(null);

                if (user == null) {
                    continue;
                }

                WarzonePlayer warzonePlayer = WarzonePlayer.fromUser(PixelWarzone.getInstance(), user);
                warzonePlayer.setWins(results.getInt("wins"));
                warzonePlayer.setLosses(results.getInt("losses"));
                players.add(warzonePlayer);
            }
        } catch (SQLException e) {
            PixelWarzone.getInstance().getLogger().warn(e.getMessage());
        }

        return players;
    }

    private static void executeSQLStatement(String sql) {
        try (Connection conn = PixelWarzone.getInstance().getConnection()) {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            PixelWarzone.getInstance().getLogger().warn(e.getMessage());
        }
    }
}
