package genie.brain;

import genie.entity.GenieEntity;
import genie.util.GenieMathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Utility-based AI brain for Kuban Genie
 * Implements decision making, threat assessment, and behavior selection
 */
public class GenieBrain {
    private final GenieEntity genie;
    private final Map<GenieDecision, Goal> decisionGoals = new EnumMap<>(GenieDecision.class);
    private GenieBehaviorMode currentMode = GenieBehaviorMode.FOLLOW;
    private GenieDecision currentDecision = GenieDecision.NONE;
    private LivingEntity targetThreat;
    private LivingEntity owner;
    private float threatLevel = 0.0f;
    private long lastDecisionTime = 0;
    private int decisionCooldown = 0;
    private boolean isActive = true;

    // Configuration parameters
    private static final int DECISION_INTERVAL_TICKS = 20;
    private static final float THREAT_DETECTION_RANGE = 16.0f;
    private static final float RESCUE_RANGE = 8.0f;
    private static final float PROJECTILE_INTERCEPT_RANGE = 12.0f;
    private static final float MOVEMENT_SPEED_FACTOR = 1.2f;
    private static final float GUARD_DISTANCE = 5.0f;

    public GenieBrain(GenieEntity genie) {
        this.genie = genie;
        this.owner = genie.getOwner();
    }

    /**
     * Main decision loop called every tick
     */
    public void tick() {
        if (!isActive || genie.level().isClientSide) return;

        // Update owner reference
        this.owner = genie.getOwner();

        // Cooldown for decisions
        if (decisionCooldown > 0) {
            decisionCooldown--;
            return;
        }

        // Make decisions periodically
        if (genie.tickCount % DECISION_INTERVAL_TICKS == 0) {
            makeDecision();
        }

        // Execute current decision
        executeCurrentDecision();
    }

    /**
     * Main decision making function using utility theory
     */
    private void makeDecision() {
        if (owner == null) {
            currentDecision = GenieDecision.NONE;
            return;
        }

        // Calculate utility scores for each decision
        Map<GenieDecision, Float> utilityScores = new EnumMap<>(GenieDecision.class);

        // Rescue owner decision
        float rescueUtility = calculateRescueUtility();
        utilityScores.put(GenieDecision.RESCUE_OWNER, rescueUtility);

        // Intercept projectile decision
        float interceptUtility = calculateInterceptUtility();
        utilityScores.put(GenieDecision.INTERCEPT_PROJECTILE, interceptUtility);

        // Repel threat decision
        float repelUtility = calculateRepelUtility();
        utilityScores.put(GenieDecision.REPEL_THREAT, repelUtility);

        // Movement decisions based on mode
        float followUtility = calculateFollowUtility();
        utilityScores.put(GenieDecision.FOLLOW_OWNER, followUtility);

        float guardUtility = calculateGuardUtility();
        utilityScores.put(GenieDecision.GUARD_POSITION, guardUtility);

        float scoutUtility = calculateScoutUtility();
        utilityScores.put(GenieDecision.SCOUT_AHEAD, scoutUtility);

        // Select decision with highest utility
        GenieDecision bestDecision = GenieDecision.NONE;
        float maxUtility = Float.NEGATIVE_INFINITY;

        for (Map.Entry<GenieDecision, Float> entry : utilityScores.entrySet()) {
            if (entry.getValue() > maxUtility) {
                maxUtility = entry.getValue();
                bestDecision = entry.getKey();
            }
        }

        // Apply mode-based filtering
        bestDecision = filterByMode(bestDecision);

        // Apply minimum utility threshold
        if (maxUtility > 0.1f) {
            currentDecision = bestDecision;
            decisionCooldown = 5; // Short cooldown after decision change
        }
    }

    private float calculateRescueUtility() {
        if (owner == null || !owner.isAlive()) return 0.0f;

        double distance = genie.distanceToSqr(owner);
        float healthRatio = genie.getHealth() / genie.getMaxHealth();

        // Higher utility when owner is in danger or far away
        float dangerFactor = owner.getLastHurtByMobTimestamp() > 0 ? 1.5f : 1.0f;
        float distanceFactor = (float) Math.min(distance / (RESCUE_RANGE * RESCUE_RANGE), 1.0);
        float healthFactor = 1.0f - healthRatio; // More utility when low on health

        return dangerFactor * (1.0f - distanceFactor) * (0.8f + healthFactor * 0.5f);
    }

    private float calculateInterceptUtility() {
        // Check for projectiles in range
        if (genie.getTarget() != null && genie.getTarget() instanceof net.minecraft.world.entity.projectile.Projectile) {
            double distance = genie.distanceToSqr(genie.getTarget());
            if (distance < PROJECTILE_INTERCEPT_RANGE * PROJECTILE_INTERCEPT_RANGE) {
                return 0.9f + (float) (Math.random() * 0.1);
            }
        }
        return 0.0f;
    }

    private float calculateRepelUtility() {
        // Find nearby hostile entities
        LivingEntity closestThreat = findClosestThreat();
        if (closestThreat != null) {
            this.targetThreat = closestThreat;
            double distance = genie.distanceToSqr(closestThreat);
            float threatFactor = getThreatLevel(closestThreat);

            return threatFactor * (1.0f - (float) Math.min(distance / (THREAT_DETECTION_RANGE * THREAT_DETECTION_RANGE), 1.0));
        }
        return 0.0f;
    }

    private float calculateFollowUtility() {
        if (currentMode == GenieBehaviorMode.FOLLOW && owner != null) {
            double distance = genie.distanceToSqr(owner);
            float distanceFactor = (float) Math.min(distance / (GUARD_DISTANCE * GUARD_DISTANCE), 1.0);

            return 0.7f * (1.0f - distanceFactor);
        }
        return 0.0f;
    }

    private float calculateGuardUtility() {
        if (currentMode == GenieBehaviorMode.GUARD && owner != null) {
            double distance = genie.distanceToSqr(owner);

            // Higher utility when close to guard position
            if (distance < GUARD_DISTANCE * GUARD_DISTANCE) {
                return 0.8f + (float) (Math.random() * 0.2);
            }
        }
        return 0.0f;
    }

    private float calculateScoutUtility() {
        if (currentMode == GenieBehaviorMode.SCOUT) {
            return 0.6f + (float) (Math.random() * 0.3);
        }
        return 0.0f;
    }

    private GenieDecision filterByMode(GenieDecision decision) {
        return switch (currentMode) {
            case FOLLOW -> switch (decision) {
                case FOLLOW_OWNER, SCOUT_AHEAD -> decision;
                default -> GenieDecision.FOLLOW_OWNER;
            };
            case STAY -> switch (decision) {
                case GUARD_POSITION, SCOUT_AHEAD -> decision;
                default -> GenieDecision.NONE;
            };
            case GUARD -> switch (decision) {
                case GUARD_POSITION, RESCUE_OWNER -> decision;
                default -> GenieDecision.GUARD_POSITION;
            };
            case SCOUT -> GenieDecision.SCOUT_AHEAD;
        };
    }

    private void executeCurrentDecision() {
        switch (currentDecision) {
            case RESCUE_OWNER:
                executeRescueOwner();
                break;
            case INTERCEPT_PROJECTILE:
                executeInterceptProjectile();
                break;
            case REPEL_THREAT:
                executeRepelThreat();
                break;
            case FOLLOW_OWNER:
                executeFollowOwner();
                break;
            case GUARD_POSITION:
                executeGuardPosition();
                break;
            case SCOUT_AHEAD:
                executeScoutAhead();
                break;
            case NONE:
                // Default behavior - wander near owner
                if (owner != null && genie.random.nextInt(40) == 0) {
                    Vec3 randomPos = LandRandomPos.getPos(genie, 8, 4);
                    if (randomPos != null) {
                        genie.getNavigation().moveTo(randomPos.x, randomPos.y, randomPos.z, 1.0);
                    }
                }
                break;
        }
    }

    private void executeRescueOwner() {
        if (owner == null || !owner.isAlive()) return;

        // Move towards owner
        genie.getNavigation().moveTo(owner, 1.2);

        // If close enough, provide healing aura
        if (genie.distanceToSqr(owner) < 4.0) {
            owner.heal(0.5f);
            genie.heal(0.3f);
        }
    }

    private void executeInterceptProjectile() {
        if (genie.getTarget() instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            // Move to intercept projectile
            genie.getNavigation().moveTo(projectile, 1.5);

            // If close enough, deflect projectile
            if (genie.distanceToSqr(projectile) < 2.0) {
                projectile.discard();
                genie.level().explode(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                    0.5f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
        }
    }

    private void executeRepelThreat() {
        if (targetThreat != null && targetThreat.isAlive()) {
            // Move towards threat
            genie.getNavigation().moveTo(targetThreat, 1.3);

            // Attack if in range
            if (genie.distanceToSqr(targetThreat) < 4.0) {
                genie.doHurtTarget(targetThreat);
            }
        }
    }

    private void executeFollowOwner() {
        if (owner != null) {
            // Follow owner at a distance
            Vec3 followPos = owner.position().add(owner.getLookAngle().scale(-2.0));
            genie.getNavigation().moveTo(followPos.x, followPos.y, followPos.z, 1.1);
        }
    }

    private void executeGuardPosition() {
        if (owner != null) {
            // Stay near owner but don't follow too closely
            Vec3 guardPos = owner.position().add(owner.getLookAngle().scale(-3.0).add(0, 1, 0));
            genie.getNavigation().moveTo(guardPos.x, guardPos.y, guardPos.z, 1.0);
        }
    }

    private void executeScoutAhead() {
        if (owner != null) {
            // Scout ahead of owner
            Vec3 lookVec = owner.getLookAngle().normalize();
            Vec3 scoutPos = owner.position().add(lookVec.scale(5.0));
            genie.getNavigation().moveTo(scoutPos.x, scoutPos.y, scoutPos.z, 1.3);
        }
    }

    /**
     * Find closest hostile entity within detection range
     */
    @Nullable
    private LivingEntity findClosestThreat() {
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : genie.level().getEntitiesOfClass(LivingEntity.class, genie.getBoundingBox().inflate(THREAT_DETECTION_RANGE))) {
            if (isHostile(entity) && entity.isAlive()) {
                double distance = genie.distanceToSqr(entity);
                if (distance < closestDistance) {
                    closest = entity;
                    closestDistance = distance;
                }
            }
        }

        return closest;
    }

    /**
     * Determine threat level of an entity
     */
    private float getThreatLevel(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.monster.Monster) {
            return 1.0f;
        }
        if (entity instanceof net.minecraft.world.entity.monster.piglin.Piglin) {
            return 0.8f;
        }
        if (entity instanceof net.minecraft.world.entity.animal.IronGolem) {
            return 0.5f;
        }
        return 0.3f;
    }

    /**
     * Check if entity is hostile to genie
     */
    private boolean isHostile(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Monster ||
               entity instanceof net.minecraft.world.entity.monster.piglin.Piglin ||
               entity instanceof net.minecraft.world.entity.monster.warden.Warden ||
               entity.getType().getDescription().getString().toLowerCase().contains("hostile");
    }

    /**
     * Set behavior mode
     */
    public void setBehaviorMode(GenieBehaviorMode mode) {
        this.currentMode = mode;
        this.currentDecision = GenieDecision.NONE;
    }

    /**
     * Get current behavior mode
     */
    public GenieBehaviorMode getBehaviorMode() {
        return currentMode;
    }

    /**
     * Get current decision
     */
    public GenieDecision getCurrentDecision() {
        return currentDecision;
    }

    /**
     * Set active state
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Get threat level
     */
    public float getThreatLevel() {
        return threatLevel;
    }

    /**
     * Add to threat level (clamped 0-1)
     */
    public void addThreat(float amount) {
        this.threatLevel = Math.min(1.0f, Math.max(0.0f, this.threatLevel + amount));
    }

    /**
     * Reduce threat level over time
     */
    public void reduceThreatOverTime() {
        this.threatLevel = Math.max(0.0f, this.threatLevel - 0.01f);
    }
}