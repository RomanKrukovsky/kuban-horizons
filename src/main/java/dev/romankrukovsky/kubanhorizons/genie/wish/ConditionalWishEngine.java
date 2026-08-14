package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.KubanSteppeResonance;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedConditionalWish;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.ConditionalWishPreview;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Движок условных и отложенных желаний (Redstone 2.0 / Conditional Wish Engine). */
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

    public static void tickConditionalWishes(KubanGenie genie, ServerLevel level) {
        // 1. Условие «Если идет дождь -> форсировать рост растений в степи»
        if (level.isRaining()) {
            KubanSteppeResonance.tickResonance(genie, level);
        }

        // 2. Условие «Если глухая ночь -> зажечь душевный огонь вокруг хозяина»
        if (level.getGameTime() % 24000L > 12000L) {
            var owner = genie.getOwner();
            if (owner != null && owner.distanceToSqr(genie) < 256.0D) {
                if (level.getRandom().nextInt(10) == 0) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            owner.getX(), owner.getY() + 0.1D, owner.getZ(), 4, 0.3D, 0.1D, 0.3D, 0.01D);
                }
            }
        }
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
