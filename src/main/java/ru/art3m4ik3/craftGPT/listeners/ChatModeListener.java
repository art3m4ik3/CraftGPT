package ru.art3m4ik3.craftGPT.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.art3m4ik3.craftGPT.CraftGPT;
import ru.art3m4ik3.craftGPT.commands.CraftGPTCommand;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatModeListener implements Listener {
    private final CraftGPT plugin;
    private CraftGPTCommand command;
    private final Set<UUID> chatModeEnabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ChatModeListener(CraftGPT plugin, CraftGPTCommand command) {
        this.plugin = plugin;
        this.command = command;
    }

    public void updateCommand(CraftGPTCommand command) {
        this.command = command;
    }

    public boolean toggleChatMode(Player player) {
        UUID id = player.getUniqueId();
        if (chatModeEnabled.contains(id)) {
            chatModeEnabled.remove(id);
            return false;
        } else {
            chatModeEnabled.add(id);
            return true;
        }
    }

    public boolean isChatModeEnabled(Player player) {
        return chatModeEnabled.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!chatModeEnabled.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage();
        String lang = plugin.getConfiguration().getDefaultLanguage();

        // Route message to AI command handler on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () ->
                command.handlePlayerPrompt(player, message, lang));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatModeEnabled.remove(event.getPlayer().getUniqueId());
    }
}
