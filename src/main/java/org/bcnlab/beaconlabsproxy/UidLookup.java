package org.bcnlab.beaconlabsproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.CommandSender;
import org.bcnlab.beaconlabsproxy.Utils.UUIDFetcher;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UidLookup extends Command {

    private final BeaconLabsProxy plugin;
    private static final String PERMISSION = "beaconlabs.uid"; // Permission required to use the command

    public UidLookup(BeaconLabsProxy plugin) {
        super("uidlookup", PERMISSION, "uid");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Usage: /uid <player>"));
            return;
        }

        String playerName = args[0];
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                UUID uuid = UUIDFetcher.getUUID(playerName);

                if (uuid != null) {
                    TextComponent message = new TextComponent(ChatColor.GREEN + "The UUID of " + playerName + " is ");
                    TextComponent uuidComponent = new TextComponent(ChatColor.YELLOW + uuid.toString());
                    uuidComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent("Click to copy UUID")}));
                    uuidComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, uuid.toString()));
                    message.addExtra(uuidComponent);
                    ((ProxiedPlayer) sender).sendMessage(message);
                } else {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "Failed to fetch UUID for player: " + playerName));
                }
            } catch (Exception e) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "An error occurred while fetching the UUID: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }
}
