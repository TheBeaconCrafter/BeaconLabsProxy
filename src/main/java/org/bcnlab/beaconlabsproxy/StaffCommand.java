package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Collection;

public class StaffCommand extends Command {

    private final BeaconLabsProxy plugin;

    public StaffCommand(BeaconLabsProxy plugin) {
        super("staff", "beaconlabs.visual.staff", "team");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Only players can use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        Collection<ProxiedPlayer> onlinePlayers = plugin.getProxy().getPlayers();
        TextComponent staffList = new TextComponent(ChatColor.GREEN + "Staff Online:\n");

        for (ProxiedPlayer onlinePlayer : onlinePlayers) {
            if (onlinePlayer.hasPermission("beaconlabs.visual.staff")) {
                String displayName = getDisplayName(onlinePlayer);
                TextComponent playerComponent = new TextComponent(" - " + displayName + "\n");
                playerComponent.setColor(ChatColor.AQUA);
                staffList.addExtra(playerComponent);
            }
        }

        player.sendMessage(staffList);
    }

    private String getDisplayName(ProxiedPlayer player) {
        return player.getDisplayName();
    }
}
