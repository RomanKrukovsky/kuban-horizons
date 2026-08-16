package genie.brain.behavior;

import genie.brain.GenieBehaviorMode;
import genie.entity.GenieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Goal for genie to follow its owner
 * Handles navigation, positioning, and behavior based on mode
 */
public class FollowGenieOwnerGoal extends Goal {
    private final GenieEntity genie;
    private final double speedModifier;
    private final float stopDistance;
    private final float startDistance;
    private LivingEntity owner;
    private int timeToRecalcPath;
    private float oldWaterCost;

    public FollowGenieOwnerGoal(GenieEntity genie, double speedModifier, float startDistance, float stopDistance) {
        this.genie = genie;
        this.speedModifier = speedModifier;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.owner = this.genie.getOwner();
        if (this.owner == null) {
            return false;
        }
        if (this.genie.distanceToSqr(this.owner) < (double)(this.startDistance * this.startDistance)) {
            return false;
        }
        if (this.genie.getBehaviorMode() == GenieBehaviorMode.PASSIVE) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.genie.getNavigation().isDone()) {
            return false;
        }
        if (this.genie.getBehaviorMode() == GenieBehaviorMode.PASSIVE) {
            return false;
        }
        return this.owner != null && !this.owner.isDeadOrDying() && this.genie.distanceToSqr(this.owner) > (double)(this.stopDistance * this.stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.genie.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER);
        this.genie.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        this.genie.getNavigation().stop();
        this.genie.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, this.oldWaterCost);
    }

    @Override
    public void tick() {
        this.genie.getLookControl().setLookAt(this.owner, 10.0F, (float)this.genie.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.genie.isLeashed() && !this.genie.isPassenger()) {
                if (this.genie.distanceToSqr(this.owner) >= 144.0D) {
                    this.teleportToOwner();
                } else {
                    this.genie.getNavigation().moveTo(this.owner, this.speedModifier);
                }
            }
        }
    }

    private void teleportToOwner() {
        Vec3 vec3 = this.owner.position();
        for (int i = 0; i < 10; ++i) {
            double d0 = this.random.nextDouble() * 2.0D * 6.2831854820251465D;
            double d1 = this.random.nextDouble() * 0.5D + 0.5D;
            double d2 = Math.cos(d0) * d1;
            double d3 = Math.sin(d0) * d1;
            int j = this.random.nextInt(2 * 8) - 8;
            int k = this.random.nextInt(2 * 4) - 2;
            int l = this.random.nextInt(2 * 8) - 8;
            boolean flag = this.mayPositionBeSafe(vec3.x() + d2, vec3.y() + (double)k, vec3.z() + d3);
            if (flag) {
                this.genie.moveTo(vec3.x() + d2, vec3.y() + (double)k, vec3.z() + d3, this.genie.getYRot(), this.genie.getXRot());
                this.genie.getNavigation().stop();
                return;
            }
        }
    }

    private boolean mayPositionBeSafe(double p_25368_, double p_25369_, double p_25370_) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(p_25368_, p_25369_, p_25370_);
        return this.genie.level().isEmptyBlock(blockpos$mutableblockpos) && this.genie.level().isEmptyBlock(blockpos$mutableblockpos.move(net.minecraft.core.Direction.DOWN));
    }

    @Nullable
    private Vec3 getPosition() {
        LivingEntity owner = this.genie.getOwner();
        if (owner == null) {
            return null;
        }

        // Calculate desired position based on behavior mode
        Vec3 lookVec = owner.getLookAngle().normalize();
        Vec3 ownerPos = owner.position();

        switch (this.genie.getBehaviorMode()) {
            case FOLLOW:
                // Stay close to owner
                return ownerPos.add(lookVec.scale(-1.5)).add(0, 0.5, 0);
            case GUARD:
                // Stay at preferred distance
                return ownerPos.add(lookVec.scale(-3.0)).add(0, 1, 0);
            case SCOUT:
                // Scout ahead
                return ownerPos.add(lookVec.scale(4.0)).add(0, 0.5, 0);
            default:
                return ownerPos.add(lookVec.scale(-2.0)).add(0, 0.5, 0);
        }
    }
}