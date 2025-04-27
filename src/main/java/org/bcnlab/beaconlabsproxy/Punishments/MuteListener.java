package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MuteListener implements Listener {

    private final BeaconLabsProxy plugin;

    public MuteListener(BeaconLabsProxy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        UUID uuid = player.getUniqueId();

        if (isPlayerMuted(uuid)) {
            String message = event.getMessage();
            if (!message.startsWith("/")) {
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You are muted and cannot send messages."));
                sendMuteReason(player, uuid); // Send mute reason for commands
                event.setCancelled(true);
            } else {
                String command = message.split(" ")[0].substring(1).toLowerCase();
                if (isMutedCommand(command)) {
                    player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You are muted and cannot use this command."));
                    sendMuteReason(player, uuid); // Send mute reason for commands
                    event.setCancelled(true);
                }
            }
        }
    }

    private void sendMuteReason(ProxiedPlayer player, UUID uuid) {
        try (Connection conn = DatabasePunishments.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT reason, end_time FROM punishments WHERE player_uuid = ? AND type = 'Mute' ORDER BY start_time DESC LIMIT 1")) {
            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String reason = rs.getString("reason");
                    long endTimeEpoch = rs.getLong("end_time");

                    // Convert epoch time to LocalDateTime
                    LocalDateTime endTime;
                    if (endTimeEpoch == 0) {
                        endTime = null; // Handle cases where there's no end time set (e.g., permanent mute)
                    } else {
                        Instant instant = Instant.ofEpochMilli(endTimeEpoch);
                        endTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    }

                    // Format end time into a readable format
                    String endTimeFormatted = endTime != null ? endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "permanent";

                    player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You are muted for: " + reason + " until " + endTimeFormatted));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving mute reason for player " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isPlayerMuted(UUID uuid) {
        try (Connection conn = DatabasePunishments.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM punishments WHERE player_uuid = ? AND type = 'Mute'")) {
            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long endTimeEpoch = rs.getLong("end_time");

                    // Convert epoch time to LocalDateTime
                    LocalDateTime endTime;
                    if (endTimeEpoch == 0) {
                        endTime = null; // Handle cases where there's no end time set (e.g., permanent mute)
                    } else {
                        Instant instant = Instant.ofEpochMilli(endTimeEpoch);
                        endTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    }

                    // Check if end time is in the future (meaning the mute is active)
                    if (endTime == null || endTime.isAfter(LocalDateTime.now())) {
                        // Player is still muted
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking mute for player " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        // No active mute found
        return false;
    }

    private boolean isMutedCommand(String command) {
        // List of commands to block for muted players
        switch (command) {
            case "msg":
            case "tell":
            case "whisper":
            case "message":
            case "say":
            case "me":
            case "w":
                return true;
            default:
                return false;
        }
    }
}
