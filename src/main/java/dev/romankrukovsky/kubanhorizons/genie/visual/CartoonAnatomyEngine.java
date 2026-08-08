package dev.romankrukovsky.kubanhorizons.genie.visual;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Мультяшно-комедийная анатомия Джиннии (сплющивание в блин, растяжение рук на 5 блоков, выныривание из лампы). */
public final class CartoonAnatomyEngine {
    private CartoonAnatomyEngine() {
    }

    public static void triggerFlatten(KubanGenie genie, ServerLevel level) {
        Vec3 pos = genie.position();
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x(), pos.y() + 0.1D, pos.z(),
                25, 0.8D, 0.05D, 0.8D, 0.02D);
        level.sendParticles(ParticleTypes.WITCH, pos.x(), pos.y() + 0.1D, pos.z(),
                15, 0.6D, 0.05D, 0.6D, 0.01D);
        genie.playHurt();
    }

    public static void stretchArmToItem(KubanGenie genie, ServerLevel level, Vec3 itemPos) {
        Vec3 start = genie.position().add(0.0D, 1.2D, 0.0D);
        Vec3 dir = itemPos.subtract(start);
        double distance = dir.length();
        int steps = Math.min(20, (int) (distance * 4));

        for (int i = 0; i <= steps; i++) {
            double factor = (double) i / steps;
            Vec3 p = start.add(dir.scale(factor));
            level.sendParticles(ParticleTypes.WITCH, p.x(), p.y(), p.z(), 2, 0.05D, 0.05D, 0.05D, 0.0D);
            level.sendParticles(ParticleTypes.ENCHANT, p.x(), p.y(), p.z(), 1, 0.02D, 0.02D, 0.02D, 0.01D);
        }
    }

    public static void emergeFromVessel(KubanGenie genie, ServerLevel level, Vec3 vesselPos) {
        Vec3 geniePos = genie.position();
        level.sendParticles(ParticleTypes.PORTAL, vesselPos.x(), vesselPos.y() + 0.5D, vesselPos.z(),
                40, 0.3D, 0.8D, 0.3D, 0.1D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, geniePos.x(), geniePos.y() + 1.0D, geniePos.z(),
                20, 0.4D, 0.6D, 0.4D, 0.05D);
        genie.playSpawn();
    }
}
