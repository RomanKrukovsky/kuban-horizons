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
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Кавказская овчарка: прирученный защитник хозяйства.
 *
 * <p>Роль в экосистеме — ответ игрока на давление. Овчарка сама атакует
 * кабанов и нутрий, поэтому у угрозы посевам появляется второй контр-приём,
 * кроме ограды: живая охрана, которую надо приручить и развести. Без неё
 * кабан — просто налог; с ней у игрока есть выбор стратегии.</p>
 *
 * <p>Наследует {@link TamableAnimal}, поэтому ванильные приручение, посадка по
 * команде и телепорт к владельцу работают без собственного кода.</p>
 */
public final class CaucasianShepherd extends net.minecraft.world.entity.TamableAnimal {
    public static final TagKey<net.minecraft.world.item.Item> FOOD_TAG =
            ItemTags.create(dev.romankrukovsky.kubanhorizons.util.KHIds.of("caucasian_shepherd_foods"));
    public static final TagKey<net.minecraft.world.item.Item> TAMING_TAG =
            ItemTags.create(dev.romankrukovsky.kubanhorizons.util.KHIds.of("caucasian_shepherd_taming"));

    public CaucasianShepherd(EntityType<? extends net.minecraft.world.entity.TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        goalSelector.addGoal(4, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // Смысл существа: сама, без приказа, гонит вредителей с участка.
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, WildBoar.class, true));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Nutria.class, true));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD_TAG);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        CaucasianShepherd child = KHEntities.CAUCASIAN_SHEPHERD.get()
                .create(level, EntitySpawnReason.BREEDING);
        if (child != null && getOwnerReference() != null) {
            // Потомство наследует владельца: разведение охраны имеет смысл.
            child.setOwnerReference(getOwnerReference());
            child.setTame(true, true);
        }
        return child;
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isTame() && held.is(TAMING_TAG)) {
            if (!level().isClientSide()) {
                held.consume(1, player);
                if (random.nextInt(3) == 0) {
                    tame(player);
                    navigation.stop();
                    setTarget(null);
                    setOrderedToSit(true);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.CAUCASIAN_SHEPHERD_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.CAUCASIAN_SHEPHERD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.CAUCASIAN_SHEPHERD_DEATH.get();
    }
}
