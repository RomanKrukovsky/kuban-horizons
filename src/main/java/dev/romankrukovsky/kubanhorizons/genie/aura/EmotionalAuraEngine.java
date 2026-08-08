package dev.romankrukovsky.kubanhorizons.genie.aura;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GenieTemperament;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/** Эмоциональные проявления Джиннии на окружение (частицы, предметы, локальный атмосферный эффект). */
public final class EmotionalAuraEngine {
    private EmotionalAuraEngine() {
    }

    public static void tickAura(KubanGenie genie, ServerLevel level) {
        GenieTemperament temperament = genie.personality().temperament();
        double x = genie.getX();
        double y = genie.getY() + 1.2D;
        double z = genie.getZ();

        switch (temperament) {
            case GUARDED, PROUD -> {
                // Мягкое голубоватое свечение
                if (level.getRandom().nextInt(3) == 0) {
                    level.sendParticles(ParticleTypes.PORTAL, x, y, z, 3, 0.4D, 0.6D, 0.4D, 0.02D);
                }
            }
            case KIND -> {
                // Золотые и фиолетовые искры
                level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 5, 0.5D, 0.5D, 0.5D, 0.05D);
                if (level.getRandom().nextInt(4) == 0) {
                    level.sendParticles(ParticleTypes.GLOW, x, y, z, 2, 0.3D, 0.4D, 0.3D, 0.01D);
                }
            }
            case SARDONIC -> {
                // Тёмный дым и парение предметов
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 4, 0.3D, 0.5D, 0.3D, 0.02D);
                levitateNearbyItems(genie, level);
            }
            case DANGEROUS -> {
                // Искры гнева, потемнение ауры и локальные молнии
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 4, 0.4D, 0.6D, 0.4D, 0.03D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 6, 0.5D, 0.5D, 0.5D, 0.05D);
                levitateNearbyItems(genie, level);
            }
        }
    }

    private static void levitateNearbyItems(KubanGenie genie, ServerLevel level) {
        AABB box = genie.getBoundingBox().inflate(6.0D);
        var items = level.getEntitiesOfClass(ItemEntity.class, box);
        for (ItemEntity item : items) {
            item.setDeltaMovement(item.getDeltaMovement().add(0.0D, 0.03D, 0.0D));
            level.sendParticles(ParticleTypes.WITCH, item.getX(), item.getY() + 0.2D, item.getZ(),
                    2, 0.1D, 0.1D, 0.1D, 0.01D);
        }
    }
}
