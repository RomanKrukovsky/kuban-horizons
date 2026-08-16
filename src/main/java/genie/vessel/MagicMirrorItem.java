package genie.vessel;

import genie.capabilities.IGenieContainer;
import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * Magic mirror that acts as a smartphone replacement.
 * Can show visions, communicate across distances, and contain a genie.
 */
public class MagicMirrorItem extends Item implements ICapabilityProvider, IGenieContainer {

    private VesselKind vesselKind = VesselKind.MIRROR;
    private VesselSchool defaultSchool = VesselSchool.LUMI;

    public MagicMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Direction direction = context.getClickedFace();

        if (player != null && !level.isClientSide) {
            BlockPos pos = context.getClickedPos();

            // Try to place mirror as a block
            if (level.getBlockState(pos).canBeReplaced() || level.isEmptyBlock(pos.relative(direction))) {
                // Place block logic would go here
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Show a vision through the mirror
     */
    public void showVision(Player player, ItemStack stack, BlockPos targetPos) {
        if (player.level().isClientSide) {
            // Client-side vision display
            player.sendSystemMessage(Component.translatable("message.kuban_horizon.mirror_vision",
                targetPos.getX(), targetPos.getY(), targetPos.getZ()));
        }
    }

    /**
     * Communicate across distance
     */
    public void sendMessage(Player player, ItemStack stack, String message) {
        if (!player.level().isClientSide) {
            player.sendSystemMessage(Component.translatable("message.kuban_horizon.mirror_message", message));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.kuban_horizon.magic_mirror");
    }

    @Override
    public VesselKind getVesselKind() {
        return vesselKind;
    }

    @Override
    public VesselSchool getVesselSchool() {
        return defaultSchool;
    }

    @Override
    public void setVesselSchool(VesselSchool school) {
        this.defaultSchool = school;
    }

    @Override
    public int getMaxWishPower() {
        return 120;
    }

    @Override
    public boolean canContainGenie() {
        return true;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return LazyOptional.empty();
    }
}
