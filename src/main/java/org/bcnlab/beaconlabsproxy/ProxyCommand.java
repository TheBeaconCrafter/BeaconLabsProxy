package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class ProxyCommand extends Command {

    private final Plugin plugin;

    public ProxyCommand(Plugin plugin) {
        super("proxy", "bungee.command.proxyinfo");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6=== §cBeaconLabs Proxy §4===");
        sender.sendMessage("§eVersion: " + plugin.getDescription().getVersion());
        sender.sendMessage("§eAuthor: " + plugin.getDescription().getAuthor());
    }
}