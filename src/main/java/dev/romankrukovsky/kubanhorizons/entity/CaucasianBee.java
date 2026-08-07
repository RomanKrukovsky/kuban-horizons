package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Кавказская пчела: симбионт хозяйства.
 *
 * <p>Роль в экосистеме — единственный положительный агент давления. Пчела
 * помечает грядки в радиусе полёта как опылённые, а
 * {@link dev.romankrukovsky.kubanhorizons.soil.Pollination} превращает метку в
 * бонус к выходу урожая. В отличие от ванильной пчелы влияет не на скорость
 * роста, а на количество собранного — поэтому размещение пасеки относительно
 * полей становится решением, а не декорацией.</p>
 */
public final class CaucasianBee extends Animal {
    public static final TagKey<net.minecraft.world.item.Item> FOOD_TAG =
            ItemTags.create(dev.romankrukovsky.kubanhorizons.util.KHIds.of("caucasian_bee_foods"));
    /** Как часто пчела помечает грядку под собой. */
    private static final int POLLINATE_INTERVAL = 40;

    public CaucasianBee(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(1, new TemptGoal(this, 1.2D, this::isFood, false));
        goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        return navigation;
    }

    @Override
    public boolean isFlapping() {
        return !onGround() && tickCount % 4 == 0;
    }

    /**
     * Полёт по ванильной схеме: {@code omnidirectionalAirMover} включает
     * воздушное трение вместо гравитационного падения, поэтому пчела парит без
     * собственной физики в {@code travel()}.
     */
    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    /** Пчела не падает с высоты: полёт — её нормальное состояние. */
    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !KHServerConfig.pollinationEnabled()) {
            return;
        }
        if (tickCount % POLLINATE_INTERVAL != getId() % POLLINATE_INTERVAL) {
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        pollinateBelow(level);
    }

    /** Ищет грядку под собой и помечает её опылённой. */
    private void pollinateBelow(ServerLevel level) {
        BlockPos pos = blockPosition();
        for (int dy = 0; dy >= -4; dy--) {
            BlockPos candidate = pos.offset(0, dy, 0);
            if (level.getBlockState(candidate).is(BlockTags.CROPS)
                    && level.getBlockState(candidate.below()).is(BlockTags.SUPPORTS_CROPS)) {
                dev.romankrukovsky.kubanhorizons.soil.Pollination.mark(level, candidate.below());
                return;
            }
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD_TAG);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return KHEntities.CAUCASIAN_BEE.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.CAUCASIAN_BEE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.CAUCASIAN_BEE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.CAUCASIAN_BEE_HURT.get();
    }
}
