package genie.transaction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import genie.GenieAnchor;
import genie.wish.WishIntent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Persistent event logging system for wish transactions and causality tracking.
 * Records all wish-related events for recovery, analysis, and rollback purposes.
 */
public class CausalityLedger {
    private static final String LEDGER_DIR = "genie_ledger";
    private static final int MAX_ENTRIES_PER_FILE = 1000;
    private static final long MAX_RETENTION_MS = 24L * 60 * 60 * 1000; // 24 hours

    private final Path ledgerDirectory;
    private final Map<String, CausalLedgerEntry> recentEntries;
    private final AtomicLong totalEntries;
    private final Gson gson;

    public CausalityLedger(Path worldDirectory) {
        this.ledgerDirectory = worldDirectory.resolve(LEDGER_DIR);
        this.recentEntries = new ConcurrentHashMap<>();
        this.totalEntries = new AtomicLong(0);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(CausalLedgerEntry.class, new CausalLedgerEntry.Serializer())
            .create();

        initializeLedger();
    }

    /**
     * Initialize ledger directory and load existing entries
     */
    private void initializeLedger() {
        try {
            if (!Files.exists(ledgerDirectory)) {
                Files.createDirectories(ledgerDirectory);
            }
            cleanupOldEntries();
        } catch (IOException e) {
            System.err.println("Failed to initialize causality ledger: " + e.getMessage());
        }
    }

    /**
     * Record a transaction in the ledger
     */
    public void recordTransaction(TransactionManifest manifest) {
        CausalLedgerEntry entry = new CausalLedgerEntry(manifest);
        recentEntries.put(entry.getEntryId(), entry);
        totalEntries.incrementAndGet();
        saveEntry(entry);
    }

    /**
     * Record a wish event
     */
    public void recordWishEvent(ResourceLocation dimension, UUID playerId, WishIntent intent, BlockPos position, boolean success) {
        TransactionManifest manifest = new TransactionManifest(intent, playerId, dimension);
        manifest.setSuccess(success);
        manifest.setPosition(position);
        recordTransaction(manifest);
    }

    /**
     * Record anchor state change
     */
    public void recordAnchorStateChange(GenieAnchor anchor, GenieAnchor.AnchorState oldState, GenieAnchor.AnchorState newState) {
        TransactionManifest manifest = new TransactionManifest();
        manifest.setType("anchor_state_change");
        manifest.setTimestamp(System.currentTimeMillis());
        manifest.setAnchorId(anchor.getAnchorId());
        manifest.setOldState(oldState.name());
        manifest.setNewState(newState.name());
        recordTransaction(manifest);
    }

    /**
     * Get entry by ID
     */
    @Nullable
    public CausalLedgerEntry getEntry(String entryId) {
        CausalLedgerEntry entry = recentEntries.get(entryId);
        if (entry == null) {
            entry = loadEntry(entryId);
        }
        return entry;
    }

    /**
     * Get recent transactions
     */
    public List<CausalLedgerEntry> getRecentTransactions(int limit) {
        List<CausalLedgerEntry> entries = new ArrayList<>(recentEntries.values());
        entries.sort(Comparator.comparingLong(CausalLedgerEntry::getTimestamp).reversed());
        return entries.stream().limit(limit).toList();
    }

    /**
     * Search transactions by player
     */
    public List<CausalLedgerEntry> searchByPlayer(UUID playerId) {
        return recentEntries.values().stream()
            .filter(entry -> playerId.equals(entry.getPlayerId()))
            .sorted(Comparator.comparingLong(CausalLedgerEntry::getTimestamp).reversed())
            .toList();
    }

    /**
     * Search transactions by dimension
     */
    public List<CausalLedgerEntry> searchByDimension(ResourceLocation dimension) {
        return recentEntries.values().stream()
            .filter(entry -> dimension.equals(entry.getDimension()))
            .sorted(Comparator.comparingLong(CausalLedgerEntry::getTimestamp).reversed())
            .toList();
    }

    /**
     * Cleanup old entries (older than 24h)
     */
    public void cleanupOldEntries() {
        long cutoff = System.currentTimeMillis() - MAX_RETENTION_MS;
        List<String> toRemove = new ArrayList<>();

        for (CausalLedgerEntry entry : recentEntries.values()) {
            if (entry.getTimestamp() < cutoff) {
                toRemove.add(entry.getEntryId());
            }
        }

        for (String entryId : toRemove) {
            recentEntries.remove(entryId);
            deleteEntry(entryId);
        }
    }

    /**
     * Save entry to disk
     */
    private void saveEntry(CausalLedgerEntry entry) {
        try {
            Path filePath = ledgerDirectory.resolve(entry.getEntryId() + ".json");
            String json = gson.toJson(entry);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            System.err.println("Failed to save ledger entry: " + e.getMessage());
        }
    }

    /**
     * Load entry from disk
     */
    @Nullable
    private CausalLedgerEntry loadEntry(String entryId) {
        try {
            Path filePath = ledgerDirectory.resolve(entryId + ".json");
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                return gson.fromJson(json, CausalLedgerEntry.class);
            }
        } catch (IOException e) {
            System.err.println("Failed to load ledger entry " + entryId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Delete entry from disk
     */
    private void deleteEntry(String entryId) {
        try {
            Path filePath = ledgerDirectory.resolve(entryId + ".json");
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Failed to delete ledger entry: " + e.getMessage());
        }
    }

    /**
     * Get total number of recorded entries
     */
    public long getTotalEntries() {
        return totalEntries.get();
    }

    /**
     * Get ledger directory path
     */
    public Path getLedgerDirectory() {
        return ledgerDirectory;
    }

    /**
     * Configuration for ledger performance
     */
    public static class LedgerConfig {
        public int maxEntriesPerFile = MAX_ENTRIES_PER_FILE;
        public long maxRetentionHours = 24;
        public boolean enableCompression = true;
        public int cleanupIntervalMinutes = 60;
    }
}
