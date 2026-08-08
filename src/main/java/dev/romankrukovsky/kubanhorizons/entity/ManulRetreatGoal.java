package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Отход манула от игрока: замер — взгляд — медленный отход — побег.
 *
 * <p>Ванильная {@code AvoidEntityGoal} сразу убегает, и встреча превращается в
 * мелькнувшую спину. Здесь порядок другой и он и есть характер зверя: сначала
 * манул замирает и разглядывает игрока, и только потом уходит. Резкое
 * приближение ломает эту паузу — зверь шипит и уходит быстро.</p>
 *
 * <p>Дистанция берётся из {@link Manul#retreatDistance()}, то есть зависит от
 * характера и накопленного доверия: прогресс знакомства виден по тому, как
 * близко зверь подпускает.</p>
 */
public final class ManulRetreatGoal extends Goal {
    private final Manul manul;
    private Player threat;
    private boolean stared;

    public ManulRetreatGoal(Manul manul) {
        this.manul = manul;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        double distance = manul.retreatDistance();
        if (distance <= 0.0D) {
            // Полное доверие: отходить незачем.
            return false;
        }
        threat = manul.level().getNearestPlayer(manul, distance);
        if (threat == null || threat.isCreative() || threat.isSpectator()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (threat == null || !threat.isAlive()) {
            return false;
        }
        double distance = manul.retreatDistance();
        return manul.distanceToSqr(threat) < distance * distance * 1.6D;
    }

    @Override
    public void start() {
        stared = false;
        // Первая реакция — не бег, а взгляд. Кроме случая, когда к зверю
        // именно бросились: тогда пауза выглядела бы нелепо.
        if (Manul.isRushing(threat)) {
            manul.hiss();
            stared = true;
        } else {
            manul.freezeAndStare();
        }
    }

    @Override
    public void stop() {
        threat = null;
        stared = false;
        manul.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (threat == null) {
            return;
        }
        manul.getLookControl().setLookAt(threat, 30.0F, 30.0F);

        if (manul.isFrozen()) {
            // Пока разглядывает — стоит. Но если к нему рванули, замирание
            // прерывается: инстинкт сильнее любопытства.
            if (Manul.isRushing(threat)) {
                manul.hiss();
                stared = true;
            }
            return;
        }
        stared = true;

        Vec3 away = manul.position().subtract(threat.position())
                .multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(manul.getRandom().nextDouble() - 0.5D, 0.0D,
                    manul.getRandom().nextDouble() - 0.5D);
        }
        Vec3 target = manul.position().add(away.normalize().scale(6.0D));
        // Резкое приближение — быстрый уход; иначе достойное отступление.
        double speed = Manul.isRushing(threat) ? 1.6D : 0.9D;
        if (manul.getNavigation().isDone()) {
            manul.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }
}
