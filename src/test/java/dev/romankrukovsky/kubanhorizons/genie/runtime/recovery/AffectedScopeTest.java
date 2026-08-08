package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffectedScopeTest {
    @Test
    void normalizesBoundsAndTreatsTouchingChunksAsOverlap() {
        AffectedScope scope = new AffectedScope("minecraft:overworld", 5, 9, -2, -4);
        assertEquals(-2, scope.minChunkX());
        assertEquals(-4, scope.minChunkZ());
        assertEquals(5, scope.maxChunkX());
        assertEquals(9, scope.maxChunkZ());
        assertTrue(scope.overlaps(new AffectedScope("minecraft:overworld", 5, 9, 20, 30)));
        assertFalse(scope.overlaps(new AffectedScope("minecraft:the_nether", -2, -4, 5, 9)));
    }

    @Test
    void overlapIsSafeAtIntegerExtremes() {
        AffectedScope low = new AffectedScope("example:world", Integer.MIN_VALUE, 0, Integer.MIN_VALUE, 0);
        AffectedScope high = new AffectedScope("example:world", Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 0);
        assertFalse(low.overlaps(high));
    }

    @Test
    void validatesNamespacedDimension() {
        assertThrows(NullPointerException.class, () -> new AffectedScope(null, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new AffectedScope("overworld", 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new AffectedScope("Minecraft:overworld", 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new AffectedScope("minecraft:", 0, 0, 0, 0));
    }

    @Test
    void transactionStateOrdinalsRemainPersistedContract() {
        assertEquals(0, TransactionState.PREPARING.ordinal());
        assertEquals(1, TransactionState.PREPARED.ordinal());
        assertEquals(2, TransactionState.APPLYING.ordinal());
        assertEquals(3, TransactionState.VERIFYING.ordinal());
        assertEquals(4, TransactionState.ROLLING_BACK.ordinal());
        assertEquals(5, TransactionState.COMMITTED.ordinal());
        assertEquals(6, TransactionState.ROLLED_BACK.ordinal());
        assertEquals(7, TransactionState.FAILED_SAFE.ordinal());
        assertEquals(8, TransactionState.RETIRED.ordinal());
    }
}
