package org.bcnlab.beaconlabsproxy.Listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bcnlab.beaconlabsproxy.BeaconLabsProxy;
import org.bcnlab.beaconlabsproxy.Database.DatabasePlayers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeListener implements Listener {

    private final BeaconLabsProxy plugin;
    private final Map<UUID, Long> joinTimestamps = new HashMap<>();

    public PlaytimeListener(BeaconLabsProxy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        joinTimestamps.put(uuid, System.currentTimeMillis());

        DatabasePlayers.ensurePlayerExists(uuid.toString(), player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        long joinTime = joinTimestamps.getOrDefault(uuid, System.currentTimeMillis());
        long sessionDuration = System.currentTimeMillis() - joinTime;

        DatabasePlayers.addPlaytime(uuid.toString(), sessionDuration);
        joinTimestamps.remove(uuid);
    }

}
