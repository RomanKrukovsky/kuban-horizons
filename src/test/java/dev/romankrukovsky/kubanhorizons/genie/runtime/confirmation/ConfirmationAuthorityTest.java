package dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation;

import dev.romankrukovsky.kubanhorizons.genie.runtime.capability.CapabilityRisk;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.RestorePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.selection.RegionSelection;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotId;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationAuthorityTest {
    @Test
    void confirmationIsActorBoundExpiringAndSingleUse() {
        ConfirmationAuthority authority = new ConfirmationAuthority();
        UUID actor = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-08T12:00:00Z");
        RestorePreview preview = preview(actor, now.plusSeconds(60));
        ConfirmedRestore confirmation = authority.issue(actor, preview, now);

        assertFalse(authority.consume(confirmation, UUID.randomUUID(), now));
        assertTrue(authority.consume(confirmation, actor, now));
        assertFalse(authority.consume(confirmation, actor, now));
    }

    @Test
    void forgedConfirmationCannotUseAnIssuedIdentifier() {
        ConfirmationAuthority authority = new ConfirmationAuthority();
        UUID actor = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-08T12:00:00Z");
        RestorePreview preview = preview(actor, now.plusSeconds(60));
        ConfirmedRestore issued = authority.issue(actor, preview, now);
        ConfirmedRestore forged = new ConfirmedRestore(issued.confirmationId(), preview, now, "0".repeat(64));

        assertFalse(authority.consume(forged, actor, now));
        assertTrue(authority.consume(issued, actor, now));
    }

    @Test
    void stalePreviewCannotBeConfirmed() {
        ConfirmationAuthority authority = new ConfirmationAuthority();
        UUID actor = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-08T12:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> authority.issue(actor, preview(actor, now), now));
    }

    private static RestorePreview preview(UUID actor, Instant expiresAt) {
        String digest = "a".repeat(64);
        return new RestorePreview(UUID.randomUUID(), new SnapshotId(UUID.randomUUID(), "home"),
                digest, actor, new RegionSelection("minecraft:overworld", BlockPos.ZERO, BlockPos.ZERO),
                1, 0, CapabilityRisk.LOW, digest, digest, expiresAt);
    }
}
