package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KickCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.kick";  // Define the required permission

    public KickCommand(BeaconLabsProxy plugin) {
        super("kick");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        // Check if the commandSender has the required permission
        if (!commandSender.hasPermission(PERMISSION)) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "/kick <player> <reason>"));
            return;
        }

        String playerName = args[0];
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        ProxiedPlayer playerToKick = plugin.getProxy().getPlayer(playerName);
        if (playerToKick != null) {
            playerToKick.disconnect(new TextComponent(ChatColor.RED + reason));
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "You kicked " + playerName + " for " + reason));

            String senderName = commandSender.getName();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss"));

            plugin.sendDiscordWebhook(playerName, reason, "Kick", senderName, timestamp);
        } else {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "Player does not exist."));
        }
    }
}
