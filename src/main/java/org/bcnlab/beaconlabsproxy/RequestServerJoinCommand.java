package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.HashSet;
import java.util.Set;

public class RequestServerJoinCommand extends Command {
    private final BeaconLabsProxy plugin;
    private final Set<String> excludedServers;
    private static final String PERMISSION = "beaconlabs.requestserverjoin";

    public RequestServerJoinCommand(BeaconLabsProxy plugin) {
        super("requestserverjoin");
        this.plugin = plugin;
        this.excludedServers = new HashSet<>();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /requestserverjoin <server>");
            return;
        }

        if (!(sender instanceof ProxiedPlayer) || !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        String serverName = args[0];

        // Initialize the excluded servers set
        initializeExcludedServers();

        // Check if the server is excluded
        if (excludedServers.contains(serverName)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You cannot join this server using JoinMe.");
            return;
        }

        ServerInfo server = ProxyServer.getInstance().getServerInfo(serverName);
        if (server == null) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Server not found: " + serverName);
            return;
        }

        player.connect(server, (result, error) -> {
            if (result) {
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Successfully connected to server " + serverName);
            } else {
                String errorMessage = error != null ? error.getMessage() : "unknown error";
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to connect to server " + serverName + ": " + errorMessage);
            }
        });
    }

    private void initializeExcludedServers() {
        excludedServers.clear();
        String[] disallowedJoinmeServers = plugin.getDisallowedJoinmeServers();
        for (String server : disallowedJoinmeServers) {
            excludedServers.add(server);
        }
    }
}
