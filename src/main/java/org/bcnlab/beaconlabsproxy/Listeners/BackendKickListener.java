package org.bcnlab.beaconlabsproxy.Listeners;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bcnlab.beaconlabsproxy.BeaconLabsProxy;

import java.util.Map;

public class BackendKickListener implements Listener {
    private final BeaconLabsProxy plugin;

    public BackendKickListener(BeaconLabsProxy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerKick(ServerKickEvent event) {
        ProxiedPlayer player = event.getPlayer();

        String fallback = getFreeLobbyName();

        if (fallback != null) {
            event.setCancelled(true);
            event.setCancelServer(plugin.getProxy().getServerInfo(fallback));
            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "The server you were on went down. You've been moved to the lobby.");
        } else {
            event.setCancelled(true);
            player.disconnect(new net.md_5.bungee.api.chat.TextComponent(plugin.getPrefix() + ChatColor.RED + "The server you were on went down. You've been moved to a free server."));
        }
    }

    private String getFreeLobbyName() {
        String fallback = null;

        for (Map.Entry<String, ServerInfo> entry : plugin.getProxy().getServers().entrySet()) {
            ServerInfo server = entry.getValue();

            if (server == null) continue;
            String name = server.getName().toLowerCase();


            // This is because we want to prefer servers that contain the word lobby
            if (name.contains("lobby")) {
                return name;
            }

            // If there are none, we'll take any free server
            if (fallback == null) {
                fallback = name;
            }
        }

        return fallback;
    }
}
