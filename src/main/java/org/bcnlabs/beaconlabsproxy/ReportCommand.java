package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.report";

    private static final List<String> REASONS = Arrays.asList("HACKING", "SPAMMING", "INSULT", "CHATABUSE", "AUTOCLICKER");

    public ReportCommand(BeaconLabsProxy plugin) {
        super("report");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /report <player> <reason>"));
            return;
        }

        if (!(sender instanceof ProxiedPlayer) || !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        String reportedPlayer = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection conn = DatabaseReports.getConnection()) {
                String sql = "INSERT INTO reports (reporter, reported, reason) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, player.getName());
                    stmt.setString(2, reportedPlayer);
                    stmt.setString(3, reason);
                    stmt.executeUpdate();
                }
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Report submitted successfully."));
                TextComponent formattedMessage = new TextComponent(ChatColor.GOLD + "[Reports] " + ChatColor.RED + reportedPlayer + ChatColor.GOLD + " was reported by " + ChatColor.RED + player.getName() + ChatColor.GOLD + " for " + ChatColor.RED + reason + ChatColor.GOLD + ".");
                for (ProxiedPlayer recipient : ProxyServer.getInstance().getPlayers()) {
                    if (recipient.hasPermission("beaconlabs.viewreports")) {
                        recipient.sendMessage(formattedMessage);
                    }
                }
            } catch (SQLException e) {
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "An error occurred while submitting the report."));
                e.printStackTrace();
            }
        });
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                playerNames.add(p.getName());
            }
            return playerNames;
        } else if (args.length == 2) {
            return REASONS;
        }
        return new ArrayList<>();
    }
}
