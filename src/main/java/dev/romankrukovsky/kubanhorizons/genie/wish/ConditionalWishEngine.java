package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedConditionalWish;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.ConditionalWishPreview;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Движок условных и отложенных желаний (Redstone 2.0 / Conditional Wish Engine).
 *
 * <p>Пользовательское правило парсится из свободного текста и сохраняется в
 * персистентное хранилище {@link ConditionalRuleStore}; триггеры оцениваются
 * серверным тиком хранилища, а не жёстко заданными условиями.</p>
 */
public final class ConditionalWishEngine {
    public enum Condition {
        RAINING,
        NIGHT,
        DAY
    }

    public enum Action {
        GROW_STEPPE,
        SOUL_LIGHT,
        GROW_CROPS
    }

    public record Rule(UUID ruleId, UUID ownerId, String condition, String action, boolean enabled) {
    }

    /** Результат создания условного правила из текста. */
    public record RuleResult(boolean created, @Nullable ConditionalRule rule, Component message) {
    }

    private static final int MAX_RULES_PER_OWNER = 16;

    private record TriggerKeyword(ConditionalRule.TriggerType trigger, String... fragments) {
    }

    private static final List<TriggerKeyword> TRIGGER_KEYWORDS = List.of(
            new TriggerKeyword(ConditionalRule.TriggerType.RAIN_START, "дожд", "rain"),
            new TriggerKeyword(ConditionalRule.TriggerType.TIME_NIGHT, "ночь", "ночью", "стемне", "темне", "night"),
            new TriggerKeyword(ConditionalRule.TriggerType.TIME_DAY, "день", "днём", "днем", "рассвет", "day"),
            new TriggerKeyword(ConditionalRule.TriggerType.HEALTH_LOW, "здоров", "health"));

    private static final List<String> CONNECTORS = List.of(
            "наступит", "наступает", "наступила", "наступил", "наступило", "настанет",
            "стемнеет", "стемнело", "темнеет", "будет", "пойдёт", "пойдет", "идет", "идёт",
            "пошёл", "пошел", "начинается", "станет", "начнётся", "начнется",
            "will", "comes", "come", "falls", "fall", "is", "be", "it", "then", "то", "тогда", "затем");

    private static final Map<UUID, List<Rule>> RULES = new ConcurrentHashMap<>();
    private static final Map<UUID, ConditionalWishPreview> PREVIEWS = new ConcurrentHashMap<>();

    private ConditionalWishEngine() {
    }

    public static List<Rule> rules(Level level, UUID ownerId) {
        return new ArrayList<>(RULES.getOrDefault(ownerId, List.of()));
    }

    public static synchronized Rule addRule(Level level, UUID ownerId, Condition condition, Action action) {
        List<Rule> list = RULES.computeIfAbsent(ownerId, k -> new ArrayList<>());
        for (Rule r : list) {
            if (r.condition().equals(condition.name()) && r.action().equals(action.name())) {
                return r;
            }
        }
        Rule rule = new Rule(UUID.randomUUID(), ownerId, condition.name(), action.name(), true);
        list.add(rule);
        return rule;
    }

    public static synchronized boolean removeRule(Level level, UUID ownerId, Condition condition, Action action) {
        List<Rule> list = RULES.get(ownerId);
        if (list == null) {
            return false;
        }
        return list.removeIf(r -> r.condition().equals(condition.name()) && r.action().equals(action.name()));
    }

    public static synchronized boolean undoRule(Level level, UUID ownerId, Condition condition, Action action) {
        return removeRule(level, ownerId, condition, action);
    }

    public static synchronized void clearRules(Level level, UUID ownerId) {
        RULES.remove(ownerId);
    }

    public static ConditionalWishPreview previewRule(UUID actorId, UUID ownerId, Condition condition, Action action) {
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(2));
        String digest = hash(previewId + "|" + actorId + "|" + ownerId + "|" + condition.name() + "|" + action.name());
        ConditionalWishPreview preview = new ConditionalWishPreview(previewId, actorId, ownerId,
                condition.name(), action.name(), expiresAt, digest);
        PREVIEWS.put(previewId, preview);
        return preview;
    }

    public static ConfirmedConditionalWish confirmRule(UUID actorId, ConditionalWishPreview preview) {
        if (!preview.actorId().equals(actorId) || !preview.expiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Stale or unauthorized preview");
        }
        return new ConfirmedConditionalWish(UUID.randomUUID(), preview, Instant.now());
    }

    public static synchronized boolean executeConfirmed(Level level, UUID actorId, ConfirmedConditionalWish confirmed) {
        var preview = confirmed.preview();
        if (!preview.actorId().equals(actorId)) {
            return false;
        }
        PREVIEWS.remove(preview.previewId());
        addRule(level, preview.ownerId(), Condition.valueOf(preview.condition()), Action.valueOf(preview.action()));
        return true;
    }

    /**
     * Создаёт и сохраняет пользовательское условное правило из свободного текста.
     *
     * <p>Примеры: «когда наступит ночь, дай мне алмазы», «when it rains, create a torch».
     * Триггер извлекается по ключевым словам, остаток текста становится действием и
     * хранится формулировкой: на срабатывании она снова проходит через
     * {@link WishParser}, поэтому исполнение всегда соответствует текущему парсеру.</p>
     */
    public static RuleResult createRule(ServerLevel level, Player player, String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        TriggerMatch match = detectTrigger(normalized);
        if (match == null) {
            return new RuleResult(false, null,
                    Component.translatable("screen.kubanhorizons.genie.invalid_wish"));
        }
        String actionText = trimConnectors(normalized.substring(match.cut()));
        if (actionText.isBlank()) {
            return new RuleResult(false, null,
                    Component.translatable("screen.kubanhorizons.genie.invalid_wish"));
        }

        ConditionalRuleStore store = ConditionalRuleStore.get(level);
        List<ConditionalRule> owned = store.forOwner(player.getUUID());
        for (ConditionalRule existing : owned) {
            if (existing.enabled() && existing.trigger() == match.trigger()
                    && existing.actionDescription().equals(actionText)) {
                return new RuleResult(false, existing, Component.translatable(
                        "message.kubanhorizons.genie.conditional.added", existing.id().toString()));
            }
        }
        if (owned.size() >= MAX_RULES_PER_OWNER) {
            return new RuleResult(false, null,
                    Component.translatable("message.kubanhorizons.genie.conditional.limit"));
        }
        ConditionalRule rule = new ConditionalRule(UUID.randomUUID(), player.getUUID(),
                match.trigger(), "", actionText, true, level.getGameTime());
        store.add(rule);
        return new RuleResult(true, rule, Component.translatable(
                "message.kubanhorizons.genie.conditional.added", rule.id().toString()));
    }

    private record TriggerMatch(ConditionalRule.TriggerType trigger, int cut) {
    }

    /** Ищет первый триггер и точку текста, после которой начинается действие. */
    private static @Nullable TriggerMatch detectTrigger(String text) {
        int bestCut = -1;
        ConditionalRule.TriggerType best = null;
        for (TriggerKeyword keyword : TRIGGER_KEYWORDS) {
            for (String fragment : keyword.fragments()) {
                Matcher matcher = Pattern.compile(
                        Pattern.quote(fragment) + "[а-яёa-z]*").matcher(text);
                while (matcher.find()) {
                    if (matcher.end() > bestCut) {
                        bestCut = matcher.end();
                        best = keyword.trigger();
                    }
                }
            }
        }
        return best == null ? null : new TriggerMatch(best, bestCut);
    }

    /** Срезает связки и знаки препинания между триггером и действием. */
    private static String trimConnectors(String raw) {
        String result = raw.stripLeading().replaceAll("^[,;:.\\s]+", "");
        boolean changed;
        do {
            changed = false;
            for (String connector : CONNECTORS) {
                if (result.regionMatches(true, 0, connector, 0, connector.length())) {
                    result = result.substring(connector.length())
                            .stripLeading().replaceAll("^[,;:.\\s]+", "");
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return result;
    }

    private static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
