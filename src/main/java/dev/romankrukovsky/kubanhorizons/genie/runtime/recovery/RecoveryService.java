package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Сервис retained undo (24h window).
 * Управляет жизненным циклом undo-записей: запись, очистка устаревших.
 */
public final class RecoveryService {

    private final RecoveryJournal journal;
    private final SnapshotStore snapshotStore;
    private final TransactionManifestStore manifests;

    public RecoveryService(RecoveryJournal journal, SnapshotStore snapshotStore, TransactionManifestStore manifests) {
        this.journal = journal;
        this.snapshotStore = snapshotStore;
        this.manifests = manifests;
    }

    /**
     * Очистить все записи старше 24 часов.
     * Реальная очистка снапшотов и манифестов, чьи транзакции истекли.
     */
    public void cleanupExpiredUndo() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        journal.pruneExpired(cutoff);

        // Очищаем устаревшие снапшоты
        snapshotStore.pruneOlderThan(cutoff);

        // Очищаем устаревшие манифесты транзакций
        manifests.pruneExpired(cutoff);
    }

    public RecoveryJournal journal() {
        return journal;
    }

    /**
     * Восстановление после рестарта: очистка устаревших undo-записей.
     */
    public void recover(net.minecraft.server.MinecraftServer server) {
        cleanupExpiredUndo();
    }
}
