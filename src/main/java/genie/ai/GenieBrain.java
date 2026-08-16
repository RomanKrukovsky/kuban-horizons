package genie.ai;

import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility-based AI planner for Kuban Genie.
 * Makes decisions based on utility scoring of possible actions.
 */
public class GenieBrain {

    private final KubanGenie genie;
    private GenieBehaviorMode currentMode = GenieBehaviorMode.FOLLOW;
    private long lastDecisionTime = 0;
    private static final long DECISION_INTERVAL = 20; // ticks

    public GenieBrain(KubanGenie genie) {
        this.genie = genie;
    }

    /**
     * Make a decision based on current situation
     */
    public GenieDecision decide() {
        long currentTime = genie.level().getGameTime();
        if (currentTime - lastDecisionTime < DECISION_INTERVAL) {
            return GenieDecision.WAIT;
        }
        lastDecisionTime = currentTime;

        // Get relevant entities
        Player owner = genie.getOwner();
        List<LivingEntity> threats = findThreats();
        List<LivingEntity> projectiles = findProjectiles();
        List<LivingEntity> allies = findAllies();

        // Score each possible decision
        DecisionScores scores = scoreDecisions(owner, threats, projectiles, allies);

        // Choose highest scoring decision
        return scores.getBestDecision();
    }

    /**
     * Find nearby threats
     */
    private List<LivingEntity> findThreats() {
        List<LivingEntity> threats = new ArrayList<>();
        BlockPos center = genie.blockPosition();
        int radius = 16;

        for (Entity entity : genie.level().getEntities().getAll()) {
            if (entity instanceof LivingEntity living && entity != genie) {
                double distance = entity.distanceToSqr(genie);
                if (distance < radius * radius && isHostile(living)) {
                    threats.add(living);
                }
            }
        }

        return threats;
    }

    /**
     * Find incoming projectiles
     */
    private List<LivingEntity> findProjectiles() {
        List<LivingEntity> projectiles = new ArrayList<>();
        BlockPos center = genie.blockPosition();
        int radius = 12;

        for (Entity entity : genie.level().getEntities().getAll()) {
            if (entity instanceof net.minecraft.world.entity.projectile.AbstractArrow ||
                entity instanceof net.minecraft.world.entity.projectile.ThrownTrident) {
                double distance = entity.distanceToSqr(genie);
                if (distance < radius * radius) {
                    projectiles.add((LivingEntity) entity);
                }
            }
        }

        return projectiles;
    }

    /**
     * Find allies (including owner)
     */
    private List<LivingEntity> findAllies() {
        List<LivingEntity> allies = new ArrayList<>();
        BlockPos center = genie.blockPosition();
        int radius = 24;

        Player owner = genie.getOwner();
        if (owner != null) {
            allies.add(owner);
        }

        for (Entity entity : genie.level().getEntities().getAll()) {
            if (entity instanceof LivingEntity living && entity != genie && entity != owner) {
                double distance = entity.distanceToSqr(genie);
                if (distance < radius * radius && !isHostile(living)) {
                    allies.add(living);
                }
            }
        }

        return allies;
    }

    /**
     * Check if entity is hostile
     */
    private boolean isHostile(LivingEntity entity) {
        // Simple hostility check - would be more sophisticated in real implementation
        return entity instanceof net.minecraft.world.entity.monster.Monster ||
               entity.getType().getDescription().getString().contains("hostile");
    }

    /**
     * Score all possible decisions
     */
    private DecisionScores scoreDecisions(@Nullable Player owner, List<LivingEntity> threats,
                                          List<LivingEntity> projectiles, List<LivingEntity> allies) {
        DecisionScores scores = new DecisionScores();

        // Score RESCUE_OWNER
        if (owner != null && owner.getHealth() < owner.getMaxHealth() * 0.5f) {
            scores.scoreDecision(GenieDecision.RESCUE_OWNER, 90);
        }

        // Score INTERCEPT_PROJECTILE
        if (!projectiles.isEmpty()) {
            scores.scoreDecision(GenieDecision.INTERCEPT_PROJECTILE, 85);
        }

        // Score REPEL_THREAT
        if (!threats.isEmpty()) {
            scores.scoreDecision(GenieDecision.REPEL_THREAT, 80);
        }

        // Score FOLLOW_OWNER
        if (owner != null && currentMode == GenieBehaviorMode.FOLLOW) {
            scores.scoreDecision(GenieDecision.FOLLOW_OWNER, 70);
        }

        // Score GUARD_POSITION
        if (currentMode == GenieBehaviorMode.GUARD) {
            scores.scoreDecision(GenieDecision.GUARD_POSITION, 75);
        }

        // Score SCOUT_AHEAD
        if (currentMode == GenieBehaviorMode.SCOUT) {
            scores.scoreDecision(GenieDecision.SCOUT_AHEAD, 65);
        }

        // Score WAIT
        scores.scoreDecision(GenieDecision.WAIT, 30);

        return scores;
    }

    /**
     * Set behavior mode
     */
    public void setBehaviorMode(GenieBehaviorMode mode) {
        this.currentMode = mode;
    }

    /**
     * Get current behavior mode
     */
    public GenieBehaviorMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Decision scoring container
     */
    private static class DecisionScores {
        private final java.util.Map<GenieDecision, Integer> scores = new java.util.HashMap<>();

        void scoreDecision(GenieDecision decision, int score) {
            scores.merge(decision, score, Math::max);
        }

        GenieDecision getBestDecision() {
            return scores.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(GenieDecision.WAIT);
        }
    }
}
