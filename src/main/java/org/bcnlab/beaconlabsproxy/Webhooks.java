package org.bcnlab.beaconlabsproxy;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Webhooks {
    private final BeaconLabsProxy plugin;

    public Webhooks(BeaconLabsProxy plugin) {
        this.plugin = plugin;
    }

    public void sendKickWebhook(String username, String reason, String sender, String timestamp) {
        String jsonPayload = String.format(
                "{\n" +
                        "  \"embeds\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Kick\",\n" +
                        "      \"color\": 15844367,\n" +
                        "      \"fields\": [\n" +
                        "        {\n" +
                        "          \"name\": \"Username\",\n" +
                        "          \"value\": \"%s\",\n" +
                        "          \"inline\": true\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Reason\",\n" +
                        "          \"value\": \"%s\",\n" +
                        "          \"inline\": true\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"From\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Timestamp\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}", username, reason, sender, timestamp);
        sendWebhook(jsonPayload);
    }

    public void sendBanWebhook(String username, String reason, String duration, String sender, String timestamp) {
        String jsonPayload = String.format(
                "{\n" +
                        "  \"embeds\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Ban\",\n" +
                        "      \"color\": 16737380,\n" +
                        "      \"fields\": [\n" +
                        "        {\n" +
                        "          \"name\": \"Username\",\n" +
                        "          \"value\": \"%s\",\n" +
                        "          \"inline\": true\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Reason\",\n" +
                        "          \"value\": \"%s\",\n" +
                        "          \"inline\": true\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Duration\",\n" +
                        "          \"value\": \"%s\",\n" +
                        "          \"inline\": true\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"From\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Timestamp\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}", username, reason, duration, sender, timestamp);
        sendWebhook(jsonPayload);
    }

    public void sendUnbanWebhook(String username, String sender, String timestamp) {
        String jsonPayload = String.format(
                "{\n" +
                        "  \"embeds\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Unban\",\n" +
                        "      \"color\": 3066993,\n" +
                        "      \"fields\": [\n" +
                        "        {\n" +
                        "          \"name\": \"Username\",\n" +
                        "          \"value\": \"%s\",\n" +
                        "          \"inline\": true\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"From\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Timestamp\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}", username, sender, timestamp);
        sendWebhook(jsonPayload);
    }

    public void sendMuteWebhook(String username, String reason, String durationString, String sender, String startTimeFormatted, String endTimeFormatted) {
        String jsonPayload = String.format(
                "{\n" +
                        "  \"embeds\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Mute\",\n" +
                        "      \"color\": 14177041,\n" +
                        "      \"fields\": [\n" +
                        "        {\n" +
                        "          \"name\": \"Username\",\n" +
                        "          \"value\": \"%s\",\n" +
                        "          \"inline\": true\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Reason\",\n" +
                        "          \"value\": \"%s\",\n" +
                        "          \"inline\": true\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Duration\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Punisher\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"Start Time\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"name\": \"End Time\",\n" +
                        "          \"value\": \"%s\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}", username, reason, durationString, sender, startTimeFormatted, endTimeFormatted);

        sendWebhook(jsonPayload);
    }

    private void sendWebhook(String jsonPayload) {
        try {
            URL url = new URL(plugin.webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                plugin.getLogger().warning("Failed to send webhook: " + responseCode);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error while sending webhook: " + e.getMessage());
        }
    }
}
