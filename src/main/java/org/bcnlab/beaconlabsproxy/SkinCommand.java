package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class SkinCommand extends Command {

    private final BeaconLabsProxy plugin;

    public SkinCommand(BeaconLabsProxy plugin) {
        super("skin");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!player.hasPermission("beaconlabs.skin")) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED +  "Usage: /skin <username>"));
            return;
        }

        String username = args[0];
        sendSkinLink(sender, username);
    }

    private void sendSkinLink(CommandSender sender, String username) {
        String url = "https://namemc.com/profile/" + username;

        TextComponent message = new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Click here to view " + username + "'s skin on NameMC");
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

        sender.sendMessage(new ComponentBuilder("")
                .append(message)
                .create());
    }
}
