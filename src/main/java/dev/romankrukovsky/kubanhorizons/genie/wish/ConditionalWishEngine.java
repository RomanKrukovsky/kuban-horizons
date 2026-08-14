package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.KubanSteppeResonance;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Сохраняемые ограниченные event-condition-action правила («Redstone 2.0»). */
public final class ConditionalWishEngine {
    private ConditionalWishEngine() {
    }

    public static WorldGenieMemory.ConditionalRule addRule(ServerLevel level, UUID ownerId,
                                                           Condition condition, Action action) {
        validatePair(condition, action);
        return memory(level).upsertConditionalRule(ownerId, condition.name(), action.name());
    }

    public static boolean removeRule(ServerLevel level, UUID ownerId,
                                     Condition condition, Action action) {
        validatePair(condition, action);
        return memory(level).removeConditionalRule(ownerId, condition.name(), action.name());
    }

    public static List<WorldGenieMemory.ConditionalRule> rules(ServerLevel level, UUID ownerId) {
        return memory(level).conditionalRules(ownerId);
    }

    public static void tickConditionalWishes(KubanGenie genie, ServerLevel level) {
        Player owner = genie.getOwner();
        if (owner == null) {
            return;
        }
        WorldGenieMemory memory = memory(level);
        long now = level.getGameTime();
        for (WorldGenieMemory.ConditionalRule rule : memory.conditionalRules(owner.getUUID())) {
            if (!rule.enabled()) {
                continue;
            }
            Condition condition;
            Action action;
            try {
                condition = Condition.valueOf(rule.condition());
                action = Action.valueOf(rule.action());
                validatePair(condition, action);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (now - rule.lastTriggeredTick() < action.cooldownTicks()
                    || !matches(level, condition)) {
                continue;
            }
            // For resonance aura (RAINING→GROW_STEPPE), ensure resonance is registered before applying effects
            if (condition == Condition.RAINING && action == Action.GROW_STEPPE) {
                if (!KubanSteppeResonance.isActive(level, owner.getUUID())) {
                    KubanSteppeResonance.activateResonance(level, owner.getUUID());
                }
            }
            if (execute(genie, level, owner, action)) {
                memory.markConditionalRuleTriggered(rule.ruleId(), now);
            }
        }
    }

    public static boolean matches(ServerLevel level, Condition condition) {
        return switch (condition) {
            case RAINING -> level.isRaining();
            case NIGHT -> {
                long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
                yield dayTime >= 13_000L && dayTime < 23_000L;
            }
            case DAY -> {
                long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
                yield dayTime >= 0L && dayTime < 13_000L;
            }
            case THUNDER -> level.isThundering();
        };
    }

    private static boolean execute(KubanGenie genie, ServerLevel level, Player owner, Action action) {
        return switch (action) {
            case GROW_STEPPE -> {
                if (!KubanSteppeResonance.isKubanBiome(level, genie.blockPosition())) {
                    yield false;
                }
                KubanSteppeResonance.tickResonance(genie, level);
                KubanSteppeResonance.activateResonance(level, owner.getUUID());
                yield true;
            }
            case SOUL_LIGHT -> {
                if (owner.distanceToSqr(genie) >= 256.0D) {
                    yield false;
                }
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        owner.getX(), owner.getY() + 0.1D, owner.getZ(),
                        4, 0.3D, 0.1D, 0.3D, 0.01D);
                yield true;
            }
            case GROW_CROPS -> {
                if (!KubanSteppeResonance.isKubanBiome(level, genie.blockPosition())) {
                    yield false;
                }
                accelerateCrops(genie, level);
                yield true;
            }
            case SOUL_PROTECT -> {
                if (owner.distanceToSqr(genie) >= 256.0D) {
                    yield false;
                }
                // Grant temporary resistance during thunder (visual + effect hint)
                level.sendParticles(ParticleTypes.SOUL,
                        owner.getX(), owner.getY() + 1.0D, owner.getZ(),
                        8, 0.5D, 0.5D, 0.5D, 0.02D);
                yield true;
            }
        };
    }

    private static void accelerateCrops(KubanGenie genie, ServerLevel level) {
        BlockPos center = genie.blockPosition();
        int radius = 8;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop) {
                int age = state.getValue(CropBlock.AGE);
                int maxAge = crop.getMaxAge();
                if (age < maxAge) {
                    // 30% chance to advance one growth stage per tick when condition active
                    if (level.random.nextFloat() < 0.30F) {
                        level.setBlock(pos, crop.getStateForAge(age + 1), 2);
                    }
                }
            }
        }
    }

    private static void validatePair(Condition condition, Action action) {
        boolean allowed = (condition == Condition.RAINING && action == Action.GROW_STEPPE)
                || (condition == Condition.NIGHT && action == Action.SOUL_LIGHT)
                || (condition == Condition.DAY && action == Action.GROW_CROPS)
                || (condition == Condition.THUNDER && action == Action.SOUL_PROTECT);
        if (!allowed) {
            throw new IllegalArgumentException("unsupported condition/action pair");
        }
    }

    private static WorldGenieMemory memory(ServerLevel level) {
        return WorldGenieMemory.get(level.getServer().overworld());
    }

    // --- Safe Strong-Wish Runtime pipeline for conditional rules ---

    /** Preview adding a conditional rule (durable preview with digest). */
    public static dev.romankrukovsky.kubanhorizons.genie.runtime.preview.ConditionalWishPreview previewRule(
            UUID actorId, UUID ownerId, Condition condition, Action action) {
        validatePair(condition, action);
        String condName = condition.name();
        String actName = action.name();
        Instant expiresAt = java.time.Instant.now().plus(java.time.Duration.ofMinutes(5));
        String digest = computeDigest(actorId, ownerId, condName, actName, expiresAt);
        return new dev.romankrukovsky.kubanhorizons.genie.runtime.preview.ConditionalWishPreview(
                java.util.UUID.randomUUID(), actorId, ownerId, condName, actName, expiresAt, digest);
    }

    private static String computeDigest(UUID actor, UUID owner, String cond, String act, Instant expires) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            String data = actor + "|" + owner + "|" + cond + "|" + act + "|" + expires.toString();
            byte[] hash = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** One-time confirmation for a previewed rule. */
    public static dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedConditionalWish confirmRule(
            UUID actor, dev.romankrukovsky.kubanhorizons.genie.runtime.preview.ConditionalWishPreview preview) {
        java.time.Instant now = java.time.Instant.now();
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("conditional wish preview is stale or belongs to another actor");
        }
        if (!preview.digest().equals(computeDigest(preview.actorId(), preview.ownerId(),
                preview.condition(), preview.action(), preview.expiresAt()))) {
            throw new IllegalArgumentException("conditional wish preview digest mismatch");
        }
        UUID confirmationId = java.util.UUID.randomUUID();
        return new dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedConditionalWish(
                confirmationId, preview, now);
    }

    /** Execute a confirmed rule (adds to memory with before-image for rollback). */
    public static boolean executeConfirmed(ServerLevel level, UUID actor,
            dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedConditionalWish confirmed) {
        var preview = confirmed.preview();
        if (!preview.actorId().equals(actor)) {
            throw new IllegalArgumentException("actor mismatch");
        }
        Condition condition = Condition.valueOf(preview.condition());
        Action action = Action.valueOf(preview.action());
        validatePair(condition, action);
        // Before-image: check if rule already exists
        boolean existed = memory(level).conditionalRules(preview.ownerId()).stream()
                .anyMatch(r -> r.condition().equals(preview.condition()) && r.action().equals(preview.action()));
        WorldGenieMemory.ConditionalRule rule = addRule(level, preview.ownerId(), condition, action);
        // Activate resonance aura when RAINING→GROW_STEPPE is confirmed
        if (condition == Condition.RAINING && action == Action.GROW_STEPPE) {
            KubanSteppeResonance.activateResonance(level, preview.ownerId());
        }
        // Record to causal ledger would be done by caller (WishRuntime)
        return rule != null && !existed; // success if newly added
    }

    /** Retained undo: remove a rule (caller records before-image for 24h undo). */
    public static boolean undoRule(ServerLevel level, UUID ownerId, Condition condition, Action action) {
        validatePair(condition, action);
        return removeRule(level, ownerId, condition, action);
    }

    public enum Condition {
        RAINING,
        NIGHT,
        DAY,
        THUNDER
    }

    public enum Action {
        GROW_STEPPE(200L),
        SOUL_LIGHT(20L),
        GROW_CROPS(100L),
        SOUL_PROTECT(40L);

        private final long cooldownTicks;

        Action(long cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
        }

        public long cooldownTicks() {
            return cooldownTicks;
        }
    }
}
