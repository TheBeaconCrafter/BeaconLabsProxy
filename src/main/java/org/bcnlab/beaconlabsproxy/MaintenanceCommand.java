package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.IOException;
import java.util.Collections;

public class MaintenanceCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;

    public MaintenanceCommand(BeaconLabsProxy plugin) {
        super("maintenance");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /maintenance <on|off>");
            return;
        }

        if (!sender.hasPermission("beaconlabs.maintenance")) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        String option = args[0].toLowerCase();
        switch (option) {
            case "on":
                try {
                    plugin.setMaintenance(true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                kickNonAuthorizedPlayers();
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Maintenance mode has been enabled.");
                break;
            case "off":
                try {
                    plugin.setMaintenance(false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Maintenance mode has been disabled.");
                break;
            default:
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /maintenance <on|off>");
                break;
        }
    }

    private void kickNonAuthorizedPlayers() {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (!player.hasPermission("beaconlabs.maintenancejoin")) {
                String kickmessage = plugin.getMaintenanceMessageFormat();
                player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', kickmessage)));
            }
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return args[0].isEmpty() ? Collections.emptyList() : Collections.singletonList(args[0].toLowerCase().startsWith("o") ? "on" : "off");
        }
        return Collections.emptyList();
    }
}
