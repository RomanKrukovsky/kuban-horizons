package dev.romankrukovsky.kubanhorizons.genie;

import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Персистентный мозг спутницы: память, приказ и детерминированный выбор
 * наиболее важного действия из снимка ситуации.
 */
public final class GenieBrain {
    private static final int SCHEMA_VERSION = 1;

    private GenieBehaviorMode mode = GenieBehaviorMode.FOLLOW;
    private long rescueReadyAt;
    private int rescues;
    private int threatsRepelled;
    private int projectilesIntercepted;
    private int wishesObserved;
    private GenieDecision lastDecision = GenieDecision.OBSERVE;
    private int lastScore;

    public GenieDecision decide(Situation situation) {
        Plan best = new Plan(GenieDecision.OBSERVE, 1, "observe");
        best = better(best, rescuePlan(situation));
        best = better(best, explosionPlan(situation));
        best = better(best, projectilePlan(situation));
        best = better(best, threatPlan(situation));
        best = better(best, movementPlan(situation));
        lastDecision = best.decision();
        lastScore = best.score();
        return best.decision();
    }

    private Plan rescuePlan(Situation situation) {
        if (!situation.ownerCritical() || situation.gameTime() < rescueReadyAt) {
            return Plan.NONE;
        }
        int score = 1_000 + Math.round((1.0F - situation.healthRatio()) * 300.0F);
        if (situation.ownerFallDistance() >= 8.0D) {
            score += Math.min(250, (int) situation.ownerFallDistance() * 10);
        }
        if (situation.airRatio() < 0.25F) {
            score += 200;
        }
        return new Plan(GenieDecision.RESCUE_OWNER, score, "predicted_lethal_state");
    }

    private static Plan explosionPlan(Situation situation) {
        if (situation.armedCreepers() == 0) {
            return Plan.NONE;
        }
        int proximity = (int) Math.max(0.0D, 200.0D - situation.nearestCreeperDistanceSquared() * 8.0D);
        return new Plan(GenieDecision.PREEMPT_EXPLOSION,
                850 + situation.armedCreepers() * 80 + proximity, "armed_creeper");
    }

    private static Plan projectilePlan(Situation situation) {
        if (situation.incomingProjectiles() == 0) {
            return Plan.NONE;
        }
        int imminence = Math.max(0, 180 - situation.projectileImpactTicks() * 6);
        return new Plan(GenieDecision.INTERCEPT_PROJECTILE,
                700 + situation.incomingProjectiles() * 40 + imminence, "projected_impact");
    }

    private static Plan threatPlan(Situation situation) {
        if (situation.hostileThreats() == 0) {
            return Plan.NONE;
        }
        int proximity = (int) Math.max(0.0D, 160.0D - situation.nearestThreatDistanceSquared() * 3.0D);
        return new Plan(GenieDecision.REPEL_THREAT,
                400 + situation.hostileThreats() * 25 + proximity, "hostile_pressure");
    }

    private Plan movementPlan(Situation situation) {
        if (situation.ownerDistanceSquared() > 256.0D) {
            return new Plan(GenieDecision.RETURN_TO_OWNER, 350, "owner_out_of_range");
        }
        if (mode == GenieBehaviorMode.STAY) {
            return new Plan(GenieDecision.HOLD_POSITION, 100, "ordered_stay");
        }
        if (mode == GenieBehaviorMode.SCOUT && situation.ownerDistanceSquared() < 144.0D) {
            return new Plan(GenieDecision.SCOUT_AREA, 90, "ordered_scout");
        }
        return Plan.NONE;
    }

    private static Plan better(Plan current, Plan candidate) {
        return candidate.score() > current.score() ? candidate : current;
    }

    public void record(GenieDecision decision, long gameTime) {
        switch (decision) {
            case RESCUE_OWNER -> {
                rescues++;
                rescueReadyAt = gameTime + 600;
            }
            case PREEMPT_EXPLOSION, REPEL_THREAT -> threatsRepelled++;
            case INTERCEPT_PROJECTILE -> projectilesIntercepted++;
            case RETURN_TO_OWNER, HOLD_POSITION, SCOUT_AREA, OBSERVE -> {
            }
        }
    }

    public void recordWish() {
        wishesObserved++;
    }

    public GenieBehaviorMode mode() {
        return mode;
    }

    public GenieBehaviorMode cycleMode() {
        mode = mode.next();
        return mode;
    }

    public int rescues() {
        return rescues;
    }

    public int threatsRepelled() {
        return threatsRepelled;
    }

    public int projectilesIntercepted() {
        return projectilesIntercepted;
    }

    public void setMode(GenieBehaviorMode mode) {
        this.mode = mode != null ? mode : GenieBehaviorMode.FOLLOW;
    }

    public int wishesObserved() {
        return wishesObserved;
    }

    public GenieDecision lastDecision() {
        return lastDecision;
    }

    public int lastScore() {
        return lastScore;
    }

    public void save(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putString("Mode", mode.name());
        output.putLong("RescueReadyAt", rescueReadyAt);
        output.putInt("Rescues", rescues);
        output.putInt("ThreatsRepelled", threatsRepelled);
        output.putInt("ProjectilesIntercepted", projectilesIntercepted);
        output.putInt("WishesObserved", wishesObserved);
        output.putString("LastDecision", lastDecision.name());
        output.putInt("LastScore", lastScore);
    }

    public void load(ValueInput input) {
        try {
            mode = GenieBehaviorMode.valueOf(input.getStringOr("Mode", GenieBehaviorMode.FOLLOW.name()));
        } catch (IllegalArgumentException ignored) {
            mode = GenieBehaviorMode.FOLLOW;
        }
        rescueReadyAt = Math.max(0L, input.getLongOr("RescueReadyAt", 0L));
        rescues = readCount(input, "Rescues");
        threatsRepelled = readCount(input, "ThreatsRepelled");
        projectilesIntercepted = readCount(input, "ProjectilesIntercepted");
        wishesObserved = readCount(input, "WishesObserved");
        try {
            lastDecision = GenieDecision.valueOf(input.getStringOr("LastDecision", GenieDecision.OBSERVE.name()));
        } catch (IllegalArgumentException ignored) {
            lastDecision = GenieDecision.OBSERVE;
        }
        lastScore = readCount(input, "LastScore");
    }

    private static int readCount(ValueInput input, String key) {
        return Mth.clamp(input.getIntOr(key, 0), 0, Integer.MAX_VALUE);
    }

    public record Situation(long gameTime, double ownerDistanceSquared, float ownerHealth,
            float ownerMaxHealth, boolean ownerBurning, double ownerFallDistance,
            int ownerAir, int ownerMaxAir, int hostileThreats, double nearestThreatDistanceSquared,
            int incomingProjectiles, int projectileImpactTicks, int armedCreepers,
            double nearestCreeperDistanceSquared) {
        public boolean ownerCritical() {
            return ownerHealth <= Math.max(6.0F, ownerMaxHealth * 0.3F)
                    || ownerBurning && ownerHealth <= ownerMaxHealth * 0.6F
                    || ownerFallDistance >= 8.0D
                    || airRatio() < 0.15F;
        }

        public float healthRatio() {
            return ownerMaxHealth <= 0.0F ? 0.0F : ownerHealth / ownerMaxHealth;
        }

        public float airRatio() {
            return ownerMaxAir <= 0 ? 1.0F : (float) ownerAir / ownerMaxAir;
        }
    }

    public record Plan(GenieDecision decision, int score, String reason) {
        private static final Plan NONE = new Plan(GenieDecision.OBSERVE, 0, "none");
    }
}
