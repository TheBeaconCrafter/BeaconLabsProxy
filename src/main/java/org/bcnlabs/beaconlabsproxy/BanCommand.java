package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BanCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.ban";  // Define the required permission
    private final Webhooks webhooks;

    public BanCommand(BeaconLabsProxy plugin) {
        super("ban");
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

        if (args.length < 2) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /ban <player> <reason> [duration]"));
            return;
        }

        String playerName = args[0];
        StringBuilder reasonBuilder = new StringBuilder();
        int durationDays = -1;

        for (int i = 1; i < args.length; i++) {
            if (args[i].matches("\\d+d")) { // Check if the argument matches a duration format like 30d
                durationDays = Integer.parseInt(args[i].substring(0, args[i].length() - 1));
                break; // Exit loop after finding duration
            }
            reasonBuilder.append(args[i]).append(" ");
        }

        String reason = reasonBuilder.toString().trim();
        ProxiedPlayer playerToBan = plugin.getProxy().getPlayer(playerName);
        UUID uuid = playerToBan != null ? playerToBan.getUniqueId() : null;
        String lastName = playerToBan != null ? playerToBan.getName() : null;

        if (uuid == null) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + playerName + " not found or is offline."));
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unbanDate = durationDays > 0 ? now.plusDays(durationDays) : null; // Calculate unban date if duration is specified

        // Save the ban to database with UUID and last name
        PunishmentManager.addPunishment(uuid.toString(), lastName, player.getName(), "ban", reason, durationDays > 0 ? durationDays * 24 * 60 * 60 : 0);

        // Broadcast the ban message
        broadcastBanMessage(playerName, reason, player.getName(), now, unbanDate, durationDays);

        // Disconnect and prevent player from joining if online
        if (playerToBan != null && playerToBan.isConnected()) {
            playerToBan.disconnect(TextComponent.fromLegacyText(ChatColor.RED + "You are banned from this server. Reason: " + reason));
        }
    }

    // Method to broadcast ban message via webhook and in-game chat
    private void broadcastBanMessage(String playerName, String reason, String senderName, LocalDateTime timestamp, LocalDateTime unbanDate, int durationDays) {
        String formattedTimestamp = timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss"));
        String banMessage = playerName + " was banned by " + senderName + " for " + reason + ". Time: " + formattedTimestamp;

        if (unbanDate != null) {
            String formattedUnbanDate = unbanDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss"));
            banMessage += " (until " + formattedUnbanDate + ")";
        }

        // Broadcast to Discord webhook
        String duration = unbanDate != null ? durationDays + " days" : "Permanent";
        webhooks.sendBanWebhook(playerName, reason, duration, senderName, formattedTimestamp);

        // Broadcast to players with permission
        for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
            if (onlinePlayer.hasPermission("beaconlabs.staff.read.ban")) {
                TextComponent message = new TextComponent(plugin.getPrefix() + ChatColor.RED + banMessage);
                onlinePlayer.sendMessage(message);
            }
        }

        // Log to console
        plugin.getLogger().info(banMessage);
    }
}
