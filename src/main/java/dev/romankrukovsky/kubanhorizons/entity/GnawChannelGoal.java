package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Грызёт деревянный оросительный желоб: животное находит участок сети,
 * подгрызает его и разрушает блок, разрывая сеть.
 *
 * <p>Каменный желоб не является целью — это и делает апгрейд материала
 * механически осмысленным, а не косметическим.</p>
 */
public final class GnawChannelGoal extends Goal {
    private static final int SCAN_INTERVAL = 60;
    private static final int SCAN_RADIUS = 10;
    private static final int SCAN_HEIGHT = 3;
    /** Тиков грызения до разрушения блока. */
    private static final int GNAW_TICKS = 60;

    private final PathfinderMob mob;
    private final double speed;
    private BlockPos target;
    private int cooldown;
    private int gnawTicks;

    public GnawChannelGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!KHServerConfig.pressureEnabled() || KHServerConfig.pressureSeverity() <= 0.0D) {
            return false;
        }
        if (mob.isBaby()) {
            return false;
        }
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (mob.tickCount % SCAN_INTERVAL != mob.getId() % SCAN_INTERVAL) {
            return false;
        }
        target = findWoodenChannel();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && isWoodenChannel(mob.level().getBlockState(target));
    }

    @Override
    public void start() {
        gnawTicks = 0;
        if (target != null) {
            mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
        }
    }

    @Override
    public void stop() {
        target = null;
        gnawTicks = 0;
        cooldown = 400 + mob.getRandom().nextInt(400);
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (mob.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 2.25D) {
            if (mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
            }
            return;
        }
        gnawTicks++;
        if (gnawTicks % 15 == 0 && mob.level() instanceof ServerLevel level) {
            level.playSound(null, target, net.minecraft.sounds.SoundEvents.WOOD_BREAK,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.4F,
                    1.4F + mob.getRandom().nextFloat() * 0.2F);
        }
        if (gnawTicks < GNAW_TICKS) {
            return;
        }
        if (mob.level() instanceof ServerLevel level
                && isWoodenChannel(level.getBlockState(target))) {
            // Разрушение с дропом: сеть рвётся, но материал возвращается игроку —
            // это ремонт, а не безвозвратная потеря.
            level.destroyBlock(target, true, mob);
        }
        stop();
    }

    private BlockPos findWoodenChannel() {
        BlockPos origin = mob.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                for (int dy = -SCAN_HEIGHT; dy <= SCAN_HEIGHT; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isWoodenChannel(mob.level().getBlockState(cursor))) {
                        continue;
                    }
                    double distance = cursor.distSqr(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    /**
     * Только деревянный желоб. Сравнение по конкретному блоку, а не
     * {@code instanceof}: каменный желоб — подкласс деревянного, и проверка
     * по типу сделала бы апгрейд бесполезным.
     */
    private static boolean isWoodenChannel(BlockState state) {
        return state.is(KHBlocks.IRRIGATION_CHANNEL.get());
    }
}
