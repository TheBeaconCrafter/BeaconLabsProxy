package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ViewReportsCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.viewreports";

    public ViewReportsCommand(BeaconLabsProxy plugin) {
        super("viewreports", "", "reports");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer) || !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection conn = DatabaseReports.getConnection()) {
                if (conn != null && conn.isValid(3)) { // Check if the connection is valid within 3 seconds
                    String sql = "SELECT * FROM reports";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.isBeforeFirst()) {
                                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "There are no open reports."));
                                return;
                            }

                            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Active Reports:"));
                            while (rs.next()) {
                                int id = rs.getInt("id");
                                String reporter = rs.getString("reporter");
                                String reported = rs.getString("reported");
                                String reason = rs.getString("reason");
                                String timestamp = rs.getString("timestamp");

                                // Format each report entry
                                TextComponent reportComponent = new TextComponent(
                                        ChatColor.YELLOW + "[" + id + "] " +
                                                ChatColor.AQUA + "Reporter: " + reporter + ", " +
                                                ChatColor.AQUA + "Reported: " + ChatColor.RED + ChatColor.BOLD + reported + ChatColor.AQUA + ", " +
                                                ChatColor.AQUA + "Reason: " + ChatColor.RED + reason + ChatColor.AQUA + ", " +
                                                ChatColor.AQUA + "Timestamp: " + timestamp + "\n"
                                );

                                // Create close report button
                                TextComponent closeButton = new TextComponent(ChatColor.RED + " [Close]");
                                closeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/closereport " + id));
                                reportComponent.addExtra(closeButton);

                                // Create go to reported player button
                                TextComponent goToButton = new TextComponent(ChatColor.BLUE + " [Go To]");
                                goToButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/goto " + reported));
                                reportComponent.addExtra(goToButton);

                                // Create go to reported player button
                                TextComponent chatReportButton = new TextComponent(ChatColor.GREEN + " [Chatreport]");
                                chatReportButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatreport " + reported));
                                reportComponent.addExtra(chatReportButton);

                                // Send the formatted report to the player
                                player.sendMessage(reportComponent);
                            }
                        }
                    }
                } else {
                    player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Database connection is not valid."));
                }
            } catch (SQLException e) {
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "An error occurred while retrieving reports."));
                e.printStackTrace();
            }
        });
    }
}
