package ru.art3m4ik3.craftGPT.managers;

import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManager {
    private final Map<UUID, List<JSONObject>> histories = new ConcurrentHashMap<>();
    private final int maxHistory;

    public ConversationManager(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public List<JSONObject> getHistory(UUID playerId) {
        return histories.getOrDefault(playerId, Collections.emptyList());
    }

    public void addExchange(UUID playerId, String userMessage, String assistantMessage) {
        List<JSONObject> history = histories.computeIfAbsent(playerId, k -> new ArrayList<>());
        history.add(new JSONObject().put("role", "user").put("content", userMessage));
        history.add(new JSONObject().put("role", "assistant").put("content", assistantMessage));

        // Keep only last maxHistory pairs (each pair = 2 messages)
        int maxMessages = maxHistory * 2;
        while (history.size() > maxMessages) {
            history.remove(0);
        }
    }

    public void clearHistory(UUID playerId) {
        histories.remove(playerId);
    }

    public boolean hasHistory(UUID playerId) {
        List<JSONObject> history = histories.get(playerId);
        return history != null && !history.isEmpty();
    }

    public int getHistorySize(UUID playerId) {
        List<JSONObject> history = histories.get(playerId);
        return history == null ? 0 : history.size() / 2;
    }
}
