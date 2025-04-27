package org.bcnlab.beaconlabsproxy.ServerGuard;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bcnlab.beaconlabsproxy.Database.ServerGuardDatabase;

import java.sql.*;
import java.time.Duration;
import java.util.*;

public class ServerGuardManager {
    private static final Set<String> alwaysAllowedServers = new HashSet<>();
    private static boolean isServerGuardEnabled = false;
    private static Set<String> allowedServers = new HashSet<>();
    private static Map<String, String> serverPermissions = new HashMap<>(); // Map to store server-specific permission requirements

    public static void setAlwaysAllowedServers(Set<String> servers) {
        alwaysAllowedServers.clear();
        alwaysAllowedServers.addAll(servers);
    }

    public static void setEnabled(boolean enabled) {
        isServerGuardEnabled = enabled;
    }

    public static void setAllowedServers(Set<String> servers) {
        allowedServers.clear();
        allowedServers.addAll(servers);
    }

    /**
     * Returns the set of configured allowed servers.
     */
    public static Set<String> getAllowedServers() {
        return new HashSet<>(allowedServers);
    }

    public static boolean isServerGuardEnabled() {
        return isServerGuardEnabled;
    }
    public static boolean isAllowed(ProxiedPlayer player, String serverName) {
        // If ServerGuard is disabled, allow all connections
        if (!isServerGuardEnabled) {
            return true;
        }

        // 1. Check if server is in the allowed servers list (these should be accessible by everyone)
        if (allowedServers.contains(serverName.toLowerCase())) {
            return true;
        }

        // 2. Check always-allowed list
        if (alwaysAllowedServers.contains(serverName.toLowerCase())) {
            return true;
        }

        // 3. Check if player has bypass permission
        if (player.hasPermission("beaconlabs.serverguard.bypass")) {
            return true;
        }
        
        // 4. Check if server requires a specific permission and if player has it
        String requiredPermission = serverPermissions.get(serverName.toLowerCase());
        if (requiredPermission != null && !requiredPermission.isEmpty()) {
            if (player.hasPermission(requiredPermission)) {
                return true;
            }
        }

        // 4. Check invites database
        try (Connection conn = ServerGuardDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT permanent, valid_until FROM invites WHERE player_uuid = ? AND server = ?")) {

            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, serverName.toLowerCase());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean permanent = rs.getBoolean("permanent");
                Timestamp validUntil = rs.getTimestamp("valid_until");

                if (permanent) {
                    return true;
                } else if (validUntil != null && validUntil.after(new Timestamp(System.currentTimeMillis()))) {
                    return true; // still valid
                } else {
                    // temp invite expired, cleanup
                    removeInvite(player.getUniqueId(), serverName);
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void invitePlayer(UUID playerUUID, String serverName, boolean permanent, Duration duration) {
        try (Connection conn = ServerGuardDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO invites (player_uuid, server, permanent, valid_until) VALUES (?, ?, ?, ?)")) {

            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, serverName.toLowerCase());
            stmt.setBoolean(3, permanent);

            if (permanent || duration == null) {
                stmt.setNull(4, Types.TIMESTAMP);
            } else {
                long validUntil = System.currentTimeMillis() + duration.toMillis();
                stmt.setTimestamp(4, new Timestamp(validUntil));
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeInvite(UUID playerUUID, String serverName) {
        try (Connection conn = ServerGuardDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM invites WHERE player_uuid = ? AND server = ?")) {

            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, serverName.toLowerCase());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<UUID> getInvitedPlayers(String serverName) {
        List<UUID> invitedPlayers = new ArrayList<>();

        try (Connection conn = ServerGuardDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT player_uuid FROM invites WHERE server = ?")) {

            stmt.setString(1, serverName.toLowerCase());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                invitedPlayers.add(UUID.fromString(rs.getString("player_uuid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return invitedPlayers;
    }

    public static List<String> getInvitedServers(UUID playerUUID) {
        List<String> invitedServers = new ArrayList<>();

        try (Connection conn = ServerGuardDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT server FROM invites WHERE player_uuid = ?")) {

            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                invitedServers.add(rs.getString("server"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return invitedServers;
    }

    public static void setServerPermissions(Map<String, String> permissions) {
        serverPermissions.clear();
        serverPermissions.putAll(permissions);
    }
    
    public static Map<String, String> getServerPermissions() {
        return new HashMap<>(serverPermissions);
    }
}
