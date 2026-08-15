package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GenieAnchor;
import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.WishborneState;
import dev.romankrukovsky.kubanhorizons.registry.KHBlockEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import net.minecraft.core.BlockPos;
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
        KubanGenie anchored = GenieAnchor.find(level.getServer());
        if (anchored != null && anchored.isAlive()) {
            anchored.teleportTo(this.getBlockPos().getX() + 0.5, this.getBlockPos().getY() + 1.2, this.getBlockPos().getZ() + 0.5);
            this.genieId = anchored.getUUID();
            this.setChanged();
            return anchored;
        }

        KubanGenie genie = new KubanGenie(KHEntities.KUBAN_GENIE.get(), level);
        genie.setPos(this.getBlockPos().getX() + 0.5, this.getBlockPos().getY() + 1.2, this.getBlockPos().getZ() + 0.5);
        genie.setOwnerId(player.getUUID());
        level.addFreshEntity(genie);

        boolean accepted = GenieAnchor.admit(genie, level);
        if (!accepted) {
            genie.discard();
            return null;
        }

        this.genieId = genie.getUUID();
        this.boundPersonality = genie.personality();
        this.setChanged();
        return genie;
    }

    public void onRemoved() {
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        if (genieId != null) {
            output.putString("GenieId", genieId.toString());
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
            String id = input.getStringOr("GenieId", "");
            genieId = id.isEmpty() ? null : UUID.fromString(id);
            int x = input.getIntOr("LastX", 0);
            int y = input.getIntOr("LastY", 0);
            int z = input.getIntOr("LastZ", 0);
            lastKnownGeniePos = new BlockPos(x, y, z);
        }
    }
}
