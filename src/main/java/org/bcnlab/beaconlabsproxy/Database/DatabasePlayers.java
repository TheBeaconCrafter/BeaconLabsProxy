package org.bcnlab.beaconlabsproxy.Database;

import java.sql.*;

public class DatabasePlayers {
    private static Connection connection;

    public static void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:./plugins/BeaconLabsProxy/beaconlabsproxy_players.db");
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS playtime (" +
                "player_uuid TEXT PRIMARY KEY," +
                "last_name TEXT," +
                "total_playtime_ms INTEGER DEFAULT 0)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void ensurePlayerExists(String uuid, String name) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "INSERT OR IGNORE INTO playtime (player_uuid, last_name) VALUES (?, ?)")) {
            stmt.setString(1, uuid);
            stmt.setString(2, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addPlaytime(String uuid, long playtimeMs) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "UPDATE playtime SET total_playtime_ms = total_playtime_ms + ? WHERE player_uuid = ?")) {
            stmt.setLong(1, playtimeMs);
            stmt.setString(2, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static long getPlaytime(String uuid) {
        String sql = "SELECT total_playtime_ms FROM playtime WHERE player_uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()) {
                return rs.getLong("total_playtime_ms");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
