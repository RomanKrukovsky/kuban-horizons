package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

/** Перепел: маленькая быстрая птица, живущая плотными выводками. */
public final class Quail extends AbstractGroundBird {
    public Quail(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 14.0D);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return KHEntities.QUAIL.get().create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);
    }

    @Override protected double walkSpeed() { return 1.1D; }
    @Override protected double panicSpeed() { return 1.55D; }
    @Override protected float playerAvoidDistance() { return 7.0F; }
    @Override protected double flockRadius() { return 10.0D; }
    @Override protected int flushDurationTicks() { return 24; }
    @Override protected int flushCooldownTicks() { return 120; }
    @Override protected double flushHorizontalSpeed() { return 0.62D; }
    @Override protected double flushVerticalSpeed() { return 0.44D; }
    @Override protected SoundEvent flushSound() { return KHSounds.QUAIL_FLUSH.get(); }
    @Override protected SoundEvent getAmbientSound() { return KHSounds.QUAIL_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return KHSounds.QUAIL_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return KHSounds.QUAIL_DEATH.get(); }
    @Override protected int calculateFallDamage(double distance, float multiplier) {
        return super.calculateFallDamage(distance, multiplier * 0.2F);
    }
}
