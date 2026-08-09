package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Манул бросается на игрока сам — но только по причине, а не по факту его
 * существования.
 *
 * <p>Разница с зомби и крипером принципиальная и она вся здесь: враждебный моб
 * ищет игрока, потому что тот игрок. Манул ищет игрока, только когда тот
 * <b>уже сделал одно из трёх</b>:</p>
 *
 * <ol>
 *   <li><b>Загнал в угол.</b> Зверь пытался уйти и не смог — путь перекрыт.
 *   Это первая и главная причина: дикая кошка бьёт, когда бежать некуда.</li>
 *   <li><b>Разбудил.</b> Спящего манула подняли вплотную — реакция мгновенная,
 *   и характер тут не спасает.</li>
 *   <li><b>Убивал животных у него на глазах.</b> Доверие уже ушло в минус по
 *   {@link Manul#witnessHarm}, и зверь видит в игроке угрозу, а не человека
 *   с едой.</li>
 * </ol>
 *
 * <p>Ни в одном случае агрессия не бесконечна: она держится
 * {@link Manul#RETALIATION_TICKS} и гаснет сама. Манул огрызается и уходит —
 * преследовать через полкарты он не станет, иначе получился бы мелкий хищник
 * вместо дикой кошки.</p>
 *
 * <p>Прирученного (доверяющего) зверя это не касается вовсе: на своего игрока
 * манул не бросается ни при каких условиях.</p>
 */
public final class ManulProvokedGoal extends TargetGoal {
    /** Дальше этого зверь на игрока не смотрит: он не охотник. */
    private static final double NOTICE_RANGE = 8.0D;
    /** Вплотную: дистанция, с которой начинается «загнан в угол». */
    private static final double CORNERED_RANGE = 3.0D;
    /** Ниже этого доверия игрок считается угрозой. */
    private static final int HOSTILE_TRUST = -1;

    private final Manul manul;
    private Player provoker;

    public ManulProvokedGoal(Manul manul) {
        // mustSee = true: манул реагирует на то, что видит, а не сквозь стены.
        super(manul, true);
        this.manul = manul;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!manul.canBeProvoked()) {
            // Только что остыл: дать ему уйти, а не бросаться снова.
            return false;
        }
        if (manul.isBaby()) {
            // Котёнок только убегает: дать ему атаку — сделать мод неприятным.
            return false;
        }
        Player candidate = manul.level().getNearestPlayer(manul, NOTICE_RANGE);
        if (candidate == null || candidate.isCreative() || candidate.isSpectator()) {
            return false;
        }
        if (manul.isOwnedBy(candidate)) {
            // На своего человека зверь не бросается никогда.
            return false;
        }
        if (!canAttack(candidate, net.minecraft.world.entity.ai.targeting.TargetingConditions
                .forCombat().range(NOTICE_RANGE))) {
            return false;
        }
        if (!provoked(candidate)) {
            return false;
        }
        provoker = candidate;
        return true;
    }

    /** Проверяет три причины по возрастанию редкости. */
    private boolean provoked(Player player) {
        // 1. Разбудили вплотную: самая простая и самая честная провокация.
        if (manul.isDozing() && manul.distanceToSqr(player) <= CORNERED_RANGE * CORNERED_RANGE) {
            return true;
        }
        // 2. Игрок уже показал, что опасен: доверие ушло в минус за жестокость.
        if (manul.trustToward(player) <= HOSTILE_TRUST
                && manul.distanceToSqr(player) <= CORNERED_RANGE * CORNERED_RANGE) {
            return true;
        }
        // 3. Загнан в угол: зверь пытался уйти и не смог. Проверяется здесь, а
        // не в цели отхода, потому что решение атаковать принимается по итогу
        // побега, а не в его начале.
        return manul.isCornered() && manul.distanceToSqr(player)
                <= CORNERED_RANGE * CORNERED_RANGE;
    }

    @Override
    public void start() {
        manul.setTarget(provoker);
        manul.startRetaliation();
        manul.hiss();
        super.start();
    }

    @Override
    public void stop() {
        provoker = null;
        super.stop();
    }

    @Override
    public boolean canContinueToUse() {
        // Злость гаснет по таймеру в самой сущности: тут только следим, что
        // она ещё горит и цель жива.
        return manul.isRetaliating() && super.canContinueToUse();
    }
}
