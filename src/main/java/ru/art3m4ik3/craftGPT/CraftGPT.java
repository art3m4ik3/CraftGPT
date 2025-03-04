package ru.art3m4ik3.craftGPT;

import org.bukkit.plugin.java.JavaPlugin;
import ru.art3m4ik3.craftGPT.ai.AIService;
import ru.art3m4ik3.craftGPT.ai.CustomServerAIService;
import ru.art3m4ik3.craftGPT.commands.CraftGPTCommand;
import ru.art3m4ik3.craftGPT.config.Configuration;

public final class CraftGPT extends JavaPlugin {
    private Configuration configuration;
    private AIService aiService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configuration = new Configuration(this);
        aiService = new CustomServerAIService(configuration);

        getCommand("craftgpt").setExecutor(new CraftGPTCommand(this, aiService));

        getLogger().info("CraftGPT has been enabled!");
    }

    @Override
    public void onDisable() {
        if (aiService != null) {
            aiService.shutdown();
        }
        getLogger().info("CraftGPT has been disabled!");
    }

    public void reloadPlugin() {
        reloadConfig();
        configuration.loadConfig();

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
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
