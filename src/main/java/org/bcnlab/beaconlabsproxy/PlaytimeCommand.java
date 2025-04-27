package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.bcnlab.beaconlabsproxy.Database.DatabasePlayers;
import org.bcnlab.beaconlabsproxy.Utils.UUIDFetcher;

import java.util.UUID;

public class PlaytimeCommand extends Command {

    private final BeaconLabsProxy plugin;

    public PlaytimeCommand(BeaconLabsProxy plugin) {
        super("playtime", "", "pt");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof ProxiedPlayer)) return;

            ProxiedPlayer player = (ProxiedPlayer) sender;
            String uuid = player.getUniqueId().toString();

            long playtimeMs = DatabasePlayers.getPlaytime(uuid);
            String formatted = formatPlaytime(playtimeMs);

            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GRAY + "Your playtime is " + ChatColor.GREEN + formatted));
            return;
        }

        if (!sender.hasPermission("beaconlabs.playtime.others")) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You don’t have permission to view others' playtime."));
            return;
        }

        String targetName = args[0];

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                UUID uuid = UUIDFetcher.getUUID(targetName);

                if (uuid == null) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Could not find player: " + targetName));
                    return;
                }

                long playtimeMs = DatabasePlayers.getPlaytime(uuid.toString());
                String formatted = formatPlaytime(playtimeMs);

                sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GRAY + targetName + "'s playtime is " + ChatColor.GREEN + formatted));
            } catch (Exception e) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Error fetching UUID for " + targetName));
                e.printStackTrace();
            }
        });
    }

    private String formatPlaytime(long millis) {
        long seconds = millis / 1000;
        long minutes = (seconds / 60) % 60;
        long hours = (seconds / 3600) % 24;
        long days = seconds / 86400;

        return String.format("%dd %02dh %02dm", days, hours, minutes);
    }
}
