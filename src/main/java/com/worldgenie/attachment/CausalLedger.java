package com.worldgenie.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CausalLedger tracks all state changes caused by wishes.
 * Each entry stores the before/after CompoundTag state deltas,
 * enabling rollback functionality for ROLLBACK_LAST_WISH.
 *
 * Uses old MC 1.20.6 CompoundTag for state storage.
 */
public record CausalLedger(List<CausalEntry> entries) {

    public static final Codec<CausalLedger> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CausalEntry.CODEC.listOf().fieldOf("entries")
                            .forGetter(CausalLedger::entries)
            ).apply(instance, CausalLedger::new)
    );

    /**
     * Creates a new ledger with an additional entry.
     */
    public CausalLedger withEntry(CausalEntry entry) {
        List<CausalEntry> newEntries = new ArrayList<>(this.entries);
        newEntries.add(entry);
        return new CausalLedger(newEntries);
    }

    /**
     * Marks a specific entry as rolled back (for ROLLBACK_LAST_WISH).
     */
    public CausalLedger markAsRolledBack(UUID entryId) {
        List<CausalEntry> newEntries = new ArrayList<>();
        for (CausalEntry entry : this.entries) {
            if (entry.id().equals(entryId)) {
                newEntries.add(entry.withRollbackChoice(RollbackChoice.ROLLBACK_LAST_WISH));
            } else {
                newEntries.add(entry);
            }
        }
        return new CausalLedger(newEntries);
    }

    /**
     * Returns the most recent entry that has not been rolled back.
     */
    public Optional<CausalEntry> getLastNonRolledBackEntry() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            CausalEntry entry = entries.get(i);
            if (entry.rollbackChoice() == RollbackChoice.NONE) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a wish was rolled back.
     */
    public boolean isWishRolledBack(UUID wishId) {
        return entries.stream()
                .anyMatch(e -> e.wishId().equals(wishId) &&
                        e.rollbackChoice() == RollbackChoice.ROLLBACK_LAST_WISH);
    }

    /**
     * Returns all entries that have not been rolled back.
     */
    public List<CausalEntry> getActiveEntries() {
        List<CausalEntry> active = new ArrayList<>();
        for (CausalEntry entry : entries) {
            if (entry.rollbackChoice() == RollbackChoice.NONE) {
                active.add(entry);
            }
        }
        return active;
    }

    /**
     * Returns the number of active (non-rolled-back) entries.
     */
    public int getActiveEntryCount() {
        return getActiveEntries().size();
    }
}
