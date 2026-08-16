package genie.vessel;

import genie.capabilities.IGenieContainer;
import genie.genie.KubanGenie;
import net.minecraft.nbt.CompoundTag;
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
 * Player-owned magic lamp that persists across game sessions.
 * Can be summoned by the owner.
 */
public class PlayerGenieLampItem extends Item implements ICapabilityProvider {

    private static final String TAG_OWNER_ID = "owner_id";
    private static final String TAG_IS_SUMMONED = "is_summoned";

    public PlayerGenieLampItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && !level.isClientSide) {
            // Check if player is the owner
            if (isOwner(player, stack)) {
                // Summon or dismiss the genie
                if (stack.getTag() != null && stack.getTag().getBoolean(TAG_IS_SUMMONED)) {
                    // Dismiss genie
                    stack.getTag().putBoolean(TAG_IS_SUMMONED, false);
                    player.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_dismissed"));
                } else {
                    // Summon genie
                    stack.getTag().putBoolean(TAG_IS_SUMMONED, true);
                    player.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_summoned"));
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Check if player is the owner of this lamp
     */
    private boolean isOwner(Player player, ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_OWNER_ID)) {
            String ownerId = stack.getTag().getString(TAG_OWNER_ID);
            return ownerId.equals(player.getStringUUID());
        }
        return false;
    }

    /**
     * Set the owner of this lamp
     */
    public void setOwner(Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_OWNER_ID, player.getStringUUID());
        tag.putBoolean(TAG_IS_SUMMONED, false);
    }

    /**
     * Check if genie is currently summoned
     */
    public boolean isGenieSummoned(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_IS_SUMMONED);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.kuban_horizon.player_genie_lamp");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return LazyOptional.empty();
    }
}
