package org.bcnlab.beaconlabsproxy.ServerGuard;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bcnlab.beaconlabsproxy.BeaconLabsProxy;

public class ServerGuardListener implements Listener {

    private final BeaconLabsProxy plugin;

    public ServerGuardListener(BeaconLabsProxy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String targetServer = event.getTarget().getName();

        if (!ServerGuardManager.isAllowed(player, targetServer)) {
            // Not allowed → cancel connection
            event.setCancelled(true);

            player.sendMessage(plugin.getPrefix() + "§cYou are not allowed to join §e" + targetServer + "§c.");
        }
    }
}