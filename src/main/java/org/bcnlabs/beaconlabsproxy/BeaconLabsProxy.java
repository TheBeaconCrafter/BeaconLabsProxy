package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BeaconLabsProxy extends Plugin implements Listener {

    private String prefix = "[BeaconLabs]";
    private File file;
    private Configuration configuration;

    private static BeaconLabsProxy instance;

    public String webhookUrl;
    private final Map<UUID, BanEntry> bannedPlayers = new HashMap<>();  // Map to store banned players

    @Override
    public void onEnable() {
        setInstance(this);

        getLogger().info("BeaconLabs Proxy system was enabled.");

        loadBanData();

        // Register commands and other initialization
        ProxyServer proxy = ProxyServer.getInstance();
        proxy.getPluginManager().registerCommand(this, new PingCommand(this));
        proxy.getPluginManager().registerCommand(this, new KickCommand(this));
        proxy.getPluginManager().registerCommand(this, new BanCommand(this));
        proxy.getPluginManager().registerCommand(this, new UnbanCommand(this));
        proxy.getPluginManager().registerCommand(this, new TeamChatCommand(this));
        proxy.getPluginManager().registerCommand(this, new BroadcastCommand(this));
        proxy.getPluginManager().registerCommand(this, new GotoCommand(this));
        proxy.getPluginManager().registerCommand(this, new MsgCommand(this));
        proxy.getPluginManager().registerCommand(this, new InfoCommand(this));
        proxy.getPluginManager().registerCommand(this, new StaffCommand(this));

        getLogger().info("All commands were registered.");

        // Set up configuration file
        file = new File(getDataFolder(), "config.yml");

        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();  // Ensure parent directories exist
                file.createNewFile();  // Create new config file if it doesn't exist

                // Load default configuration and save it
                configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                configuration.set("prefix", "&6[BeaconLabs]&r ");
                configuration.set("kick-message-format", "&cYou were kicked from BeaconLabs\nReason: %s\n&6Our website: example.com");
                configuration.set("webhook.url", "https://your-discord-webhook-url");
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
            }

            // Load configuration
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            prefix = ChatColor.translateAlternateColorCodes('&', configuration.getString("prefix", "&6[BeaconLabs]&r "));
            webhookUrl = configuration.getString("webhook.url");

            getLogger().info("Webhook URL loaded: " + webhookUrl);

        } catch (IOException e) {
            getLogger().severe("Error loading configuration: " + e.getMessage());
            e.printStackTrace();
        }

        // Register event listeners
        proxy.getPluginManager().registerListener(this, this);
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

    // Getter for bannedPlayers map
    public Map<UUID, BanEntry> getBannedPlayers() {
        return bannedPlayers;
    }

    private void loadBanData() {
        File bansFile = new File(getDataFolder(), "bans.yml");

        try {
            if (!bansFile.exists()) {
                bansFile.createNewFile();
            }

            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(bansFile);

            for (String key : config.getKeys()) {
                UUID uuid = UUID.fromString(key);
                String playerName = config.getString(key + ".playerName");
                String reason = config.getString(key + ".reason");

                bannedPlayers.put(uuid, new BanEntry(reason));
            }

            getLogger().info("bannedPlayers: " + bannedPlayers);
        } catch (IOException e) {
            getLogger().severe("Error loading bans.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getKickMessageFormat() {
        return configuration.getString("kick-message-format", "&cYou were kicked from BeaconLabs\nReason: %s\nAppeal on our website: %s");
    }

    // Event listener for handling PostLoginEvent
    @EventHandler
    public void onJoin(PostLoginEvent event) {
        loadBanData();

        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        getLogger().info("A player joined");
        getLogger().info("has UUID " + uuid);

        if (isPlayerBanned(uuid)) {
            getLogger().info("Player is banned");
            String kickMessage = String.format(getKickMessageFormat(), bannedPlayers.get(uuid).getReason(), "example.com");
            player.disconnect(ChatColor.translateAlternateColorCodes('&', kickMessage));
        } else {
            getLogger().info("Player isn't banned");
        }
    }


    // Check if a player is banned
    private boolean isPlayerBanned(UUID uuid) {
        return bannedPlayers.containsKey(uuid);
    }

    // Inner class representing a ban entry
    static class BanEntry {
        private final String reason;

        public BanEntry(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
