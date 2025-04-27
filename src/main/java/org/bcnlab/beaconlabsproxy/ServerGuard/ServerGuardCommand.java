package org.bcnlab.beaconlabsproxy.ServerGuard;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bcnlab.beaconlabsproxy.BeaconLabsProxy;
import org.bcnlab.beaconlabsproxy.Utils.DurationParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServerGuardCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;

    public ServerGuardCommand(BeaconLabsProxy plugin) {
        super("serverguard", null, "sg"); // aliases: /serverguard and /sg
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer) || sender.hasPermission("beaconlabs.serverguard.admin")) {
            if (args.length == 0) {
                sendHelp(sender);
                return;
            }

            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "invite":
                    handleInvite(sender, args);
                    break;
                case "tempinvite":
                    handleTempInvite(sender, args);
                    break;
                case "remove":
                    handleRemove(sender, args);
                    break;
                case "plist":
                    handlePList(sender, args);
                    break;
                case "slist":
                    handleSList(sender, args);
                    break;
                case "alist":
                    handleAList(sender);
                    break;
                default:
                    sendHelp(sender);
                    break;
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + "§cYou don't have permission to use this command!");
        }
    }

    private void handleInvite(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getPrefix() + "§cUsage: /sg invite <player> <server>");
            return;
        }
        ProxiedPlayer target = plugin.getProxy().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "§cPlayer not found.");
            return;
        }
        String server = args[2];
        ServerGuardManager.invitePlayer(target.getUniqueId(), server, true, null);
        sender.sendMessage(plugin.getPrefix() + "§aInvited §e" + target.getName() + " §ato §e" + server + "§a permanently.");
        target.sendMessage(plugin.getPrefix() + "§aYou have been invited to §e" + server + "§a permanently.");
    }

    private void handleTempInvite(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getPrefix() + "§cUsage: /sg tempinvite <player> <server> <duration>");
            return;
        }
        ProxiedPlayer target = plugin.getProxy().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "§cPlayer not found.");
            return;
        }
        String server = args[2];
        String durationInput = args[3];

        try {
            ServerGuardManager.invitePlayer(target.getUniqueId(), server, false, DurationParser.parse(durationInput));
            sender.sendMessage(plugin.getPrefix() + "§aTemporarily invited §e" + target.getName() + " §ato §e" + server + " §afor §e" + durationInput + "§a.");
            target.sendMessage(plugin.getPrefix() + "§aYou have been invited to §e" + server + " §afor §e" + durationInput + "§a.");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getPrefix() + "§cInvalid duration format. Example: 1d, 2h, 30min");
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getPrefix() + "§cUsage: /sg remove <player> <server>");
            return;
        }
        ProxiedPlayer target = plugin.getProxy().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "§cPlayer not found.");
            return;
        }
        String server = args[2];
        ServerGuardManager.removeInvite(target.getUniqueId(), server);
        sender.sendMessage(plugin.getPrefix() + "§aRemoved invite for §e" + target.getName() + " §afrom §e" + server + "§a.");
        target.sendMessage(plugin.getPrefix() + "§aYour access to §e" + server + "§a has been revoked.");
    }

    private void handlePList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "§cUsage: /sg plist <player>");
            return;
        }
        ProxiedPlayer target = plugin.getProxy().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "§cPlayer not found.");
            return;
        }
        List<String> servers = new ArrayList<>(ServerGuardManager.getInvitedServers(target.getUniqueId()));
        sender.sendMessage(plugin.getPrefix() + "§a" + target.getName() + " is invited to: §e" + String.join(", ", servers));
    }

    private void handleSList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "§cUsage: /sg slist <server>");
            return;
        }
        String server = args[1];
        List<UUID> players = ServerGuardManager.getInvitedPlayers(server);
        sender.sendMessage(plugin.getPrefix() + "§aPlayers invited to §e" + server + "§a:");
        for (UUID uuid : players) {
            ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
            if (player != null) {
                sender.sendMessage("§7- " + player.getName());
            } else {
                sender.sendMessage("§7- §c" + uuid.toString() + " (Offline)");
            }
        }
    }

    /**
     * Lists all servers configured as allowed.
     */
    private void handleAList(CommandSender sender) {
        Set<String> allowed = ServerGuardManager.getAllowedServers();
        if (allowed.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + "§cNo allowed servers configured.");
        } else {
            sender.sendMessage(plugin.getPrefix() + "§aAllowed servers: §e" + String.join(", ", allowed));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(plugin.getPrefix() + "§7/sg invite <player> <server>"));
        sender.sendMessage(plugin.getPrefix() + "§7/sg tempinvite <player> <server> <duration>");
        sender.sendMessage(plugin.getPrefix() + "§7/sg remove <player> <server>");
        sender.sendMessage(plugin.getPrefix() + "§7/sg plist <player>");
        sender.sendMessage(plugin.getPrefix() + "§7/sg slist <server>");
        sender.sendMessage(plugin.getPrefix() + "§7/sg alist");
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("invite");
            suggestions.add("tempinvite");
            suggestions.add("remove");
            suggestions.add("plist");
            suggestions.add("slist");
            suggestions.add("alist");
        }

        return suggestions;
    }
}