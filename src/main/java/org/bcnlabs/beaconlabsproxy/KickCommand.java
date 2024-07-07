package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KickCommand extends Command {

    private final BeaconLabsProxy plugin;
    private final Webhooks webhooks;
    private static final String PERMISSION = "beaconlabs.kick";  // Define the required permission

    public KickCommand(BeaconLabsProxy plugin) {
        super("kick");
        this.plugin = plugin;
        this.webhooks = new Webhooks(this.plugin);
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        // Check if the commandSender has the required permission
        if (!commandSender.hasPermission(PERMISSION)) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "/kick <player> <reason>"));
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
            // Constructing the kick message with colors and a clickable link
            String kickMessageFormat = plugin.getKickMessageFormat();
            String formattedKickMessage = String.format(kickMessageFormat, reason, "https://go.bcnlab.org/appeal");

            BaseComponent[] kickMessage = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', formattedKickMessage));
            playerToKick.disconnect(kickMessage);

            // Broadcast kick message to players with beaconlabs.staff.read.kick permission
            for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
                if (onlinePlayer.hasPermission("beaconlabs.staff.read.kick")) {
                    onlinePlayer.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + playerName + ChatColor.GRAY + " was kicked for " + ChatColor.RED + reason + ChatColor.GRAY + " by " + ChatColor.GOLD + commandSender.getName() + ChatColor.GRAY + "."));
                }
            }

            // Send message to command sender
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You kicked " + playerName + " for " + reason));

            // Log the kick event to Discord webhook
            String senderName = commandSender.getName();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss"));
            webhooks.sendKickWebhook(playerName, reason, senderName, timestamp);
        } else {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player does not exist."));
        }
    }
}
