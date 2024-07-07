package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Collection;

public class StaffCommand extends Command {

    private final BeaconLabsProxy plugin;

    public StaffCommand(BeaconLabsProxy plugin) {
        super("staff", null, "liststaff");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!player.hasPermission("beaconlabs.visual.staff")) {
            player.sendMessage(new TextComponent(ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

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
