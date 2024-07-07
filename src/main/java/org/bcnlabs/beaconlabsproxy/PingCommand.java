package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PingCommand extends Command {

    private final BeaconLabsProxy plugin;

    public PingCommand(BeaconLabsProxy plugin) {
        super("ping");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            int ping = player.getPing();
            String prefix = plugin.getPrefix();

            player.sendMessage(new TextComponent(ChatColor.GRAY + prefix + " Your ping is: " + ChatColor.GREEN + ping + ChatColor.GRAY + "ms."));
        }
    }
}
