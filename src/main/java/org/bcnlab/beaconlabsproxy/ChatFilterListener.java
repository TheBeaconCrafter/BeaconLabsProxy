package org.bcnlab.beaconlabsproxy;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatFilterListener implements Listener {

    private final BeaconLabsProxy plugin;
    private final Set<UUID> mutedPlayers;

    public ChatFilterListener(BeaconLabsProxy plugin) {
        this.plugin = plugin;
        this.mutedPlayers = new HashSet<>();
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        String message = event.getMessage();

        // Check for bad words
        String flaggedWord = containsBadWord(message);
        if (flaggedWord != null) {
            broadcastBadWordMessage(player, message, flaggedWord);

            // Suggest mute option to team members
            suggestMute(player);
        }
    }

    private String containsBadWord(String message) {
        // Implement your logic to check for bad words here
        // For simplicity, let's assume we have a predefined list
        String[] badWords = {"hrs", "ass", "dick", "fuck", "cock", "suck", "penis", "arsch", "fick", "fck", "huso", "slut", "slt", "dck", "ayri", "bitch", "btch", "nga", "nig", "hure"};

        for (String word : badWords) {
            if (message.toLowerCase().contains(word)) {
                return word;
            }
        }
        return null;
    }

    private void broadcastBadWordMessage(ProxiedPlayer player, String message, String flaggedWord) {
        // Broadcast message to team members with permission
        String prefix = plugin.getPrefix();
        String flaggedPart = ChatColor.GOLD + flaggedWord + ChatColor.RED;
        String playerName = player.getName();

        for (ProxiedPlayer teamMember : plugin.getProxy().getPlayers()) {
            if (teamMember.hasPermission("beaconlabs.notify.badword")) {
                teamMember.sendMessage(new ComponentBuilder(prefix)
                        .color(ChatColor.GRAY)
                        .append("Player ")
                        .append(playerName)
                        .append(" said: ")
                        .append(message.replace(flaggedWord, flaggedPart)) // Highlight flagged word
                        .color(ChatColor.RED)
                        .create());
            }
        }
    }

    private void suggestMute(ProxiedPlayer player) {
        // Create a clickable suggestion in chat to mute the player
        String playerName = player.getName();
        TextComponent suggestMuteText = new TextComponent(ChatColor.RED + "[Auto Mute] " + playerName);
        suggestMuteText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/mute " + playerName + " 7d Chatabuse"));
        suggestMuteText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent(ChatColor.GRAY + "Click to mute")}));

        // Broadcast suggestion to team members with permission
        for (ProxiedPlayer teamMember : plugin.getProxy().getPlayers()) {
            if (teamMember.hasPermission("beaconlabs.mute.suggest")) {
                teamMember.sendMessage(new ComponentBuilder(plugin.getPrefix())
                        .color(ChatColor.RED)
                        .append(suggestMuteText)
                        .create());
            }
        }
    }
}
