package ru.art3m4ik3.craftGPT.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import ru.art3m4ik3.craftGPT.CraftGPT;

public class Configuration {
    private final CraftGPT plugin;
    private FileConfiguration config;

    public Configuration(CraftGPT plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public String getSystemPrompt() {
        return config.getString("ai.system_prompt", "You are a helpful Minecraft assistant.");
    }

    public String getApiKey() {
        return config.getString("ai.api_key", "");
    }

    public String getCustomServerUrl() {
        return config.getString("ai.custom_server_url", "https://text.ai.ll-u.ru/");
    }

    public int getMaxTokens() {
        return config.getInt("ai.max_tokens", 2048);
    }

    public double getTemperature() {
        return config.getDouble("ai.temperature", 0.7);
    }

    public String getDefaultLanguage() {
        return config.getString("ai.language.default", "ru");
    }

    public String getMessage(String path, String lang, Object... args) {
        String message = config.getString("ai.language.messages." + path + "." + lang,
                config.getString("ai.language.messages." + path + "." + getDefaultLanguage(),
                        "Message not found: " + path));

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }

        message = message
                .replace("{value}", args.length > 0 ? String.valueOf(args[0]) : "")
                .replace("{version}", args.length > 0 ? String.valueOf(args[0]) : "")
                .replace("{message}", args.length > 0 ? String.valueOf(args[0]) : "")
                .replace("{thinking_message}", args.length > 0 ? String.valueOf(args[0]) : "");

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getThinkingMessage(String lang) {
        return getMessage("prompts.thinking", lang);
    }

    public String getInfoPrompt(String type, String value, String lang) {
        return getMessage("prompts.info." + type, lang, value);
    }

    public boolean isDebugMode() {
        return config.getBoolean("debug", false);
    }

    public String getAiModel() {
        return config.getString("ai.model", "gpt-3.5-turbo");
    }
}
