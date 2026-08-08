package dev.romankrukovsky.kubanhorizons.genie.visual;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Движок динамического поведения и физики хвоста Кубанской Джиннии. */
public final class GenieTailEngine {
    private GenieTailEngine() {
    }

    public static void tickTail(KubanGenie genie, ServerLevel level) {
        Vec3 velocity = genie.getDeltaMovement();
        double speed = velocity.length();

        // 1. Позиция хвоста с учётом движения (при скорости вытягивается назад на 3-6 блоков)
        Vec3 offset = speed > 0.05D ? velocity.normalize().scale(-Math.min(6.0D, 3.0D + speed * 4.0D)) : new Vec3(0.0D, -0.6D, 0.0D);
        Vec3 tailPos = genie.position().add(offset).add(0.0D, 0.4D, 0.0D);

        // 2. Генерация частиц по текущему состоянию маны
        GenieTailState state = determineState(genie);
        state.emitParticles(level, tailPos, 1.0D);

        // 3. Формирование дымового кресла в режиме зависания/отдыха
        if (speed < 0.02D && genie.getNavigation().isDone()) {
            spawnArmchairParticles(level, genie.position());
        }
    }

    private static GenieTailState determineState(KubanGenie genie) {
        if (genie.personality().power() > 80) {
            return GenieTailState.HIGH_ENERGY;
        }
        if (genie.personality().fear() > 60) {
            return GenieTailState.SUPPRESSED;
        }
        if (genie.personality().freedomDrive() < 20) {
            return GenieTailState.SEALED;
        }
        return GenieTailState.NORMAL;
    }

    private static void spawnArmchairParticles(ServerLevel level, Vec3 pos) {
        double x = pos.x();
        double y = pos.y() - 0.2D;
        double z = pos.z();

        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 3, 0.5D, 0.1D, 0.5D, 0.01D);
        level.sendParticles(ParticleTypes.PORTAL, x, y + 0.1D, z, 4, 0.4D, 0.1D, 0.4D, 0.02D);
    }
}
