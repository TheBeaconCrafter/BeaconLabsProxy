package org.bcnlabs.beaconlabsproxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PunishmentChecker {

    public static boolean isPlayerMuted(String playerUUID) {
        return isPlayerPunished(playerUUID, "mute");
    }

    public static boolean isPlayerBanned(String playerUUID) {
        return isPlayerPunished(playerUUID, "ban");
    }

    private static boolean isPlayerPunished(String playerUUID, String punishmentType) {
        String sql = "SELECT 1 FROM punishments WHERE player_uuid = ? AND type = ? AND active = 1 AND (end_time IS NULL OR end_time > CURRENT_TIMESTAMP)";
        try (Connection conn = DatabasePunishments.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, punishmentType);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
