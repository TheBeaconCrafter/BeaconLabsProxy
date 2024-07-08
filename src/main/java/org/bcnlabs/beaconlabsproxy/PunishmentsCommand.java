package org.bcnlabs.beaconlabsproxy;

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class PunishmentsCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.viewpunishments";
    private static final DateTimeFormatter START_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss");

    public PunishmentsCommand(BeaconLabsProxy plugin) {
        super("punishments", "", "ps");
        this.plugin = plugin;
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

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        ProxiedPlayer player = (ProxiedPlayer) commandSender;

        // Check if the player has the required permission
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /punishments <player>"));
            return;
        }

        String targetName = args[0];

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection conn = DatabasePunishments.getConnection()) {
                ProxiedPlayer targetPlayer = plugin.getProxy().getPlayer(targetName);
                UUID targetUUID;

                if (targetPlayer == null) {
                    try {
                        targetUUID = getUUIDFromPlayerName(targetName);
                        if (targetUUID == null) {
                            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + targetName + " not found or is offline."));
                            return;
                        }
                    } catch (Exception e) {
                        player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Failed to retrieve UUID for player " + targetName));
                        e.printStackTrace();
                        return;
                    }
                } else {
                    targetUUID = targetPlayer.getUniqueId();
                }

                String sql = "SELECT * FROM punishments WHERE player_uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, targetUUID.toString());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "No punishments found for player " + targetName + "."));
                            return;
                        }

                        StringBuilder punishmentsMessage = new StringBuilder(ChatColor.YELLOW + "Punishments for " + targetName + ":\n");
                        int count = 1;

                        do {
                            String punisher = rs.getString("punisher");
                            String type = rs.getString("type");
                            String reason = rs.getString("reason");
                            String startDateStr = rs.getString("start_time");
                            long endTimeEpoch = rs.getLong("end_time");

                            LocalDateTime startDate = LocalDateTime.parse(startDateStr, START_DATE_FORMATTER);
                            LocalDateTime endDate = endTimeEpoch != 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeEpoch), ZoneId.systemDefault()) : null;

                            LocalDateTime displayStartDate = startDate.plusHours(2); // Add 2 hours for display only

                            String startDateFormatted = displayStartDate.format(DISPLAY_FORMATTER);
                            String endDateFormatted = endDate != null ? endDate.format(DISPLAY_FORMATTER) : "N/A";
                            String banLength = endDate != null ? ChronoUnit.DAYS.between(startDate, endDate) + " days" : "Permanent";

                            punishmentsMessage.append(ChatColor.YELLOW).append("[").append(count).append("] ")
                                    .append(ChatColor.YELLOW).append("Type: ").append(ChatColor.WHITE).append(type).append("\n")
                                    .append(ChatColor.YELLOW).append("Reason: ").append(ChatColor.WHITE).append(reason).append("\n")
                                    .append(ChatColor.YELLOW).append("Punished by: ").append(ChatColor.WHITE).append(punisher).append("\n")
                                    .append(ChatColor.YELLOW).append("Start Date: ").append(ChatColor.WHITE).append(startDateFormatted).append("\n")
                                    .append(ChatColor.YELLOW).append("End Date: ").append(ChatColor.WHITE).append(endDateFormatted).append("\n")
                                    .append(ChatColor.YELLOW).append("Length: ").append(ChatColor.WHITE).append(banLength).append("\n\n");

                            count++;
                        } while (rs.next());

                        player.sendMessage(new TextComponent(punishmentsMessage.toString()));
                    }
                }
            } catch (SQLException e) {
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "An error occurred while retrieving the punishments."));
                e.printStackTrace();
            }
        });
    }
}
