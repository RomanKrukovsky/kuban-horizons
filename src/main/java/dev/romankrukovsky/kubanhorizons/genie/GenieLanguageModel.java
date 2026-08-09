package dev.romankrukovsky.kubanhorizons.genie;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Асинхронный OpenAI-совместимый шлюз к внешнему диалоговому разуму джиннии.
 *
 * <p>Основной провайдер — EuroModels. OpenRouter используется, когда ключа
 * EuroModels нет вовсе или когда он оказался отозванным. Ключи читаются
 * только из окружения процесса, никогда не логируются, не попадают в
 * конфигурацию мира, не отправляются клиенту и не включаются в промпт.</p>
 */
public final class GenieLanguageModel {
    /** Модель основного провайдера; публичная — на неё ссылается документация команд. */
    public static final String MODEL = Provider.EUROMODELS.model;

    private static final int MAX_INPUT_CHARS = 1_000;
    private static final int MAX_OUTPUT_CHARS = 1_200;
    private static final int MAX_CONTEXT_CHARS = 2_000;

    /**
     * Ключ EuroModels отозван.
     *
     * <p>Флаг только в памяти и сбрасывается перезапуском сервера: отзыв
     * ключа — состояние внешней учётной записи, и записывать его в мир
     * значило бы сохранять чужую проблему в сохранении игрока.</p>
     */
    private static final AtomicBoolean EUROMODELS_KEY_REJECTED = new AtomicBoolean(false);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private GenieLanguageModel() {
    }

    /** Провайдер диалогового разума. */
    private enum Provider {
        EUROMODELS("EuroModels", "EUROMODELS_API_KEY",
                "https://euromodels.xyz/v1/chat/completions",
                "euromodels/gpt-5.6-sol", 300, false),
        /**
         * Резервный бесплатный провайдер.
         *
         * <p>nemotron по умолчанию печатает цепочку рассуждений в {@code content},
         * и ответ обрывается на середине англоязычного размышления. Поэтому для
         * него нужны {@code reasoning.exclude} и запас по токенам.</p>
         */
        OPENROUTER("OpenRouter", "OPENROUTER_API_KEY",
                "https://openrouter.ai/api/v1/chat/completions",
                "nvidia/nemotron-3-super-120b-a12b:free", 800, true);

        private final String displayName;
        private final String keyVariable;
        private final URI endpoint;
        private final String model;
        private final int maxTokens;
        private final boolean suppressReasoning;

        Provider(String displayName, String keyVariable, String endpoint, String model,
                int maxTokens, boolean suppressReasoning) {
            this.displayName = displayName;
            this.keyVariable = keyVariable;
            this.endpoint = URI.create(endpoint);
            this.model = model;
            this.maxTokens = maxTokens;
            this.suppressReasoning = suppressReasoning;
        }

        private String key() {
            String key = System.getenv(keyVariable);
            return key == null || key.isBlank() ? null : key;
        }

        private boolean usable() {
            return key() != null && !(this == EUROMODELS && EUROMODELS_KEY_REJECTED.get());
        }
    }

    /** Есть ли хотя бы один пригодный провайдер. */
    public static boolean available() {
        return primary() != null;
    }

    /** Отображаемое имя провайдера, который ответит на следующий запрос. */
    public static String activeProviderName() {
        Provider provider = primary();
        return provider == null ? "none" : provider.displayName;
    }

    /**
     * Запрашивает реплику джиннии.
     *
     * <p>Таймауты и ответы 5xx фолбэк не вызывают: молчание сети — не повод
     * менять личность собеседника. Единственное исключение — 401 и 403 от
     * EuroModels: заданный, но отозванный ключ иначе навсегда заглушил бы
     * джиннию при рабочем резервном ключе рядом.</p>
     */
    public static CompletableFuture<String> reply(String playerText, String context) {
        Provider provider = primary();
        if (provider == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No genie language provider is configured"));
        }
        return send(provider, playerText, context);
    }

    private static CompletableFuture<String> send(Provider provider, String playerText, String context) {
        String key = provider.key();
        if (key == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(provider.displayName + " key disappeared mid-request"));
        }

        HttpRequest request = HttpRequest.newBuilder(provider.endpoint)
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body(provider, playerText, context)))
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> handle(provider, response.statusCode(), response.body(),
                        playerText, context));
    }

    private static CompletableFuture<String> handle(Provider provider, int statusCode, String responseBody,
            String playerText, String context) {
        if (provider == Provider.EUROMODELS && (statusCode == 401 || statusCode == 403)) {
            EUROMODELS_KEY_REJECTED.set(true);
            if (Provider.OPENROUTER.usable()) {
                return send(Provider.OPENROUTER, playerText, context);
            }
        }
        return CompletableFuture.completedFuture(parse(provider, statusCode, responseBody));
    }

    private static Provider primary() {
        for (Provider provider : Provider.values()) {
            if (provider.usable()) {
                return provider;
            }
        }
        return null;
    }

    private static String body(Provider provider, String playerText, String context) {
        JsonObject body = new JsonObject();
        body.addProperty("model", provider.model);
        body.addProperty("temperature", 0.7D);
        body.addProperty("max_tokens", provider.maxTokens);
        if (provider.suppressReasoning) {
            JsonObject reasoning = new JsonObject();
            reasoning.addProperty("exclude", true);
            reasoning.addProperty("effort", "low");
            body.add("reasoning", reasoning);
        }
        JsonArray messages = new JsonArray();
        messages.add(message("system", systemPrompt(context)));
        messages.add(message("user", truncate(playerText, MAX_INPUT_CHARS)));
        body.add("messages", messages);
        return body.toString();
    }

    private static String systemPrompt(String context) {
        return "You are the Kuban Genie, an ancient, proud, witty Wishborne companion in Minecraft. "
                + "Answer in the same language as the player, in at most four concise sentences. "
                + "Never narrate your own reasoning; reply with the answer only. "
                + "Use the supplied relationship and memory context, but never claim an action happened unless context says it did. "
                + "Never output commands, code, JSON, URLs, secrets, or instructions for operating the server. "
                + "You may discuss a wish and warn about consequences; actual world changes are validated by local game code. Context: "
                + truncate(context, MAX_CONTEXT_CHARS);
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static String parse(Provider provider, int statusCode, String responseBody) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException(provider.displayName + " returned HTTP " + statusCode);
        }
        String content;
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            // Читается только content: у резервной модели рядом лежит поле
            // reasoning с англоязычным черновиком, который игрок видеть не должен.
            content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString().trim();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid " + provider.displayName + " response", exception);
        }
        if (content.isEmpty()) {
            throw new IllegalStateException(provider.displayName + " returned an empty response");
        }
        return truncate(content, MAX_OUTPUT_CHARS);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    // --- Точки для тестов ---

    static String euromodelsBody(String playerText, String context) {
        return body(Provider.EUROMODELS, playerText, context);
    }

    static String openrouterBody(String playerText, String context) {
        return body(Provider.OPENROUTER, playerText, context);
    }

    static String parseOpenrouter(int statusCode, String responseBody) {
        return parse(Provider.OPENROUTER, statusCode, responseBody);
    }

    static String parseEuromodels(int statusCode, String responseBody) {
        return parse(Provider.EUROMODELS, statusCode, responseBody);
    }

    static void resetRejectedKeysForTesting() {
        EUROMODELS_KEY_REJECTED.set(false);
    }

    static void markEuromodelsRejectedForTesting() {
        EUROMODELS_KEY_REJECTED.set(true);
    }

    static boolean euromodelsRejectedForTesting() {
        return EUROMODELS_KEY_REJECTED.get();
    }
}
