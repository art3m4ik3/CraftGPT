package ru.art3m4ik3.craftGPT.ai;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AIService {
    CompletableFuture<String> generateResponse(String prompt, List<JSONObject> history);

    default CompletableFuture<String> generateResponse(String prompt) {
        return generateResponse(prompt, Collections.emptyList());
    }

    void shutdown();
}
