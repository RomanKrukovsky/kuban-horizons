package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;

/**
 * Лампа — сосуд школы WISH_EXECUTION.
 * Исполняет желания через зарегистрированные capability.
 */
public class LampVesselItem extends VesselItem {

    public LampVesselItem(Properties props) {
        super(VesselType.LAMP, props);
    }
}