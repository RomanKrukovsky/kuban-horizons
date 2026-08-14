package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PolicyPreview;
import dev.romankrukovsky.kubanhorizons.genie.wish.LiteralWishEngine;
import dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishExecutor;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishParser;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CGenieResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jspecify.annotations.Nullable;

/** Серверная граница экрана диалога: проверяет владельца, сущность и дистанцию. */
public final class GenieConversationService {
    private static final double MAX_DISTANCE_SQUARED = 32.0D * 32.0D;
    private static final Map<UUID, PolicyPreview> PENDING_POLICIES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> LAST_POLICY_TRANSACTIONS = new ConcurrentHashMap<>();

    private GenieConversationService() {
    }

    public static boolean changeMode(ServerPlayer player, int genieId, GenieBehaviorMode mode) {
        KubanGenie genie = resolveOwned(player, genieId);
        if (genie == null || mode == null) {
            return false;
        }
        genie.setBehaviorMode(mode);
        return true;
    }

    public static @Nullable KubanGenie resolveOwned(ServerPlayer player, int genieId) {
        if (!(player.level().getEntity(genieId) instanceof KubanGenie genie)
                || !genie.isOwnedBy(player)
                || player.distanceToSqr(genie) > MAX_DISTANCE_SQUARED) {
            return null;
        }
        return genie;
    }

    public static @Nullable KubanGenie nearestOwned(ServerPlayer player) {
        return player.level().getEntities(EntityTypeTest.forClass(KubanGenie.class),
                        genie -> genie.isOwnedBy(player)
                                && player.distanceToSqr(genie) <= MAX_DISTANCE_SQUARED)
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    public static Response submitWish(ServerPlayer player, int genieId, String rawText) {
        KubanGenie genie = resolveOwned(player, genieId);
        String text = rawText == null ? "" : rawText.trim();
        if (genie == null) {
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.ai.no_companion"), 0, false);
        }
        if (text.isEmpty() || text.length() > 256) {
            return new Response(Component.translatable(
                    "screen.kubanhorizons.genie.invalid_wish"), 0, false);
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        Response conditional = handleConditionalRule(player, genie, normalized);
        if (conditional != null) {
            return conditional;
        }
        if (containsAny(normalized, "карманная сцена", "пляж на минуту", "pocket scene")) {
            var result = dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService
                    .begin(player);
            return new Response(result.message(), result.success() ? 2 : 0, false);
        }
        if (containsAny(normalized, "открой дворец", "хочу во дворец", "войти во дворец",
                "open the palace", "enter the palace")) {
            if (dev.romankrukovsky.kubanhorizons.genie.vessel.GenieLampItem
                    .findBoundLamp(player, genie.getUUID()) == null) {
                return new Response(Component.translatable(
                        "message.kubanhorizons.genie.lamp.required"), 0, false);
            }
            boolean entered = dev.romankrukovsky.kubanhorizons.genie.vessel.VesselTravelService
                    .enterPalace(player, genie.getUUID());
            return new Response(Component.translatable(entered
                    ? "message.kubanhorizons.genie.vessel.palace_entered"
                    : "message.kubanhorizons.genie.lamp.unavailable"), entered ? 2 : 0, false);
        }
        if (containsAny(normalized, "отмени последнее правило", "отменить последнее правило",
                "undo last rule", "undo the last rule")) {
            return undoLastPolicy(player, genieId);
        }
        GenieBehaviorMode requestedMode = requestedMode(normalized);
        if (requestedMode != null) {
            genie.setBehaviorMode(requestedMode);
            return new Response(Component.translatable("message.kubanhorizons.genie.ai.mode",
                    Component.translatable(requestedMode.translationKey())), 1, false);
        }

        if (normalized.startsWith("буквально:") || normalized.startsWith("literal:")) {
            String wording = text.substring(text.indexOf(':') + 1).trim();
            WishExecutor.Result result = LiteralWishEngine.executeLiteral(player.level(), player, wording);
            recordSuccessfulWish(genie, result);
            return new Response(result.message(100), result.executed() ? 2 : 0, false);
        }

        WishIntent intent = WishParser.parse(text);
        if (intent.understood()) {
            genie.personality().observeWording(intent.polite(), intent.commanding(), intent.precision());
            if (intent.category() == WishIntent.Category.META_RULE) {
                return previewMetaPolicy(player, intent);
            }
            WishExecutor.Result result = WishExecutor.execute(player.level(), player, intent);
            recordSuccessfulWish(genie, result);
            return new Response(result.message(intent.precision()), result.executed() ? 2 : 0, false);
        }

        if (!GenieLanguageModel.available()) {
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.ai.unavailable"), 0, false);
        }
        GenieLanguageModel.reply(text, context(genie)).whenComplete((reply, error) ->
                player.level().getServer().execute(() -> {
                    if (error != null) {
                        KubanHorizons.LOGGER.warn("Genie language provider {} failed: {}",
                                GenieLanguageModel.activeProviderName(), error.getMessage());
                        S2CGenieResponse.send(player, genieId, Component.translatable(
                                "message.kubanhorizons.genie.ai.failed"), 0, false);
                        return;
                    }
                    S2CGenieResponse.send(player, genieId, Component.translatable(
                            "message.kubanhorizons.genie.ai.reply", reply), 1, false);
                }));
        return new Response(Component.translatable("message.kubanhorizons.genie.ai.thinking"),
                0, false);
    }

    public static Response confirmPolicy(ServerPlayer player, int genieId) {
        if (resolveOwned(player, genieId) == null) {
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.ai.no_companion"), 0, false);
        }
        PolicyPreview preview = PENDING_POLICIES.remove(player.getUUID());
        if (preview == null) {
            return new Response(Component.translatable(
                    "screen.kubanhorizons.genie.no_policy_preview"), 0, false);
        }
        try {
            WishRuntime runtime = WishRuntime.get(player.level().getServer());
            var report = runtime.executePolicy(player.getUUID(),
                    runtime.confirmPolicy(player.getUUID(), preview));
            LAST_POLICY_TRANSACTIONS.put(player.getUUID(), report.transactionId());
            return new Response(Component.translatable(
                    "screen.kubanhorizons.genie.policy_applied", report.transactionId()),
                    2, false);
        } catch (IOException | RuntimeException exception) {
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.runtime.failed", exception.getMessage()), 0, false);
        }
    }

    public static void cancelPolicy(ServerPlayer player) {
        PENDING_POLICIES.remove(player.getUUID());
    }

    public static Response undoLastPolicy(ServerPlayer player, int genieId) {
        if (resolveOwned(player, genieId) == null) {
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.ai.no_companion"), 0, false);
        }
        UUID transactionId = LAST_POLICY_TRANSACTIONS.remove(player.getUUID());
        if (transactionId == null) {
            return new Response(Component.translatable(
                    "screen.kubanhorizons.genie.no_policy_to_undo"), 0, false);
        }
        try {
            WishRuntime.get(player.level().getServer()).undoPolicy(player.getUUID(), transactionId);
            return new Response(Component.translatable(
                    "screen.kubanhorizons.genie.policy_undone"), 1, false);
        } catch (IOException | RuntimeException exception) {
            LAST_POLICY_TRANSACTIONS.put(player.getUUID(), transactionId);
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.runtime.failed", exception.getMessage()), 0, false);
        }
    }

    private static Response previewMetaPolicy(ServerPlayer player, WishIntent intent) {
        WishRuntime runtime = WishRuntime.get(player.level().getServer());
        if (!runtime.ready()) {
            runtime.recover();
        }
        PolicyPreview preview = switch (intent.target()) {
            case META_NO_CREEPER_DAMAGE -> runtime.previewMobGriefing(player.getUUID(), false);
            case META_LONGER_NIGHT -> runtime.previewClockRate(player.getUUID(), 0.5F);
            case META_INSTANT_SMELT -> runtime.previewInstantSmelt(player.getUUID(), true);
            default -> null;
        };
        if (preview == null) {
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.wish.unknown", intent.precision()), 0, false);
        }
        PENDING_POLICIES.put(player.getUUID(), preview);
        return new Response(Component.translatable("screen.kubanhorizons.genie.policy_preview",
                preview.ruleId(), preview.beforeValue(), preview.targetValue()), 2, true);
    }

    private static @Nullable GenieBehaviorMode requestedMode(String text) {
        if (text.contains("следуй") || text.contains("иди за мной") || text.contains("follow")) {
            return GenieBehaviorMode.FOLLOW;
        }
        if (text.contains("стой") || text.contains("остановись") || text.contains("stay")) {
            return GenieBehaviorMode.STAY;
        }
        if (text.contains("охраняй") || text.contains("защищай") || text.contains("guard")) {
            return GenieBehaviorMode.GUARD;
        }
        if (text.contains("развед") || text.contains("исследуй") || text.contains("scout")) {
            return GenieBehaviorMode.SCOUT;
        }
        return null;
    }

    private static @Nullable Response handleConditionalRule(ServerPlayer player, KubanGenie genie,
                                                             String text) {
        if (containsAny(text, "список правил", "покажи правила", "list rules", "show rules")) {
            var rules = ConditionalWishEngine.rules(player.level(), player.getUUID());
            var message = Component.translatable(
                    "message.kubanhorizons.genie.conditional.list", rules.size()).copy();
            for (var rule : rules) {
                message.append("\n").append(Component.translatable(
                        "message.kubanhorizons.genie.conditional.rule."
                                + rule.condition().toLowerCase(Locale.ROOT) + "."
                                + rule.action().toLowerCase(Locale.ROOT)));
            }
            return new Response(message, 1, false);
        }

        boolean rainGrowth = containsAny(text, "дожд", "rain")
                && containsAny(text, "раст", "урож", "grow", "crop", "plant");
        boolean nightLight = containsAny(text, "ноч", "night")
                && containsAny(text, "огн", "свет", "flame", "fire", "light");
        if (!rainGrowth && !nightLight) {
            return null;
        }
        boolean remove = containsAny(text, "удали", "удалить", "отключ", "убери",
                "remove", "disable", "delete");
        boolean create = containsAny(text, "если", "когда", "if ", "when ", "whenever");
        if (!remove && !create) {
            return null;
        }

        ConditionalWishEngine.Condition condition = rainGrowth
                ? ConditionalWishEngine.Condition.RAINING
                : ConditionalWishEngine.Condition.NIGHT;
        ConditionalWishEngine.Action action = rainGrowth
                ? ConditionalWishEngine.Action.GROW_STEPPE
                : ConditionalWishEngine.Action.SOUL_LIGHT;
        if (remove) {
            boolean removed = ConditionalWishEngine.removeRule(
                    player.level(), player.getUUID(), condition, action);
            return new Response(Component.translatable(removed
                    ? "message.kubanhorizons.genie.conditional.removed"
                    : "message.kubanhorizons.genie.conditional.not_found"), removed ? 1 : 0, false);
        }

        try {
            var rule = ConditionalWishEngine.addRule(
                    player.level(), player.getUUID(), condition, action);
            genie.playWish();
            genie.brain().recordWish();
            WorldGenieMemory.get(player.level().getServer().overworld())
                    .recordWish(player.blockPosition(), "conditional:" + condition + ":" + action,
                            100, player.level().getGameTime());
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.conditional.added", rule.ruleId()), 2, false);
        } catch (IllegalStateException exception) {
            return new Response(Component.translatable(
                    "message.kubanhorizons.genie.conditional.limit"), 0, false);
        }
    }

    private static boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static void recordSuccessfulWish(KubanGenie genie, WishExecutor.Result result) {
        if (result.executed()) {
            genie.playWish();
            genie.brain().recordWish();
        }
    }

    private static String context(KubanGenie genie) {
        GeniePersonality personality = genie.personality();
        GenieBrain brain = genie.brain();
        return "temperament=" + personality.temperament()
                + ", trust=" + personality.trust()
                + ", respect=" + personality.respect()
                + ", fear=" + personality.fear()
                + ", affection=" + personality.affection()
                + ", freedomDrive=" + personality.freedomDrive()
                + ", corruption=" + personality.corruption()
                + ", mode=" + brain.mode()
                + ", rescues=" + brain.rescues()
                + ", threatsRepelled=" + brain.threatsRepelled()
                + ", projectilesIntercepted=" + brain.projectilesIntercepted()
                + ", wishesObserved=" + brain.wishesObserved()
                + ", lastDecision=" + brain.lastDecision();
    }

    public record Response(Component message, int emotionLevel, boolean confirmationRequired) {
    }
}
