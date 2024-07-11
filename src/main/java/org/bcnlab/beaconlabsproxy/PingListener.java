package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing.Players;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Random;

public class PingListener implements Listener {

    private final BeaconLabsProxy plugin;

    public PingListener(BeaconLabsProxy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProxyPing(ProxyPingEvent ev) {
        Random ran = new Random();

        String motd = plugin.getMOTD();
        String[] dynamicMsgs = plugin.getDynamicMSGs();

        // Select a random dynamic message from the array
        String dynamicMsg = dynamicMsgs[ran.nextInt(dynamicMsgs.length)];

        String finalMotd = ChatColor.translateAlternateColorCodes('&', motd.replaceAll("@dynamicmsg@", dynamicMsg));

        ev.getResponse().setDescription(finalMotd);

        Players players = ev.getResponse().getPlayers();
        int totalMax = plugin.getMaxPlayers();
        players.setMax(totalMax);
        ev.getResponse().setPlayers(players);
    }
}
