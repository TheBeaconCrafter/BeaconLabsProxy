package org.bcnlab.beaconlabsproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class UnbanCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.unban";  // Define the required permission
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Webhooks webhooks;

    public UnbanCommand(BeaconLabsProxy plugin) {
        super("unban");
        this.plugin = plugin;
        this.webhooks = new Webhooks(this.plugin);
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (args.length != 1) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /unban <player>"));
            return;
        }

        if (!(commandSender instanceof ProxiedPlayer) || !commandSender.hasPermission(PERMISSION)) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        String playerName = args[0];

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                UUID uuid = getUUIDFromPlayerName(playerName);

                if (uuid == null) {
                    player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + playerName + " not found or is offline."));
                    return;
                }

                // Remove ban from database and update original ban record
                unbanPlayer(uuid, player.getName()); // Pass player's name who executed the unban

                webhooks.sendUnbanWebhook(playerName, player.getName(), LocalDateTime.now().format(formatter));

                // Broadcast unban message to players with beaconlabs.staff.read.unban permission
                for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
                    if (onlinePlayer.hasPermission("beaconlabs.staff.read.unban")) {
                        onlinePlayer.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + playerName + ChatColor.GRAY + " was unbanned by " + ChatColor.GOLD + commandSender.getName() + ChatColor.GRAY + "."));
                    }
                }

                // Inform staff and player
                plugin.getLogger().info(player.getName() + " unbanned player " + playerName);
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Player " + playerName + " has been unbanned."));

            } catch (Exception e) {
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "An error occurred while unbanning player: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private UUID getUUIDFromPlayerName(String playerName) throws Exception {
        String urlString = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() != 200) {
            return null;
        }

        InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        reader.close();

        String uuidString = json.get("id").getAsString();
        return parseUUIDFromString(uuidString);
    }

    private UUID parseUUIDFromString(String uuidString) {
        String formattedUUID = uuidString.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(formattedUUID);
    }

    // Method to unban player (update original ban record and insert unban record)
    private void unbanPlayer(UUID uuid, String punisherName) {
        try (Connection conn = DatabasePunishments.getConnection()) {
            LocalDateTime now = LocalDateTime.now();

            // Update end_time to start_time for existing ban
            String updateSql = "UPDATE punishments SET end_time = start_time WHERE player_uuid = ? AND type = 'ban'";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, uuid.toString());
                updateStmt.executeUpdate();
            }

            // Insert unban record
            String insertSql = "INSERT INTO punishments (player_uuid, punisher, type, reason, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, uuid.toString());
                insertStmt.setString(2, punisherName); // Use the name of the player who executed the unban
                insertStmt.setString(3, "Unban");
                insertStmt.setString(4, "Player unbanned");
                insertStmt.setString(5, now.format(formatter));
                insertStmt.setString(6, now.format(formatter)); // End time same as start time for unban
                insertStmt.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error while updating/unbanning player: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
