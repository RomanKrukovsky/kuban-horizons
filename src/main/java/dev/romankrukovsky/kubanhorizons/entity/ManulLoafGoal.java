package dev.romankrukovsky.kubanhorizons.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Манул сидит неподвижно: греется, караулит, ничего не делает.
 *
 * <p>Это то, за что настоящего манула и знают: он умеет сидеть с
 * невозмутимым видом там, где другому зверю сидеть незачем. Механически цель
 * даёт моду сцену вместо мельтешения — зверь на камне у балки, на стоге сена,
 * на плетне, посреди дороги. Игрок, который найдёт манула, чаще всего найдёт
 * его именно так: сидящим.</p>
 *
 * <p>Отличие от ванильного {@code CatSitOnBlockGoal} принципиальное: коту
 * нужно быть прирученным и он садится на сундук, печь и кровать, то есть на
 * мебель игрока. Манул дикий — он садится на природные и хозяйственные
 * поверхности и не требует ни владельца, ни доверия. Кроме того, это не поза
 * «сидеть по команде»: приказать ему нельзя.</p>
 *
 * <p>Длительность зависит от характера ({@link ManulPersonality#idleWeight()}
 * через {@link #sitTicksFor}), поэтому ленивый действительно сидит дольше
 * храброго, а не отличается от него подписью в коде.</p>
 */
public final class ManulLoafGoal extends Goal {
    /** Радиус поиска подходящего места. */
    private static final int SEARCH_RANGE = 8;
    /** Базовая длительность сидения в тиках (~10 секунд). */
    private static final int BASE_SIT_TICKS = 200;
    /** Пауза между попытками усесться, чтобы зверь не залипал на месте. */
    private static final int COOLDOWN_TICKS = 300;

    private final Manul manul;
    private BlockPos spot;
    private int sitTicks;
    private int cooldown;

    public ManulLoafGoal(Manul manul) {
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
        // Пока рядом кто-то, от кого он уходит, сидеть не время.
        double retreat = manul.retreatDistance();
        if (retreat > 0.0D && manul.level().getNearestPlayer(manul, retreat) != null) {
            return false;
        }
        spot = findSpot();
        return spot != null;
    }

    @Override
    public boolean canContinueToUse() {
        return spot != null && sitTicks > 0 && manul.getTarget() == null;
    }

    @Override
    public void start() {
        sitTicks = sitTicksFor(manul.personality());
        if (spot != null) {
            manul.getNavigation().moveTo(spot.getX() + 0.5D, spot.getY() + 1.0D,
                    spot.getZ() + 0.5D, 0.8D);
        }
    }

    @Override
    public void stop() {
        spot = null;
        sitTicks = 0;
        cooldown = COOLDOWN_TICKS;
        // Поза снимается явно: иначе зверь ушёл бы, продолжая выглядеть сидящим.
        manul.setLoafing(false);
        manul.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (spot == null) {
            return;
        }
        sitTicks--;
        boolean arrived = spot.closerToCenterThan(manul.position(), 1.5D);
        if (arrived) {
            // Доехал — садится и перестаёт двигаться. Невозмутимость тут
            // буквальная: навигация останавливается совсем.
            manul.getNavigation().stop();
            manul.setLoafing(true);
        } else if (manul.getNavigation().isDone()) {
            // Место не достигнуто и путь потерян — не упорствуем: манул
            // передумал, а не застрял в попытках дойти.
            sitTicks = 0;
        }
    }

    /**
     * Сколько тиков особь просидит: ленивый — заметно дольше храброго.
     *
     * <p>Публичная и статическая, чтобы разницу характеров можно было
     * проверить тестом, не гоняя зверя по миру.</p>
     */
    public static int sitTicksFor(ManulPersonality personality) {
        return Math.round(BASE_SIT_TICKS * personality.idleWeight());
    }

    /** Ближайшее место, где манулу уместно усесться. */
    private BlockPos findSpot() {
        BlockPos origin = manul.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-SEARCH_RANGE, -2, -SEARCH_RANGE),
                origin.offset(SEARCH_RANGE, 2, SEARCH_RANGE))) {
            if (!isGoodSpot(manul.level(), pos)) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    /**
     * Годится ли блок как насест.
     *
     * <p>Список намеренно «кубанский», а не «любая твёрдая поверхность»: на
     * стоге сена, плетне, камне у балки и на дороге сидящий манул читается как
     * часть места. Разрешить всё значило бы, что зверь садится где попало и
     * сцена не складывается.</p>
     */
    private static boolean isGoodSpot(LevelReader level, BlockPos pos) {
        if (!level.isEmptyBlock(pos.above())) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.HAY_BLOCK)
                || state.is(BlockTags.FENCES)
                || state.is(BlockTags.WALLS)
                || state.is(Blocks.STONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DIRT_PATH)
                || dev.romankrukovsky.kubanhorizons.entity.ManulWorldHooks.isShelter(state)
                || state.is(dev.romankrukovsky.kubanhorizons.registry.KHBlocks.SHELL_ROCK.get());
    }
}
