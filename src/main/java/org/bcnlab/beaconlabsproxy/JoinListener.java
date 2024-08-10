package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;

import java.time.LocalDateTime;
import java.util.UUID;

public class JoinListener implements Listener {

    private final BeaconLabsProxy plugin;
    private final WhitelistCommand whitelistCommand;

    public JoinListener(BeaconLabsProxy plugin, WhitelistCommand whitelistCommand) {
        this.plugin = plugin;
        this.whitelistCommand = whitelistCommand;
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if the player is banned
        if (plugin.isPlayerBanned(uuid)) {
            // Fetch ban reason and unban date
            String banReason = plugin.getPlayerBanReason(uuid);
            LocalDateTime unbanDate = plugin.getPlayerUnbanDate(uuid);

            // Format the ban message including reason and unban date
            String banMessage = plugin.formatBanMessage(banReason, unbanDate);

            // Disconnect the player with the formatted ban message
            player.disconnect(ChatColor.translateAlternateColorCodes('&', banMessage));
            return;
        }

        // Check if the server is in maintenance mode
        if (plugin.getMaintenance()) {
            plugin.getLogger().info("Maintenance is on");
            if (!player.hasPermission("beaconlabs.maintenancejoin")) {
                String message = plugin.getMaintenanceMessageFormat();
                player.disconnect(ChatColor.translateAlternateColorCodes('&', message));
                plugin.getLogger().info("Player was disconnected for not having maintenance bypass");
                return;
            }
        }

        // Check if the server is whitelisted and if the player is whitelisted
        if (plugin.getWhiteList()) {
            if (player.hasPermission("beaconlabs.whitelistbypass")) {
                return;
            } else if (!whitelistCommand.isPlayerWhitelisted(player.getName())) {
                String kickMessage = plugin.getWhitelistMessageFormat();
                player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', kickMessage)));
            }
        }
    }
}
