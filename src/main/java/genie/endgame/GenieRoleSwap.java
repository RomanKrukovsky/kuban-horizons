package genie.endgame;

import genie.KubanGenie;
import genie.events.GenieEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * System for swapping roles between players and genies.
 * Allows players to temporarily or permanently exchange roles with their genie companions.
 */
@Mod.EventBusSubscriber(modid = KubanGenie.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GenieRoleSwap {

    // Role swap cooldowns
    private static final Map<UUID, Long> roleSwapCooldowns = new HashMap<>();
    private static final long ROLE_SWAP_COOLDOWN = 20 * 60 * 10; // 10 minutes

    // Active role swaps
    private static final Map<UUID, UUID> activeRoleSwaps = new HashMap<>(); // playerId -> genieId
    private static final Map<UUID, UUID> reverseRoleSwaps = new HashMap<>(); // genieId -> playerId

    /**
     * Check if player can perform a role swap
     */
    public static boolean canRoleSwap(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // Check cooldown
        Long cooldownEnd = roleSwapCooldowns.get(playerId);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            return false;
        }

        // Check if player has a genie
        return hasGenie(player);
    }

    /**
     * Check if player has a genie companion
     */
    private static boolean hasGenie(ServerPlayer player) {
        // TODO: Implement actual genie detection
        // This would check if player has a genie vessel or summoned genie
        return player.getInventory().items.stream()
            .anyMatch(stack -> stack.getItem() instanceof genie.vessel.GenieLampItem);
    }

    /**
     * Perform a role swap between player and their genie
     */
    public static boolean swapRoles(ServerPlayer player) {
        if (!canRoleSwap(player)) {
            return false;
        }

        UUID playerId = player.getUUID();

        // Check if already swapped
        if (activeRoleSwaps.containsKey(playerId)) {
            return false;
        }

        // Find genie to swap with
        UUID genieId = findGenieForSwap(player);
        if (genieId == null) {
            return false;
        }

        // Perform the swap
        activeRoleSwaps.put(playerId, genieId);
        reverseRoleSwaps.put(genieId, playerId);

        // Set cooldown
        roleSwapCooldowns.put(playerId, System.currentTimeMillis() + ROLE_SWAP_COOLDOWN);

        // Post event
        GenieEvents.ROLE_SWAP_PERFORMED.post(
            new RoleSwapPerformedEvent(playerId, genieId)
        );

        KubanGenie.LOGGER.info("Player {} swapped roles with genie", player.getName().getString());
        return true;
    }

    /**
     * Find a suitable genie for role swap
     */
    private static UUID findGenieForSwap(ServerPlayer player) {
        // TODO: Implement actual genie finding logic
        // This would search for nearby genies or the player's summoned genie

        // For now, return a dummy UUID
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    /**
     * Complete the role swap - actually exchange control
     */
    public static boolean completeRoleSwap(UUID playerId, UUID genieId) {
        if (!activeRoleSwaps.containsKey(playerId)) {
            return false;
        }

        // TODO: Implement actual role exchange
        // This would:
        // 1. Transfer player control to genie
        // 2. Transfer genie control to player
        // 3. Update visual representations
        // 4. Swap abilities and inventory

        // For now, just log the event
        KubanGenie.LOGGER.info("Completing role swap between player and genie");
        return true;
    }

    /**
     * Revert a role swap
     */
    public static boolean revertRoleSwap(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (!activeRoleSwaps.containsKey(playerId)) {
            return false;
        }

        UUID genieId = activeRoleSwaps.get(playerId);
        activeRoleSwaps.remove(playerId);
        reverseRoleSwaps.remove(genieId);

        // Post event
        GenieEvents.ROLE_SWAP_REVERTED.post(
            new RoleSwapRevertedEvent(playerId, genieId)
        );

        KubanGenie.LOGGER.info("Reverted role swap between player and genie");
        return true;
    }

    /**
     * Check if a player is currently in swapped role
     */
    public static boolean isRoleSwapped(UUID playerId) {
        return activeRoleSwaps.containsKey(playerId);
    }

    /**
     * Get the genie ID that player is swapped with
     */
    public static UUID getSwappedGenieId(UUID playerId) {
        return activeRoleSwaps.get(playerId);
    }

    /**
     * Get the player ID that a genie is swapped with
     */
    public static UUID getSwappedPlayerId(UUID genieId) {
        return reverseRoleSwaps.get(genieId);
    }

    /**
     * Check if an entity is currently in a swapped role
     */
    public static boolean isInSwappedRole(UUID entityId) {
        return activeRoleSwaps.containsKey(entityId) || reverseRoleSwaps.containsKey(entityId);
    }

    /**
     * Update all active role swaps
     */
    public static void updateRoleSwaps() {
        // TODO: Implement actual role swap updates
    }

    /**
     * Event fired when a role swap is performed
     */
    public static class RoleSwapPerformedEvent {
        public final UUID playerId;
        public final UUID genieId;

        public RoleSwapPerformedEvent(UUID playerId, UUID genieId) {
            this.playerId = playerId;
            this.genieId = genieId;
        }
    }

    /**
     * Event fired when a role swap is reverted
     */
    public static class RoleSwapRevertedEvent {
        public final UUID playerId;
        public final UUID genieId;

        public RoleSwapRevertedEvent(UUID playerId, UUID genieId) {
            this.playerId = playerId;
            this.genieId = genieId;
        }
    }

    // Event handlers

    @SubscribeEvent
    public static void onPlayerTick(PlayerEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer player && event.phase == PlayerEvent.Phase.END) {
            updateRoleSwaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer clone) {
            UUID playerId = original.getUUID();

            // Transfer role swap data to cloned player
            if (activeRoleSwaps.containsKey(playerId)) {
                UUID genieId = activeRoleSwaps.get(playerId);
                activeRoleSwaps.remove(playerId);
                activeRoleSwaps.put(clone.getUUID(), genieId);
            }

            if (reverseRoleSwaps.containsKey(playerId)) {
                UUID swappedPlayerId = reverseRoleSwaps.get(playerId);
                reverseRoleSwaps.remove(playerId);
                reverseRoleSwaps.put(clone.getUUID(), swappedPlayerId);
            }

            if (roleSwapCooldowns.containsKey(playerId)) {
                Long cooldown = roleSwapCooldowns.get(playerId);
                roleSwapCooldowns.remove(playerId);
                roleSwapCooldowns.put(clone.getUUID(), cooldown);
            }
        }
    }

    /**
     * Enable role swap ability for a player
     */
    public static void enableRoleSwapAbility(ServerPlayer player) {
        KubanGenie.LOGGER.info("Enabled role swap ability for player {}", player.getName().getString());
    }

    /**
     * Disable role swap ability for a player
     */
    public static void disableRoleSwapAbility(ServerPlayer player) {
        KubanGenie.LOGGER.info("Disabled role swap ability for player {}", player.getName().getString());
    }

    /**
     * Check if player has role swap ability
     */
    public static boolean hasRoleSwapAbility(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    /**
     * Get all active role swaps
     */
    public static Map<UUID, UUID> getActiveRoleSwaps() {
        return new HashMap<>(activeRoleSwaps);
    }

    /**
     * Clear all active role swaps (admin function)
     */
    public static void clearAllRoleSwaps() {
        activeRoleSwaps.clear();
        reverseRoleSwaps.clear();
        KubanGenie.LOGGER.info("All active role swaps have been cleared");
    }
}