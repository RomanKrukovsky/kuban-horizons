package dev.romankrukovsky.kubanhorizons.genie.runtime.capability;

import dev.romankrukovsky.kubanhorizons.genie.runtime.plan.PlanGate;
import dev.romankrukovsky.kubanhorizons.genie.runtime.plan.WishPlan;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanGateTest {
    @Test
    void acceptsRegisteredCapabilityAndRejectsUnknownOrExpiredPlans() {
        CapabilityRegistry registry = new CapabilityRegistry();
        PlanGate gate = new PlanGate(registry);
        Instant now = Instant.parse("2026-08-08T12:00:00Z");
        UUID actor = UUID.randomUUID();
        WishPlan valid = new WishPlan(1, UUID.randomUUID(), actor,
                CapabilityRegistry.SNAPSHOT_CREATE, Map.of(
                        "name", "home",
                        "dimension", "minecraft:overworld",
                        "min", "0,64,0",
                        "max", "3,67,3"), now.plusSeconds(30));

        assertEquals(CapabilityRegistry.SNAPSHOT_CREATE, gate.validate(valid, now).capability().id());
        assertThrows(IllegalArgumentException.class, () -> gate.validate(
                new WishPlan(1, UUID.randomUUID(), actor, CapabilityId.parse("evil:command.run@1"),
                        Map.of(), now.plusSeconds(30)), now));
        assertThrows(IllegalArgumentException.class, () -> gate.validate(
                new WishPlan(1, UUID.randomUUID(), actor, CapabilityRegistry.SNAPSHOT_CREATE,
                        Map.of(), now), now));
        assertThrows(IllegalArgumentException.class, () -> gate.validate(
                new WishPlan(1, UUID.randomUUID(), actor, CapabilityRegistry.SNAPSHOT_CREATE,
                        Map.of("name", "home", "dimension", "minecraft:overworld",
                                "min", "0,64,0", "max", "3,67,3", "forceAdmin", "true"),
                        now.plusSeconds(30)), now));
    }

    @Test
    void capabilityIdsAreStrictAndVersioned() {
        assertEquals("genie:region.restore@1", CapabilityRegistry.REGION_RESTORE.serialized());
        assertThrows(IllegalArgumentException.class, () -> CapabilityId.parse("genie:restore"));
        assertThrows(IllegalArgumentException.class, () -> CapabilityId.parse("Genie:restore@1"));
        assertThrows(IllegalArgumentException.class, () -> new CapabilityId("genie", "restore", 0));
    }
}
