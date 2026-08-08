package dev.romankrukovsky.kubanhorizons.genie.visual;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Состояния силы магического хвоста Кубанской Джиннии (замена традиционному полотну маны/HUD). */
public enum GenieTailState {
    /** Обычное состояние: мягкий голубоватый дым. */
    NORMAL,
    /** Высокая магическая энергия: фиолетовое свечение с золотыми искрами. */
    HIGH_ENERGY,
    /** Исполнение грандиозного желания: кубанский орнамент внутри хвоста. */
    WISH_ORNAMENT,
    /** Подавление магии: полупрозрачный, тусклый хвост. */
    SUPPRESSED,
    /** Запечатанное состояние: золотые цепи, спирально огибающие хвост. */
    SEALED;

    public void emitParticles(ServerLevel level, Vec3 tailPos, double scaleFactor) {
        double x = tailPos.x();
        double y = tailPos.y();
        double z = tailPos.z();

        switch (this) {
            case NORMAL -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, (int) (4 * scaleFactor), 0.3D, 0.4D, 0.3D, 0.02D);
            }
            case HIGH_ENERGY -> {
                level.sendParticles(ParticleTypes.PORTAL, x, y, z, (int) (8 * scaleFactor), 0.4D, 0.5D, 0.4D, 0.05D);
                level.sendParticles(ParticleTypes.GLOW, x, y, z, (int) (3 * scaleFactor), 0.2D, 0.3D, 0.2D, 0.01D);
            }
            case WISH_ORNAMENT -> {
                level.sendParticles(ParticleTypes.ENCHANT, x, y, z, (int) (12 * scaleFactor), 0.5D, 0.6D, 0.5D, 0.08D);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, (int) (4 * scaleFactor), 0.3D, 0.4D, 0.3D, 0.02D);
            }
            case SUPPRESSED -> {
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, (int) (2 * scaleFactor), 0.2D, 0.3D, 0.2D, 0.01D);
            }
            case SEALED -> {
                level.sendParticles(ParticleTypes.CRIT, x, y, z, (int) (6 * scaleFactor), 0.3D, 0.5D, 0.3D, 0.04D);
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, (int) (2 * scaleFactor), 0.1D, 0.3D, 0.1D, 0.01D);
            }
        }
    }
}
