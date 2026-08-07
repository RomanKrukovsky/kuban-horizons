package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.soil.SoilFertility;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Поиск грядки и потрава: животное идёт к ближайшей farmland с культурой,
 * уничтожает растение и вытаптывает почву.
 *
 * <p>Ограда работает без специальной логики: цель ищется по прямой,
 * а путь до неё строит обычный навигатор, который через ограду не проходит.
 * Поэтому огороженная ферма защищена ровно настолько, насколько замкнут
 * периметр.</p>
 */
public final class RaidFarmlandGoal extends Goal {
    private static final int SCAN_INTERVAL = 40;
    private static final int SCAN_RADIUS = 12;
    private static final int SCAN_HEIGHT = 3;
    /** Тиков подкопа перед потравой — окно, в которое игрок может вмешаться. */
    private static final int DIG_TICKS = 30;

    private final PathfinderMob mob;
    private final double speed;
    private BlockPos target;
    private int cooldown;
    private int digTicks;

    public RaidFarmlandGoal(PathfinderMob mob, double speed) {
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
        target = findCrop();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && isRaidableCrop(mob.level().getBlockState(target))
                && !mob.getNavigation().isDone() || digTicks > 0;
    }

    @Override
    public void start() {
        digTicks = 0;
        if (target != null) {
            mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
        }
    }

    @Override
    public void stop() {
        target = null;
        digTicks = 0;
        cooldown = 200 + mob.getRandom().nextInt(200);
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
        digTicks++;
        if (digTicks < DIG_TICKS) {
            return;
        }
        trample();
        stop();
    }

    /** Уничтожает культуру и вытаптывает грядку под ней. */
    private void trample() {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = level.getBlockState(target);
        if (!isRaidableCrop(state)) {
            return;
        }
        level.destroyBlock(target, true, mob);
        BlockPos soil = target.below();
        if (level.getBlockState(soil).is(BlockTags.SUPPORTS_CROPS)) {
            SoilFertility.onTrample(level, soil);
            level.setBlockAndUpdate(soil, Blocks.DIRT.defaultBlockState());
        }
    }

    /** Ближайшая созревающая культура на грядке в радиусе сканирования. */
    private BlockPos findCrop() {
        BlockPos origin = mob.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                for (int dy = -SCAN_HEIGHT; dy <= SCAN_HEIGHT; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isRaidableCrop(mob.level().getBlockState(cursor))) {
                        continue;
                    }
                    if (!mob.level().getBlockState(cursor.below()).is(BlockTags.SUPPORTS_CROPS)) {
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

    private static boolean isRaidableCrop(BlockState state) {
        return state.is(BlockTags.CROPS);
    }
}
