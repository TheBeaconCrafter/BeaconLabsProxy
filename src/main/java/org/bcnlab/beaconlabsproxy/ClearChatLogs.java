package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.chat.TextComponent;

public class ClearChatLogs extends Command {

    private static final String PERMISSION = "beaconlabs.clearchatlogs";
    private static final String CONFIRMATION_PHRASE = "confirm iunderstand";

    private final BeaconLabsProxy plugin;

    public ClearChatLogs(BeaconLabsProxy plugin) {
        super("clearchatlogs");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check if the sender has the required permission
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        // Check if the correct number of arguments are provided
        if (args.length != 2) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /clearchatlogs confirm iunderstand"));
            return;
        }

        // Check if the confirmation phrase is correct
        if (!args[0].equalsIgnoreCase("confirm") || !args[1].equalsIgnoreCase("iunderstand")) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /clearchatlogs confirm iunderstand"));
            return;
        }

        // Clear chat logs
        plugin.clearLogDirectory();

        // Notify the sender
        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Chat logs have been cleared."));
    }
}
