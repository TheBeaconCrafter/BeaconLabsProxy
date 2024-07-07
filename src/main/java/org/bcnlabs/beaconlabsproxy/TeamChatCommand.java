package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashSet;
import java.util.Set;

public class TeamChatCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.teamchat";  // Define the required permission

    private static final Set<ProxiedPlayer> teamChatMembers = new HashSet<>();  // Set to hold staff members for team chat

    public TeamChatCommand(BeaconLabsProxy plugin) {
        super("teamchat", null, "tc");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "Only players can use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;

        // Check if the player has the required permission
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /teamchat <message>"));
            return;
        }

        // Construct the team chat message
        StringBuilder messageBuilder = new StringBuilder();
        for (String word : args) {
            messageBuilder.append(word).append(" ");
        }
        String message = messageBuilder.toString().trim();

        // Send the message to all team chat members including sender
        TextComponent formattedMessage = new TextComponent(ChatColor.GREEN + "[Team Chat] " + ChatColor.GOLD + player.getName() + ChatColor.GRAY + ": " + message);
        for (ProxiedPlayer recipient : ProxyServer.getInstance().getPlayers()) {
            if (recipient.hasPermission(PERMISSION)) {
                recipient.sendMessage(formattedMessage);
            }
        }

        // Send message to console
        ProxyServer.getInstance().getConsole().sendMessage(formattedMessage);
    }

    // Method to add a player to the team chat members set
    public static void addTeamChatMember(ProxiedPlayer player) {
        teamChatMembers.add(player);
    }

    // Method to remove a player from the team chat members set
    public static void removeTeamChatMember(ProxiedPlayer player) {
        teamChatMembers.remove(player);
    }
}
