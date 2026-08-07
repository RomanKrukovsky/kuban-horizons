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

/** Фазан: более крупная и выносливая наземная птица с сильным взлётом. */
public final class Pheasant extends AbstractGroundBird {
    public Pheasant(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return KHEntities.PHEASANT.get().create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);
    }

    @Override protected double walkSpeed() { return 1.0D; }
    @Override protected double panicSpeed() { return 1.45D; }
    @Override protected float playerAvoidDistance() { return 8.0F; }
    @Override protected double flockRadius() { return 12.0D; }
    @Override protected int flushDurationTicks() { return 40; }
    @Override protected int flushCooldownTicks() { return 160; }
    @Override protected double flushHorizontalSpeed() { return 0.75D; }
    @Override protected double flushVerticalSpeed() { return 0.58D; }
    @Override protected SoundEvent flushSound() { return KHSounds.PHEASANT_FLUSH.get(); }
    @Override protected SoundEvent getAmbientSound() { return KHSounds.PHEASANT_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return KHSounds.PHEASANT_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return KHSounds.PHEASANT_DEATH.get(); }
    @Override protected int calculateFallDamage(double distance, float multiplier) {
        return super.calculateFallDamage(distance, multiplier * 0.25F);
    }
}
