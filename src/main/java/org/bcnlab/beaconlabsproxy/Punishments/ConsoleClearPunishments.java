package org.bcnlab.beaconlabsproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.io.Console;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class ConsoleClearPunishments extends Command {

    private final BeaconLabsProxy plugin;
    private final Webhooks webhooks;

    public ConsoleClearPunishments(BeaconLabsProxy plugin) {
        super("consoleclearpunish", "", "consolecp");
        this.plugin = plugin;
        this.webhooks = new Webhooks(this.plugin);
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        // Check if the sender is the console
        if (commandSender instanceof ProxiedPlayer) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "This command can only be executed from the console."));
            return;
        }

        if (args.length != 1) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /clearpunishments <player>"));
            return;
        }

        String playerName = args[0];

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                UUID uuid = getUUIDFromPlayerName(playerName);

                if (uuid == null) {
                    commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + playerName + " not found or is offline."));
                    return;
                }

                // Delete all punishment records for the player
                clearPunishments(uuid);

                commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "All punishments for player " + playerName + "have been cleared."));
                        webhooks.sendCpunishWebhook(playerName, "Console");

            } catch (Exception e) {
                commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "An error occurred while clearing punishments: " + e.getMessage()));
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

    private void clearPunishments(UUID uuid) {
        try (Connection conn = DatabasePunishments.getConnection()) {
            // Delete all punishment records for the player
            String deleteSql = "DELETE FROM punishments WHERE player_uuid = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, uuid.toString());
                deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error while clearing punishments for player: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
