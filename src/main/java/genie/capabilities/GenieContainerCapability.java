package genie.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Capability system for genie containers.
 * Allows any entity or block to implement IGenieContainer.
 */
public class GenieContainerCapability {

    /**
     * The capability instance
     */
    public static final Capability<IGenieContainer> GENIE_CONTAINER_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

    /**
     * Register the capability
     */
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(IGenieContainer.class);
        }
    }

    /**
     * Serialize IGenieContainer to NBT
     */
    public static CompoundTag serialize(IGenieContainer container) {
        CompoundTag tag = new CompoundTag();
        tag.putString("vessel_kind", container.getVesselKind().name());
        tag.putString("vessel_school", container.getVesselSchool().name());
        return tag;
    }

    /**
     * Deserialize IGenieContainer from NBT
     */
    public static void deserialize(CompoundTag tag, IGenieContainer container) {
        if (tag.contains("vessel_kind")) {
            container.setVesselSchool(genie.vessel.VesselSchool.valueOf(tag.getString("vessel_school")));
        }
    }
}
