package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Общая серверная логика наземных птиц: размножение, стайность и короткий
 * испуганный взлёт без режима полноценного полёта.
 */
public abstract class AbstractGroundBird extends Animal {
    public static final TagKey<net.minecraft.world.item.Item> FOOD_TAG =
            ItemTags.create(KHIds.of("ground_bird_foods"));
    private static final EntityDataAccessor<Boolean> FLUSHING =
            SynchedEntityData.defineId(AbstractGroundBird.class, EntityDataSerializers.BOOLEAN);
    private static final int SCHEMA_VERSION = 1;
    private static final int FLOCK_INTERVAL = 20;
    private static final int MAX_FLOCK_NEIGHBORS = 8;

    private int flushTicks;
    private int flushCooldown;

    protected AbstractGroundBird(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, panicSpeed()));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, playerAvoidDistance(),
                walkSpeed(), panicSpeed()));
        goalSelector.addGoal(3, new BreedGoal(this, walkSpeed()));
        goalSelector.addGoal(4, new TemptGoal(this, walkSpeed(), this::isFood, false));
        goalSelector.addGoal(5, new GroundBirdFlockGoal(this));
        goalSelector.addGoal(6, new RandomStrollGoal(this, walkSpeed()));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLUSHING, false);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD_TAG);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (flushCooldown > 0) {
            flushCooldown--;
        }
        if (flushTicks > 0) {
            flushTicks--;
            if (flushTicks == 0) {
                entityData.set(FLUSHING, false);
                flushCooldown = flushCooldownTicks();
            }
        } else if (flushCooldown == 0 && !isBaby() && tickCount % 5 == getId() % 5) {
            Player nearbyPlayer = level().getNearestPlayer(this, playerAvoidDistance());
            if (nearbyPlayer != null && !nearbyPlayer.isCreative() && !nearbyPlayer.isSpectator()) {
                tryFlush(nearbyPlayer.position());
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isFlushing() && !onGround()) {
            Vec3 movement = getDeltaMovement();
            if (movement.y < -0.12D) {
                setDeltaMovement(movement.x, -0.12D, movement.z);
            }
        }
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        if (isFlushing() && movement.y < 0.0D) {
            movement = movement.multiply(1.0D, 0.45D, 1.0D);
        }
        super.move(type, movement);
    }

    /** Запускает ограниченный взлёт; повторный запуск невозможен до cooldown. */
    public boolean tryFlush(Vec3 awayFrom) {
        if (level().isClientSide() || isBaby() || flushTicks > 0 || flushCooldown > 0) {
            return false;
        }
        Vec3 horizontal = position().subtract(awayFrom).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 0.01D) {
            horizontal = new Vec3(random.nextDouble() - 0.5D, 0.0D,
                    random.nextDouble() - 0.5D);
        }
        horizontal = horizontal.normalize().scale(flushHorizontalSpeed());
        setDeltaMovement(horizontal.x, flushVerticalSpeed(), horizontal.z);
        flushTicks = flushDurationTicks();
        entityData.set(FLUSHING, true);
        playSound(flushSound(), 0.8F, 0.9F + random.nextFloat() * 0.2F);
        return true;
    }

    public boolean isFlushing() {
        return entityData.get(FLUSHING);
    }

    public int flushTicksRemaining() {
        return flushTicks;
    }

    public int flushCooldownRemaining() {
        return flushCooldown;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putInt("FlushCooldown", flushCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        flushCooldown = Math.max(0, input.getIntOr("FlushCooldown", 0));
        flushTicks = 0;
        entityData.set(FLUSHING, false);
    }

    List<AbstractGroundBird> nearbyFlockMembers() {
        AABB area = getBoundingBox().inflate(flockRadius(), 3.0D, flockRadius());
        return level().getEntitiesOfClass(AbstractGroundBird.class, area,
                        bird -> bird != this && bird.getType() == getType() && bird.isAlive())
                .stream()
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .limit(MAX_FLOCK_NEIGHBORS)
                .toList();
    }

    boolean shouldCheckFlock() {
        return tickCount % FLOCK_INTERVAL == getId() % FLOCK_INTERVAL;
    }

    protected abstract double walkSpeed();

    protected abstract double panicSpeed();

    protected abstract float playerAvoidDistance();

    protected abstract double flockRadius();

    protected abstract int flushDurationTicks();

    protected abstract int flushCooldownTicks();

    protected abstract double flushHorizontalSpeed();

    protected abstract double flushVerticalSpeed();

    protected abstract net.minecraft.sounds.SoundEvent flushSound();

    @Override
    public abstract AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level,
                                                  AgeableMob partner);
}
