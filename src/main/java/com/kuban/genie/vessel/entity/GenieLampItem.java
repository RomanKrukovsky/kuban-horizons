package com.kuban.genie.vessel.entity;

import com.kuban.genie.KubanGenie;
import com.kuban.genie.vessel.VesselKind;
import com.kuban.genie.vessel.VesselSchool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Living Genie Lamp - a portable vessel that can store and release genies.
 * When right-clicked, it can summon a genie or store an existing one.
 */
public class GenieLampItem extends Item {

    private static final String OWNER_TAG = "Owner";
    private static final String BOUND_GENIE_TAG = "BoundGenie";
    private static final String WISHES_STORED_TAG = "WishesStored";
    private static final String SCHOOL_TAG = "School";
    private static final String CREATION_TIME_TAG = "CreationTime";

    public GenieLampItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        // Check if lamp is bound to a genie
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(BOUND_GENIE_TAG)) {
            // Lamp has a genie - try to release it
            return releaseGenie(serverPlayer, stack, tag);
        } else {
            // Empty lamp - try to bind a genie
            return bindGenie(serverPlayer, stack);
        }
    }

    /**
     * Bind a genie to this lamp
     */
    private InteractionResultHolder<ItemStack> bindGenie(ServerPlayer player, ItemStack stack) {
        // Check if player has a genie selected
        VesselTracker tracker = KubanGenie.getVesselTracker();
        UUID boundGenieId = tracker.getBoundGenieId(player);

        if (boundGenieId == null) {
            player.sendSystemMessage(Component.literal("§cNo genie bound to you. Use /bindgenie first."));
            return InteractionResultHolder.fail(stack);
        }

        // Check if lamp already has a genie
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.hasUUID(BOUND_GENIE_TAG)) {
            player.sendSystemMessage(Component.literal("§cThis lamp already contains a genie!"));
            return InteractionResultHolder.fail(stack);
        }

        // Bind the genie to the lamp
        tag.putUUID(BOUND_GENIE_TAG, boundGenieId);
        tag.putUUID(OWNER_TAG, player.getUUID());
        tag.putString(SCHOOL_TAG, VesselSchool.ARCANE.getName());
        tag.putLong(CREATION_TIME_TAG, System.currentTimeMillis());
        tag.putInt(WISHES_STORED_TAG, 0);

        // Record event
        KubanGenie.getGenieMemory().recordEvent(
            player.getUUID(),
            "bound_genie_to_lamp",
            Map.of("genie", boundGenieId.toString())
        );

        player.sendSystemMessage(Component.literal("§aGenie bound to lamp!"));
        return InteractionResultHolder.success(stack);
    }

    /**
     * Release a genie from this lamp
     */
    private InteractionResultHolder<ItemStack> releaseGenie(ServerPlayer player, ItemStack stack, CompoundTag tag) {
        UUID genieId = tag.getUUID(BOUND_GENIE_TAG);
        UUID ownerId = tag.getUUID(OWNER_TAG);

        // Check ownership
        if (!ownerId.equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cThis is not your lamp!"));
            return InteractionResultHolder.fail(stack);
        }

        // Check if genie exists
        if (!KubanGenie.getGenieMemory().hasGenie(genieId)) {
            player.sendSystemMessage(Component.literal("§cBound genie no longer exists!"));
            stack.removeTagKey(BOUND_GENIE_TAG);
            return InteractionResultHolder.pass(stack);
        }

        // Release the genie
        tag.remove(BOUND_GENIE_TAG);

        // Record event
        KubanGenie.getGenieMemory().recordEvent(
            player.getUUID(),
            "released_genie_from_lamp",
            Map.of("genie", genieId.toString())
        );

        player.sendSystemMessage(Component.literal("§aGenie released from lamp!"));
        return InteractionResultHolder.success(stack);
    }

    /**
     * Get the bound genie ID
     */
    @Nullable
    public UUID getBoundGenie(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(BOUND_GENIE_TAG) ? tag.getUUID(BOUND_GENIE_TAG) : null;
    }

    /**
     * Check if lamp has a genie bound
     */
    public boolean hasGenie(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(BOUND_GENIE_TAG);
    }

    /**
     * Store a wish in the lamp
     */
    public boolean storeWish(ItemStack stack, String wishText) {
        CompoundTag tag = stack.getOrCreateTag();
        int wishes = tag.getInt(WISHES_STORED_TAG);

        if (wishes >= VesselKind.LAMP.getMaxWishes()) {
            return false;
        }

        // Store wish
        String wishesKey = WISHES_STORED_TAG + "_" + wishes;
        tag.putString(wishesKey, wishText);
        tag.putInt(WISHES_STORED_TAG, wishes + 1);
        return true;
    }

    /**
     * Get stored wishes
     */
    public List<String> getStoredWishes(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return List.of();
        }

        int wishes = tag.getInt(WISHES_STORED_TAG);
        ImmutableList.Builder<String> builder = ImmutableList.builder();

        for (int i = 0; i < wishes; i++) {
            String wishesKey = WISHES_STORED_TAG + "_" + i;
            if (tag.contains(wishesKey)) {
                builder.add(tag.getString(wishesKey));
            }
        }

        return builder.build();
    }

    /**
     * Clear stored wishes
     */
    public void clearWishes(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }

        int wishes = tag.getInt(WISHES_STORED_TAG);
        for (int i = 0; i < wishes; i++) {
            tag.remove(WISHES_STORED_TAG + "_" + i);
        }

        tag.putInt(WISHES_STORED_TAG, 0);
    }

    /**
     * Set the vessel school
     */
    public void setSchool(ItemStack stack, VesselSchool school) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(SCHOOL_TAG, school.getName());
    }

    /**
     * Get the vessel school
     */
    public VesselSchool getSchool(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return VesselSchool.ARCANE;
        }

        String schoolName = tag.getString(SCHOOL_TAG);
        return VesselSchool.byName(schoolName);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        boolean hasGenie = tag != null && tag.hasUUID(BOUND_GENIE_TAG);
        boolean hasOwner = tag != null && tag.hasUUID(OWNER_TAG);

        if (hasGenie) {
            UUID genieId = tag.getUUID(BOUND_GENIE_TAG);
            tooltip.add(Component.literal("§6Bound Genie: " + genieId.toString().substring(0, 8) + "...").withStyle(ChatFormatting.GOLD));
        }

        if (hasOwner) {
            UUID ownerId = tag.getUUID(OWNER_TAG);
            tooltip.add(Component.literal("§7Owner: " + ownerId.toString().substring(0, 8) + "...").withStyle(ChatFormatting.GRAY));
        }

        VesselSchool school = getSchool(stack);
        tooltip.add(Component.literal("§9School: " + school.getDisplayName()).withStyle(ChatFormatting.BLUE));

        int wishes = tag != null ? tag.getInt(WISHES_STORED_TAG) : 0;
        tooltip.add(Component.literal("§eWishes: " + wishes + "/" + VesselKind.LAMP.getMaxWishes())
            .withStyle(ChatFormatting.YELLOW));

        long creationTime = tag != null ? tag.getLong(CREATION_TIME_TAG) : 0;
        if (creationTime > 0) {
            tooltip.add(Component.literal("§8Created: " + new Date(creationTime)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        if (hasGenie(stack)) {
            return Component.literal("§6Living Genie Lamp").withStyle(ChatFormatting.GOLD);
        }
        return super.getName(stack);
    }

    /**
     * Get vessel kind
     */
    @Override
    public VesselKind getVesselKind() {
        return VesselKind.LAMP;
    }
}
