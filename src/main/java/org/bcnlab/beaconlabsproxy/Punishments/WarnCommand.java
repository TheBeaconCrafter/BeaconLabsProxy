package org.bcnlab.beaconlabsproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class WarnCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;
    private final WarnManager warnManager;

    // Predefined reasons and their respective durations (in seconds)
    private final Map<String, PunishmentDetails> punishments;
    private static final String PERMISSION = "beaconlabs.warn";
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    public WarnCommand(BeaconLabsProxy plugin) {
        super("warn");
        this.plugin = plugin;
        this.warnManager = new WarnManager(plugin);

        // Initialize punishments map
        punishments = new HashMap<>();
        punishments.put("hacking", new PunishmentDetails("Hacking", 1209600)); // 14 day ban
        punishments.put("chatabuse", new PunishmentDetails("Chat Abuse", 604800)); // 7 day mute
        punishments.put("insult", new PunishmentDetails("Insult", 604800)); // 7 day mute
        punishments.put("advertising", new PunishmentDetails("Advertising", 86400)); // 1 day mute
        punishments.put("serverinsult", new PunishmentDetails("Server Insult", 604800)); // 7 day mute
        punishments.put("racism", new PunishmentDetails("Racism", 1209600)); // 14 day ban
        punishments.put("spamming1", new PunishmentDetails("Spam", 0)); // Kick
        punishments.put("spamming2", new PunishmentDetails("Spam", 86400)); // 1 day mute
        punishments.put("bugusing1", new PunishmentDetails("Bug Using", 0)); // Kick
        punishments.put("bugusing2", new PunishmentDetails("Bug Using", 86400)); // 1 day ban
        punishments.put("griefing", new PunishmentDetails("Griefing", 86400)); // 1 day ban
        punishments.put("reportabuse", new PunishmentDetails("Report Abuse", 604800)); // 7 day ban
        punishments.put("skin", new PunishmentDetails("Skin", 604800)); // 7 day ban
        punishments.put("name", new PunishmentDetails("Name", 604800)); // 7 day ban
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = (ProxiedPlayer) sender;
        // Check if the player has the required permission
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /warn <player> <reason>"));
            return;
        }

        String playerName = args[0];
        String reasonKey = args[1].toLowerCase();

        if (!punishments.containsKey(reasonKey)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Invalid reason. Valid reasons: " + String.join(", ", punishments.keySet())));
            return;
        }

        // Check if the player is online
        ProxiedPlayer playerToWarn = plugin.getProxy().getPlayer(playerName);
        if (playerToWarn == null) {
            UUID playerUUID = null;
            try {
                playerUUID = getUUIDFromPlayerName(playerName);
                sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "UUID is " + playerUUID));
            } catch (Exception e) {
                plugin.getLogger().severe("Cannot get UUID from Mojang API for player: " + playerName);
                sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Error fetching player data. Please try again later."));
                return;
            }

            if (playerUUID == null) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player not found."));
                return;
            }

            playerToWarn = plugin.getProxy().getPlayer(playerUUID);
            if (playerToWarn == null) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player not found."));
                return;
            }
        }

        PunishmentDetails details = punishments.get(reasonKey);
        String reason = details.getReason();
        long durationSeconds = details.getDuration();

        // Determine the type of punishment based on the reason
        switch (reasonKey) {
            case "hacking":
                warnManager.banPlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "racism":
                // Mute
                warnManager.mutePlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "bugusing1":
                // Ban
                warnManager.kickPlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason);
                break;
            case "bugusing2":
                // Ban
                warnManager.banPlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "reportabuse":
                // Ban
                warnManager.banPlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "chatabuse":
                // Mute
                warnManager.mutePlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "insult":
                // Mute
                warnManager.mutePlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "advertising":
                // Mute
                warnManager.mutePlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "serverinsult":
                // Mute
                warnManager.mutePlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "spamming1":
                // Mute
                warnManager.kickPlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason);
                break;
            case "spamming2":
                // Mute
                warnManager.mutePlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "skin":
                // Mute
                warnManager.banPlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "name":
                // Mute
                warnManager.banPlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
            case "griefing":
                // Mute
                warnManager.banPlayer(playerToWarn.getUniqueId(), playerToWarn.getName(), sender.getName(), reason, durationSeconds);
                break;
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Get list of online player names and filter them based on input
            return filterTabOptions(args[0], plugin.getProxy().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .toArray(String[]::new));
        } else if (args.length == 2) {
            // Provide tab completion for reasons
            return filterTabOptions(args[1], punishments.keySet().toArray(new String[0]));
        }
        return Collections.emptyList();
    }

    private Iterable<String> filterTabOptions(String input, String... options) {
        String lowerInput = input.toLowerCase();
        Set<String> matches = new HashSet<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lowerInput)) {
                matches.add(option);
            }
        }
        return matches.isEmpty() ? Collections.emptyList() : matches;
    }


    private static class PunishmentDetails {
        private final String reason;
        private final long duration;

        public PunishmentDetails(String reason, long duration) {
            this.reason = reason;
            this.duration = duration;
        }

        public String getReason() {
            return reason;
        }

        public long getDuration() {
            return duration;
        }
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
}
