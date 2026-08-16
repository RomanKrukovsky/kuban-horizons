package genie.vessel;

import genie.capabilities.IGenieContainer;
import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * Block entity for Kuban jug that can contain a genie.
 * Handles genie storage, capability management, and network synchronization.
 */
public class KubanJugBlockEntity extends BlockEntity implements IGenieContainer {

    private static final String TAG_CONTAINED_GENIE = "contained_genie";
    private static final String TAG_GENIE_NBT = "genie_nbt";

    @Nullable
    private KubanGenie containedGenie;
    private CompoundTag genieNBT;
    private VesselKind vesselKind = VesselKind.JUG;
    private VesselSchool vesselSchool = VesselSchool.GEO;

    private final LazyOptional<IGenieContainer> capabilityHolder = LazyOptional.of(() -> this);

    public KubanJugBlockEntity(BlockPos pos, BlockState state) {
        super(genie.vessel.VesselBlockEntities.KUBAN_JUG.get(), pos, state);
        this.genieNBT = new CompoundTag();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_GENIE_NBT)) {
            this.genieNBT = tag.getCompound(TAG_GENIE_NBT);
            // Genie will be loaded when accessed
        }
        if (tag.contains("vessel_kind")) {
            this.vesselKind = VesselKind.valueOf(tag.getString("vessel_kind"));
        }
        if (tag.contains("vessel_school")) {
            this.vesselSchool = VesselSchool.valueOf(tag.getString("vessel_school"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.genieNBT != null && !this.genieNBT.isEmpty()) {
            tag.put(TAG_GENIE_NBT, this.genieNBT);
        }
        tag.putString("vessel_kind", this.vesselKind.name());
        tag.putString("vessel_school", this.vesselSchool.name());
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (this.genieNBT != null && !this.genieNBT.isEmpty()) {
            tag.put(TAG_GENIE_NBT, this.genieNBT);
        }
        tag.putString("vessel_kind", this.vesselKind.name());
        tag.putString("vessel_school", this.vesselSchool.name());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.handleUpdateTag(tag);
        }
    }

    /**
     * Bind a genie from an item to this jug
     */
    @Nullable
    public KubanGenie bindGenieFromItem(ItemStack itemStack) {
        // Implementation would extract genie from item
        // For now, create a new genie instance
        this.containedGenie = new KubanGenie(this.level, this.worldPosition);
        this.genieNBT = new CompoundTag();
        this.containedGenie.saveToNBT(this.genieNBT);
        this.setChanged();
        return this.containedGenie;
    }

    /**
     * Release the contained genie as an item
     */
    @Nullable
    public ItemStack releaseGenie() {
        if (this.containedGenie != null) {
            ItemStack itemStack = new ItemStack(genie.vessel.GenieLampItem.INSTANCE);
            CompoundTag tag = new CompoundTag();
            this.containedGenie.saveToNBT(tag);
            itemStack.setTag(tag);
            this.containedGenie = null;
            this.genieNBT = new CompoundTag();
            this.setChanged();
            return itemStack;
        }
        return null;
    }

    /**
     * Get the contained genie
     */
    @Nullable
    public KubanGenie getContainedGenie() {
        if (this.containedGenie == null && this.genieNBT != null && !this.genieNBT.isEmpty()) {
            this.containedGenie = KubanGenie.loadFromNBT(this.genieNBT, this.level);
        }
        return this.containedGenie;
    }

    /**
     * Check if this jug contains a genie
     */
    public boolean hasGenie() {
        return this.containedGenie != null || (this.genieNBT != null && !this.genieNBT.isEmpty());
    }

    @Override
    public VesselKind getVesselKind() {
        return this.vesselKind;
    }

    @Override
    public VesselSchool getVesselSchool() {
        return this.vesselSchool;
    }

    @Override
    public void setVesselSchool(VesselSchool school) {
        this.vesselSchool = school;
        this.setChanged();
    }

    @Override
    public int getMaxWishPower() {
        return 80;
    }

    @Override
    public boolean canContainGenie() {
        return true;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, capabilityHolder);
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capabilityHolder.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        capabilityHolder = LazyOptional.of(() -> this);
    }
}
