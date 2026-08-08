package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.KubanSteppeResonance;
import net.minecraft.server.level.ServerLevel;

/** Движок условных и отложенных желаний (Redstone 2.0 / Conditional Wish Engine). */
public final class ConditionalWishEngine {
    private ConditionalWishEngine() {
    }

    public static void tickConditionalWishes(KubanGenie genie, ServerLevel level) {
        // 1. Условие «Если идет дождь -> форсировать рост растений в степи»
        if (level.isRaining()) {
            KubanSteppeResonance.tickResonance(genie, level);
        }

        // 2. Условие «Если глухая ночь -> зажечь душевный огонь вокруг хозяина»
        if (level.getGameTime() % 24000L > 12000L) {
            var owner = genie.getOwner();
            if (owner != null && owner.distanceToSqr(genie) < 256.0D) {
                if (level.getRandom().nextInt(10) == 0) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            owner.getX(), owner.getY() + 0.1D, owner.getZ(), 4, 0.3D, 0.1D, 0.3D, 0.01D);
                }
            }
        }
    }
}
