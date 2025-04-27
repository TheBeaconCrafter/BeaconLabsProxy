package org.bcnlab.beaconlabsproxy.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ServerGuardDatabase {
    private static Connection connection;

    public static void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:./plugins/BeaconLabsProxy/beaconlabsproxy_serverguard.db");
            createInvitesTable();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createInvitesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS invites (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid TEXT NOT NULL," +
                "server TEXT NOT NULL," +
                "permanent BOOLEAN NOT NULL," +
                "valid_until TIMESTAMP," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize(); // reconnect if something scuffed the connection
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}