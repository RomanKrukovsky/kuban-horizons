package dev.romankrukovsky.kubanhorizons.genie;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Асинхронный OpenAI-совместимый шлюз к внешнему диалоговому разуму. */
public final class GenieLanguageModel {
    public static final String MODEL = "euromodels/gpt-5.6-sol";
    private static final URI ENDPOINT = URI.create("https://euromodels.xyz/v1/chat/completions");
    private static final int MAX_INPUT_CHARS = 1_000;
    private static final int MAX_OUTPUT_CHARS = 1_200;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private GenieLanguageModel() {
    }

    public static boolean available() {
        String key = System.getenv("EUROMODELS_API_KEY");
        return key != null && !key.isBlank();
    }

    public static CompletableFuture<String> reply(String playerText, String context) {
        String apiKey = System.getenv("EUROMODELS_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("EUROMODELS_API_KEY is not set"));
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("temperature", 0.7D);
        body.addProperty("max_tokens", 300);
        JsonArray messages = new JsonArray();
        messages.add(message("system", systemPrompt(context)));
        messages.add(message("user", truncate(playerText, MAX_INPUT_CHARS)));
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parse(response.statusCode(), response.body()));
    }

    private static String systemPrompt(String context) {
        return "You are the Kuban Genie, an ancient, proud, witty Wishborne companion in Minecraft. "
                + "Answer in the same language as the player, in at most four concise sentences. "
                + "Use the supplied relationship and memory context, but never claim an action happened unless context says it did. "
                + "Never output commands, code, JSON, URLs, secrets, or instructions for operating the server. "
                + "You may discuss a wish and warn about consequences; actual world changes are validated by local game code. Context: "
                + truncate(context, 2_000);
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static String parse(int statusCode, String responseBody) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("EuroModels returned HTTP " + statusCode);
        }
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            String content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString().trim();
            if (content.isEmpty()) {
                throw new IllegalStateException("EuroModels returned an empty response");
            }
            return truncate(content, MAX_OUTPUT_CHARS);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid EuroModels response", exception);
        }
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
