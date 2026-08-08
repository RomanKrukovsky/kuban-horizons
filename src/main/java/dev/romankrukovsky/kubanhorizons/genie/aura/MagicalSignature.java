package dev.romankrukovsky.kubanhorizons.genie.aura;

import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Узнаваемая магическая подпись Кубанской Джиннии (сине-фиолетовые руны, кубанский орнамент, щелчок). */
public final class MagicalSignature {
    private MagicalSignature() {
    }

    public static void cast(ServerLevel level, Vec3 pos) {
        cast(level, pos.x(), pos.y(), pos.z());
    }

    public static void cast(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.2D, z, 30, 0.6D, 0.8D, 0.6D, 0.1D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 1.0D, z, 15, 0.4D, 0.6D, 0.4D, 0.03D);
        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.4D, z, 20, 0.5D, 0.5D, 0.5D, 0.05D);
        level.sendParticles(ParticleTypes.WITCH, x, y + 1.0D, z, 10, 0.3D, 0.4D, 0.3D, 0.02D);

        level.playSound(null, BlockPos.containing(x, y, z), KHSounds.GENIE_SNAP.get(),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}
