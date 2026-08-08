package dev.romankrukovsky.kubanhorizons.genie.runtime.transaction;

import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionPersistenceTest {
    @TempDir
    Path temp;

    @Test
    void manifestAndLedgerSurviveReopen() throws Exception {
        TransactionManifest manifest = new TransactionManifest(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                new RegionSelection("minecraft:overworld", BlockPos.ZERO, new BlockPos(3, 3, 3)),
                "1".repeat(64), "2".repeat(64), "3".repeat(64), Instant.EPOCH);
        TransactionManifestStore store = new TransactionManifestStore(temp.resolve("manifests"));
        store.publish(manifest);
        assertEquals(manifest, new TransactionManifestStore(temp.resolve("manifests"))
                .load(manifest.transactionId()).orElseThrow());

        CausalLedgerEntry entry = new CausalLedgerEntry(manifest.transactionId(), manifest.actorId(),
                manifest.targetSnapshotId(), manifest.beforeImageId(), "minecraft:overworld",
                Instant.EPOCH, TransactionOutcome.COMPLETED);
        CausalLedger ledger = new CausalLedger(temp.resolve("ledger.tsv"));
        ledger.append(entry);
        assertEquals(java.util.List.of(entry), new CausalLedger(temp.resolve("ledger.tsv")).readAll());
    }

    @Test
    void overlappingRegionLocksAreRejectedUntilRelease() {
        RegionLockManager locks = new RegionLockManager();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        RegionSelection region = new RegionSelection("minecraft:overworld", BlockPos.ZERO,
                new BlockPos(31, 10, 31));
        assertTrue(locks.acquire(first, region));
        org.junit.jupiter.api.Assertions.assertFalse(locks.acquire(second,
                new RegionSelection("minecraft:overworld", new BlockPos(16, 0, 16), new BlockPos(20, 5, 20))));
        locks.release(first);
        assertTrue(locks.acquire(second, region));
    }
}
