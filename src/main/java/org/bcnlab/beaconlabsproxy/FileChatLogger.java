package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class FileChatLogger implements Listener {

    private final String logDirectory;

    public FileChatLogger(String logDirectory) {
        this.logDirectory = logDirectory;
        clearLogs();  // Clear logs on initialization (optional, based on your preference)
    }

    // This method handles logging chat messages to a file
    public void logChat(UUID playerId, String playerName, String message, long logStartTime) {
        String filePath = logDirectory + "/" + playerId.toString() + ".log";
        File logFile = new File(filePath);
        boolean isNewFile = !logFile.exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            // If this is a new log file, write the header with username, UUID, and file creation timestamp
            if (isNewFile) {
                String fileCreationTime = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss").format(new Date());
                writer.write("Username: " + playerName);
                writer.newLine();
                writer.write("UUID: " + playerId);
                writer.newLine();
                writer.write("File created: " + fileCreationTime);
                writer.newLine();
                writer.newLine(); // Add an empty line after the header
            }

            // Format the log message
            long currentTime = System.currentTimeMillis();
            String formattedMessage = formatLogMessage(message, logStartTime, currentTime);
            writer.write(formattedMessage);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method reads the chat log for a specific player
    public String readChatLog(UUID playerId) throws IOException {
        String filePath = logDirectory + "/" + playerId.toString() + ".log";
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    // This method deletes all log files in the log directory
    private void clearLogs() {
        File dir = new File(logDirectory);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isDirectory()) {
                        file.delete();  // Delete the file
                    }
                }
            }
        }
    }

    // This method listens for chat events and logs the messages
    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;  // Ignore non-player senders
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        String message = event.getMessage();

        long logStartTime = System.currentTimeMillis();  // Store the start time of the log
        String logMessage = String.format("%s", message);

        // Log the chat message
        logChat(playerId, playerName, logMessage, logStartTime);
    }

    // Helper method to format the log message
    private String formatLogMessage(String message, long logStartTime, long currentTime) {
        long elapsedTime = currentTime - logStartTime;
        Duration duration = Duration.ofMillis(elapsedTime);

        long days = duration.toDays();
        long totalHours = duration.toHours();
        long hours = totalHours % 24;
        long totalMinutes = duration.toMinutes();
        long minutes = totalMinutes % 60;

        String formattedTime = String.format("[%dd %dh %dm ago]", days, hours, minutes);
        String timestamp = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss").format(new Date(currentTime));

        return String.format("%s %s | %s", formattedTime, timestamp, message);
    }
}
