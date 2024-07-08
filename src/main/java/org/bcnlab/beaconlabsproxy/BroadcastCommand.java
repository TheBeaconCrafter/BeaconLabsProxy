package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BroadcastCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.broadcast";  // Define the required permission

    public BroadcastCommand(BeaconLabsProxy plugin) {
        super("broadcast", null, "bc");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        ProxiedPlayer player = (ProxiedPlayer) commandSender;

        // Check if the player has the required permission
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /broadcast <message>"));
            return;
        }

        // Construct the team chat message
        StringBuilder messageBuilder = new StringBuilder();
        for (String word : args) {
            messageBuilder.append(word).append(" ");
        }
        String message = messageBuilder.toString().trim();

        // Send the message to all team chat members including sender
        TextComponent formattedMessage = new TextComponent(ChatColor.RED + "[Broadcast] " + ChatColor.GOLD + message);
        for (ProxiedPlayer recipient : ProxyServer.getInstance().getPlayers()) {
            recipient.sendMessage(formattedMessage);
        }

        // Send message to console
        ProxyServer.getInstance().getConsole().sendMessage(formattedMessage);
    }
}
