package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Журнал восстановления для retained undo (24h window).
 * Записывает события undo для возможности отката в течение 24 часов.
 */
public final class RecoveryJournal {

    private final Path journalPath;

    public RecoveryJournal(Path journalPath) {
        this.journalPath = journalPath;
        try {
            Files.createDirectories(journalPath.getParent());
            if (!Files.exists(journalPath)) {
                Files.createFile(journalPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize RecoveryJournal at " + journalPath, e);
        }
    }

    /**
     * Записать событие undo для conditional wish.
     */
    public void recordConditionalWishUndo(UUID actorId, String condition, String action, boolean existed) {
        String line = String.format("%s|CONDITIONAL_WISH_UNDO|%s|%s|%s|%s|%s%n",
                Instant.now().toString(),
                actorId.toString(),
                condition,
                action,
                existed ? "existed" : "absent",
                "24h_retained");
        try {
            Files.writeString(journalPath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to RecoveryJournal", e);
        }
    }

    /**
     * Сканировать журнал и вернуть все записи.
     */
    public List<String> scan() {
        try {
            if (!Files.exists(journalPath)) {
                return List.of();
            }
            return Files.readAllLines(journalPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Очистить устаревшие записи (старше 24 часов).
     */
    public void pruneExpired(Instant cutoff) {
        try {
            if (!Files.exists(journalPath)) {
                return;
            }
            List<String> lines = Files.readAllLines(journalPath, StandardCharsets.UTF_8);
            List<String> kept = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", 2);
                if (parts.length > 0) {
                    try {
                        Instant ts = Instant.parse(parts[0]);
                        if (ts.isAfter(cutoff)) {
                            kept.add(line);
                        }
                    } catch (Exception ignored) {
                        kept.add(line); // keep unparseable lines
                    }
                }
            }
            Files.write(journalPath, kept, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            // Non-fatal
        }
    }
}
