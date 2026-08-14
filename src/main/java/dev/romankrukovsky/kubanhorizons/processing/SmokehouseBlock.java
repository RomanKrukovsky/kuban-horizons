package dev.romankrukovsky.kubanhorizons.processing;

import com.mojang.serialization.MapCodec;
import dev.romankrukovsky.kubanhorizons.blockentity.SmokehouseBlockEntity;
import dev.romankrukovsky.kubanhorizons.registry.KHBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Коптильня.
 *
 * <p>ПКМ с продуктом — положить (если есть рецепт копчения); ПКМ с дровами —
 * подбросить в топку; ПКМ пустой рукой — забрать последнее. Без GUI, как
 * сушильная рама и разделочный стол.</p>
 *
 * <p>Порядок разбора предмета в руке важен: <b>сначала проверяются дрова</b>.
 * Иначе бревно, для которого однажды появился бы рецепт копчения, попадало бы
 * в слот продукта вместо топки, и игрок не смог бы протопить коптильню тем,
 * что у него в руках.</p>
 *
 * <p>Состояние {@link #LIT} — не украшение: оно выставляется из
 * {@link SmokehouseBlockEntity#isLit()}, то есть светится и дымит ровно та
 * коптильня, которая действительно работает (есть дрова и есть продукт).
 * Игрок видит снабжение по блоку, не открывая меню, — устройство без GUI
 * обязано сообщать о себе внешним видом.</p>
 */
public class SmokehouseBlock extends BaseEntityBlock {
    public static final MapCodec<SmokehouseBlock> CODEC = simpleCodec(SmokehouseBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Работает ли коптильня: дрова горят и внутри есть продукт. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    public SmokehouseBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<SmokehouseBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (itemStack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SmokehouseBlockEntity smokehouse) {
            // Дрова — раньше продукта: топка приоритетнее слота.
            if (SmokehouseBlockEntity.isFuel(itemStack)) {
                if (smokehouse.addFuel(serverLevel, itemStack)) {
                    serverLevel.playSound(null, pos, SoundEvents.WOOD_PLACE,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
            if (smokehouse.insert(serverLevel, itemStack)) {
                serverLevel.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SmokehouseBlockEntity smokehouse) {
            ItemStack taken = smokehouse.removeLast(serverLevel);
            if (!taken.isEmpty()) {
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                serverLevel.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /** Дым из трубы, пока коптильня действительно топится. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.3D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.3D,
                    0.0D, 0.03D, 0.0D);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmokehouseBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, KHBlockEntities.SMOKEHOUSE.get(), SmokehouseBlockEntity::serverTick);
    }
}
