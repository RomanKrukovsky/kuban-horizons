package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GenieAnchor;
import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.WishborneState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.UUID;

public class KubanJugBlockEntity extends BlockEntity {

    private static final int SCHEMA_VERSION = 1;

    private UUID genieId;
    private BlockPos lastKnownGeniePos;
    private GeniePersonality boundPersonality;

    public KubanJugBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.KUBAN_JUG.get(), pos, state);
    }

    public boolean hasGenie() {
        return genieId != null;
    }

    public KubanGenie getOrSummonGenie(ServerLevel level, Player player) {
        if (genieId == null) {
            // First time - create new genie and anchor it
            KubanGenie genie = new KubanGenie(KHEntities.KUBAN_GENIE.get(), level);
            genie.setPos(this.getBlockPos().getX() + 0.5, this.getBlockPos().getY() + 1.2, this.getBlockPos().getZ() + 0.5);
            genie.setOwner(player.getUUID());
            level.addFreshEntity(genie);
            this.genieId = genie.getUUID();
            GenieAnchor.admit(genie, level);
            this.boundPersonality = genie.getPersonality();
            this.setChanged();
            return genie;
        } else {
            // Try to find existing genie
            KubanGenie existing = (KubanGenie) level.getEntity(genieId);
            if (existing != null && existing.isAlive()) {
                // Teleport to jug
                existing.teleportTo(this.getBlockPos().getX() + 0.5, this.getBlockPos().getY() + 1.2, this.getBlockPos().getZ() + 0.5);
                return existing;
            } else {
                // Genie lost - recreate (rare)
                KubanGenie newGenie = new KubanGenie(KHEntities.KUBAN_GENIE.get(), level);
                newGenie.setPos(this.getBlockPos().getX() + 0.5, this.getBlockPos().getY() + 1.2, this.getBlockPos().getZ() + 0.5);
                newGenie.setOwner(player.getUUID());
                level.addFreshEntity(newGenie);
                this.genieId = newGenie.getUUID();
                GenieAnchor.admit(newGenie, level);
                this.setChanged();
                return newGenie;
            }
        }
    }

    public void onRemoved() {
        // Optional: release genie or keep anchored
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        if (genieId != null) {
            output.putUUID("GenieId", genieId);
        }
        if (lastKnownGeniePos != null) {
            output.putInt("LastX", lastKnownGeniePos.getX());
            output.putInt("LastY", lastKnownGeniePos.getY());
            output.putInt("LastZ", lastKnownGeniePos.getZ());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        int version = input.getIntOr("SchemaVersion", 0);
        if (version == SCHEMA_VERSION) {
            genieId = input.getUUID("GenieId").orElse(null);
            int x = input.getIntOr("LastX", 0);
            int y = input.getIntOr("LastY", 0);
            int z = input.getIntOr("LastZ", 0);
            lastKnownGeniePos = new BlockPos(x, y, z);
        }
    }
}
