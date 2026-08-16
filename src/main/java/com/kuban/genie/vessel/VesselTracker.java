package com.kuban.genie.vessel;

import com.kuban.genie.KubanGenie;
import com.kuban.genie.memory.WorldGenieMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Tracks vessel ownership and selection across players.
 * Manages which vessel a player has selected and handles vessel binding.
 */
@Mod.EventBusSubscriber(modid = KubanGenie.MODID)
public class VesselTracker {

    private static final String SELECTED_VESSEL_TAG = "SelectedVessel";
    private static final String BOUND_VESSELS_TAG = "BoundVessels";
    private static final int MAX_BOUND_VESSELS = 5;

    private final Map<UUID, VesselBinding> playerBindings = new HashMap<>();

    /**
     * Player vessel binding data
     */
    public static class VesselBinding implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private VesselKind selectedVessel = VesselKind.LAMP;
        private final List<ResourceLocation> boundVessels = new ArrayList<>();
        private UUID boundGenieId;

        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            return cap == KubanGenieCapabilities.VESSEL_TRACKER ? LazyOptional.of(() -> this).cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("SelectedVessel", selectedVessel.getName());
            tag.putInt("BoundVesselsCount", boundVessels.size());

            for (int i = 0; i < boundVessels.size(); i++) {
                tag.putString("BoundVessel_" + i, boundVessels.get(i).toString());
            }

            if (boundGenieId != null) {
                tag.putUUID("BoundGenieId", boundGenieId);
            }

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            selectedVessel = VesselKind.byName(tag.getString("SelectedVessel"));
            int count = tag.getInt("BoundVesselsCount");

            boundVessels.clear();
            for (int i = 0; i < count; i++) {
                String vesselId = tag.getString("BoundVessel_" + i);
                boundVessels.add(new ResourceLocation(vesselId));
            }

            if (tag.hasUUID("BoundGenieId")) {
                boundGenieId = tag.getUUID("BoundGenieId");
            }
        }

        public VesselKind getSelectedVessel() {
            return selectedVessel;
        }

        public void setSelectedVessel(VesselKind vessel) {
            this.selectedVessel = vessel;
        }

        public List<ResourceLocation> getBoundVessels() {
            return boundVessels;
        }

        public boolean addBoundVessel(ResourceLocation vesselId) {
            if (boundVessels.size() >= MAX_BOUND_VESSELS) {
                return false;
            }
            return boundVessels.add(vesselId);
        }

        public boolean removeBoundVessel(ResourceLocation vesselId) {
            return boundVessels.remove(vesselId);
        }

        public boolean hasBoundVessel(ResourceLocation vesselId) {
            return boundVessels.contains(vesselId);
        }

        public UUID getBoundGenieId() {
            return boundGenieId;
        }

        public void setBoundGenieId(UUID genieId) {
            this.boundGenieId = genieId;
        }

        public boolean isVesselBound() {
            return boundGenieId != null;
        }
    }

    /**
     * Get vessel tracker for a player
     */
    public VesselBinding getPlayerBinding(Player player) {
        return playerBindings.computeIfAbsent(player.getUUID(), k -> new VesselBinding());
    }

    /**
     * Select a vessel type for the player
     */
    public boolean selectVesselType(Player player, VesselKind vessel) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        VesselBinding binding = getPlayerBinding(serverPlayer);
        binding.setSelectedVessel(vessel);

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "selected_vessel_type",
            Map.of("vessel", vessel.getName())
        );

        return true;
    }

    /**
     * Bind a vessel to a genie
     */
    public boolean bindVesselToGenie(Player player, ResourceLocation vesselId, UUID genieId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        VesselBinding binding = getPlayerBinding(serverPlayer);
        if (!binding.addBoundVessel(vesselId)) {
            return false;
        }

        binding.setBoundGenieId(genieId);

        // Record event
        WorldGenieMemory memory = KubanGenie.getGenieMemory();
        memory.recordEvent(
            serverPlayer.getUUID(),
            "bound_vessel_to_genie",
            Map.of("vessel", vesselId.toString(), "genie", genieId.toString())
        );

        return true;
    }

    /**
     * Unbind a vessel from a genie
     */
    public boolean unbindVessel(Player player, ResourceLocation vesselId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        VesselBinding binding = getPlayerBinding(serverPlayer);
        if (binding.removeBoundVessel(vesselId)) {
            if (binding.getBoundGenieId() != null && binding.getBoundVessels().isEmpty()) {
                binding.setBoundGenieId(null);
            }
            return true;
        }

        return false;
    }

    /**
     * Get selected vessel for player
     */
    public VesselKind getSelectedVessel(Player player) {
        VesselBinding binding = getPlayerBinding(player);
        return binding.getSelectedVessel();
    }

    /**
     * Get bound vessels for player
     */
    public List<ResourceLocation> getBoundVessels(Player player) {
        VesselBinding binding = getPlayerBinding(player);
        return binding.getBoundVessels();
    }

    /**
     * Check if player has a vessel bound
     */
    public boolean hasBoundVessel(Player player) {
        VesselBinding binding = getPlayerBinding(player);
        return binding.isVesselBound();
    }

    /**
     * Get bound genie ID
     */
    @Nullable
    public UUID getBoundGenieId(Player player) {
        VesselBinding binding = getPlayerBinding(player);
        return binding.getBoundGenieId();
    }

    /**
     * Save all player bindings
     */
    public CompoundTag saveAllBindings() {
        CompoundTag tag = new CompoundTag();
        int index = 0;

        for (Map.Entry<UUID, VesselBinding> entry : playerBindings.entrySet()) {
            CompoundTag bindingTag = entry.getValue().serializeNBT();
            tag.put("binding_" + index++, bindingTag);
        }

        tag.putInt("count", playerBindings.size());
        return tag;
    }

    /**
     * Load player bindings
     */
    public void loadAllBindings(CompoundTag tag) {
        playerBindings.clear();
        int count = tag.getInt("count");

        for (int i = 0; i < count; i++) {
            CompoundTag bindingTag = tag.getCompound("binding_" + i);
            VesselBinding binding = new VesselBinding();
            binding.deserializeNBT(bindingTag);
            playerBindings.put(binding.getBoundGenieId() != null ? binding.getBoundGenieId() : UUID.randomUUID(), binding);
        }
    }

    /**
     * Get vessel statistics
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        int totalBound = 0;
        int totalPlayers = playerBindings.size();

        for (VesselBinding binding : playerBindings.values()) {
            totalBound += binding.getBoundVessels().size();
        }

        stats.put("total_players_with_vessels", totalPlayers);
        stats.put("total_bound_vessels", totalBound);
        stats.put("max_vessels_per_player", MAX_BOUND_VESSELS);
        return stats;
    }

    /**
     * Handle player login - initialize vessel tracker
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        VesselBinding binding = getPlayerBinding(player);

        // Set default vessel if none selected
        if (binding.getSelectedVessel() == VesselKind.LAMP) {
            KubanGenie.LOGGER.info("Player {} logged in with default vessel", player.getName().getString());
        }
    }

    /**
     * Handle player logout
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Cleanup can happen here if needed
    }
}
