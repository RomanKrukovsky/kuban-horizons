package dev.romankrukovsky.kubanhorizons.genie.spatial;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Смена масштаба (GENIE_VISION §Физика): джинния делает игрока маленьким
 * или гигантским на время. Атрибут {@code SCALE} управляет размером, а
 * {@code STEP_HEIGHT} позволяет маленькому игроку подниматься по ступенькам.
 */
public final class ScaleShiftEngine {

    private static final Identifier SMALL_SCALE = KHIds.of("genie_scale_small");
    private static final Identifier SMALL_STEP = KHIds.of("genie_step_small");
    private static final Identifier GIANT_SCALE = KHIds.of("genie_scale_giant");

    private ScaleShiftEngine() {
    }

    /** true — маленький, false — гигант. Возвращает false, если атрибуты недоступны. */
    public static boolean shift(ServerLevel level, ServerPlayer player, boolean small) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return false;
        }
        reset(scale);
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) {
            reset(step);
        }
        if (small) {
            scale.addTransientModifier(new AttributeModifier(SMALL_SCALE,
                    0.25D, AttributeModifier.Operation.ADD_VALUE));
            if (step != null) {
                step.addTransientModifier(new AttributeModifier(SMALL_STEP,
                        0.6D, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            scale.addTransientModifier(new AttributeModifier(GIANT_SCALE,
                    1.5D, AttributeModifier.Operation.ADD_VALUE));
        }
        MagicalSignature.cast(level, player.position());
        return true;
    }

    /** Сбрасывает все модификаторы масштаба этого движка. */
    private static void reset(AttributeInstance attribute) {
        attribute.removeModifier(SMALL_SCALE);
        attribute.removeModifier(GIANT_SCALE);
        attribute.removeModifier(SMALL_STEP);
    }
}