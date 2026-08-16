package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.vessel.VesselType;

/**
 * Типы магических сосудов Джиннии (Лампа, Зеркало, Кольцо, Кувшин, Шкатулка).
 *
 * <p>Адаптер к основному {@link VesselType}, который использует реальная
 * система сосудов. Нужен для совместимости с кодом, который обращается к
 * сосудам по старым именам из {@code genie.vessel}.</p>
 */
public enum VesselKind {
    LAMP(VesselType.LAMP),
    MIRROR(VesselType.MIRROR),
    RING(VesselType.RING),
    JUG(VesselType.JUG),
    MUSIC_BOX(VesselType.MUSIC_BOX);

    private final VesselType real;

    VesselKind(VesselType real) {
        this.real = real;
    }

    public VesselType toVesselType() {
        return real;
    }

    public static VesselKind fromVesselType(VesselType type) {
        for (VesselKind kind : values()) {
            if (kind.real == type) {
                return kind;
            }
        }
        return LAMP;
    }
}
