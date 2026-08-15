package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;

/**
 * Зеркало — сосуд школы ILLUSION_ALTERNATE.
 * Открывает иллюзии и карманные измерения.
 */
public class MirrorVesselItem extends VesselItem {

    public MirrorVesselItem(Properties props) {
        super(VesselType.MIRROR, props);
    }
}