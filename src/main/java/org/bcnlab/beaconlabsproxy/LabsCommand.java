package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles plugin commands like reload.
 */
public class LabsCommand extends Command implements TabExecutor {
    private final BeaconLabsProxy plugin;

    public LabsCommand(BeaconLabsProxy plugin) {
        super("labs", "beaconlabs.reload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.saveConfig();
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Configuration reloaded."));
        } else {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.YELLOW + "Usage: /labs reload"));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("reload");
        }
        return suggestions;
    }
}
