package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;

/**
 * Кольцо — сосуд школы PERSONAL_MAGIC.
 * Усиливает личные способности владельца.
 */
public class RingVesselItem extends VesselItem {

    public RingVesselItem(Properties props) {
        super(VesselType.RING, props);
    }
}