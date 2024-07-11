package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class JoinMeCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;
    private final Set<String> excludedServers;
    private final Map<ProxiedPlayer, Long> cooldowns;
    private final long cooldownTime; // Cooldown time in milliseconds
    private static final String PERMISSION = "beaconlabs.joinme";

    public JoinMeCommand(BeaconLabsProxy plugin) {
        super("joinme");
        this.plugin = plugin;
        this.excludedServers = new HashSet<>();
        this.cooldowns = new HashMap<>();
        this.cooldownTime = TimeUnit.MINUTES.toMillis(3); // 5-minute cooldown
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "This command can only be used by players.");
            return;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        String[] disallowedJoinmeServers = plugin.getDisallowedJoinmeServers();

        // Add all disallowed joinme servers to the excludedServers set
        for (String server : disallowedJoinmeServers) {
            excludedServers.add(server);
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        String playerServer = player.getServer().getInfo().getName();

        if (excludedServers.contains(playerServer)) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You cannot create JoinMe's on this server.");
            return;
        }

        if (isInCooldown(player)) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You must wait before using this command again.");
            return;
        }

        setCooldown(player);
        String joinMessage = ChatColor.translateAlternateColorCodes('&',
                "&6[JoinMe] &e" + player.getName() + " is inviting you to play on " + playerServer + "! Click here to join.");

        TextComponent fullMessage = new TextComponent(joinMessage);
        fullMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(ChatColor.GRAY + "Click to join")}));
        fullMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/requestserverjoin " + playerServer));

        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            p.sendMessage(fullMessage);
        }
    }

    private boolean isInCooldown(ProxiedPlayer player) {
        if (!cooldowns.containsKey(player)) {
            return false;
        }
        long lastUsed = cooldowns.get(player);
        return (System.currentTimeMillis() - lastUsed) < cooldownTime;
    }

    private void setCooldown(ProxiedPlayer player) {
        cooldowns.put(player, System.currentTimeMillis());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return new HashSet<>(); // No tab completion needed for this command
    }
}
