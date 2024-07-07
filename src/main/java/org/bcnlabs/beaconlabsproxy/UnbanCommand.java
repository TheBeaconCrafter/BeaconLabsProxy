package org.bcnlabs.beaconlabsproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public class UnbanCommand extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.unban";  // Define the required permission
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Webhooks webhooks;

    public UnbanCommand(BeaconLabsProxy plugin) {
        super("unban");
        this.plugin = plugin;
        this.webhooks = new Webhooks(this.plugin);
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (args.length != 1) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /unban <player>"));
            return;
        }

        if (!(commandSender instanceof ProxiedPlayer) || !commandSender.hasPermission(PERMISSION)) {
            commandSender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        String playerName = args[0];

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                UUID uuid = getUUIDFromPlayerName(playerName);

                if (uuid == null) {
                    player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Player " + playerName + " not found or is offline."));
                    return;
                }

                // Remove ban
                removeBan(uuid);

                // Remove ban from bans.yml file
                removeFromBanFile(uuid);

                webhooks.sendUnbanWebhook(playerName, player.getName(), LocalDateTime.now().format(formatter));

                // Broadcast unban message to players with beaconlabs.staff.read.unban permission
                for (ProxiedPlayer onlinePlayer : plugin.getProxy().getPlayers()) {
                    if (onlinePlayer.hasPermission("beaconlabs.staff.read.unban")) {
                        onlinePlayer.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + playerName + ChatColor.GRAY + " was unbanned by " + ChatColor.GOLD + commandSender.getName() + ChatColor.GRAY + "."));
                    }
                }

                // Inform staff and player
                plugin.getLogger().info(player.getName() + " unbanned player " + playerName);
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.GREEN + "Player " + playerName + " has been unbanned."));

            } catch (Exception e) {
                player.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "An error occurred while fetching the UUID: " + e.getMessage()));
                e.printStackTrace();
            }
        });
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

    // Method to remove ban from the in-memory map
    private void removeBan(UUID uuid) {
        Map<UUID, BeaconLabsProxy.BanEntry> bannedPlayers = plugin.getBannedPlayers();
        if (bannedPlayers != null) {
            bannedPlayers.remove(uuid);
        }
    }

    // Method to remove ban from bans.yml file
    private void removeFromBanFile(UUID uuid) {
        File bansFile = new File(plugin.getDataFolder(), "bans.yml");

        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(bansFile);
            config.set(uuid.toString(), null); // Remove ban entry from config
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, bansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error removing ban from bans.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
