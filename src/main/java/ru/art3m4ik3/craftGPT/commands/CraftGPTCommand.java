package ru.art3m4ik3.craftGPT.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONObject;
import ru.art3m4ik3.craftGPT.CraftGPT;
import ru.art3m4ik3.craftGPT.ai.AIService;
import ru.art3m4ik3.craftGPT.context.PlayerContextBuilder;
import ru.art3m4ik3.craftGPT.managers.ConversationManager;
import ru.art3m4ik3.craftGPT.managers.CooldownManager;
import ru.art3m4ik3.craftGPT.managers.UsageTracker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CraftGPTCommand implements CommandExecutor {
    private final CraftGPT plugin;
    private final AIService aiService;
    private final ConversationManager conversationManager;
    private final CooldownManager cooldownManager;
    private final UsageTracker usageTracker;

    public CraftGPTCommand(CraftGPT plugin, AIService aiService,
                           ConversationManager conversationManager,
                           CooldownManager cooldownManager,
                           UsageTracker usageTracker) {
        this.plugin = plugin;
        this.aiService = aiService;
        this.conversationManager = conversationManager;
        this.cooldownManager = cooldownManager;
        this.usageTracker = usageTracker;
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

            case "clear":
                if (!sender.hasPermission("minegpt.command.prompt")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                if (sender instanceof Player) {
                    conversationManager.clearHistory(((Player) sender).getUniqueId());
                    sender.sendMessage(plugin.getConfiguration().getMessage("general.history_cleared", lang));
                }
                break;

            case "history":
                if (!sender.hasPermission("minegpt.command.prompt")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    int size = conversationManager.getHistorySize(p.getUniqueId());
                    int uses = usageTracker.getCount(p.getUniqueId());
                    sender.sendMessage(plugin.getConfiguration().getMessage("general.history_info", lang, size, uses));
                }
                break;

            case "chat":
                if (!sender.hasPermission("minegpt.command.prompt")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.player_only", lang));
                    return true;
                }
                if (!plugin.getConfiguration().isChatModeEnabled()) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.chat_mode_disabled", lang));
                    return true;
                }
                Player chatPlayer = (Player) sender;
                boolean enabled = plugin.getChatModeListener().toggleChatMode(chatPlayer);
                if (enabled) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("general.chat_mode_on", lang));
                } else {
                    sender.sendMessage(plugin.getConfiguration().getMessage("general.chat_mode_off", lang));
                }
                break;

            case "stats":
                if (!sender.hasPermission("minegpt.command.admin.help")) {
                    sender.sendMessage(plugin.getConfiguration().getMessage("errors.no_permission", lang));
                    return true;
                }
                if (args.length >= 2) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.getConfiguration().getMessage("errors.player_not_found", lang, args[1]));
                        return true;
                    }
                    int uses = usageTracker.getCount(target.getUniqueId());
                    int histSize = conversationManager.getHistorySize(target.getUniqueId());
                    sender.sendMessage(plugin.getConfiguration().getMessage("general.stats", lang, target.getName(), uses, histSize));
                }
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

    /** Public entry point for ChatModeListener. */
    public void handlePlayerPrompt(Player player, String message, String lang) {
        handlePrompt(player, message, lang);
    }

    private void handlePrompt(CommandSender sender, String prompt, String lang) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Cooldown check
            if (cooldownManager.isOnCooldown(player.getUniqueId())) {
                int remaining = cooldownManager.getRemainingCooldown(player.getUniqueId());
                sender.sendMessage(plugin.getConfiguration().getMessage("errors.cooldown", lang, remaining));
                return;
            }

            // Usage limit check
            if (!usageTracker.recordAndCheck(player.getUniqueId())) {
                sender.sendMessage(plugin.getConfiguration().getMessage("errors.usage_limit", lang,
                        usageTracker.getMaxRequests()));
                return;
            }

            cooldownManager.recordUsage(player.getUniqueId());

            // Inject player context if enabled
            String finalPrompt = prompt;
            if (plugin.getConfiguration().isContextInjectionEnabled()) {
                String context = PlayerContextBuilder.build(player, lang);
                finalPrompt = context + "\n\n" + prompt;
            }

            // Get conversation history
            List<JSONObject> history = plugin.getConfiguration().isConversationEnabled()
                    ? conversationManager.getHistory(player.getUniqueId())
                    : Collections.emptyList();

            final String originalPrompt = prompt;
            final String requestPrompt = finalPrompt;
            sender.sendMessage(plugin.getConfiguration().getMessage("prompts.thinking", lang));

            CompletableFuture<String> future = aiService.generateResponse(requestPrompt, history);
            future.thenAccept(response -> {
                // Save to conversation history
                if (plugin.getConfiguration().isConversationEnabled()) {
                    conversationManager.addExchange(player.getUniqueId(), originalPrompt, response);
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(plugin.getConfiguration().getMessage("general.response", lang));
                    for (String line : response.split("\n")) {
                        sender.sendMessage(line);
                    }
                });
            }).exceptionally(throwable -> {
                // Refund usage on error
                usageTracker.reset(player.getUniqueId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(plugin.getConfiguration().getMessage("error", lang, throwable.getMessage()));
                    if (plugin.getConfiguration().isDebugMode()) {
                        throwable.printStackTrace();
                    }
                });
                return null;
            });

        } else {
            // Console / non-player: no cooldown, no context
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
        sender.sendMessage(plugin.getConfiguration().getMessage("help.chat", lang));
        sender.sendMessage(plugin.getConfiguration().getMessage("help.clear", lang));
        sender.sendMessage(plugin.getConfiguration().getMessage("help.history", lang));

        if (sender.hasPermission("minegpt.command.admin.help")) {
            sender.sendMessage(plugin.getConfiguration().getMessage("help.admin_header", lang));
            sender.sendMessage(plugin.getConfiguration().getMessage("help.reload", lang));
            sender.sendMessage(plugin.getConfiguration().getMessage("help.version", lang));
            sender.sendMessage(plugin.getConfiguration().getMessage("help.stats", lang));
        }
    }
}
