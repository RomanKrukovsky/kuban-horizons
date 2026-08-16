package dev.romankrukovsky.kubanhorizons.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Ошибка Реальности — концептуальная сущность (GENIE_VISION §Закон равновесия).
 *
 * <p>Её нельзя победить уроном: «Концептуальные сущности и парадоксы нельзя
 * победить уроном или удалить обходом валидатора». Вместо здоровья у неё
 * якорение реальности, которое не растёт от ударов — сущность рассеивается
 * только по правилам (закон/договор/знание), как и сама джинния.</p>
 */
public class RealityErrorEntity extends PathfinderMob {

    public RealityErrorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** Неуязвим к урону: парадокс нельзя победить мечом (Закон равновесия). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!level.isClientSide()) {
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0D, getZ(),
                    20, 0.5D, 1.0D, 0.5D, 0.05D);
            level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.0D, getZ(),
                    10, 0.4D, 0.6D, 0.4D, 0.1D);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }
}