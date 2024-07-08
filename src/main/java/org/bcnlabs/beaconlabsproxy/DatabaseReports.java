package org.bcnlabs.beaconlabsproxy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseReports {
    private static Connection connection;

    public static void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:./plugins/BeaconLabsProxy/beaconlabsproxy_reports.db");
            createReportTable();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createReportTable() {
        String sql = "CREATE TABLE IF NOT EXISTS reports (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "reporter TEXT," +
                "reported TEXT," +
                "reason TEXT," +
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
                initialize(); // Reinitialize the connection if it's null or closed
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
