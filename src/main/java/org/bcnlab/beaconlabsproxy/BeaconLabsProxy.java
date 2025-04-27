package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bcnlab.beaconlabsproxy.Database.DatabasePlayers;
import org.bcnlab.beaconlabsproxy.Database.DatabasePunishments;
import org.bcnlab.beaconlabsproxy.Database.DatabaseReports;
import org.bcnlab.beaconlabsproxy.Database.ServerGuardDatabase;
import org.bcnlab.beaconlabsproxy.Listeners.BackendKickListener;
import org.bcnlab.beaconlabsproxy.Listeners.JoinListener;
import org.bcnlab.beaconlabsproxy.Listeners.PingListener;
import org.bcnlab.beaconlabsproxy.Listeners.PlaytimeListener;
import org.bcnlab.beaconlabsproxy.Punishments.*;
import org.bcnlab.beaconlabsproxy.ServerGuard.ServerGuardCommand;
import org.bcnlab.beaconlabsproxy.ServerGuard.ServerGuardListener;
import org.bcnlab.beaconlabsproxy.ServerGuard.ServerGuardManager;
import org.bcnlab.beaconlabsproxy.ServerGuard.ServerPermissionsCommand;
import org.bcnlab.beaconlabsproxy.LabsCommand;
import org.bcnlab.beaconlabsproxy.InviteCommand;

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
import java.util.*;

public final class BeaconLabsProxy extends Plugin implements Listener {

    private String prefix = "[BeaconLabs]";
    private String versionNumber = "1.5";
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
        DatabasePlayers.initialize();
        ServerGuardDatabase.initialize();

        // Create an instance of WhitelistCommand
        WhitelistCommand whitelistCommand = new WhitelistCommand(this);

        String logDirectory = getDataFolder() + "/chatlogs";
        File logDir = new File(logDirectory);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // Create an instance of FileChatLogger
        FileChatLogger chatLogger = new FileChatLogger(logDirectory);

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
        proxy.getPluginManager().registerCommand(this, new JoinMeCommand(this));
        proxy.getPluginManager().registerCommand(this, new RequestServerJoinCommand(this));
        proxy.getPluginManager().registerCommand(this, new InviteCommand(this));
        proxy.getPluginManager().registerCommand(this, new MaintenanceCommand(this));
        proxy.getPluginManager().registerCommand(this, new LobbyCommand(this));
        proxy.getPluginManager().registerCommand(this, new ProxyCommand(this));
        proxy.getPluginManager().registerCommand(this, new WhitelistCommand(this));
        proxy.getPluginManager().registerCommand(this, new WarnCommand(this));
        proxy.getPluginManager().registerCommand(this, new ChatReportCommand(chatLogger, this));
        proxy.getPluginManager().registerCommand(this, new ClearChatLogs(this));
        proxy.getPluginManager().registerCommand(this, new ConsoleClearPunishments(this));
        proxy.getPluginManager().registerCommand(this, new PlaytimeCommand(this));
        proxy.getPluginManager().registerCommand(this, new ServerGuardCommand(this));
        proxy.getPluginManager().registerCommand(this, new ServerPermissionsCommand(this));
        proxy.getPluginManager().registerCommand(this, new LabsCommand(this));

        getLogger().info("All commands were registered.");

        // Register event listeners
        proxy.getPluginManager().registerListener(this, this);
        proxy.getPluginManager().registerListener(this, new JoinListener(this, whitelistCommand));
        proxy.getPluginManager().registerListener(this, new MuteListener(this));
        proxy.getPluginManager().registerListener(this, new ChatFilterListener(this));
        proxy.getPluginManager().registerListener(this, new PingListener(this));
        proxy.getPluginManager().registerListener(this, new PlaytimeListener(this));
        proxy.getPluginManager().registerListener(this, new BackendKickListener(this));
        proxy.getPluginManager().registerListener(this, new ServerGuardListener(this));
        proxy.getPluginManager().registerListener(this, chatLogger);

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
                configuration.set("maintenance", false);
                configuration.set("whitelist", false);
                configuration.set("serverguard.enabled", true);
                configuration.set("serverguard.allowed-servers", new String[]{"lobby", "server1", "server2"});
                
                // Initialize server-specific permissions configuration
                configuration.set("serverguard.server-permissions.creative", "beaconlabs.servers.creative");
                configuration.set("serverguard.server-permissions.survival", "beaconlabs.servers.survival");
                configuration.set("ban-message-format", "&c&oBanned by an Admin\n&7\n&cReason: %s\n&7\n&cUnban Date &8» &7%s\n&7\n&8Unban applications on Discord\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
                configuration.set("kick-message-format", "&c&oKicked by an Admin\n&7\n&cReason: %s\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
                configuration.set("maintenance-message-format", "&6&lServer currently in maintenance\n&7\n&cWe are working on getting the server back online!\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
                configuration.set("whitelist-message-format", "&6&lWhitelist is currently on\n&7\n&cYou are not permitted to join this server!\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
                configuration.set("webhook.url", "https://your-discord-webhook-url");
                configuration.set("maxplayers", 100);
                configuration.set("motd", "&b&lBEACON Lab &f&lTraining\n@dynamicmsg@");
                configuration.set("motd-maintenance", "&b&lBEACON Lab &f&lTraining\n&6&lCurrently in maintenance!");
                configuration.set("dynamicmsgs", new String[]{"&d&lCome and enjoy our brand new games!", "&d&lNew KnockbackFFA mode!", "&d&lCome join us!"});
                configuration.set("disallowedJoinmeServers", new String[]{"lobby", "lobby-1", "lobby-2"});
                configuration.set("lobby-server", "lobby");
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
            }

            // Load configuration
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            prefix = ChatColor.translateAlternateColorCodes('&', configuration.getString("prefix", "&6[BeaconLabs]&r "));
            webhookUrl = configuration.getString("webhook.url");

            // Load Server Guard settings
            boolean isServerGuardEnabled = configuration.getBoolean("serverguard.enabled", true);
            String[] allowedServers = configuration.getStringList("serverguard.allowed-servers").toArray(new String[0]);
            
            // Load server-specific permission requirements
            Map<String, String> serverPermissions = new HashMap<>();
            
            // First check under serverguard.server-permissions (correct format)
            if (configuration.contains("serverguard.server-permissions")) {
                Configuration permConfig = configuration.getSection("serverguard.server-permissions");
                for (String server : permConfig.getKeys()) {
                    String permission = permConfig.getString(server);
                    if (permission != null && !permission.isEmpty()) {
                        serverPermissions.put(server.toLowerCase(), permission);
                    }
                }
            }
            
            // Also check at root level for backward compatibility
            if (configuration.contains("server-permissions")) {
                Configuration permConfig = configuration.getSection("server-permissions");
                for (String server : permConfig.getKeys()) {
                    String permission = permConfig.getString(server);
                    if (permission != null && !permission.isEmpty()) {
                        serverPermissions.put(server.toLowerCase(), permission);
                    }
                }
            }

            // Apply server guard settings
            ServerGuardManager.setEnabled(isServerGuardEnabled);
            ServerGuardManager.setAllowedServers(new HashSet<>(Arrays.asList(allowedServers)));
            ServerGuardManager.setServerPermissions(serverPermissions);
            getLogger().info("ServerGuard enabled: " + isServerGuardEnabled);
            getLogger().info("Allowed Servers: " + String.join(", ", allowedServers));
            if (!serverPermissions.isEmpty()) {
                getLogger().info("Server-specific permissions configured: " + serverPermissions.size());
            }

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
        clearLogDirectory();
        getLogger().info("BeaconLabs Proxy system was disabled.");
    }

    public void clearLogDirectory() {
        File logDir = new File(getDataFolder(), "chatlogs");
        if (logDir.exists() && logDir.isDirectory()) {
            for (File file : logDir.listFiles()) {
                if (!file.delete()) {
                    getLogger().warning("Failed to delete file: " + file.getName());
                }
            }
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public String getVersion() {
        return versionNumber;
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

    public String getMaintenanceMessageFormat() {
        return configuration.getString("maintenance-message-format", "&6&lServer currently in maintenance\n&7\n&cWe are working on getting the server back online!\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
    }

    public String getWhitelistMessageFormat() {
        return configuration.getString("whitelist-message-format", "&6&lWhitelist is currently on\n&7\n&cYou are not permitted to join this server!\n&7\n&eDiscord &8» &c&ndc.example.com\n&eWebsite &8» &c&eexample.com");
    }

    public void setMaintenance(Boolean maintenance) throws IOException {
        configuration.set("maintenance", maintenance);
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
    }

    public void setWhitelist(Boolean whitelist) throws IOException {
        configuration.set("whitelist", whitelist);
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
    }

    public boolean getMaintenance() {
        return configuration.getBoolean("maintenance");
    }

    public boolean getWhiteList() {
        return configuration.getBoolean("whitelist");
    }

    public String getMOTD() {
        return configuration.getString("motd", "&6&lERROR in your MOTD config");
    }

    public String getMOTDMaintenance() {
        return configuration.getString("motd-maintenance", "&6&lERROR in your MOTD Maintenance config");
    }

    public String[] getDynamicMSGs() {
        return configuration.getStringList("dynamicmsgs").toArray(new String[0]);
    }

    public String[] getDisallowedJoinmeServers() {
        return configuration.getStringList("disallowedJoinmeServers").toArray(new String[0]);
    }

    public String getLobbyServer() {
        return configuration.getString("lobby-server", "lobby");
    }

    public int getMaxPlayers() {
        return configuration.getInt("maxplayers");
    }

    public boolean isPlayerBanned(UUID uuid) {
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

    public String getPlayerBanReason(UUID uuid) {
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

    public LocalDateTime getPlayerUnbanDate(UUID uuid) {
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

    public String formatBanMessage(String reason, LocalDateTime unbanDate) {
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

    /**
     * Reloads the configuration from disk and updates plugin settings.
     */
    public void reloadConfig() {
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            getLogger().severe("Failed to reload config: " + e.getMessage());
            return;
        }
        // Update prefix
        prefix = ChatColor.translateAlternateColorCodes('&', configuration.getString("prefix", prefix));
        // Update webhook URL
        webhookUrl = configuration.getString("webhook.url", webhookUrl);
        // Reload ServerGuard settings
        boolean isServerGuardEnabled = configuration.getBoolean("serverguard.enabled", true);
        Set<String> allowed = new HashSet<>(configuration.getStringList("serverguard.allowed-servers"));
        Map<String, String> perms = new HashMap<>();
        if (configuration.contains("serverguard.server-permissions")) {
            Configuration permConfig = configuration.getSection("serverguard.server-permissions");
            for (String srv : permConfig.getKeys()) {
                String p = permConfig.getString(srv);
                if (p != null && !p.isEmpty()) perms.put(srv.toLowerCase(), p);
            }
        }
        if (configuration.contains("server-permissions")) {
            Configuration permConfig = configuration.getSection("server-permissions");
            for (String srv : permConfig.getKeys()) {
                String p = permConfig.getString(srv);
                if (p != null && !p.isEmpty()) perms.put(srv.toLowerCase(), p);
            }
        }
        ServerGuardManager.setEnabled(isServerGuardEnabled);
        ServerGuardManager.setAllowedServers(allowed);
        ServerGuardManager.setServerPermissions(perms);
        getLogger().info("Configuration reloaded.");
    }

    /** Accessors for configuration. */
    public Configuration getConfiguration() { return configuration; }
    public File getConfigFile() { return file; }
    /** Saves the current configuration to disk. */
    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
        } catch (IOException e) {
            getLogger().severe("Failed to save config: " + e.getMessage());
        }
    }
}
