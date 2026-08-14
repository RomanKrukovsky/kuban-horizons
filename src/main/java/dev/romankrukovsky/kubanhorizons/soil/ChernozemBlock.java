package dev.romankrukovsky.kubanhorizons.soil;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jspecify.annotations.Nullable;

/**
 * Чернозём — нетронутая тучная земля степи, высший ярус почвы.
 *
 * <p>Найденный блок, а не крафтимый: в главном цикле мода
 * (GAME_DESIGN.md §2) вторым шагом стоит именно «поиск чернозёма». Рецепт
 * превратил бы поиск в формальность, поэтому чернозём добывается лопатой
 * там, где он лежит, и переносится на свою ферму руками. Стоимость —
 * дорога и объём: на грядку 9×9 нужно 81 перенесённый блок.</p>
 *
 * <p>Вспахивается мотыгой в {@link ChernozemFarmlandBlock}, сохраняя ярус.
 * Ванильная таблица {@code HoeItem.TILLABLES} про мод не знает, поэтому
 * переопределяется {@code getToolModifiedState} — путь, который NeoForge
 * оставил как раз для этого.</p>
 */
public class ChernozemBlock extends Block {
    public static final MapCodec<ChernozemBlock> CODEC = simpleCodec(ChernozemBlock::new);

    public ChernozemBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ChernozemBlock> codec() {
        return CODEC;
    }

    /**
     * Мотыга по чернозёму даёт грядку чернозёма.
     *
     * <p>Условие «сверху воздух» и запрет вспашки снизу повторяют ванильное
     * {@code HoeItem.onlyIfAirAbove}: без него грядку можно было бы сделать
     * под уже стоящим блоком, а она там мгновенно рассыплется.</p>
     */
    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context,
            ItemAbility ability, boolean simulate) {
        if (ability == ItemAbilities.HOE_TILL
                && context.getClickedFace() != Direction.DOWN
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            return KHBlocks.CHERNOZEM_FARMLAND.get().defaultBlockState();
        }
        return super.getToolModifiedState(state, context, ability, simulate);
    }
}
