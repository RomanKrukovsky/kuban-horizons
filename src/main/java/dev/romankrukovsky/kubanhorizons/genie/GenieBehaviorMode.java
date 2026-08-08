package dev.romankrukovsky.kubanhorizons.genie;

/** Долговременный приказ, внутри которого мозг принимает ситуативные решения. */
public enum GenieBehaviorMode {
    FOLLOW,
    STAY,
    GUARD,
    SCOUT;

    public GenieBehaviorMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return "genie.kubanhorizons.mode." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
