package org.bcnlab.beaconlabsproxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PunishmentManager {

    public static void addPunishment(String playerUUID, String lastName, String punisher, String type, String reason, long durationSeconds) {
        String sql = "INSERT INTO punishments (player_uuid, last_name, punisher, type, reason, start_time, end_time) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
        try (Connection conn = DatabasePunishments.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, lastName);
            pstmt.setString(3, punisher);
            pstmt.setString(4, type);
            pstmt.setString(5, reason);
            pstmt.setTimestamp(6, durationSeconds > 0 ? new java.sql.Timestamp(System.currentTimeMillis() + durationSeconds * 1000) : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removePunishment(int id) {
        String sql = "UPDATE punishments SET active = 0 WHERE id = ?";
        try (Connection conn = DatabasePunishments.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
