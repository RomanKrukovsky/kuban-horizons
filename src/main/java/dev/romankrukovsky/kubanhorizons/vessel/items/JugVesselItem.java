package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;

/**
 * Кувшин — сосуд школы CREATURE_CREATION.
 * Порождает и связывает существ.
 */
public class JugVesselItem extends VesselItem {

    public JugVesselItem(Properties props) {
        super(VesselType.JUG, props);
    }
}