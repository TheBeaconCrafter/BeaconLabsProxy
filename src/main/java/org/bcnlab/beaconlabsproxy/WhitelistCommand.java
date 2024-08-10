package org.bcnlab.beaconlabsproxy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WhitelistCommand extends Command implements TabExecutor {

    private final BeaconLabsProxy plugin;
    private final Set<String> whitelistedPlayers = new HashSet<>();
    private boolean whitelistEnabled = false;
    private final File whitelistFile;
    private final Gson gson = new Gson();

    public WhitelistCommand(BeaconLabsProxy plugin) {
        super("pwhitelist", "", "pwh", "proxywhitelist", "pwl");
        this.plugin = plugin;
        this.whitelistFile = new File(plugin.getDataFolder(), "whitelist.json");
        loadWhitelist();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0 || args.length > 2) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /whitelist <on|off|add|remove|list> [player]");
            return;
        }

        if (!sender.hasPermission("beaconlabs.whitelist")) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        String option = args[0].toLowerCase();

        switch (option) {
            case "on":
                whitelistEnabled = true;
                try {
                    plugin.setWhitelist(true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                kickNonWhitelistedPlayers();
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Whitelist has been enabled.");
                break;
            case "off":
                whitelistEnabled = false;
                try {
                    plugin.setWhitelist(false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Whitelist has been disabled.");
                break;
            case "add":
                if (args.length != 2) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /whitelist add <player>");
                    return;
                }
                String playerToAdd = args[1].toLowerCase();
                whitelistedPlayers.add(playerToAdd);
                saveWhitelist();
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Player " + playerToAdd + " has been added to the whitelist.");
                break;
            case "remove":
                if (args.length != 2) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /whitelist remove <player>");
                    return;
                }
                String playerToRemove = args[1].toLowerCase();
                if (whitelistedPlayers.remove(playerToRemove)) {
                    saveWhitelist();
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Player " + playerToRemove + " has been removed from the whitelist.");
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Player " + playerToRemove + " is not on the whitelist.");
                }
                break;
            case "list":
                if (whitelistedPlayers.isEmpty()) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "No players are whitelisted.");
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Whitelisted players: " + String.join(", ", whitelistedPlayers));
                }
                break;
            default:
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /whitelist <on|off|add|remove|list> [player]");
                break;
        }
    }

    private void kickNonWhitelistedPlayers() {
        if (!whitelistEnabled) {
            return;
        }
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (!whitelistedPlayers.contains(player.getName().toLowerCase())) {
                String kickMessage = plugin.getWhitelistMessageFormat();
                player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', kickMessage)));
            }
        }
    }

    private void loadWhitelist() {
        if (!whitelistFile.exists()) {
            saveWhitelist(); // Ensure file is created if it doesn't exist
            return;
        }
        try (FileReader reader = new FileReader(whitelistFile)) {
            Type setType = new TypeToken<Set<String>>() {}.getType();
            Set<String> loadedWhitelist = gson.fromJson(reader, setType);
            if (loadedWhitelist != null) {
                whitelistedPlayers.clear();
                whitelistedPlayers.addAll(loadedWhitelist);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load whitelist from whitelist.json");
            e.printStackTrace();
        }
    }

    private void saveWhitelist() {
        try (FileWriter writer = new FileWriter(whitelistFile)) {
            gson.toJson(whitelistedPlayers, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save whitelist to whitelist.json");
            e.printStackTrace();
        }
    }

    public boolean isPlayerWhitelisted(String playerName) {
        loadWhitelist();
        return whitelistedPlayers.contains(playerName.toLowerCase());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return args[0].isEmpty() ? Collections.emptyList() : filterTabOptions(args[0], "on", "off", "add", "remove", "list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return filterTabOptions(args[1], whitelistedPlayers.toArray(new String[0]));
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
}
