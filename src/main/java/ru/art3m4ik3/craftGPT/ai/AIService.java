package ru.art3m4ik3.craftGPT.ai;

import java.util.concurrent.CompletableFuture;

public interface AIService {
    CompletableFuture<String> generateResponse(String prompt);

    void shutdown();
}
