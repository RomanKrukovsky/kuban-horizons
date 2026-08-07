package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;

/**
 * Саранча: летающий вредитель, приходящий налётом.
 *
 * <p>Роль в экосистеме — временное давление на посевы и, что важнее, корм для
 * наземных птиц. Фазаны и перепела охотятся на саранчу
 * ({@link HuntLocustGoal}), поэтому птицы на участке перестают быть просто
 * мясом и становятся защитой урожая. Это и есть смысл существа: оно связывает
 * уже существующую фауну с фермерством.</p>
 *
 * <p>Саранча живёт ограниченное время ({@link #LIFETIME_TICKS}) и исчезает
 * сама — налёт заканчивается без вмешательства игрока и не оставляет
 * бесконечно растущей популяции на сервере.</p>
 */
public final class Locust extends PathfinderMob {
    private static final int SCHEMA_VERSION = 1;
    /** Налёт длится примерно четыре игровые минуты. */
    private static final int LIFETIME_TICKS = 4800;
    /** Как часто особь пытается объесть культуру под собой. */
    private static final int EAT_INTERVAL = 60;

    private int lifeTicks;

    public Locust(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.FLYING_SPEED, 0.32D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 12.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new EatCropGoal());
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal(this, 1.0D));
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
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
        return !onGround() && tickCount % 3 == 0;
    }

    /**
     * Парящий полёт: {@code omnidirectionalAirMover} даёт воздушное трение
     * вместо падения, поэтому рой висит над полем без собственной физики.
     */
    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    /** Саранча не разбивается: полёт — её нормальное состояние. */
    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        // Налёт самоограничен по времени: популяция не растёт бесконечно.
        if (++lifeTicks > LIFETIME_TICKS) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putInt("LifeTicks", lifeTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        lifeTicks = Mth.clamp(input.getIntOr("LifeTicks", 0), 0, LIFETIME_TICKS);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.LOCUST_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.LOCUST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.LOCUST_HURT.get();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    /**
     * Объедает культуру под собой: откатывает возраст на одну стадию, а
     * незрелую — уничтожает. Не трогает плодородие: саранча ест растение, а не
     * портит почву, в отличие от кабана.
     */
    private final class EatCropGoal extends net.minecraft.world.entity.ai.goal.Goal {
        @Override
        public boolean canUse() {
            return KHServerConfig.pressureEnabled()
                    && KHServerConfig.pressureSeverity() > 0.0D
                    && tickCount % EAT_INTERVAL == getId() % EAT_INTERVAL;
        }

        @Override
        public void start() {
            if (!(level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos pos = blockPosition();
            for (int dy = 0; dy >= -2; dy--) {
                BlockPos candidate = pos.offset(0, dy, 0);
                BlockState state = level.getBlockState(candidate);
                if (!state.is(BlockTags.CROPS)) {
                    continue;
                }
                if (state.getBlock() instanceof CropBlock crop && crop.getAge(state) > 0) {
                    level.setBlockAndUpdate(candidate, crop.getStateForAge(crop.getAge(state) - 1));
                } else {
                    level.destroyBlock(candidate, false, Locust.this);
                }
                level.playSound(null, candidate, KHSounds.LOCUST_AMBIENT.get(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 0.3F, 1.0F);
                return;
            }
        }
    }
}
