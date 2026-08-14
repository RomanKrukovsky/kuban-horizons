package dev.romankrukovsky.kubanhorizons.genie.visual;

import com.geckolib.animation.object.DataTicket;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;

/**
 * DataTicket для передачи состояния деформации хвоста джиннии в GeckoLib модель.
 * Используется AnimationController в KubanGenieModel.
 */
public final class GenieTailData {
    private GenieTailData() {}

    /** DataTicket для float-значения sway (отклонение хвоста). */
    public static final DataTicket<Float> TAIL_SWAY =
            DataTicket.create("tail_sway", Float.class);

    /** DataTicket для частоты (скорости) покачивания. */
    public static final DataTicket<Float> TAIL_FREQUENCY =
            DataTicket.create("tail_frequency", Float.class);

    /**
     * Устанавливает sway и frequency в entity data для передачи в модель.
     * Вызывается из VisualTailEngine.applyTailRotation.
     */
    public static void setTailState(KubanGenie genie, float sway, float frequency) {
        genie.getEntityData().set(TAIL_SWAY, sway);
        genie.getEntityData().set(TAIL_FREQUENCY, frequency);
    }
}
