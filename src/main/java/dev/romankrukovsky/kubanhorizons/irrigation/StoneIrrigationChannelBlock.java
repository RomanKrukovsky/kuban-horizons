package dev.romankrukovsky.kubanhorizons.irrigation;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Каменный оросительный желоб: гидравлически идентичен деревянному, но
 * нутрия его не грызёт.
 *
 * <p>Наследование, а не флаг в свойстве блока: вся логика сети опирается на
 * {@code instanceof IrrigationChannelBlock}, поэтому подкласс включается в
 * существующие волны распространения без изменений в них. Разница только в
 * материале — и именно она делает апгрейд сети осмысленным выбором, а не
 * косметикой.</p>
 */
public final class StoneIrrigationChannelBlock extends IrrigationChannelBlock {
    public static final MapCodec<StoneIrrigationChannelBlock> CODEC =
            simpleCodec(StoneIrrigationChannelBlock::new);

    public StoneIrrigationChannelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<StoneIrrigationChannelBlock> codec() {
        return CODEC;
    }
}
