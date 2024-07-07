package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public class UnbanIDCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.unban";  // Define the required permission
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Webhooks webhooks;

    public UnbanIDCommand(BeaconLabsProxy plugin) {
        super("unbanid");
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

        if (args.length != 1) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /unban <player>"));
            return;
        }

        String playerName = args[0];
        UUID uuid = null;

        // Check if the player is online
        ProxiedPlayer playerToUnban = plugin.getProxy().getPlayer(playerName);
        if (playerToUnban != null) {
            uuid = playerToUnban.getUniqueId();
        } else {
            // Check bans.yml for offline player
            File bansFile = new File(plugin.getDataFolder(), "bans.yml");
            try {
                Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(bansFile);
                if (config.contains(playerName)) {
                    uuid = UUID.fromString(playerName);  // Assume playerName is UUID string
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Error loading bans.yml: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (uuid == null) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + playerName + " not found or is offline."));
            return;
        }

        // Remove ban
        removeBan(uuid);

        // Remove ban from bans.yml file
        removeFromBanFile(uuid);

        webhooks.sendUnbanWebhook(playerName, player.getName(), LocalDateTime.now().format(formatter));
        // Broadcast kick message to players with beaconlabs.staff.read.kick permission
        for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
            if (onlinePlayer.hasPermission("beaconlabs.staff.read.unban")) {
                onlinePlayer.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + playerName + ChatColor.GRAY + " was unbanned by " + ChatColor.GOLD + commandSender.getName() + ChatColor.GRAY + "."));
            }
        }

        // Inform staff and player
        plugin.getLogger().info(player.getName() + " unbanned player " + playerName);
        player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Player " + playerName + " has been unbanned."));
    }

    // Method to remove ban from the in-memory map
    private void removeBan(UUID uuid) {
        Map<UUID, BeaconLabsProxy.BanEntry> bannedPlayers = plugin.getBannedPlayers();
        if (bannedPlayers != null) {
            bannedPlayers.remove(uuid);
        }
    }

    // Method to remove ban from bans.yml file
    private void removeFromBanFile(UUID uuid) {
        File bansFile = new File(plugin.getDataFolder(), "bans.yml");

        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(bansFile);
            config.set(uuid.toString(), null); // Remove ban entry from config
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, bansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error removing ban from bans.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
