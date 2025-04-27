package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Allows players to send clickable invites to others.
 */
public class InviteCommand extends Command {
    private final BeaconLabsProxy plugin;

    public InviteCommand(BeaconLabsProxy plugin) {
        super("invite", "beaconlabs.invite");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer) || !sender.hasPermission("beaconlabs.invite")) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You don't have permission to invite."));
            return;
        }
        ProxiedPlayer inviter = (ProxiedPlayer) sender;
        if (args.length < 1) {
            inviter.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /invite <player> [server]"));
            return;
        }
        String targetName = args[0];
        String server;
        if (args.length >= 2) {
            server = args[1];
        } else {
            server = inviter.getServer().getInfo().getName();
        }
        ProxiedPlayer target = plugin.getProxy().getPlayer(targetName);
        if (target == null) {
            inviter.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player not online."));
            return;
        }
        TextComponent msg = new TextComponent(plugin.getPrefix() + ChatColor.GREEN + inviter.getName() + " has invited you to join server " + ChatColor.AQUA + server + ChatColor.GREEN + ". ");
        TextComponent accept = new TextComponent(ChatColor.BOLD + "[Accept]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/requestserverjoin " + server));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to join " + server)));
        msg.addExtra(accept);
        target.sendMessage(msg);
        inviter.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Invitation sent to " + target.getName() + "."));
    }
}
