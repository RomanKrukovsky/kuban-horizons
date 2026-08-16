package genie.vessel;

import genie.capabilities.IGenieContainer;
import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * Kuban jug decorative block that can contain a genie.
 * Has special properties for water storage and granting wishes.
 */
public class KubanJugBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    public KubanJugBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof KubanJugBlockEntity jugEntity) {
            // Try to bind genie to this jug
            if (heldItem.getItem() instanceof GenieLampItem lampItem) {
                KubanGenie genie = jugEntity.getContainedGenie();
                if (genie == null) {
                    // Container is empty, try to bind
                    genie = jugEntity.bindGenieFromItem(heldItem);
                    if (genie != null) {
                        player.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_bound_to_jug"));
                        if (!player.isCreative()) {
                            heldItem.shrink(1);
                        }
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    // Container has genie, try to release
                    ItemStack releasedItem = jugEntity.releaseGenie();
                    if (releasedItem != null) {
                        player.setItemInHand(hand, releasedItem);
                        player.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_released_from_jug"));
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KubanJugBlockEntity(pos, state);
    }

    /**
     * Get the vessel kind for this block
     */
    public VesselKind getVesselKind() {
        return VesselKind.JUG;
    }

    /**
     * Get the maximum wish power this vessel can handle
     */
    public int getMaxWishPower() {
        return 80;
    }
}
