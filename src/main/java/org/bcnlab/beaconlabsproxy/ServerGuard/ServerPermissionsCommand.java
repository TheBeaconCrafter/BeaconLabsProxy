package org.bcnlab.beaconlabsproxy.ServerGuard;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import org.bcnlab.beaconlabsproxy.BeaconLabsProxy;

import java.util.HashMap;
import java.util.Map;

public class ServerPermissionsCommand extends Command {
    private final BeaconLabsProxy plugin;

    public ServerPermissionsCommand(BeaconLabsProxy plugin) {
        super("serverperm", "beaconlabs.admin.serverperm");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                listServerPermissions(sender);
                break;
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() +
                        ChatColor.RED + "Usage: /serverperm set <server> <permission>"));
                    return;
                }
                setServerPermission(sender, args[1], args[2]);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /serverperm remove <server>"));
                    return;
                }
                removeServerPermission(sender, args[1]);
                break;
            default:
                showHelp(sender);
                break;
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.YELLOW + "Server Permissions Commands:"));
        sender.sendMessage(new TextComponent(ChatColor.GOLD + "/serverperm list " + 
            ChatColor.WHITE + "- List all server permission requirements"));
        sender.sendMessage(new TextComponent(ChatColor.GOLD + "/serverperm set <server> <permission> " + 
            ChatColor.WHITE + "- Set permission requirement for a server"));
        sender.sendMessage(new TextComponent(ChatColor.GOLD + "/serverperm remove <server> " + 
            ChatColor.WHITE + "- Remove permission requirement for a server"));
    }

    private void listServerPermissions(CommandSender sender) {
        Map<String, String> permissions = ServerGuardManager.getServerPermissions();
        
        if (permissions.isEmpty()) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() +
                ChatColor.YELLOW + "No server-specific permissions are configured."));
            return;
        }

        sender.sendMessage(new TextComponent(plugin.getPrefix() +
            ChatColor.YELLOW + "Server Permission Requirements:"));
        
        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            sender.sendMessage(new TextComponent(ChatColor.GOLD + entry.getKey() + ": " + 
                ChatColor.WHITE + entry.getValue()));
        }
    }

    private void setServerPermission(CommandSender sender, String server, String permission) {
        Map<String, String> permissions = new HashMap<>(ServerGuardManager.getServerPermissions());
        permissions.put(server.toLowerCase(), permission);
        ServerGuardManager.setServerPermissions(permissions);
        
        sender.sendMessage(new TextComponent(plugin.getPrefix() +
            ChatColor.GREEN + "Server " + server + " now requires permission: " + permission));
    }

    private void removeServerPermission(CommandSender sender, String server) {
        Map<String, String> permissions = new HashMap<>(ServerGuardManager.getServerPermissions());
        String removed = permissions.remove(server.toLowerCase());
        
        if (removed != null) {
            ServerGuardManager.setServerPermissions(permissions);
            sender.sendMessage(new TextComponent(plugin.getPrefix() +
                ChatColor.GREEN + "Permission requirement removed for server: " + server));
        } else {
            sender.sendMessage(new TextComponent(plugin.getPrefix() +
                ChatColor.YELLOW + "Server " + server + " did not have any permission requirement."));
        }
    }
}
