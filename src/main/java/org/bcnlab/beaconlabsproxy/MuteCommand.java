package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MuteCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.mute";  // Define the required permission
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Webhooks webhooks;

    public MuteCommand(BeaconLabsProxy plugin) {
        super("mute");
        this.plugin = plugin;
        this.webhooks = new Webhooks(this.plugin);
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent(ChatColor.RED + "This command can only be executed by a player."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;

        // Check if the player has the required permission
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /mute <player> <duration> <reason>"));
            return;
        }

        String playerName = args[0];
        String durationString = args[1];
        StringBuilder reasonBuilder = new StringBuilder();

        // Combine all remaining arguments as reason
        for (int i = 2; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        ProxiedPlayer playerToMute = plugin.getProxy().getPlayer(playerName);
        UUID uuid = playerToMute != null ? playerToMute.getUniqueId() : null;
        String lastName = playerToMute != null ? playerToMute.getName() : null;

        if (uuid == null) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + playerName + " not found or is offline."));
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime muteEnd = calculateMuteEnd(durationString);

        // Save the mute to database with UUID and last name
        addMuteToDatabase(uuid.toString(), lastName, player.getName(), reason, now, muteEnd);

        // Send the mute webhook
        sendMuteWebhook(playerName, reason, durationString, player.getName(), now, muteEnd);

        // Inform staff and player
        plugin.getLogger().info(player.getName() + " muted player " + playerName + " for " + durationString + " with reason: " + reason);
        player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Player " + playerName + " has been muted for " + durationString + "."));

        // Optionally broadcast mute message to players with beaconlabs.staff.read.mute permission
        // broadcastMuteMessage(playerName, durationString, reason, player.getName(), now, muteEnd);
    }

    private LocalDateTime calculateMuteEnd(String durationString) {
        // Implement logic to parse durationString into LocalDateTime for mute end time
        // Example logic:
        LocalDateTime muteEnd = null;

        if (durationString.matches("\\d+d")) {
            int days = Integer.parseInt(durationString.substring(0, durationString.length() - 1));
            muteEnd = LocalDateTime.now().plusDays(days);
        } else if (durationString.matches("\\d+h")) {
            int hours = Integer.parseInt(durationString.substring(0, durationString.length() - 1));
            muteEnd = LocalDateTime.now().plusHours(hours);
        } else if (durationString.matches("\\d+m")) {
            int minutes = Integer.parseInt(durationString.substring(0, durationString.length() - 1));
            muteEnd = LocalDateTime.now().plusMinutes(minutes);
        } else if (durationString.matches("\\d+s")) {
            int seconds = Integer.parseInt(durationString.substring(0, durationString.length() - 1));
            muteEnd = LocalDateTime.now().plusSeconds(seconds);
        }

        return muteEnd;
    }

    private void addMuteToDatabase(String uuid, String lastName, String punisherName, String reason, LocalDateTime startTime, LocalDateTime endTime) {
        try (Connection conn = DatabasePunishments.getConnection()) {
            String insertSql = "INSERT INTO punishments (player_uuid, punisher, type, reason, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, uuid);
                insertStmt.setString(2, punisherName);
                insertStmt.setString(3, "Mute");
                insertStmt.setString(4, reason);
                insertStmt.setString(5, startTime.format(formatter));
                insertStmt.setString(6, endTime != null ? endTime.format(formatter) : null); // End time can be null for permanent mutes
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding mute record to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMuteWebhook(String playerName, String reason, String durationString, String punisherName, LocalDateTime startTime, LocalDateTime endTime) {
        String formattedStartTime = startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss"));
        String formattedEndTime = endTime != null ? endTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) : "Permanent";

        // Implement webhook sending logic here
        // Example:
        webhooks.sendMuteWebhook(playerName, reason, durationString, punisherName, formattedStartTime, formattedEndTime);
    }
}
