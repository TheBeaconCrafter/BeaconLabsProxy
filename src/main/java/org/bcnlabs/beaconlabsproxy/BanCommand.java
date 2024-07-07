package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.ban";  // Define the required permission
    private final Map<UUID, BanEntry> bannedPlayers = new HashMap<>();  // Map to store banned players
    private final Webhooks webhooks;

    public BanCommand(BeaconLabsProxy plugin) {
        super("ban");
        this.plugin = plugin;
        this.webhooks = new Webhooks(this.plugin);
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
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

        if (uuid == null) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + playerName + " not found or is offline."));
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unbanDate = durationDays > 0 ? now.plusDays(durationDays) : null; // Calculate unban date if duration is specified
        BanEntry banEntry = new BanEntry(playerName, reason, now, durationDays);
        bannedPlayers.put(uuid, banEntry);

        // Save the ban to YAML file
        saveBanData();

        // Broadcast the ban message
        broadcastBanMessage(playerName, reason, player.getName(), now, unbanDate, durationDays);

        // Disconnect and prevent player from joining if online
        if (playerToBan != null && playerToBan.isConnected()) {
            playerToBan.disconnect(TextComponent.fromLegacyText(ChatColor.RED + "You are banned from this server. Reason: " + reason));
        }
    }

    // Method to save banned player data to YAML file
    private void saveBanData() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        File bansFile = new File(dataFolder, "bans.yml");
        Configuration config;

        try {
            if (!bansFile.exists()) {
                bansFile.createNewFile();
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(bansFile);

            for (Map.Entry<UUID, BanEntry> entry : bannedPlayers.entrySet()) {
                String key = entry.getKey().toString();
                BanEntry banEntry = entry.getValue();

                config.set(key + ".playerName", banEntry.getPlayerName());
                config.set(key + ".reason", banEntry.getReason());
                config.set(key + ".banDate", banEntry.getBanDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                config.set(key + ".durationDays", banEntry.getBanDurationDays());
            }

            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, bansFile);
        } catch (IOException e) {
            e.printStackTrace();
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
