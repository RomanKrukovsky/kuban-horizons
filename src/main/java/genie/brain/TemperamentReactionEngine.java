package genie.brain;

import genie.entity.GenieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Engine for handling genie reactions based on temperament and events
 * Controls emotional responses, behavior changes, and personality-based decisions
 */
public class TemperamentReactionEngine {
    private final GenieEntity genie;
    private final Random random;
    private Temperament temperament;
    private float currentMood = 0.5f; // 0.0 (angry) to 1.0 (happy)
    private long lastReactionTime = 0;
    private final Map<String, ReactionHistory> reactionHistory = new HashMap<>();

    // Temperament types with their base moods and reactions
    public enum Temperament {
        CALM("calm", 0.7f, 0.2f, "The genie remains serene and collected."),
        EXCITED("excited", 0.9f, 0.4f, "The genie is full of energy and enthusiasm!"),
        GRUMPY("grumpy", 0.3f, -0.3f, "The genie is in a bad mood and grumbles."),
        PLAYFUL("playful", 0.8f, 0.5f, "The genie is playful and mischievous."),
        SERIOUS("serious", 0.5f, 0.1f, "The genie is focused and determined.");

        private final String name;
        private final float baseMood;
        private final float moodChangeRate;
        private final String description;

        Temperament(String name, float baseMood, float moodChangeRate, String description) {
            this.name = name;
            this.baseMood = baseMood;
            this.moodChangeRate = moodChangeRate;
            this.description = description;
        }

        public String getName() { return name; }
        public float getBaseMood() { return baseMood; }
        public float getMoodChangeRate() { return moodChangeRate; }
        public String getDescription() { return description; }
    }

    // Reaction types
    public enum ReactionType {
        OWNER_HURT,
        PROJECTILE_NEAR,
        THREAT_DETECTED,
        OWNER_COMMAND,
        BLOCK_INTERACT,
        FLUID_ENTER,
        DAMAGE_TAKEN,
        SUCCESS_ACTION,
        FAILURE_ACTION,
        MUSIC_PLAY,
        PARTY_EVENT
    }

    // Reaction responses
    private static class ReactionResponse {
        final float moodChange;
        final GenieBehaviorMode modeChange;
        final String animation;
        final boolean defensive;
        final String message;

        ReactionResponse(float moodChange, GenieBehaviorMode modeChange, String animation,
                        boolean defensive, String message) {
            this.moodChange = moodChange;
            this.modeChange = modeChange;
            this.animation = animation;
            this.defensive = defensive;
            this.message = message;
        }
    }

    // Reaction mappings
    private final Map<ReactionType, ReactionResponse> reactionResponses = new HashMap<>();

    public TemperamentReactionEngine(GenieEntity genie) {
        this.genie = genie;
        this.random = new Random(genie.getUUID().getMostSignificantBits());
        this.temperament = Temperament.CALM; // Default temperament
        initializeReactionMappings();
    }

    private void initializeReactionMappings() {
        // Owner hurt reactions
        reactionResponses.put(ReactionType.OWNER_HURT, new ReactionResponse(
            -0.3f, GenieBehaviorMode.FOLLOW, "rescue", true,
            "Owner is in danger! Protecting you!"
        ));

        // Projectile near reactions
        reactionResponses.put(ReactionType.PROJECTILE_NEAR, new ReactionResponse(
            0.1f, GenieBehaviorMode.GUARD, "alert", true,
            "Incoming projectile detected!"
        ));

        // Threat detected reactions
        reactionResponses.put(ReactionType.THREAT_DETECTED, new ReactionResponse(
            -0.2f, GenieBehaviorMode.GUARD, "alert", true,
            "Threat detected! Standing guard!"
        ));

        // Owner command reactions
        reactionResponses.put(ReactionType.OWNER_COMMAND, new ReactionResponse(
            0.15f, GenieBehaviorMode.FOLLOW, "happy", false,
            "Yes, master! Following your command!"
        ));

        // Block interact reactions
        reactionResponses.put(ReactionType.BLOCK_INTERACT, new ReactionResponse(
            0.05f, null, "curious", false,
            "Interesting block!"
        ));

        // Fluid enter reactions
        reactionResponses.put(ReactionType.FLUID_ENTER, new ReactionResponse(
            -0.1f, null, "disgust", false,
            "Ugh, wet!"
        ));

        // Damage taken reactions
        reactionResponses.put(ReactionType.DAMAGE_TAKEN, new ReactionResponse(
            -0.25f, GenieBehaviorMode.GUARD, "pain", true,
            "Ouch! That hurt!"
        ));

        // Success action reactions
        reactionResponses.put(ReactionType.SUCCESS_ACTION, new ReactionResponse(
            0.2f, null, "happy", false,
            "Yes! Mission accomplished!"
        ));

        // Failure action reactions
        reactionResponses.put(ReactionType.FAILURE_ACTION, new ReactionResponse(
            -0.15f, null, "sad", false,
            "Hmm, that didn't go as planned..."
        ));

        // Music play reactions
        reactionResponses.put(ReactionType.MUSIC_PLAY, new ReactionResponse(
            0.3f, GenieBehaviorMode.SCOUT, "dance", false,
            "Music makes me happy!"
        ));

        // Party event reactions
        reactionResponses.put(ReactionType.PARTY_EVENT, new ReactionResponse(
            0.4f, GenieBehaviorMode.SCOUT, "celebrate", false,
            "Party time! Let's celebrate!"
        ));
    }

    /**
     * Tick method called every game tick
     */
    public void tick() {
        if (genie.level().isClientSide) return;

        // Gradually return to base mood
        float baseMood = temperament.getBaseMood();
        if (currentMood < baseMood) {
            currentMood = Math.min(baseMood, currentMood + 0.001f);
        } else if (currentMood > baseMood) {
            currentMood = Math.max(baseMood, currentMood - 0.001f);
        }

        // React to environment periodically
        if (genie.tickCount % 40 == 0) {
            reactToEnvironment();
        }

        // Update last reaction time
        if (lastReactionTime > 0 && genie.tickCount - lastReactionTime > 200) {
            // Reset reaction history after timeout
            reactionHistory.clear();
        }
    }

    /**
     * React to the current environment and events
     */
    public void reactToEnvironment() {
        // Check for nearby events
        checkForOwnerEvents();
        checkForThreats();
        checkForOwnerCommands();
        checkForMusic();
    }

    private void checkForOwnerEvents() {
        LivingEntity owner = genie.getOwner();
        if (owner == null) return;

        // React to owner being hurt
        if (owner.getLastHurtByMobTimestamp() > 0 && owner.hurtTime > 0) {
            triggerReaction(ReactionType.OWNER_HURT, owner);
            return;
        }

        // React to owner's health
        float healthRatio = owner.getHealth() / owner.getMaxHealth();
        if (healthRatio < 0.3f) {
            triggerReaction(ReactionType.OWNER_HURT, owner);
        }
    }

    private void checkForThreats() {
        // Check for nearby projectiles
        for (Entity entity : genie.level().getEntities()) {
            if (entity instanceof net.minecraft.world.entity.projectile.Projectile &&
                entity.distanceTo(genie) < 8.0f) {
                triggerReaction(ReactionType.PROJECTILE_NEAR, entity);
                return;
            }
        }

        // Check for hostile mobs
        for (LivingEntity entity : genie.level().getEntitiesOfClass(LivingEntity.class,
                genie.getBoundingBox().inflate(12.0))) {
            if (isHostile(entity) && entity.distanceTo(genie) < 10.0f) {
                triggerReaction(ReactionType.THREAT_DETECTED, entity);
                return;
            }
        }
    }

    private void checkForOwnerCommands() {
        if (genie.getOwner() instanceof Player player) {
            // Check if player has interacted recently
            if (player.getLastActionTime() > genie.getLastActionTime() + 5) {
                triggerReaction(ReactionType.OWNER_COMMAND, player);
            }
        }
    }

    private void checkForMusic() {
        // Check if music disc is playing nearby
        for (Entity entity : genie.level().getEntities()) {
            if (entity instanceof net.minecraft.world.item.ItemEntity itemEntity) {
                if (itemEntity.getItem().is(net.minecraft.world.item.Items.MUSIC_DISC_CREATOR)) {
                    triggerReaction(ReactionType.MUSIC_PLAY, entity);
                    return;
                }
            }
        }
    }

    /**
     * Trigger a reaction based on event type
     */
    public void triggerReaction(ReactionType type, @Nullable Entity source) {
        if (genie.level().isClientSide) return;

        ReactionResponse response = reactionResponses.get(type);
        if (response == null) return;

        // Record reaction in history
        String key = type.name() + (source != null ? "_" + source.getType().getDescription().getString() : "");
        reactionHistory.put(key, new ReactionHistory(genie.tickCount, response));
        lastReactionTime = genie.tickCount;

        // Apply mood change
        currentMood = Math.max(0.0f, Math.min(1.0f, currentMood + response.moodChange));

        // Apply behavior mode change if specified
        if (response.modeChange != null) {
            genie.setBehaviorMode(response.modeChange);
        }

        // Send message to owner
        if (response.message != null && genie.getOwner() instanceof Player player) {
            genie.sendMessageToOwner(response.message);
        }

        // Log reaction for debugging
        if (genie.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_COMMAND_BLOCK_OUTPUT)) {
            System.out.println("Genie " + genie.getName().getString() + " reacted to " + type +
                             " with mood change: " + response.moodChange);
        }
    }

    /**
     * Get current mood (0.0-1.0)
     */
    public float getCurrentMood() {
        return currentMood;
    }

    /**
     * Get temperament
     */
    public Temperament getTemperament() {
        return temperament;
    }

    /**
     * Set temperament
     */
    public void setTemperament(Temperament temperament) {
        this.temperament = temperament;
        // Adjust mood toward new temperament's base
        currentMood = (currentMood + temperament.getBaseMood()) / 2.0f;
    }

    /**
     * Get mood as percentage string
     */
    public String getMoodDescription() {
        float mood = getCurrentMood();
        if (mood > 0.8f) return "Very Happy 😊";
        if (mood > 0.6f) return "Happy 🙂";
        if (mood > 0.4f) return "Neutral 😐";
        if (mood > 0.2f) return "Unhappy 😕";
        return "Angry 😠";
    }

    /**
     * Check if genie is in a good mood
     */
    public boolean isHappy() {
        return currentMood > 0.6f;
    }

    /**
     * Check if genie is angry
     */
    public boolean isAngry() {
        return currentMood < 0.3f;
    }

    /**
     * Get temperament description
     */
    public String getTemperamentDescription() {
        return temperament.getDescription();
    }

    /**
     * React to block interaction
     */
    public void reactToBlock(BlockPos pos, BlockState state) {
        triggerReaction(ReactionType.BLOCK_INTERACT, null);
    }

    /**
     * React to fluid interaction
     */
    public void reactToFluid(FluidState fluid) {
        triggerReaction(ReactionType.FLUID_ENTER, null);
    }

    /**
     * React to damage taken
     */
    public void reactToDamage(float amount) {
        triggerReaction(ReactionType.DAMAGE_TAKEN, null);
    }

    /**
     * React to successful action
     */
    public void reactToSuccess() {
        triggerReaction(ReactionType.SUCCESS_ACTION, null);
    }

    /**
     * React to failed action
     */
    public void reactToFailure() {
        triggerReaction(ReactionType.FAILURE_ACTION, null);
    }

    /**
     * React to party event
     */
    public void reactToParty() {
        triggerReaction(ReactionType.PARTY_EVENT, null);
    }

    /**
     * Check if entity is hostile
     */
    private boolean isHostile(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Monster ||
               entity instanceof net.minecraft.world.entity.monster.piglin.Piglin ||
               entity instanceof net.minecraft.world.entity.monster.warden.Warden;
    }

    /**
     * Reaction history entry
     */
    private static class ReactionHistory {
        final long timestamp;
        final ReactionResponse response;

        ReactionHistory(long timestamp, ReactionResponse response) {
            this.timestamp = timestamp;
            this.response = response;
        }
    }
}