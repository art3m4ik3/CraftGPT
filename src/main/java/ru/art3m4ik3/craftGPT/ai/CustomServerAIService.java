package ru.art3m4ik3.craftGPT.ai;

import com.google.gson.Gson;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.art3m4ik3.craftGPT.config.Configuration;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CustomServerAIService implements AIService {
    private OkHttpClient client;
    private final Configuration config;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String CUSTOM_SERVER_URL = "https://text.ai.ll-u.ru/";
    private volatile boolean isShutdown = false;

    public CustomServerAIService(Configuration config) {
        this.config = config;
        initializeClient();
    }

    private synchronized void initializeClient() {
        if (client != null) {
            shutdown();
        }

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(64);
        dispatcher.setMaxRequestsPerHost(5);

        this.client = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        isShutdown = false;
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (isShutdown) {
            future.completeExceptionally(new IllegalStateException("AIService has been shut down"));
            return future;
        }

        String serverUrl = config.getCustomServerUrl();
        boolean isCustomServer = CUSTOM_SERVER_URL.equals(serverUrl);

        JSONObject requestBody = isCustomServer ? createCustomServerRequest(prompt) : createOpenAIRequest(prompt);

        if (config.isDebugMode()) {
            System.out.println("Request URL: " + serverUrl);
            System.out.println("Request Body: " + requestBody.toString(2));
        }

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();

        if (!isCustomServer && !config.getApiKey().isEmpty()) {
            request = request.newBuilder()
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .build();
        }

        synchronized (this) {
            if (isShutdown) {
                future.completeExceptionally(new IllegalStateException("AIService has been shut down"));
                return future;
            }

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (isShutdown) {
                        future.completeExceptionally(new IllegalStateException("AIService has been shut down"));
                    } else {
                        future.completeExceptionally(e);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful() || responseBody == null) {
                            String errorBody = responseBody != null ? responseBody.string() : "null";
                            if (config.isDebugMode()) {
                                System.out.println("Error Response: " + errorBody);
                            }
                            future.completeExceptionally(
                                    new IOException("Unexpected response: " + response + ", Body: " + errorBody));
                            return;
                        }

                        String responseData = responseBody.string();
                        if (config.isDebugMode()) {
                            System.out.println("Response: " + responseData);
                        }

                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);

                            if (isCustomServer) {
                                future.complete(jsonResponse.getString("response"));
                            } else {
                                JSONObject choice = jsonResponse.getJSONArray("choices").getJSONObject(0);
                                JSONObject message = choice.getJSONObject("message");
                                future.complete(message.getString("content"));
                            }
                        } catch (Exception e) {
                            future.completeExceptionally(
                                    new IOException("Failed to parse response: " + e.getMessage()));
                        }
                    }
                }
            });
        }

        return future;
    }

    private JSONObject createCustomServerRequest(String prompt) {
        JSONObject json = new JSONObject();
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", config.getSystemPrompt()));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", prompt));
        json.put("messages", messages);
        return json;
    }

    private JSONObject createOpenAIRequest(String prompt) {
        JSONObject json = new JSONObject();
        json.put("model", config.getAiModel());

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", config.getSystemPrompt()));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", prompt));

        json.put("messages", messages);
        json.put("temperature", config.getTemperature());
        json.put("max_tokens", config.getMaxTokens());
        return json;
    }

    @Override
    public synchronized void shutdown() {
        if (isShutdown) {
            return;
        }

        isShutdown = true;
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            client = null;
        }
    }

    public synchronized void reinitialize() {
        initializeClient();
    }
}
