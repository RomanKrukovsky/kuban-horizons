package genie.vessel;

import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * System for selecting and binding vessels to owners.
 * Handles the vessel selection process and ownership transfer.
 */
public class VesselPull {

    /**
     * Attempt to bind a vessel to a player
     * @param player The player attempting to bind
     * @param vesselStack The vessel item stack
     * @param vesselKind The type of vessel
     * @return true if binding was successful
     */
    public static boolean bindVesselToPlayer(Player player, ItemStack vesselStack, VesselKind vesselKind) {
        if (player.level().isClientSide) {
            return false;
        }

        // Check if player already has a vessel
        VesselTracker tracker = VesselTracker.get(player.level());
        UUID existingVessel = tracker.getVesselOwnedByPlayer(player.getUUID());

        if (existingVessel != null) {
            player.sendSystemMessage(Component.translatable("message.kuban_horizon.already_has_vessel"));
            return false;
        }

        // Create new vessel registration
        UUID vesselId = UUID.randomUUID();
        VesselSchool school = getSchoolForVessel(vesselKind);

        tracker.registerVessel(vesselId, vesselKind, school, player.getUUID());

        // Bind the vessel to the player
        tracker.setVesselOwner(vesselId, player.getUUID());

        // Mark the item as bound
        vesselStack.getOrCreateTag().putUUID("vessel_id", vesselId);
        vesselStack.getOrCreateTag().putUUID("owner_id", player.getUUID());

        player.sendSystemMessage(Component.translatable("message.kuban_horizon.vessel_bound",
            Component.translatable("vessel.kind." + vesselKind.name().toLowerCase())));

        return true;
    }

    /**
     * Attempt to summon a vessel to player's location
     * @param player The player summoning the vessel
     * @param vesselId The vessel ID to summon
     * @return true if summoning was successful
     */
    public static boolean summonVessel(Player player, UUID vesselId) {
        if (player.level().isClientSide) {
            return false;
        }

        VesselTracker tracker = VesselTracker.get(player.level());
        VesselTracker.VesselData data = tracker.getVesselData(vesselId);

        if (data == null) {
            player.sendSystemMessage(Component.translatable("message.kuban_horizon.vessel_not_found"));
            return false;
        }

        if (!tracker.isPlayerOwner(player, vesselId)) {
            player.sendSystemMessage(Component.translatable("message.kuban_horizon.not_vessel_owner"));
            return false;
        }

        // Teleport vessel to player location
        BlockPos playerPos = player.blockPosition();
        tracker.updateVesselLocation(vesselId, playerPos);

        player.sendSystemMessage(Component.translatable("message.kuban_horizon.vessel_summoned"));
        return true;
    }

    /**
     * Transfer vessel ownership to another player
     * @param currentOwner The current owner
     * @param newOwner The new owner
     * @param vesselId The vessel ID
     * @return true if transfer was successful
     */
    public static boolean transferVesselOwnership(Player currentOwner, Player newOwner, UUID vesselId) {
        if (currentOwner.level().isClientSide) {
            return false;
        }

        VesselTracker tracker = VesselTracker.get(currentOwner.level());

        if (!tracker.isPlayerOwner(currentOwner, vesselId)) {
            currentOwner.sendSystemMessage(Component.translatable("message.kuban_horizon.not_vessel_owner"));
            return false;
        }

        // Check if new owner already has a vessel
        UUID existingVessel = tracker.getVesselOwnedByPlayer(newOwner.getUUID());
        if (existingVessel != null) {
            currentOwner.sendSystemMessage(Component.translatable("message.kuban_horizon.new_owner_has_vessel"));
            return false;
        }

        // Transfer ownership
        tracker.setVesselOwner(vesselId, newOwner.getUUID());

        currentOwner.sendSystemMessage(Component.translatable("message.kuban_horizon.vessel_transferred_to",
            newOwner.getName()));
        newOwner.sendSystemMessage(Component.translatable("message.kuban_horizon.vessel_received_from",
            currentOwner.getName()));

        return true;
    }

    /**
     * Get the appropriate school for a vessel type
     */
    private static VesselSchool getSchoolForVessel(VesselKind kind) {
        return switch (kind) {
            case LAMP -> VesselSchool.LUMI;
            case MIRROR -> VesselSchool.LUMI;
            case RING -> VesselSchool.AERO;
            case JUG -> VesselSchool.HYDRO;
            case MUSIC_BOX -> VesselSchool.GEO;
        };
    }

    /**
     * Check if a player can bind a new vessel
     */
    public static boolean canBindNewVessel(Player player) {
        VesselTracker tracker = VesselTracker.get(player.level());
        return tracker.getVesselOwnedByPlayer(player.getUUID()) == null;
    }

    /**
     * Get the vessel ID from an item stack
     */
    @Nullable
    public static UUID getVesselIdFromItem(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("vessel_id")) {
            return stack.getTag().getUUID("vessel_id");
        }
        return null;
    }

    /**
     * Check if an item stack is a bound vessel
     */
    public static boolean isBoundVessel(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("vessel_id");
    }
}
