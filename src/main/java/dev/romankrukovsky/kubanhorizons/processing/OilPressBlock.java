package dev.romankrukovsky.kubanhorizons.processing;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.blockentity.OilPressBlockEntity;
import dev.romankrukovsky.kubanhorizons.registry.KHBlockEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Блок маслопресса.
 *
 * <p>Взаимодействия:</p>
 * <ul>
 *   <li>ПКМ с пустой рукой + shift — открыть меню;</li>
 *   <li>ПКМ с пустой рукой — один оборот винта (ручной отжим), при
 *       отсутствии рецепта открывается меню;</li>
 *   <li>ПКМ с предметом — открыть меню.</li>
 * </ul>
 */
public class OilPressBlock extends BaseEntityBlock {
    public static final MapCodec<OilPressBlock> CODEC = simpleCodec(OilPressBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 15.0);

    public OilPressBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<OilPressBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof OilPressBlockEntity press && level instanceof ServerLevel serverLevel) {
            if (!player.isShiftKeyDown() && press.turnScrew(serverLevel)) {
                // Ручной оборот винта: скрип + без открытия меню.
                serverLevel.playSound(null, pos, KHSounds.OIL_PRESS_CREAK.get(), SoundSource.BLOCKS, 0.8F,
                        0.9F + serverLevel.getRandom().nextFloat() * 0.2F);
                return InteractionResult.CONSUME;
            }
            player.openMenu(press);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OilPressBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, KHBlockEntities.OIL_PRESS.get(), OilPressBlockEntity::serverTick);
    }
}
