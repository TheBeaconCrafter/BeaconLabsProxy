package org.bcnlabs.beaconlabsproxy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabasePunishments {
    private static Connection connection;

    public static void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:./plugins/BeaconLabsProxy/beaconlabsproxy_punishments.db");
            createPunishmentsTable();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createPunishmentsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS punishments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid TEXT NOT NULL," +
                "last_name TEXT," +
                "punisher TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "reason TEXT," +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "end_time TIMESTAMP," +
                "active BOOLEAN DEFAULT 1" +
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
                initialize(); // Reinitialize the connection if it's null or closed
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
