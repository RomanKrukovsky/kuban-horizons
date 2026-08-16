package genie.capabilities;

import genie.vessel.VesselKind;
import genie.vessel.VesselSchool;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface for entities or blocks that can contain a genie.
 * Implemented by vessels and other genie containers.
 */
public interface IGenieContainer extends ICapabilityProvider {

    /**
     * Get the vessel kind
     */
    VesselKind getVesselKind();

    /**
     * Get the vessel school of magic
     */
    VesselSchool getVesselSchool();

    /**
     * Set the vessel school
     */
    void setVesselSchool(VesselSchool school);

    /**
     * Get the maximum wish power this vessel can handle
     */
    int getMaxWishPower();

    /**
     * Check if this container can contain a genie
     */
    boolean canContainGenie();

    @Nonnull
    @Override
    default <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == genie.capabilities.GenieContainerCapability.GENIE_CONTAINER_CAPABILITY ?
            LazyOptional.of(() -> this).cast() : LazyOptional.empty();
    }
}
