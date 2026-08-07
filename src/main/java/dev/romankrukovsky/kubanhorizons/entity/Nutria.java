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
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Нутрия: полуводный грызун плавней и лиманов.
 *
 * <p>Роль в экосистеме — вредитель оросительной сети. Нутрия подгрызает
 * деревянные желоба ({@link GnawChannelGoal}), из-за чего сеть рвётся и
 * перестаёт увлажнять грядки. Каменный желоб ей не по зубам — так у игрока
 * появляется путь апгрейда вместо безответного налога.</p>
 *
 * <p>Плавает уверенно: {@link WaterBoundPathNavigation} и амфибийный
 * move control, поэтому вода не мешает ей добраться до сети у берега.</p>
 */
public final class Nutria extends Animal {
    public static final TagKey<net.minecraft.world.item.Item> FOOD_TAG =
            ItemTags.create(dev.romankrukovsky.kubanhorizons.util.KHIds.of("nutria_foods"));

    public Nutria(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.15F, 0.6F, false);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 14.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PanicGoal(this, 1.4D));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.0D, 1.4D));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1D, this::isFood, false));
        goalSelector.addGoal(4, new GnawChannelGoal(this, 1.1D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D, 0.6F));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return false;
    }

    /** Нутрия не тонет: в воде она в своей среде и не паникует от неё. */
    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD_TAG);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return KHEntities.NUTRIA.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.NUTRIA_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.NUTRIA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.NUTRIA_DEATH.get();
    }
}
