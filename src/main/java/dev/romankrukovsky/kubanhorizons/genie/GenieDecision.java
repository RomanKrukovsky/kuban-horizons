package dev.romankrukovsky.kubanhorizons.genie;

/** Одно приоритетное действие, выбранное мозгом на текущем цикле оценки. */
public enum GenieDecision {
    RESCUE_OWNER,
    PREEMPT_EXPLOSION,
    INTERCEPT_PROJECTILE,
    REPEL_THREAT,
    RETURN_TO_OWNER,
    HOLD_POSITION,
    SCOUT_AREA,
    OBSERVE;

    public String translationKey() {
        return "genie.kubanhorizons.decision." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
