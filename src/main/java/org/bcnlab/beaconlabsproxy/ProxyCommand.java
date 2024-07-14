package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class ProxyCommand extends Command {

    private final BeaconLabsProxy plugin;

    public ProxyCommand(BeaconLabsProxy plugin) {
        super("proxy", "", "labsproxy", "proxyinfo");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = (ProxiedPlayer) sender;
        // Check if the player has the required permission
        if (!player.hasPermission("beaconlabs.proxyinfo")) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "BeaconLabsProxy Version " + ChatColor.GOLD + plugin.getVersion() + ChatColor.RED + " by ItsBeacon");
    }
}