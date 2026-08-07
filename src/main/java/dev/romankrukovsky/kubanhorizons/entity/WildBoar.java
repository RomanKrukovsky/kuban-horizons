package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;

/**
 * Дикий кабан: ночной антагонист хозяйства.
 *
 * <p>Роль в экосистеме — давление на незащищённую ферму. Кабан не охотится на
 * игрока и не агрессивен сам по себе; он идёт к посевам, уничтожает растение и
 * вытаптывает грядку ({@link RaidFarmlandGoal}). Отвечает только на удар, и то
 * ограниченное время — это угроза хозяйству, а не моб-угроза игроку.</p>
 *
 * <p>Плетень и любая замкнутая ограда останавливают его без специальной
 * логики: навигатор не строит путь через ограду.</p>
 */
public final class WildBoar extends Animal {
    public static final TagKey<net.minecraft.world.item.Item> FOOD_TAG =
            ItemTags.create(dev.romankrukovsky.kubanhorizons.util.KHIds.of("wild_boar_foods"));
    private static final int SCHEMA_VERSION = 1;
    /** Сколько тиков кабан помнит обиду, прежде чем вернуться к потраве. */
    private static final int ANGER_TICKS = 300;

    private int angerTicks;

    public WildBoar(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, false));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(3, new RaidFarmlandGoal(this, 1.15D));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Отвечает только на удар: HurtByTargetGoal, без NearestAttackableTarget.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD_TAG);
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        if (target != null) {
            angerTicks = ANGER_TICKS;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (angerTicks > 0 && --angerTicks == 0) {
            setTarget(null);
            setLastHurtByMob(null);
        }
    }

    /** Кабан подбирает жмых и овощи с земли — так его можно отвести от поля. */
    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity item) {
        ItemStack stack = item.getItem();
        if (!isFood(stack)) {
            return;
        }
        if (getHealth() < getMaxHealth()) {
            heal(2.0F);
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return KHEntities.WILD_BOAR.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putInt("AngerTicks", angerTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        angerTicks = Mth.clamp(input.getIntOr("AngerTicks", 0), 0, ANGER_TICKS);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.WILD_BOAR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.WILD_BOAR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.WILD_BOAR_DEATH.get();
    }
}
