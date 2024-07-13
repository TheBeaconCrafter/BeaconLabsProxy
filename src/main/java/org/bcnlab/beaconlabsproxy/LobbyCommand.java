package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.config.ServerInfo;

public class LobbyCommand extends Command {

    private final BeaconLabsProxy plugin;

    public LobbyCommand(BeaconLabsProxy plugin) {
        super("lobby", "beaconlabs.lobby");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use this command!");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        String targetServerName = plugin.getLobbyServer();

        // Check if the server exists
        ServerInfo targetServer = plugin.getProxy().getServerInfo(targetServerName);
        if (targetServer == null) {
            player.sendMessage(plugin.getPrefix() + "§cThe target server is not available.");
            return;
        }

        // Send the player to the target server
        player.connect(targetServer);
    }
}
