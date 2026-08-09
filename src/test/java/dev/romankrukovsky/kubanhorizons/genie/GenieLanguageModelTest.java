package dev.romankrukovsky.kubanhorizons.genie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Контракт шлюза к диалоговому разуму без обращения к сети.
 *
 * <p>Живые вызовы облачных провайдеров сюда не входят: тест должен падать
 * из-за ошибки в моде, а не из-за чужого сервера.</p>
 */
class GenieLanguageModelTest {

    @AfterEach
    void resetState() {
        GenieLanguageModel.resetRejectedKeysForTesting();
    }

    @Test
    void euromodelsRequestOmitsReasoningControls() {
        JsonObject body = JsonParser.parseString(
                GenieLanguageModel.euromodelsBody("привет", "trust=10")).getAsJsonObject();

        assertEquals("euromodels/gpt-5.6-sol", body.get("model").getAsString());
        assertEquals(300, body.get("max_tokens").getAsInt());
        assertFalse(body.has("reasoning"),
                "Основной провайдер не понимает управление размышлениями");
    }

    @Test
    void openrouterRequestSuppressesReasoning() {
        JsonObject body = JsonParser.parseString(
                GenieLanguageModel.openrouterBody("привет", "trust=10")).getAsJsonObject();

        assertEquals("nvidia/nemotron-3-super-120b-a12b:free", body.get("model").getAsString());
        // Без подавления размышлений nemotron печатает англоязычный черновик в
        // content и обрывает ответ на середине.
        assertTrue(body.getAsJsonObject("reasoning").get("exclude").getAsBoolean());
        assertTrue(body.get("max_tokens").getAsInt() > 300,
                "Резервной модели нужен запас токенов после отключения размышлений");
    }

    @Test
    void promptCarriesPlayerTextAndContext() {
        JsonObject body = JsonParser.parseString(
                GenieLanguageModel.euromodelsBody("сколько до дома?", "trust=42")).getAsJsonObject();
        var messages = body.getAsJsonArray("messages");

        assertEquals("system", messages.get(0).getAsJsonObject().get("role").getAsString());
        assertTrue(messages.get(0).getAsJsonObject().get("content").getAsString().contains("trust=42"));
        assertEquals("сколько до дома?", messages.get(1).getAsJsonObject().get("content").getAsString());
    }

    @Test
    void readsContentAndIgnoresReasoningField() {
        String response = """
                {"choices":[{"message":{"role":"assistant",
                "content":"Я рядом, хозяин.",
                "reasoning":"We need to answer in Russian..."}}]}
                """;

        assertEquals("Я рядом, хозяин.", GenieLanguageModel.parseOpenrouter(200, response));
    }

    @Test
    void rejectsEmptyAndFailedResponses() {
        String empty = "{\"choices\":[{\"message\":{\"content\":\"   \"}}]}";
        assertThrows(IllegalStateException.class, () -> GenieLanguageModel.parseEuromodels(200, empty));
        assertThrows(IllegalStateException.class, () -> GenieLanguageModel.parseEuromodels(500, "{}"));
        assertThrows(IllegalStateException.class, () -> GenieLanguageModel.parseEuromodels(200, "not json"));
    }

    @Test
    void rejectedPrimaryKeyIsRemembered() {
        assertFalse(GenieLanguageModel.euromodelsRejectedForTesting());

        GenieLanguageModel.markEuromodelsRejectedForTesting();

        // Пометка живёт до перезапуска сервера: отозванный ключ не станет
        // рабочим сам, и спрашивать его каждый раз бессмысленно.
        assertTrue(GenieLanguageModel.euromodelsRejectedForTesting());
    }
}
