package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Редкая и ограниченная проверка связи птицы с ближайшей стаей. */
final class GroundBirdFlockGoal extends Goal {
    private final AbstractGroundBird bird;
    private Vec3 center;

    GroundBirdFlockGoal(AbstractGroundBird bird) {
        this.bird = bird;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!bird.shouldCheckFlock() || bird.isFlushing()) {
            return false;
        }
        List<AbstractGroundBird> flock = bird.nearbyFlockMembers();
        if (flock.isEmpty()) {
            return false;
        }
        center = flock.stream().map(AbstractGroundBird::position)
                .reduce(Vec3.ZERO, Vec3::add).scale(1.0D / flock.size());
        double distance = bird.position().distanceToSqr(center);
        return distance > 16.0D && distance < bird.flockRadius() * bird.flockRadius();
    }

    @Override
    public boolean canContinueToUse() {
        return center != null && !bird.getNavigation().isDone()
                && bird.position().distanceToSqr(center) > 4.0D;
    }

    @Override
    public void start() {
        bird.getNavigation().moveTo(center.x, center.y, center.z, bird.walkSpeed());
    }

    @Override
    public void stop() {
        center = null;
    }
}
