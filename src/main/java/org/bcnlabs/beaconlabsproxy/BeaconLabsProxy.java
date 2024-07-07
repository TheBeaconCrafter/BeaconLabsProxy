package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class BeaconLabsProxy extends Plugin {

    public String prefix = "[BeaconLabs]";
    private File file;
    private Configuration configuration;

    private static BeaconLabsProxy instance;

    private String webhookUrl;

    @Override
    public void onEnable() {
        setInstance(this);

        getLogger().info("BeaconLabs Proxy system was enabled.");
        getProxy().getPluginManager().registerCommand(this, new PingCommand(this));
        getProxy().getPluginManager().registerCommand(this, new KickCommand(this));

        file = new File(ProxyServer.getInstance().getPluginsFolder() + "/BeaconLabs/config.yml");
        File parentDir = file.getParentFile();

        try {
            if (!parentDir.exists()) {
                parentDir.mkdirs();  // Create the directories if they do not exist
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

            if (!configuration.contains("webhook.url")) {
                configuration.set("webhook.url", "https://your-discord-webhook-url");
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
            }

            webhookUrl = configuration.getString("webhook.url");
            getLogger().info("Webhook URL loaded: " + webhookUrl);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("BeaconLabs Proxy system was disabled.");
    }

    public String getPrefix() {
        return prefix;
    }

    public static BeaconLabsProxy getInstance() {
        return instance;
    }

    private static void setInstance(BeaconLabsProxy instance) {
        BeaconLabsProxy.instance = instance;
    }

    public void sendDiscordWebhook(String username, String reason, String duration, String sender, String timestamp) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);

            String jsonPayload = String.format(
                    "{\n" +
                            "  \"embeds\": [\n" +
                            "    {\n" +
                            "      \"title\": \"Punishment\",\n" +
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

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                getLogger().warning("Failed to send webhook: " + responseCode);
            }

        } catch (Exception e) {
            getLogger().warning("Error while sending webhook: " + e.getMessage());
        }
    }
}
