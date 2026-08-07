package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Цапля: длинноногая болотная птица плавней.
 *
 * <p>Роль в экосистеме — хищник мелкой рыбы: бродит по отмели и охотится, из-за
 * чего плавни читаются как живая экосистема, а не декорация. Ходит по воде, не
 * плавая: {@code PathType.WATER} проходим, поэтому она держится отмелей.</p>
 */
public final class Heron extends Animal {
    public static final TagKey<net.minecraft.world.item.Item> FOOD_TAG =
            ItemTags.create(dev.romankrukovsky.kubanhorizons.util.KHIds.of("heron_foods"));

    public Heron(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1D, this::isFood, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D, 0.4F));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Охотится на мелкую рыбу — этим плавни и оживают.
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.animal.fish.AbstractFish.class, true));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD_TAG);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return KHEntities.HERON.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.HERON_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.HERON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.HERON_HURT.get();
    }
}
