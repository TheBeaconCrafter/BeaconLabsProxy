package org.bcnlab.beaconlabsproxy;

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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BeaconLabsProxy extends Plugin implements Listener {

    private String prefix = "[BeaconLabs]";
    private File file;
    private Configuration configuration;

    private static BeaconLabsProxy instance;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String webhookUrl;
    private final Map<UUID, BanEntry> bannedPlayers = new HashMap<>();  // Map to store banned players

    @Override
    public void onEnable() {
        setInstance(this);

        getLogger().info("BeaconLabs Proxy system was enabled.");

        DatabaseReports.initialize();
        DatabasePunishments.initialize();

        // Register commands and other initialization
        ProxyServer proxy = ProxyServer.getInstance();
        proxy.getPluginManager().registerCommand(this, new PingCommand(this));
        proxy.getPluginManager().registerCommand(this, new KickCommand(this));
        proxy.getPluginManager().registerCommand(this, new BanCommand(this));
        proxy.getPluginManager().registerCommand(this, new UnbanCommand(this));
        proxy.getPluginManager().registerCommand(this, new UnbanIDCommand(this));
        proxy.getPluginManager().registerCommand(this, new TeamChatCommand(this));
        proxy.getPluginManager().registerCommand(this, new BroadcastCommand(this));
        proxy.getPluginManager().registerCommand(this, new GotoCommand(this));
        proxy.getPluginManager().registerCommand(this, new MsgCommand(this));
        proxy.getPluginManager().registerCommand(this, new InfoCommand(this));
        proxy.getPluginManager().registerCommand(this, new StaffCommand(this));
        proxy.getPluginManager().registerCommand(this, new UidLookup(this));
        proxy.getPluginManager().registerCommand(this, new ReportCommand(this));
        proxy.getPluginManager().registerCommand(this, new ViewReportsCommand(this));
        proxy.getPluginManager().registerCommand(this, new ClosereportCommand(this));
        proxy.getPluginManager().registerCommand(this, new PunishmentsCommand(this));
        proxy.getPluginManager().registerCommand(this, new ClearPunishmentsCommand(this));
        proxy.getPluginManager().registerCommand(this, new MuteCommand(this));
        proxy.getPluginManager().registerCommand(this, new UnmuteCommand(this));
        proxy.getPluginManager().registerCommand(this, new SkinCommand(this));

        getLogger().info("All commands were registered.");

        // Register event listeners
        proxy.getPluginManager().registerListener(this, this);
        proxy.getPluginManager().registerListener(this, new MuteListener(this));
        proxy.getPluginManager().registerListener(this, new ChatFilterListener(this));
        proxy.getPluginManager().registerListener(this, new PingListener(this));

        getLogger().info("All listeners were registered.");

        // Set up configuration file
        file = new File(getDataFolder(), "config.yml");

        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();  // Ensure parent directories exist
                file.createNewFile();  // Create new config file if it doesn't exist

                // Load default configuration and save it
                configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                configuration.set("prefix", "&6BeaconLabs &8» ");
                configuration.set("ban-message-format", "&c&oBanned by an Admin\n&7\n&cReason: %s\n&7\n&cUnban Date &8» &7%s\n&7\n&8Unban applications on Discord\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
                configuration.set("kick-message-format", "&c&oKicked by an Admin\n&7\n&cReason: %s\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
                configuration.set("webhook.url", "https://your-discord-webhook-url");
                configuration.set("maxplayers", 100);
                configuration.set("motd", "&b&lBEACON Lab &f&lTraining\n@dynamicmsg@");
                configuration.set("dynamicmsgs", new String[]{"&d&lCome and enjoy our brand new games!", "&d&lNew KnockbackFFA mode!", "&d&lCome join us!"});
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

    public String getKickMessageFormat() {
        return configuration.getString("kick-message-format", "&c&oKicked by an Admin\n&7\n&cReason: %s\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
    }

    public String getBanMessageFormat() {
        return configuration.getString("ban-message-format", "&c&oBanned by an Admin\n&7\n&cReason: %s\n&7\n&cUnban Date &8» &7%s\n&7\n&8Unban applications on Discord\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
    }

    public String getMOTD() {
        return configuration.getString("motd", "&6&lERROR in your MOTD config");
    }

    public String[] getDynamicMSGs() {
        return configuration.getStringList("dynamicmsgs").toArray(new String[0]);
    }

    public int getMaxPlayers() {
        return configuration.getInt("maxplayers");
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        getLogger().info("A player joined with UUID: " + uuid);

        if (isPlayerBanned(uuid)) {
            getLogger().info("Player is banned");

            // Fetch ban reason and unban date
            String banReason = getPlayerBanReason(uuid);
            LocalDateTime unbanDate = getPlayerUnbanDate(uuid);

            // Format the ban message including reason and unban date
            String banMessage = formatBanMessage(banReason, unbanDate);

            // Disconnect the player with the formatted ban message
            player.disconnect(ChatColor.translateAlternateColorCodes('&', banMessage));
        } else {
            getLogger().info("Player isn't banned");
        }
    }

    private boolean isPlayerBanned(UUID uuid) {
        try (Connection conn = DatabasePunishments.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM punishments WHERE player_uuid = ? AND type = 'ban'")) {
            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long endTimeEpoch = rs.getLong("end_time");

                    // Convert epoch time to LocalDateTime
                    LocalDateTime endTime;
                    if (endTimeEpoch == 0) {
                        endTime = null; // Handle cases where there's no end time set (e.g., permanent ban)
                    } else {
                        Instant instant = Instant.ofEpochMilli(endTimeEpoch);
                        endTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    }

                    // Check if end time is in the future (meaning the ban is active)
                    if (endTime == null || endTime.isAfter(LocalDateTime.now())) {
                        // Player is still banned
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Error checking ban for player " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        // No active ban found
        return false;
    }

    private String getPlayerBanReason(UUID uuid) {
        try (Connection conn = DatabasePunishments.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT reason FROM punishments WHERE player_uuid = ? AND type = 'ban' ORDER BY start_time DESC LIMIT 1")) {
            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("reason");
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Error retrieving ban reason for player " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
        return "Unknown Reason";
    }

    private LocalDateTime getPlayerUnbanDate(UUID uuid) {
        try (Connection conn = DatabasePunishments.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT end_time FROM punishments WHERE player_uuid = ? AND type = 'ban' ORDER BY start_time DESC LIMIT 1")) {
            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long endTimeEpoch = rs.getLong("end_time");

                    // Convert epoch time to LocalDateTime
                    if (endTimeEpoch != 0) {
                        Instant instant = Instant.ofEpochMilli(endTimeEpoch);
                        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    }
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Error retrieving unban date for player " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Default to null if no valid unban date is found
    }

    private String formatBanMessage(String reason, LocalDateTime unbanDate) {
        String banMessageFormat = getBanMessageFormat();
        String formattedUnbanDate = (unbanDate != null) ? unbanDate.toString() : "Permanent";
        return String.format(banMessageFormat, reason, formattedUnbanDate);
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
