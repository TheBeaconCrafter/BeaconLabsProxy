package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ClosereportCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.closereport";

    public ClosereportCommand(BeaconLabsProxy plugin) {
        super("closereport", "", "close");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer) || !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /closereport <id>"));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        try {
            int id = Integer.parseInt(args[0]);

            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                try (Connection conn = DatabaseReports.getConnection()) {
                    if (conn != null && conn.isValid(3)) { // Check if the connection is valid within 3 seconds
                        String sql = "DELETE FROM reports WHERE id = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setInt(1, id);
                            int rowsAffected = stmt.executeUpdate();

                            if (rowsAffected > 0) {
                                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Report #" + id + " closed successfully."));
                            } else {
                                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Report #" + id + " not found or could not be closed."));
                            }
                        }
                    } else {
                        player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Database connection is not valid."));
                    }
                } catch (SQLException e) {
                    player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "An error occurred while closing report."));
                    e.printStackTrace();
                }
            });

        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Invalid report ID. Please enter a valid integer ID."));
        }
    }
}
