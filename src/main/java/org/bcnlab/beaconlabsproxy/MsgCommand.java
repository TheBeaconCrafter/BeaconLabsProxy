package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class MsgCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.msg";  // Define the required permission

    public MsgCommand(BeaconLabsProxy plugin) {
        super("msg", null, "message", "tell", "whisper");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "Only players can use this command."));
            return;
        }

        ProxiedPlayer sender = (ProxiedPlayer) commandSender;

        // Check if the player has the required permission
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /msg <player> <message>"));
            return;
        }

        String targetName = args[0];
        ProxiedPlayer targetPlayer = plugin.getProxy().getPlayer(targetName);

        if (targetPlayer == null || !targetPlayer.isConnected()) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + targetName + " is not online."));
            return;
        }

        // Construct the message
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        // Format and send the message to sender and recipient
        sender.sendMessage(new TextComponent(ChatColor.GRAY + "[" + ChatColor.DARK_RED + "You " + ChatColor.GRAY + "-> " + ChatColor.RED + targetPlayer.getName() + ChatColor.GRAY + "] " + message));
        targetPlayer.sendMessage(new TextComponent(ChatColor.GRAY + "[" + ChatColor.RED + sender.getName() + ChatColor.GRAY + " ->" + ChatColor.DARK_RED + " You" + ChatColor.GRAY + "] " + message));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                playerNames.add(p.getName());
            }
            return playerNames;
        }
        return new ArrayList<>();
    }
}
