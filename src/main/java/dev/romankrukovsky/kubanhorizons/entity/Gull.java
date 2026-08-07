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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Чайка: морская птица побережий и лиманов.
 *
 * <p>Роль в экосистеме — падальщик и примета берега: кружит над водой, подбирает
 * брошенную еду с земли. Она не вредитель посевов (в отличие от саранчи) и не
 * добыча ради мяса — её задача населить побережье и лиманы, где иначе пусто.</p>
 */
public final class Gull extends Animal {
    public static final TagKey<net.minecraft.world.item.Item> FOOD_TAG =
            ItemTags.create(dev.romankrukovsky.kubanhorizons.util.KHIds.of("gull_foods"));

    public Gull(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 15, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(1, new TemptGoal(this, 1.1D, this::isFood, false));
        goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public boolean isFlapping() {
        return !onGround() && tickCount % 4 == 0;
    }

    /** Парящий полёт по ванильной схеме, без собственной физики. */
    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    /** Падальщик: подбирает брошенную еду, поэтому берег выглядит живым. */
    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity item) {
        ItemStack stack = item.getItem();
        if (!isFood(stack)) {
            return;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        }
        playSound(KHSounds.GULL_AMBIENT.get(), 0.6F, 1.0F);
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD_TAG);
    }

    @Override
    protected int calculateFallDamage(double distance, float multiplier) {
        return 0;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return KHEntities.GULL.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.GULL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.GULL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.GULL_HURT.get();
    }
}
