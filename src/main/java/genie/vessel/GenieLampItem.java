package genie.vessel;

import genie.GenieStateSnapshot;
import genie.WishborneState;
import genie.capabilities.IGenieContainer;
import genie.capabilities.GenieContainerCapability;
import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * Living magic lamp that can contain a genie.
 * Classic vessel type that grants wishes when rubbed.
 */
public class GenieLampItem extends Item implements ICapabilityProvider {

    private final VesselKind vesselKind = VesselKind.LAMP;
    private final VesselSchool defaultSchool = VesselSchool.LUMI;

    public GenieLampItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Direction direction = context.getClickedFace();

        // Try to place the lamp as a block
        if (player != null && !level.isClientSide) {
            BlockState blockState = level.getBlockState(pos);
            if (blockState.canBeReplaced() || level.isEmptyBlock(pos.relative(direction))) {
                // Place block logic would go here
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, net.minecraft.world.entity.LivingEntity target, InteractionHand hand) {
        if (target instanceof KubanGenie genie) {
            // Try to bind genie to this lamp
            if (!player.level().isClientSide) {
                genie.bindToVessel(stack, vesselKind, defaultSchool);
                if (player.getItemInHand(hand) == stack) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.kuban_horizon.genie_lamp");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return LazyOptional.empty();
    }

    /**
     * Get the vessel kind
     */
    public VesselKind getVesselKind() {
        return vesselKind;
    }

    /**
     * Get the default magic school
     */
    public VesselSchool getDefaultSchool() {
        return defaultSchool;
    }

    /**
     * Check if this vessel can contain a genie
     */
    public boolean canContainGenie() {
        return true;
    }

    /**
     * Get the maximum wish power this vessel can handle
     */
    public int getMaxWishPower() {
        return 100;
    }
}
