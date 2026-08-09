package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Дневной сон манула в укрытии.
 *
 * <p>Спека мода говорит, что манул сумеречный: он выходит утром, вечером и
 * ночью, а днём прячется в высокой траве, под камнями или в заброшенных
 * сараях. Без этой цели «сумеречный» оставался словами в документации:
 * ограничение стояло только на спавн, а уже появившийся зверь бодро гулял по
 * полудню — то есть игрок видел ровно обратное обещанному.</p>
 *
 * <p>Модель умеет рисовать сон клубком ({@code ManulModel.poseSleeping}), и
 * до этой цели поза была недостижима: флаг сна никто не выставлял. Тот самый
 * класс мёртвого кода, из-за которого из мода удалили пчелу, — код есть,
 * в игре не проявляется.</p>
 *
 * <p>Сон прерывается на любую тревогу: подошёл игрок, ударили, появилась
 * цель. Разбуженный зверь не засыпает снова сразу ({@link #WAKE_COOLDOWN}),
 * иначе он мигал бы позой на глазах у игрока.</p>
 */
public final class ManulSleepGoal extends Goal {
    /** Радиус поиска укрытия. */
    private static final int SEARCH_RANGE = 6;
    /** Пауза после пробуждения, прежде чем снова искать укрытие. */
    private static final int WAKE_COOLDOWN = 400;

    private final Manul manul;
    private BlockPos den;
    private int cooldown;

    public ManulSleepGoal(Manul manul) {
        this.manul = manul;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (manul.isBaby() || manul.isHissing() || manul.getTarget() != null) {
            return false;
        }
        if (!isDaytime()) {
            return false;
        }
        // Пока рядом тот, от кого зверь уходит, спать он не станет.
        double retreat = manul.retreatDistance();
        if (retreat > 0.0D && manul.level().getNearestPlayer(manul, retreat) != null) {
            return false;
        }
        den = findDen();
        return den != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (den == null || manul.getTarget() != null || manul.isHissing()) {
            return false;
        }
        if (!isDaytime()) {
            // Наступил вечер — пора на охоту.
            return false;
        }
        // Разбудить может подошедший игрок: зверь дикий и сон у него чуткий.
        double retreat = manul.retreatDistance();
        return retreat <= 0.0D || manul.level().getNearestPlayer(manul, retreat * 0.6D) == null;
    }

    @Override
    public void start() {
        if (den != null) {
            manul.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D, 0.8D);
        }
    }

    @Override
    public void stop() {
        // Кулдаун ставится только если зверь действительно спал и его
        // разбудили. Раньше он ставился при любой остановке — в том числе
        // когда цель просто не довела зверя до норы, — и следующая попытка
        // откладывалась на 400 тиков без причины. Из-за этого дневной сон
        // выглядел случайным: цель прерывалась на полпути и надолго умолкала.
        boolean wasAsleep = manul.isDozing();
        manul.setSleeping(false);
        manul.getNavigation().stop();
        den = null;
        cooldown = wasAsleep ? WAKE_COOLDOWN : 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (den == null) {
            return;
        }
        boolean arrived = manul.blockPosition().distSqr(den) <= 2.0D;
        if (!arrived) {
            manul.setSleeping(false);
            if (manul.getNavigation().isDone()) {
                manul.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D,
                        0.8D);
            }
            return;
        }
        // Дошёл: спит, пока не разбудят или не стемнеет.
        manul.getNavigation().stop();
        manul.setSleeping(true);
    }

    /**
     * День ли снаружи.
     *
     * <p>Считается по времени суток, а не по освещённости в точке зверя. С
     * освещённостью цель противоречила сама себе: манул заходил под крышу,
     * свет там падал ниже порога, «день» заканчивался — и зверь просыпался в
     * тот же миг, как засыпал. Тест на дневной сон падал через раз именно
     * поэтому, и увеличение таймаута лишь спрятало бы причину.</p>
     *
     * <p>{@code isBrightOutside()} смотрит на потемнение неба, а не на свет в
     * точке зверя, поэтому под крышей оно остаётся правдой. Тот же признак
     * использует ванильный код, отделяя день от ночи, и правило спавна манула
     * — значит «день» у сна и у спавна означает одно и то же.</p>
     */
    private boolean isDaytime() {
        return manul.level().isBrightOutside();
    }

    /**
     * Ближайшее укрытие: тень под чем-нибудь, высокая трава или сено.
     *
     * <p>Приоритет отдаётся закрытым сверху местам, а не просто траве: именно
     * они читаются игроком как «нора», и именно там манула логично найти
     * днём.</p>
     */
    private BlockPos findDen() {
        BlockPos origin = manul.blockPosition();
        BlockPos best = null;
        double bestScore = -1.0D;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -SEARCH_RANGE; dx <= SEARCH_RANGE; dx++) {
            for (int dz = -SEARCH_RANGE; dz <= SEARCH_RANGE; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    double score = scoreDen(cursor);
                    if (score <= 0.0D) {
                        continue;
                    }
                    // Ближе — лучше: за укрытием не нужно идти через полстепи.
                    score -= cursor.distSqr(origin) * 0.01D;
                    if (score > bestScore) {
                        bestScore = score;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    /** Оценка места: 0 — не годится, больше — лучше укрытие. */
    private double scoreDen(BlockPos pos) {
        var level = manul.level();
        if (!level.getBlockState(pos).isAir() && !isHidingPlant(level.getBlockState(pos))) {
            return 0.0D;
        }
        if (!level.getBlockState(pos.below()).isSolidRender()) {
            return 0.0D;
        }
        double score = 0.0D;
        // Крыша над головой — главный признак норы. Проверяется прямым
        // просмотром блоков вверх, а не canSeeSky: последний опирается на
        // карту освещения, которая после установки блоков обновляется не
        // мгновенно, и цель то находила укрытие, то нет на одной и той же
        // геометрии. Тест на дневной сон падал через раз именно из-за этого.
        if (hasRoof(pos)) {
            score += 10.0D;
        }
        // Высокая трава и сено: манул прячется в них в открытой степи.
        if (isHidingPlant(level.getBlockState(pos))) {
            score += 4.0D;
        }
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.HAY_BLOCK) || below.is(BlockTags.BASE_STONE_OVERWORLD)) {
            score += 3.0D;
        }
        return score;
    }

    /**
     * Есть ли над точкой перекрытие в пределах разумной высоты норы.
     *
     * <p>Смотрит блоки, а не освещённость: геометрия детерминирована, а свет
     * — нет. Шести блоков достаточно: нора, сарай, навес, выступ скалы.</p>
     */
    private boolean hasRoof(BlockPos pos) {
        var level = manul.level();
        for (int dy = 1; dy <= 6; dy++) {
            if (level.getBlockState(pos.above(dy)).isSolidRender()) {
                return true;
            }
        }
        return false;
    }

    /** Растение, в котором зверь может укрыться. */
    private static boolean isHidingPlant(BlockState state) {
        return state.is(BlockTags.REPLACEABLE_BY_TREES)
                && !state.isAir();
    }
}
