package dev.romankrukovsky.kubanhorizons.genie.history;

import dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.genie.runtime.snapshot.SnapshotService;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.CausalLedgerEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Способность «А что если?» — альтернативные версии мира из причинного индекса.
 *
 * <p>Способность ничего не меняет: она находит в {@code CausalLedger} последнюю
 * отменённую (или последнюю исполненную) транзакцию владельца, загружает её
 * целевой снимок — мир, каким он стал бы, если бы желание осталось, — и
 * сравнивает его с текущим миром. Итог — описательный отчёт: сколько блоков
 * и какие именно отличались бы.</p>
 */
public final class AlternativeCausalityEngine {
    private static final int EXAMPLE_LIMIT = 3;

    private AlternativeCausalityEngine() {
    }

    public static record WhatIfResult(UUID wishId, String wishText, String actualOutcome,
                                      String alternativeOutcome, int changedBlocks, long gameTime) {
        public WhatIfResult {
            Objects.requireNonNull(wishId, "wishId");
            Objects.requireNonNull(wishText, "wishText");
            Objects.requireNonNull(actualOutcome, "actualOutcome");
            Objects.requireNonNull(alternativeOutcome, "alternativeOutcome");
            if (changedBlocks < 0) {
                throw new IllegalArgumentException("changedBlocks must not be negative");
            }
        }
    }

    /**
     * Отвечает на «а что если?» для владельца: пустой результат — альтернативы нет.
     *
     * <p>Форма «что если бы я не отменил желание» смотрит на последнюю отменённую
     * транзакцию. Форма «сделать X вместо Y» — на целевую версию последнего
     * исполненного желания.</p>
     */
    public static Optional<WhatIfResult> whatIf(ServerLevel level, UUID ownerUuid, String query) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(ownerUuid, "ownerUuid");
        WishRuntime runtime = WishRuntime.get(level.getServer());
        List<CausalLedgerEntry> entries;
        try {
            entries = runtime.causalIndex();
        } catch (IOException exception) {
            return Optional.empty();
        }
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        boolean instead = containsAny(normalized, "вместо", "instead of", "rather than");
        Optional<CausalLedgerEntry> selected = instead
                ? mostRecentAlternative(entries, ownerUuid)
                : lastUndone(entries, ownerUuid);
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        CausalLedgerEntry entry = selected.get();
        if (!level.dimension().identifier().toString().equals(entry.dimension())) {
            return Optional.empty();
        }
        RegionSnapshot target;
        try {
            target = runtime.findSnapshot(entry.targetSnapshotId()).orElse(null);
        } catch (IOException exception) {
            return Optional.empty();
        }
        if (target == null || !target.ownerId().equals(ownerUuid)) {
            return Optional.empty();
        }
        try {
            return Optional.of(buildResult(level, entries, ownerUuid, entry, target, instead));
        } catch (IllegalStateException exception) {
            // Область снимка не загружена — сравнить нельзя, это не ошибка способности.
            return Optional.empty();
        }
    }

    /** Последняя транзакция владельца, чьи последствия затем были отменены. */
    private static Optional<CausalLedgerEntry> lastUndone(List<CausalLedgerEntry> entries,
                                                          UUID ownerUuid) {
        CausalLedgerEntry undone = null;
        for (int index = 0; index < entries.size(); index++) {
            CausalLedgerEntry candidate = entries.get(index);
            if (!candidate.actorId().equals(ownerUuid)) {
                continue;
            }
            for (int earlier = 0; earlier < index; earlier++) {
                CausalLedgerEntry before = entries.get(earlier);
                if (before.actorId().equals(ownerUuid)
                        && before.beforeImageId().equals(candidate.targetSnapshotId())) {
                    undone = before;
                }
            }
        }
        return Optional.ofNullable(undone);
    }

    /** Последняя запись владельца; если это отмена, возвращает отменённую транзакцию. */
    private static Optional<CausalLedgerEntry> mostRecentAlternative(List<CausalLedgerEntry> entries,
                                                                     UUID ownerUuid) {
        for (int index = entries.size() - 1; index >= 0; index--) {
            CausalLedgerEntry last = entries.get(index);
            if (!last.actorId().equals(ownerUuid)) {
                continue;
            }
            for (int earlier = 0; earlier < index; earlier++) {
                CausalLedgerEntry before = entries.get(earlier);
                if (before.actorId().equals(ownerUuid)
                        && before.beforeImageId().equals(last.targetSnapshotId())) {
                    return Optional.of(before);
                }
            }
            return Optional.of(last);
        }
        return Optional.empty();
    }

    private static WhatIfResult buildResult(ServerLevel level, List<CausalLedgerEntry> entries,
                                            UUID ownerUuid, CausalLedgerEntry entry,
                                            RegionSnapshot target, boolean instead) {
        SnapshotService.SnapshotState current = SnapshotService.captureState(level, target.selection());
        int changedBlocks = 0;
        List<String> examples = new ArrayList<>();
        BlockPos origin = target.selection().min();
        for (int index = 0; index < target.blocks().size(); index++) {
            RegionSnapshot.BlockRecord want = target.blocks().get(index);
            RegionSnapshot.BlockRecord have = current.blocks().get(index);
            if (!want.blockState().equals(have.blockState())) {
                changedBlocks++;
                if (examples.size() < EXAMPLE_LIMIT) {
                    BlockPos absolute = origin.offset(want.relativeX(), want.relativeY(),
                            want.relativeZ());
                    examples.add(have.blockState().getStringOr("Name", "minecraft:air")
                            + " -> " + want.blockState().getStringOr("Name", "minecraft:air")
                            + " at " + absolute.getX() + "," + absolute.getY() + "," + absolute.getZ());
                }
            }
        }
        boolean undone = !instead || isUndone(entries, ownerUuid, entry);
        String actualOutcome = undone
                ? "rolled back; the world returned to its previous state"
                : "executed; the world kept the change";
        String alternativeOutcome = examples.isEmpty()
                ? "no blocks would differ"
                : String.join("; ", examples);
        String name = target.id().name();
        String wishText = (name.startsWith("t_") || name.startsWith("u_"))
                ? (undone ? "the last undone wish" : "the last wish")
                : name;
        return new WhatIfResult(entry.transactionId(), wishText, actualOutcome,
                alternativeOutcome, changedBlocks, level.getGameTime());
    }

    /** Была ли транзакция позже отменена (её before-image восстановлен как чей-то target). */
    private static boolean isUndone(List<CausalLedgerEntry> entries, UUID ownerUuid,
                                    CausalLedgerEntry entry) {
        for (CausalLedgerEntry other : entries) {
            if (other.actorId().equals(ownerUuid)
                    && other.targetSnapshotId().equals(entry.beforeImageId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
