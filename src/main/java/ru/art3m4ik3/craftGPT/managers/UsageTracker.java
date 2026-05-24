package ru.art3m4ik3.craftGPT.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UsageTracker {
    private final Map<UUID, AtomicInteger> counts = new ConcurrentHashMap<>();
    private final int maxRequestsPerSession;

    public UsageTracker(int maxRequestsPerSession) {
        this.maxRequestsPerSession = maxRequestsPerSession;
    }

    /** Returns false if limit is reached. */
    public boolean recordAndCheck(UUID playerId) {
        AtomicInteger count = counts.computeIfAbsent(playerId, k -> new AtomicInteger(0));
        int newCount = count.incrementAndGet();
        return maxRequestsPerSession <= 0 || newCount <= maxRequestsPerSession;
    }

    public int getCount(UUID playerId) {
        AtomicInteger count = counts.get(playerId);
        return count == null ? 0 : count.get();
    }

    public void reset(UUID playerId) {
        counts.remove(playerId);
    }

    public void resetAll() {
        counts.clear();
    }

    public int getMaxRequests() {
        return maxRequestsPerSession;
    }
}
