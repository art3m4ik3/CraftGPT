package ru.art3m4ik3.craftGPT;

import org.bukkit.plugin.java.JavaPlugin;
import ru.art3m4ik3.craftGPT.ai.AIService;
import ru.art3m4ik3.craftGPT.ai.CustomServerAIService;
import ru.art3m4ik3.craftGPT.commands.CraftGPTCommand;
import ru.art3m4ik3.craftGPT.config.Configuration;
import ru.art3m4ik3.craftGPT.listeners.ChatModeListener;
import ru.art3m4ik3.craftGPT.managers.ConversationManager;
import ru.art3m4ik3.craftGPT.managers.CooldownManager;
import ru.art3m4ik3.craftGPT.managers.UsageTracker;

public final class CraftGPT extends JavaPlugin {
    private Configuration configuration;
    private AIService aiService;
    private ConversationManager conversationManager;
    private CooldownManager cooldownManager;
    private UsageTracker usageTracker;
    private ChatModeListener chatModeListener;
    private CraftGPTCommand craftGPTCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configuration = new Configuration(this);
        initManagers();

        aiService = new CustomServerAIService(configuration);
        craftGPTCommand = new CraftGPTCommand(this, aiService, conversationManager, cooldownManager, usageTracker);

        getCommand("craftgpt").setExecutor(craftGPTCommand);

        chatModeListener = new ChatModeListener(this, craftGPTCommand);
        getServer().getPluginManager().registerEvents(chatModeListener, this);

        getLogger().info("CraftGPT has been enabled!");
    }

    @Override
    public void onDisable() {
        if (aiService != null) {
            aiService.shutdown();
        }
        getLogger().info("CraftGPT has been disabled!");
    }

    private void initManagers() {
        int cooldown = configuration.isCooldownEnabled() ? configuration.getCooldownSeconds() : 0;
        cooldownManager = new CooldownManager(cooldown);

        int maxHistory = configuration.getMaxConversationHistory();
        conversationManager = new ConversationManager(maxHistory);

        int maxRequests = configuration.getMaxRequestsPerSession();
        usageTracker = new UsageTracker(maxRequests);
    }

    public void reloadPlugin() {
        reloadConfig();
        configuration.loadConfig();
        initManagers();

        if (aiService != null) {
            if (aiService instanceof CustomServerAIService) {
                ((CustomServerAIService) aiService).reinitialize();
            } else {
                aiService.shutdown();
                aiService = new CustomServerAIService(configuration);
            }
        } else {
            aiService = new CustomServerAIService(configuration);
        }

        craftGPTCommand = new CraftGPTCommand(this, aiService, conversationManager, cooldownManager, usageTracker);
        getCommand("craftgpt").setExecutor(craftGPTCommand);

        if (chatModeListener != null) {
            chatModeListener.updateCommand(craftGPTCommand);
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ChatModeListener getChatModeListener() {
        return chatModeListener;
    }
}
