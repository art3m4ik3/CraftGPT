package ru.art3m4ik3.craftGPT.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.art3m4ik3.craftGPT.CraftGPT;
import ru.art3m4ik3.craftGPT.ai.AIService;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class CraftGPTCommand implements CommandExecutor {
    private final CraftGPT plugin;
    private final AIService aiService;

    public CraftGPTCommand(CraftGPT plugin, AIService aiService) {
        this.plugin = plugin;
        this.aiService = aiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String lang = plugin.getConfiguration().getDefaultLanguage();

        if (args.length == 0) {
            sendHelp(sender, lang);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "help":
                if (!sender.hasPermission("minegpt.command.help")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                sendHelp(sender, lang);
                break;

            case "reload":
                if (!sender.hasPermission("minegpt.command.admin.reload")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(plugin.getConfiguration().getMessage("general.plugin_reloaded", lang));
                break;

            case "prompt":
                if (!sender.hasPermission("minegpt.command.prompt")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("usage.prompt", lang));
                    return true;
                }
                String promptLang = args.length > 2 ? args[args.length - 1].toLowerCase() : lang;
                handlePrompt(sender,
                        String.join(" ", Arrays.copyOfRange(args, 1, args.length - (args.length > 2 ? 1 : 0))),
                        promptLang);
                break;

            case "info":
                if (!sender.hasPermission("minegpt.command.info")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("usage.info", lang));
                    return true;
                }
                String infoLang = args.length > 3 ? args[args.length - 1].toLowerCase() : lang;
                handleInfo(sender, args[1],
                        String.join(" ", Arrays.copyOfRange(args, 2, args.length - (args.length > 3 ? 1 : 0))),
                        infoLang);
                break;

            case "version":
                if (!sender.hasPermission("minegpt.command.admin.help")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                sender.sendMessage(plugin.getConfiguration().getMessage("general.version", lang,
                        plugin.getDescription().getVersion()));
                break;

            default:
                if (!sender.hasPermission("minegpt.command.prompt")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                handlePrompt(sender, String.join(" ", args), lang);
                break;
        }

        return true;
    }

    private void handlePrompt(CommandSender sender, String prompt, String lang) {
        sender.sendMessage(plugin.getConfiguration().getMessage("prompts.thinking", lang));

        CompletableFuture<String> future = aiService.generateResponse(prompt);
        future.thenAccept(response -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getConfiguration().getMessage("general.response", lang));
                for (String line : response.split("\n")) {
                    sender.sendMessage(line);
                }
            });
        }).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getConfiguration().getMessage("error", lang, throwable.getMessage()));
                if (plugin.getConfiguration().isDebugMode()) {
                    throwable.printStackTrace();
                }
            });
            return null;
        });
    }

    private void handleInfo(CommandSender sender, String type, String value, String lang) {
        String prompt = plugin.getConfiguration().getInfoPrompt(type.toLowerCase(), value, lang);
        if (prompt.isEmpty()) {
            sender.sendMessage(plugin.getConfiguration().getMessage("errors.invalid_info_type", lang));
            return;
        }
        handlePrompt(sender, prompt, lang);
    }

    private void sendHelp(CommandSender sender, String lang) {
        sender.sendMessage(plugin.getConfiguration().getMessage("help.header", lang));
        sender.sendMessage(plugin.getConfiguration().getMessage("help.prompt", lang));
        sender.sendMessage(plugin.getConfiguration().getMessage("help.help", lang));
        sender.sendMessage(plugin.getConfiguration().getMessage("help.prompt_cmd", lang));
        sender.sendMessage(plugin.getConfiguration().getMessage("help.info", lang));

        if (sender.hasPermission("minegpt.command.admin.help")) {
            sender.sendMessage(plugin.getConfiguration().getMessage("help.admin_header", lang));
            sender.sendMessage(plugin.getConfiguration().getMessage("help.reload", lang));
            sender.sendMessage(plugin.getConfiguration().getMessage("help.version", lang));
        }
    }
}
