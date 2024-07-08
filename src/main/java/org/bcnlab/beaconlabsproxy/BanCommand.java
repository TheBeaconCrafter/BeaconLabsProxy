package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        int durationSeconds = 0;

        // Regex pattern to match durations like Xd, Xy, Xm, Xmin, Xs
        Pattern durationPattern = Pattern.compile("(\\d+)([dDyYmMsS]|min)");

        for (int i = 1; i < args.length; i++) {
            Matcher matcher = durationPattern.matcher(args[i]);
            if (matcher.matches()) {
                int amount = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2).toLowerCase();

                switch (unit) {
                    case "d":
                        durationSeconds += amount * 24 * 60 * 60; // days to seconds
                        break;
                    case "y":
                        durationSeconds += amount * 365 * 24 * 60 * 60; // years to seconds (approximate)
                        break;
                    case "m":
                        durationSeconds += amount * 30 * 24 * 60 * 60; // months to seconds (approximate)
                        break;
                    case "min":
                        durationSeconds += amount * 60; // minutes to seconds
                        break;
                    case "s":
                        durationSeconds += amount; // seconds
                        break;
                    default:
                        break;
                }
            } else {
                reasonBuilder.append(args[i]).append(" ");
            }
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
        LocalDateTime unbanDate = durationSeconds > 0 ? now.plusSeconds(durationSeconds) : null; // Calculate unban date if duration is specified

        // Save the ban to database with UUID and last name
        PunishmentManager.addPunishment(uuid.toString(), lastName, player.getName(), "ban", reason, durationSeconds);

        // Broadcast the ban message
        broadcastBanMessage(playerName, reason, player.getName(), now, unbanDate, durationSeconds);

        // Disconnect and prevent player from joining if online
        if (playerToBan != null && playerToBan.isConnected()) {
            playerToBan.disconnect(TextComponent.fromLegacyText(ChatColor.RED + "You are banned from this server. Reason: " + reason));
        }
    }

    // Method to broadcast ban message via webhook and in-game chat
    private void broadcastBanMessage(String playerName, String reason, String senderName, LocalDateTime timestamp, LocalDateTime unbanDate, int durationSeconds) {
        String formattedTimestamp = timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss"));
        String banMessage = playerName + " was banned by " + senderName + " for " + reason + ". Time: " + formattedTimestamp;

        if (unbanDate != null) {
            String formattedUnbanDate = unbanDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss"));
            banMessage += " (until " + formattedUnbanDate + ")";
        }

        // Broadcast to Discord webhook
        String duration = unbanDate != null ? formatDuration(durationSeconds) : "Permanent";
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

    // Method to format duration in a human-readable format
    private String formatDuration(int durationSeconds) {
        if (durationSeconds <= 0) {
            return "Permanent";
        }

        long days = durationSeconds / (24 * 60 * 60);
        durationSeconds = durationSeconds % (24 * 60 * 60);
        long hours = durationSeconds / (60 * 60);
        durationSeconds = durationSeconds % (60 * 60);
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;

        StringBuilder formattedDuration = new StringBuilder();
        if (days > 0) {
            formattedDuration.append(days).append("d ");
        }
        if (hours > 0) {
            formattedDuration.append(hours).append("h ");
        }
        if (minutes > 0) {
            formattedDuration.append(minutes).append("min ");
        }
        if (seconds > 0) {
            formattedDuration.append(seconds).append("s");
        }

        return formattedDuration.toString().trim();
    }
}
