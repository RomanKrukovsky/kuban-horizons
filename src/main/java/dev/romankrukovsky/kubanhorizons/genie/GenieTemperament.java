package dev.romankrukovsky.kubanhorizons.genie;

/** Выводимый характер джиннии. Он не хранится отдельно от отношений. */
public enum GenieTemperament {
    KIND,
    SARDONIC,
    PROUD,
    CUNNING,
    DANGEROUS,
    GUARDED;

    public String translationKey() {
        return "genie.kubanhorizons.temperament." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
