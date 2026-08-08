package dev.romankrukovsky.kubanhorizons.genie.runtime.preview;

import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRisk;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;

/** Строит read-only preview и привязывает его к текущему состоянию области. */
public final class PreviewService {
    public RestorePreview preview(ServerLevel level, UUID actorId, RegionSnapshot snapshot,
                                  Instant now) throws IOException {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(snapshot, "snapshot");
        RegionSelection selection = snapshot.selection();
        if (!level.dimension().identifier().toString().equals(selection.dimension())) {
            throw new IllegalArgumentException("snapshot belongs to another dimension");
        }
        SnapshotService.SnapshotState current = SnapshotService.captureState(level, selection);
        int changedBlocks = 0;
        int changedBlockEntities = 0;
        for (int index = 0; index < snapshot.blocks().size(); index++) {
            RegionSnapshot.BlockRecord target = snapshot.blocks().get(index);
            RegionSnapshot.BlockRecord actual = current.blocks().get(index);
            if (!target.blockState().equals(actual.blockState())) {
                changedBlocks++;
            }
            if (!Objects.equals(target.blockEntity(), actual.blockEntity())) {
                changedBlockEntities++;
            }
        }
        String currentDigest = SnapshotService.digest(current);
        CapabilityRisk risk = selection.chunkCount() > 64 ? CapabilityRisk.HIGH
                : changedBlocks > 4096 ? CapabilityRisk.ELEVATED : CapabilityRisk.LOW;
        UUID previewId = UUID.randomUUID();
        Instant expiresAt = now.plus(Duration.ofMinutes(2));
        String previewDigest = sha256(previewId + "|" + snapshot.id().value() + "|"
                + snapshot.contentDigest() + "|" + actorId + "|" + currentDigest + "|"
                + changedBlocks + "|" + changedBlockEntities + "|" + risk + "|" + expiresAt);
        return new RestorePreview(previewId, snapshot.id(), snapshot.contentDigest(), actorId,
                selection, changedBlocks, changedBlockEntities, risk, currentDigest, previewDigest, expiresAt);
    }

    public boolean stillCurrent(ServerLevel level, RegionSnapshot snapshot, RestorePreview preview,
                                Instant now) throws IOException {
        if (!preview.expiresAt().isAfter(now) || !snapshot.contentDigest().equals(preview.snapshotDigest())) {
            return false;
        }
        return SnapshotService.digest(SnapshotService.captureState(level, snapshot.selection()))
                .equals(preview.currentStateDigest());
    }

    public static List<RegionSnapshot.BlockRecord> captureCurrent(ServerLevel level,
                                                                  RegionSelection selection) {
        return SnapshotService.captureBlocks(level, selection);
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
