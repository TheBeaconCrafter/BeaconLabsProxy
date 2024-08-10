package org.bcnlab.beaconlabsproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatReportCommand extends Command implements TabExecutor {

    private final FileChatLogger chatLogger;
    private static final String PERMISSION = "beaconlabs.chatreport";  // Define the required permission
    private static final String PASTEBIN_URL = "https://paste.md-5.net/documents";
    private final BeaconLabsProxy plugin;

    public ChatReportCommand(FileChatLogger chatLogger, BeaconLabsProxy plugin) {
        super("chatreport", null, "reportchat", "cr");
        this.chatLogger = chatLogger;
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Only players can use this command."));
            return;
        }

        ProxiedPlayer sender = (ProxiedPlayer) commandSender;

        // Check if the player has the required permission
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /chatreport <player>"));
            return;
        }

        String targetName = args[0];

        sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GRAY + "Gathering chat logs from proxy server..."));

        UUID playerId;
        try {
            playerId = getUUIDFromPlayerName(targetName);
            if (playerId == null) {
                sender.sendMessage(new TextComponent("Player " + targetName + " does not exist or is not online."));
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(new TextComponent("Failed to retrieve player UUID."));
            return;
        }

        long startTime = System.currentTimeMillis();  // Start time measurement

        // Read the chat log for the player using the FileChatLogger instance
        String chatLog;
        try {
            chatLog = chatLogger.readChatLog(playerId);
        } catch (IOException e) {
            e.printStackTrace();
            sender.sendMessage(new TextComponent("Failed to retrieve chat logs."));
            return;
        }

        // Upload chat log to paste.md-5.net
        String pasteLink;
        try {
            pasteLink = uploadToPastebin(chatLog);
        } catch (IOException e) {
            e.printStackTrace();
            sender.sendMessage(new TextComponent("Failed to upload chat logs."));
            return;
        }

        long endTime = System.currentTimeMillis();  // End time measurement
        long duration = endTime - startTime;  // Calculate the duration in milliseconds

        // Create clickable link
        TextComponent linkMessage = new TextComponent(plugin.getPrefix() + ChatColor.GRAY + "Chat log for " + ChatColor.RED + targetName + ChatColor.GRAY + " has been uploaded. ");
        TextComponent link = new TextComponent(ChatColor.RED + "[Click here to view]");
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, pasteLink));
        linkMessage.addExtra(link);

        // Add time taken
        TextComponent timeMessage = new TextComponent(ChatColor.GRAY + " (took " + duration + " ms)");

        // Send the combined message to the player
        linkMessage.addExtra(timeMessage);
        sender.sendMessage(linkMessage);
    }

    private String uploadToPastebin(String content) throws IOException {
        URL url = new URL(PASTEBIN_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/plain");

        // Write the content to the output stream
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(content.getBytes());
            outputStream.flush();
        } finally {
            outputStream.close();  // Ensure the stream is closed
        }

        // Read the response to get the key
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String key;
        try {
            key = reader.readLine();
        } finally {
            reader.close();  // Ensure the reader is closed
        }
        return "https://paste.md-5.net/" + key.split("\"")[3];  // Example response is {"key":"ecumusuhil"} so we need [3] element in the array
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
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                playerNames.add(p.getName());
            }
            return playerNames;
        }
        return new ArrayList<>();
    }
}
