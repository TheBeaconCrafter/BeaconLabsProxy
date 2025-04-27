package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class UnmuteCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.unmute";  // Define the required permission
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Webhooks webhooks;
    LocalDateTime now = LocalDateTime.now();

    public UnmuteCommand(BeaconLabsProxy plugin) {
        super("unmute");
        this.plugin = plugin;
        this.webhooks = new Webhooks(this.plugin);
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "This command can only be executed by a player."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;

        // Check if the player has the required permission
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /unmute <player>"));
            return;
        }

        String playerName = args[0];
        ProxiedPlayer playerToUnmute = plugin.getProxy().getPlayer(playerName);
        UUID uuid = playerToUnmute != null ? playerToUnmute.getUniqueId() : null;

        if (uuid == null) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + playerName + " not found or is offline."));
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Update the mute record in the database
        if (updateMuteRecord(uuid.toString(), now, player.getName())) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Player " + playerName + " has been unmuted."));
            webhooks.sendUnmuteWebhook(playerName, player.getName());
            broadcastUnmuteMessage(playerName, player.getName());
        } else {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Failed to unmute player " + playerName + "."));
        }
    }

    private boolean updateMuteRecord(String uuid, LocalDateTime endTime, String punisherName) {
        try (Connection conn = DatabasePunishments.getConnection()) {
            String updateSql = "UPDATE punishments SET end_time = ? WHERE player_uuid = ? AND type = 'Mute' AND end_time > ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setLong(1, endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()); // Convert LocalDateTime to milliseconds since epoch
                updateStmt.setString(2, uuid);
                updateStmt.setLong(3, LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());  // Only update if the current end_time is in the future
                int rowsUpdated = updateStmt.executeUpdate();

                // Check if any rows were updated
                if (rowsUpdated > 0) {
                    // Insert unban record
                    String insertSql = "INSERT INTO punishments (player_uuid, punisher, type, reason, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, uuid);
                        insertStmt.setString(2, punisherName); // Use the name of the player who executed the unban
                        insertStmt.setString(3, "Unmute");
                        insertStmt.setString(4, "Player unmuted");
                        insertStmt.setString(5, now.format(formatter));
                        insertStmt.setString(6, String.valueOf(endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())); // End time same as start time for unmute
                        insertStmt.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating mute record in database: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private void broadcastUnmuteMessage(String playerName, String senderName) {
        String msg = playerName + " was unmuted by " + senderName + ".";

        // Broadcast to players with permission
        for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
            if (onlinePlayer.hasPermission("beaconlabs.staff.read.unmute")) {
                TextComponent message = new TextComponent(plugin.getPrefix() + ChatColor.RED + msg);
                onlinePlayer.sendMessage(message);
            }
        }

        // Log to console
        plugin.getLogger().info(msg);
    }
}
