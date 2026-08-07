package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

/**
 * Охота наземной птицы на саранчу.
 *
 * <p>Это связующее звено между уже существующей фауной и фермерством: птица
 * на участке перестаёт быть источником мяса и становится защитой урожая.
 * Поедание саранчи — единственная причина держать фазанов рядом с полем, и
 * она появляется без единой новой сущности со стороны птиц.</p>
 */
public final class HuntLocustGoal extends Goal {
    private static final int SEARCH_INTERVAL = 20;
    /** Дистанция, с которой птица успевает схватить саранчу. */
    private static final double CATCH_DISTANCE_SQR = 1.4D;

    private final PathfinderMob bird;
    private final double speed;
    private final double searchRadius;
    private Locust target;

    public HuntLocustGoal(PathfinderMob bird, double speed, double searchRadius) {
        this.bird = bird;
        this.speed = speed;
        this.searchRadius = searchRadius;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (bird.isBaby()) {
            return false;
        }
        if (bird.tickCount % SEARCH_INTERVAL != bird.getId() % SEARCH_INTERVAL) {
            return false;
        }
        target = nearestLocust();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive()
                && bird.distanceToSqr(target) <= searchRadius * searchRadius;
    }

    @Override
    public void start() {
        if (target != null) {
            bird.getNavigation().moveTo(target, speed);
        }
    }

    @Override
    public void stop() {
        target = null;
        bird.getNavigation().stop();
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
        bird.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (bird.distanceToSqr(target) <= CATCH_DISTANCE_SQR) {
            // Съедена: птица лечится, налёт слабеет. Урон от птицы, чтобы
            // сработали ванильные события смерти и loot.
            target.hurtServer((net.minecraft.server.level.ServerLevel) bird.level(),
                    bird.damageSources().mobAttack(bird), 4.0F);
            if (bird.getHealth() < bird.getMaxHealth()) {
                bird.heal(1.0F);
            }
            stop();
            return;
        }
        if (bird.getNavigation().isDone()) {
            bird.getNavigation().moveTo(target, speed);
        }
    }

    private Locust nearestLocust() {
        AABB area = bird.getBoundingBox().inflate(searchRadius, 4.0D, searchRadius);
        List<Locust> found = bird.level().getEntitiesOfClass(Locust.class, area, Locust::isAlive);
        Locust best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Locust locust : found) {
            double distance = bird.distanceToSqr(locust);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = locust;
            }
        }
        return best;
    }
}
