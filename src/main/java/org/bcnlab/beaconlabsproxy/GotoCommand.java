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

public class GotoCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.goto";  // Define the required permission

    public GotoCommand(BeaconLabsProxy plugin) {
        super("goto");
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

        if (args.length != 1) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /goto <player>"));
            return;
        }

        String targetName = args[0];
        ProxiedPlayer targetPlayer = plugin.getProxy().getPlayer(targetName);

        if (targetPlayer == null || !targetPlayer.isConnected()) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + targetName + " is not online."));
            return;
        }

        // Teleport the command sender to the target player
        player.connect(targetPlayer.getServer().getInfo());
        player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Teleported to " + targetPlayer.getName() + "."));
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
