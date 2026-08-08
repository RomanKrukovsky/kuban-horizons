package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Манул сам подходит рассмотреть игрока.
 *
 * <p>Это вторая половина характера. {@link ManulRetreatGoal} отвечает за
 * «уйти», а эта цель — за «подойти и посмотреть»: без неё
 * {@link ManulPersonality#curiosity()} был бы числом, которое ничего не
 * делает, и любопытный манул отличался бы от осторожного только скоростью
 * доверия. Здесь разница видна глазами — один зверь наблюдает с забора,
 * другой не покажется вовсе.</p>
 *
 * <p>Цель не следование: манул подходит на дистанцию наблюдения
 * ({@link #WATCH_DISTANCE}) и останавливается, а через
 * {@link #MAX_WATCH_TICKS} теряет интерес и уходит по своим делам. Именно
 * поэтому даже полностью доверяющий зверь не превращается в питомца,
 * ходящего по пятам, — за игроком он не идёт, он его разглядывает.</p>
 */
public final class ManulObserveGoal extends Goal {
    /** С какого расстояния зверь считает, что уже всё разглядел. */
    private static final double WATCH_DISTANCE = 4.0D;
    /** Дальше этого он даже не заинтересуется. */
    private static final double NOTICE_DISTANCE = 16.0D;
    /** Сколько тиков он готов наблюдать, прежде чем потеряет интерес. */
    private static final int MAX_WATCH_TICKS = 200;
    /** Как часто проверяется желание подойти. */
    private static final int TRY_INTERVAL = 40;

    private final Manul manul;
    private Player watched;
    private int watchTicks;

    public ManulObserveGoal(Manul manul) {
        this.manul = manul;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Пока зверь дикий, любопытство проигрывает страху: подходить он
        // начинает только с той ступени, где перестал убегать.
        if (!manul.trust().atLeast(ManulTrust.WARY)) {
            return false;
        }
        if (manul.isBaby() || manul.isHissing() || manul.isFrozen()) {
            return false;
        }
        if (manul.tickCount % TRY_INTERVAL != manul.getId() % TRY_INTERVAL) {
            return false;
        }
        Player candidate = manul.level().getNearestPlayer(manul, NOTICE_DISTANCE);
        if (candidate == null || candidate.isCreative() || candidate.isSpectator()) {
            return false;
        }
        // К бегущему человеку не подходит никто: сначала он должен успокоиться.
        if (Manul.isRushing(candidate)) {
            return false;
        }
        // Бросок кости по характеру: любопытный подойдёт часто, ленивый почти
        // никогда. Проверка редкая (раз в TRY_INTERVAL), поэтому шанс читается
        // как «склонность», а не как мгновенное решение.
        if (manul.getRandom().nextFloat() > manul.personality().curiosity()) {
            return false;
        }
        watched = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (watched == null || !watched.isAlive() || watchTicks > MAX_WATCH_TICKS) {
            return false;
        }
        // Резкое движение прекращает наблюдение: доверие не значит бесстрашие.
        if (Manul.isRushing(watched)) {
            return false;
        }
        return manul.distanceToSqr(watched) <= NOTICE_DISTANCE * NOTICE_DISTANCE;
    }

    @Override
    public void start() {
        watchTicks = 0;
    }

    @Override
    public void stop() {
        watched = null;
        watchTicks = 0;
        manul.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (watched == null) {
            return;
        }
        watchTicks++;
        manul.getLookControl().setLookAt(watched, 30.0F, 30.0F);

        double distanceSqr = manul.distanceToSqr(watched);
        if (distanceSqr <= WATCH_DISTANCE * WATCH_DISTANCE) {
            // Подошёл достаточно — дальше только смотрит. Именно эта остановка
            // отличает наблюдение от следования за хозяином.
            manul.getNavigation().stop();
            return;
        }
        if (manul.getNavigation().isDone()) {
            // Подходит медленно: это интерес, а не бег навстречу.
            manul.getNavigation().moveTo(watched, 0.7D);
        }
    }
}
