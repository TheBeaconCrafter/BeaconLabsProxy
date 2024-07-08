package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class InfoCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.info";

    public InfoCommand(BeaconLabsProxy plugin) {
        super("info", "", "check");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "Only players can use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /info <player>"));
            return;
        }

        String targetName = args[0];
        ProxiedPlayer targetPlayer = plugin.getProxy().getPlayer(targetName);

        if (targetPlayer == null || !targetPlayer.isConnected()) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + targetName + " is not online."));
            return;
        }

        String ipAddress = targetPlayer.getSocketAddress().toString();
        String serverName = targetPlayer.getServer() != null ? targetPlayer.getServer().getInfo().getName() : "Not connected to any server";

        TextComponent infoMessage = new TextComponent(ChatColor.YELLOW + "Information about " + targetPlayer.getName() + ":\n");
        infoMessage.addExtra(ChatColor.YELLOW + "Display Name: " + ChatColor.WHITE + targetPlayer.getDisplayName() + "\n");
        infoMessage.addExtra(ChatColor.YELLOW + "UUID: ");

        TextComponent uuidComponent = new TextComponent(ChatColor.WHITE + targetPlayer.getUniqueId().toString());
        uuidComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, targetPlayer.getUniqueId().toString()));
        uuidComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(ChatColor.GRAY + "Click to copy UUID")}));
        infoMessage.addExtra(uuidComponent);
        infoMessage.addExtra("\n");

        infoMessage.addExtra(ChatColor.YELLOW + "Locale: " + ChatColor.WHITE + targetPlayer.getLocale() + "\n");
        infoMessage.addExtra(ChatColor.YELLOW + "Chat Mode: " + ChatColor.WHITE + targetPlayer.getChatMode() + "\n");
        infoMessage.addExtra(ChatColor.YELLOW + "View Distance: " + ChatColor.WHITE + targetPlayer.getViewDistance() + "\n");
        infoMessage.addExtra(ChatColor.YELLOW + "Main Hand: " + ChatColor.WHITE + targetPlayer.getMainHand() + "\n");

        TextComponent ipComponent = new TextComponent(ChatColor.YELLOW + "IP Address: " + ChatColor.WHITE + ipAddress + "\n");
        ipComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ipAddress));
        ipComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(ChatColor.GRAY + "Click to copy IP address")}));
        infoMessage.addExtra(ipComponent);

        infoMessage.addExtra(ChatColor.YELLOW + "Ping: " + ChatColor.WHITE + targetPlayer.getPing() + "\n");
        infoMessage.addExtra(ChatColor.YELLOW + "Current Server: ");

        if (targetPlayer.getServer() != null) {
            TextComponent serverNameComponent = new TextComponent(ChatColor.AQUA + serverName);
            serverNameComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + serverName));
            infoMessage.addExtra(serverNameComponent);
        } else {
            infoMessage.addExtra(ChatColor.WHITE + serverName);
        }

        player.sendMessage(infoMessage);
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
