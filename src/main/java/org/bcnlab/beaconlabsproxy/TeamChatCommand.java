package org.bcnlab.beaconlabsproxy;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashSet;
import java.util.Set;

public class TeamChatCommand extends Command {

    private final BeaconLabsProxy plugin;
    private final LuckPerms luckPermsApi;  // LuckPerms API instance
    private static final String PERMISSION = "beaconlabs.teamchat";  // Define the required permission

    private static final Set<ProxiedPlayer> teamChatMembers = new HashSet<>();  // Set to hold team chat members

    public TeamChatCommand(BeaconLabsProxy plugin) {
        super("teamchat", null, "tc");
        this.plugin = plugin;
        this.luckPermsApi = LuckPermsProvider.get();
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "Only players can use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;

        // Check if the player has the required permission
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /teamchat <message>"));
            return;
        }

        // Construct the team chat message
        StringBuilder messageBuilder = new StringBuilder();
        for (String word : args) {
            messageBuilder.append(word).append(" ");
        }
        String message = messageBuilder.toString().trim();

        // Fetch player prefix and suffix from LuckPerms
        String playerPrefix = getPlayerPrefix(player);
        String playerSuffix = getPlayerSuffix(player);

        // Create the formatted message
        TextComponent formattedMessage = new TextComponent(
                ChatColor.GREEN + "[Team Chat] " + playerPrefix + ChatColor.GRAY + player.getName() + playerSuffix + ": " + ChatColor.GOLD + ChatColor.translateAlternateColorCodes('&', message)
        );

        // Send the message to all team chat members including the sender
        for (ProxiedPlayer recipient : ProxyServer.getInstance().getPlayers()) {
            if (recipient.hasPermission(PERMISSION)) {
                recipient.sendMessage(formattedMessage);
            }
        }

        // Send message to console
        ProxyServer.getInstance().getConsole().sendMessage(formattedMessage);
    }

    private String getPlayerPrefix(ProxiedPlayer player) {
        User user = luckPermsApi.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
        if (user == null) return ChatColor.GRAY + "";  // Default color if no prefix

        ContextManager contextManager = luckPermsApi.getContextManager();
        QueryOptions queryOptions = contextManager.getQueryOptions(user).orElse(QueryOptions.defaultContextualOptions());
        CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);

        String prefix = metaData.getPrefix() != null ? ChatColor.translateAlternateColorCodes('&', metaData.getPrefix()) : "";
        return prefix;
    }

    private String getPlayerSuffix(ProxiedPlayer player) {
        User user = luckPermsApi.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
        if (user == null) return "";  // Default if no suffix

        ContextManager contextManager = luckPermsApi.getContextManager();
        QueryOptions queryOptions = contextManager.getQueryOptions(user).orElse(QueryOptions.defaultContextualOptions());
        CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);

        String suffix = metaData.getSuffix() != null ? ChatColor.translateAlternateColorCodes('&', metaData.getSuffix()) : "";
        return suffix;
    }
}
