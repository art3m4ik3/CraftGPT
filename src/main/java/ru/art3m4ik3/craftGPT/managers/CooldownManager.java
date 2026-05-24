package ru.art3m4ik3.craftGPT.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private final Map<UUID, Long> lastUsed = new ConcurrentHashMap<>();
    private final int cooldownSeconds;

    public CooldownManager(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    /** Returns remaining cooldown in seconds, or 0 if player can send. */
    public int getRemainingCooldown(UUID playerId) {
        if (cooldownSeconds <= 0) return 0;
        Long last = lastUsed.get(playerId);
        if (last == null) return 0;
        long elapsed = (System.currentTimeMillis() - last) / 1000;
        int remaining = (int) (cooldownSeconds - elapsed);
        return remaining > 0 ? remaining : 0;
    }

    public boolean isOnCooldown(UUID playerId) {
        return getRemainingCooldown(playerId) > 0;
    }

    public void recordUsage(UUID playerId) {
        lastUsed.put(playerId, System.currentTimeMillis());
    }

    public void clearCooldown(UUID playerId) {
        lastUsed.remove(playerId);
    }
}
