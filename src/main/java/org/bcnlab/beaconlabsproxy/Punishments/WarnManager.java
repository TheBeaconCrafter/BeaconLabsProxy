package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class WarnManager {

    private final BeaconLabsProxy plugin;
    private final Webhooks webhooks;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WarnManager(BeaconLabsProxy plugin) {
        this.plugin = plugin;
        this.webhooks = new Webhooks(this.plugin);
    }

    // Handles banning a player
    public void banPlayer(UUID uuid, String lastName, String staffName, String reason, long durationSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unbanDate = now.plusSeconds(durationSeconds);

        // Add ban to database
        PunishmentManager.addPunishment(uuid.toString(), lastName, staffName, "ban", reason, durationSeconds);

        // Broadcast ban message to staff
        broadcastBanMessage(lastName, reason, staffName, now, unbanDate, durationSeconds);

        // Send ban webhook
        webhooks.sendBanWebhook(lastName, reason, formatDuration(durationSeconds), staffName, now.format(formatter));

        // Disconnect player
        String banMessageFormat = plugin.getBanMessageFormat();
        String formattedBanMessage = String.format(banMessageFormat, reason, unbanDate.format(formatter));
        disconnectPlayer(uuid, formattedBanMessage);
    }

    // Handles muting a player
    public void mutePlayer(UUID uuid, String lastName, String staffName, String reason, long durationSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime muteEnd = now.plusSeconds(durationSeconds);

        // Add mute to database
        addMuteToDatabase(uuid.toString(), lastName, staffName, reason, now, muteEnd);

        // Broadcast mute message to staff
        broadcastMuteMessage(lastName, reason, staffName, now, muteEnd, durationSeconds);

        // Send mute webhook
        webhooks.sendMuteWebhook(lastName, reason, formatDuration(durationSeconds), staffName, now.format(formatter), muteEnd.format(formatter));

        // Notify the muted player
        sendMuteNotification(uuid, reason, muteEnd);
    }

    // Handles kicking a player
    public void kickPlayer(UUID uuid, String lastName, String staffName, String reason) {
        LocalDateTime now = LocalDateTime.now();

        // Add kick to database
        PunishmentManager.addPunishment(uuid.toString(), lastName, staffName, "kick", reason, 0);

        // Broadcast kick message to staff
        broadcastKickMessage(lastName, reason, staffName);

        // Send kick webhook
        webhooks.sendKickWebhook(lastName, reason, staffName, now.format(formatter));

        // Disconnect player
        String kickMessageFormat = plugin.getKickMessageFormat();
        String formattedKickMessage = String.format(kickMessageFormat, reason);
        disconnectPlayer(uuid, formattedKickMessage);
    }

    private void broadcastBanMessage(String playerName, String reason, String staffName, LocalDateTime now, LocalDateTime unbanDate, long durationSeconds) {
        String durationFormatted = formatDuration(durationSeconds);
        String message = String.format(ChatColor.RED + "%s" + ChatColor.GOLD + " was banned for" + ChatColor.RED + " %s " + ChatColor.GOLD + "for %s by %s. They will be unbanned on" + ChatColor.RED + " %s.",
                playerName, reason, durationFormatted, staffName, unbanDate.format(formatter));

        for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
            if (onlinePlayer.hasPermission("beaconlabs.staff.read.ban")) {
                onlinePlayer.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + message));
            }
        }
    }

    private void broadcastMuteMessage(String playerName, String reason, String staffName, LocalDateTime now, LocalDateTime muteEnd, long durationSeconds) {
        String durationFormatted = formatDuration(durationSeconds);
        String message = String.format(ChatColor.RED + "%s" + ChatColor.GOLD + " was muted for " + ChatColor.RED + "%s " + ChatColor.GOLD + "for %s by %s. They will be unmuted on" + ChatColor.RED + " %s.",
                playerName, reason, durationFormatted, staffName, muteEnd.format(formatter));

        for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
            if (onlinePlayer.hasPermission("beaconlabs.staff.read.mute")) {
                onlinePlayer.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + message));
            }
        }
    }

    private void broadcastKickMessage(String playerName, String reason, String staffName) {
        String message = String.format(ChatColor.RED + "%s " + ChatColor.GOLD + "was kicked for " + ChatColor.RED + "%s" + ChatColor.GOLD + " by %s.",
                playerName, reason, staffName);

        for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
            if (onlinePlayer.hasPermission("beaconlabs.staff.read.kick")) {
                onlinePlayer.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + message));
            }
        }
    }

    public void addMuteToDatabase(String uuid, String lastName, String punisherName, String reason, LocalDateTime startTime, LocalDateTime endTime) {
        try (Connection conn = DatabasePunishments.getConnection()) {
            String insertSql = "INSERT INTO punishments (player_uuid, punisher, type, reason, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, uuid);
                insertStmt.setString(2, punisherName);
                insertStmt.setString(3, "Mute");
                insertStmt.setString(4, reason);
                insertStmt.setString(5, startTime.format(formatter));
                insertStmt.setLong(6, endTime != null ? endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0); // End time in milliseconds, 0 for permanent mutes
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding mute record to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMuteNotification(UUID uuid, String reason, LocalDateTime muteEnd) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
        if (player != null) {
            player.sendMessage(new TextComponent(ChatColor.RED + "You have been muted for " + reason + " until " + muteEnd.format(formatter)));
        }
    }

    private void disconnectPlayer(UUID uuid, String message) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
        if (player != null) {
            player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
        }
    }

    private String formatDuration(long durationSeconds) {
        long days = durationSeconds / 86400;
        long hours = (durationSeconds % 86400) / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
