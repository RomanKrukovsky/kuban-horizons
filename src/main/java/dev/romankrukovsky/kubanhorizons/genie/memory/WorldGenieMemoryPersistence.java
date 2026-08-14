package dev.romankrukovsky.kubanhorizons.genie.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.CausalLedger;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.CausalLedgerEntry;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * JSON-based persistence layer for WorldGenieMemory.
 *
 * <p>Provides save/load to world saved data directory using JSON format.
 * Integrates with CausalLedger for transaction-aware persistence:
 * ledger entries can be recorded when memory state changes occur as part
 * of wish transactions.</p>
 *
 * <p>JSON format enables:
 * <ul>
 *   <li>Human-readable world memory inspection</li>
 *   <li>External tooling and analysis</li>
 *   <li>Version-controlled memory state</li>
 *   <li>Cross-world memory migration</li>
 * </ul>
 */
public final class WorldGenieMemoryPersistence {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String MEMORY_FILE = "world_genie_memory.json";
    private static final String LEDGER_FILE = "causal_ledger.json";

    private final Path saveDirectory;

    public WorldGenieMemoryPersistence(Path saveDirectory) {
        this.saveDirectory = Objects.requireNonNull(saveDirectory, "saveDirectory");
    }

    /**
     * Saves WorldGenieMemory state to JSON file in the save directory.
     */
    public void save(WorldGenieMemory memory) throws IOException {
        Objects.requireNonNull(memory, "memory");
        Files.createDirectories(saveDirectory);

        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", WorldGenieMemory.SCHEMA_VERSION);
        root.addProperty("savedAt", Instant.now().toString());

        // Core memory fields
        root.addProperty("firstDiscoveredPos", memory.firstDiscoveredPos().asLong());
        if (memory.firstOwnerId() != null) {
            root.addProperty("firstOwnerId", memory.firstOwnerId().toString());
        }
        root.addProperty("totalWishesGranted", memory.totalWishesGranted());
        root.addProperty("totalRescuesPerformed", memory.totalRescuesPerformed());
        root.addProperty("savedVillagesCount", memory.savedVillagesCount());

        // Genie anchor
        if (memory.anchoredGenieId() != null) {
            root.addProperty("anchoredGenieId", memory.anchoredGenieId().toString());
        }
        memory.anchoredGenieDimension().ifPresent(dim ->
                root.addProperty("anchoredGenieDimension", dim.location().toString()));
        root.addProperty("anchoredGeniePos", memory.anchoredGeniePosition().asLong());

        if (memory.anchoredGenieState().isPresent()) {
            root.add("anchoredGenieState", encodeGenieStateSnapshot(memory.anchoredGenieState().get()));
        }

        // Memory entries
        JsonArray entries = new JsonArray();
        for (WorldGenieMemory.MemoryEntry entry : memory.entries()) {
            JsonObject entryJson = new JsonObject();
            entryJson.addProperty("pos", entry.pos().asLong());
            entryJson.addProperty("type", entry.type());
            entryJson.addProperty("note", entry.note());
            entryJson.addProperty("gameTime", entry.gameTime());
            entries.add(entryJson);
        }
        root.add("entries", entries);

        // Active pocket scenes
        JsonArray pocketScenes = new JsonArray();
        for (WorldGenieMemory.ActivePocketScene scene : memory.activePocketScenes()) {
            JsonObject sceneJson = new JsonObject();
            sceneJson.addProperty("actorId", scene.actorId().toString());
            sceneJson.addProperty("transactionId", scene.transactionId().toString());
            sceneJson.addProperty("dimension", scene.dimension());
            sceneJson.addProperty("expiresAtTick", scene.expiresAtTick());
            pocketScenes.add(sceneJson);
        }
        root.add("activePocketScenes", pocketScenes);

        // Conditional rules
        JsonArray rules = new JsonArray();
        for (WorldGenieMemory.ConditionalRule rule : memory.conditionalRules(null)) {
            JsonObject ruleJson = new JsonObject();
            ruleJson.addProperty("ruleId", rule.ruleId().toString());
            ruleJson.addProperty("ownerId", rule.ownerId().toString());
            ruleJson.addProperty("condition", rule.condition());
            ruleJson.addProperty("action", rule.action());
            ruleJson.addProperty("enabled", rule.enabled());
            ruleJson.addProperty("lastTriggeredTick", rule.lastTriggeredTick());
            rules.add(ruleJson);
        }
        root.add("conditionalRules", rules);

        Path memoryFile = saveDirectory.resolve(MEMORY_FILE);
        String json = GSON.toJson(root);
        Files.writeString(memoryFile, json, StandardCharsets.UTF_8);
    }

    /**
     * Loads WorldGenieMemory state from JSON file.
     * Returns a new memory instance populated from saved data.
     */
    public WorldGenieMemory load() throws IOException {
        Path memoryFile = saveDirectory.resolve(MEMORY_FILE);
        if (!Files.isRegularFile(memoryFile)) {
            return new WorldGenieMemory();
        }

        String json = Files.readString(memoryFile, StandardCharsets.UTF_8);
        JsonObject root = GSON.fromJson(json, JsonObject.class);

        WorldGenieMemory memory = new WorldGenieMemory();

        // Core fields
        if (root.has("firstDiscoveredPos")) {
            memory.recordFirstDiscovery(
                    BlockPos.of(root.get("firstDiscoveredPos").getAsLong()),
                    root.has("firstOwnerId")
                            ? UUID.fromString(root.get("firstOwnerId").getAsString())
                            : null);
        }

        // Restore totals via dedicated helper
        int wishes = root.has("totalWishesGranted") ? root.get("totalWishesGranted").getAsInt() : 0;
        int rescues = root.has("totalRescuesPerformed") ? root.get("totalRescuesPerformed").getAsInt() : 0;
        int villages = root.has("savedVillagesCount") ? root.get("savedVillagesCount").getAsInt() : 0;
        memory.restoreTotals(wishes, rescues, villages);

        // Restore memory entries
        if (root.has("entries")) {
            List<WorldGenieMemory.MemoryEntry> restoredEntries = new ArrayList<>();
            for (JsonElement elem : root.getAsJsonArray("entries")) {
                if (elem.isJsonObject()) {
                    JsonObject e = elem.getAsJsonObject();
                    BlockPos pos = BlockPos.of(e.get("pos").getAsLong());
                    String type = e.get("type").getAsString();
                    String note = e.get("note").getAsString();
                    long time = e.get("gameTime").getAsLong();
                    restoredEntries.add(new WorldGenieMemory.MemoryEntry(pos, type, note, time));
                }
            }
            memory.restoreEntries(restoredEntries);
        }

        // Restore pocket scenes
        if (root.has("activePocketScenes")) {
            List<WorldGenieMemory.ActivePocketScene> scenes = new ArrayList<>();
            for (JsonElement elem : root.getAsJsonArray("activePocketScenes")) {
                if (elem.isJsonObject()) {
                    JsonObject s = elem.getAsJsonObject();
                    UUID actor = UUID.fromString(s.get("actorId").getAsString());
                    UUID tx = UUID.fromString(s.get("transactionId").getAsString());
                    String dim = s.get("dimension").getAsString();
                    long expires = s.get("expiresAtTick").getAsLong();
                    scenes.add(new WorldGenieMemory.ActivePocketScene(actor, tx, dim, expires));
                }
            }
            memory.restorePocketScenes(scenes);
        }

        // Restore conditional rules
        if (root.has("conditionalRules")) {
            List<WorldGenieMemory.ConditionalRule> rules = new ArrayList<>();
            for (JsonElement elem : root.getAsJsonArray("conditionalRules")) {
                if (elem.isJsonObject()) {
                    JsonObject r = elem.getAsJsonObject();
                    UUID ruleId = UUID.fromString(r.get("ruleId").getAsString());
                    UUID ownerId = UUID.fromString(r.get("ownerId").getAsString());
                    String condition = r.get("condition").getAsString();
                    String action = r.get("action").getAsString();
                    boolean enabled = r.get("enabled").getAsBoolean();
                    long triggered = r.get("lastTriggeredTick").getAsLong();
                    rules.add(new WorldGenieMemory.ConditionalRule(ruleId, ownerId, condition, action, enabled, triggered));
                }
            }
            memory.restoreConditionalRules(rules);
        }

        // Anchor restoration
        if (root.has("anchoredGenieId")) {
            UUID genieId = UUID.fromString(root.get("anchoredGenieId").getAsString());
            ResourceKey<Level> dimension = null;
            if (root.has("anchoredGenieDimension")) {
                Identifier dimId = Identifier.tryParse(root.get("anchoredGenieDimension").getAsString());
                if (dimId != null) {
                    dimension = ResourceKey.create(Registries.DIMENSION, dimId);
                }
            }
            BlockPos pos = BlockPos.of(root.get("anchoredGeniePos").getAsLong());
            memory.anchorGenie(genieId, dimension, pos);
        }

        if (root.has("anchoredGenieState")) {
            GenieStateSnapshot snapshot = decodeGenieStateSnapshot(root.getAsJsonObject("anchoredGenieState"));
            memory.rememberGenieState(snapshot);
        }

        // Entries are recorded via recordEvent which is package-private
        // For full restoration, we would need additional API or use serialization directly
        // This implementation focuses on the JSON layer; full round-trip requires
        // exposing restore methods or using the existing NBT path for complete state

        return memory;
    }

    /**
     * Records a CausalLedgerEntry for a memory-affecting transaction.
     * Creates or appends to the causal ledger JSON file.
     */
    public void recordCausalEntry(CausalLedgerEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        Files.createDirectories(saveDirectory);

        Path ledgerFile = saveDirectory.resolve(LEDGER_FILE);
        JsonArray ledger;

        if (Files.isRegularFile(ledgerFile)) {
            String existing = Files.readString(ledgerFile, StandardCharsets.UTF_8);
            ledger = GSON.fromJson(existing, JsonArray.class);
        } else {
            ledger = new JsonArray();
        }

        JsonObject entryJson = new JsonObject();
        entryJson.addProperty("transactionId", entry.transactionId().toString());
        entryJson.addProperty("actorId", entry.actorId().toString());
        entryJson.addProperty("targetSnapshotId", entry.targetSnapshotId().toString());
        entryJson.addProperty("beforeImageId", entry.beforeImageId().toString());
        entryJson.addProperty("dimension", entry.dimension());
        entryJson.addProperty("committedAt", entry.committedAt().toString());
        entryJson.addProperty("outcome", entry.outcome().name());

        ledger.add(entryJson);

        Files.writeString(ledgerFile, GSON.toJson(ledger), StandardCharsets.UTF_8);
    }

    /**
     * Reads all CausalLedgerEntry records from the JSON ledger.
     */
    public List<CausalLedgerEntry> readCausalLedger() throws IOException {
        Path ledgerFile = saveDirectory.resolve(LEDGER_FILE);
        if (!Files.isRegularFile(ledgerFile)) {
            return List.of();
        }

        String json = Files.readString(ledgerFile, StandardCharsets.UTF_8);
        JsonArray array = GSON.fromJson(json, JsonArray.class);

        List<CausalLedgerEntry> entries = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            try {
                CausalLedgerEntry entry = new CausalLedgerEntry(
                        UUID.fromString(obj.get("transactionId").getAsString()),
                        UUID.fromString(obj.get("actorId").getAsString()),
                        UUID.fromString(obj.get("targetSnapshotId").getAsString()),
                        UUID.fromString(obj.get("beforeImageId").getAsString()),
                        obj.get("dimension").getAsString(),
                        Instant.parse(obj.get("committedAt").getAsString()),
                        TransactionOutcome.valueOf(obj.get("outcome").getAsString())
                );
                entries.add(entry);
            } catch (Exception ignored) {
                // Skip malformed entries
            }
        }
        return List.copyOf(entries);
    }

    /**
     * Creates a CausalLedger instance backed by the JSON file in this persistence directory.
     * Useful for integrating with existing CausalLedger-based code.
     */
    public CausalLedger createCausalLedger() {
        return new CausalLedger(saveDirectory.resolve(LEDGER_FILE));
    }

    private JsonObject encodeGenieStateSnapshot(GenieStateSnapshot snapshot) {
        JsonObject json = new JsonObject();
        if (snapshot.ownerId() != null) {
            json.addProperty("ownerId", snapshot.ownerId().toString());
        }
        json.add("personality", compoundTagToJson(snapshot.personality()));
        json.add("brain", compoundTagToJson(snapshot.brain()));
        json.add("wishborne", compoundTagToJson(snapshot.wishborne()));
        return json;
    }

    private GenieStateSnapshot decodeGenieStateSnapshot(JsonObject json) {
        UUID ownerId = json.has("ownerId")
                ? UUID.fromString(json.get("ownerId").getAsString())
                : null;
        CompoundTag personality = jsonToCompoundTag(json.getAsJsonObject("personality"));
        CompoundTag brain = jsonToCompoundTag(json.getAsJsonObject("brain"));
        CompoundTag wishborne = jsonToCompoundTag(json.getAsJsonObject("wishborne"));
        return new GenieStateSnapshot(ownerId, personality, brain, wishborne);
    }

    private JsonObject compoundTagToJson(CompoundTag tag) {
        JsonObject json = new JsonObject();
        for (String key : tag.getAllKeys()) {
            var tagValue = tag.get(key);
            if (tagValue instanceof net.minecraft.nbt.StringTag) {
                json.addProperty(key, ((net.minecraft.nbt.StringTag) tagValue).getAsString());
            } else if (tagValue instanceof net.minecraft.nbt.IntTag) {
                json.addProperty(key, ((net.minecraft.nbt.IntTag) tagValue).getAsInt());
            } else if (tagValue instanceof net.minecraft.nbt.LongTag) {
                json.addProperty(key, ((net.minecraft.nbt.LongTag) tagValue).getAsLong());
            } else if (tagValue instanceof net.minecraft.nbt.FloatTag) {
                json.addProperty(key, ((net.minecraft.nbt.FloatTag) tagValue).getAsFloat());
            } else if (tagValue instanceof net.minecraft.nbt.DoubleTag) {
                json.addProperty(key, ((net.minecraft.nbt.DoubleTag) tagValue).getAsDouble());
            } else if (tagValue instanceof net.minecraft.nbt.ByteTag) {
                json.addProperty(key, ((net.minecraft.nbt.ByteTag) tagValue).getAsByte());
            } else if (tagValue instanceof CompoundTag) {
                json.add(key, compoundTagToJson((CompoundTag) tagValue));
            } else if (tagValue instanceof net.minecraft.nbt.ListTag) {
                JsonArray arr = new JsonArray();
                net.minecraft.nbt.ListTag list = (net.minecraft.nbt.ListTag) tagValue;
                for (var item : list) {
                    if (item instanceof CompoundTag) {
                        arr.add(compoundTagToJson((CompoundTag) item));
                    } else if (item instanceof net.minecraft.nbt.StringTag) {
                        arr.add(new JsonPrimitive(((net.minecraft.nbt.StringTag) item).getAsString()));
                    }
                }
                json.add(key, arr);
            }
        }
        return json;
    }

    private CompoundTag jsonToCompoundTag(JsonObject json) {
        CompoundTag tag = new CompoundTag();
        for (String key : json.keySet()) {
            JsonElement value = json.get(key);
            if (value.isJsonPrimitive()) {
                JsonPrimitive prim = value.getAsJsonPrimitive();
                if (prim.isString()) {
                    tag.putString(key, prim.getAsString());
                } else if (prim.isNumber()) {
                    Number num = prim.getAsNumber();
                    if (num instanceof Integer || num instanceof Short || num instanceof Byte) {
                        tag.putInt(key, num.intValue());
                    } else if (num instanceof Long) {
                        tag.putLong(key, num.longValue());
                    } else {
                        tag.putDouble(key, num.doubleValue());
                    }
                } else if (prim.isBoolean()) {
                    tag.putBoolean(key, prim.getAsBoolean());
                }
            } else if (value.isJsonObject()) {
                tag.put(key, jsonToCompoundTag(value.getAsJsonObject()));
            } else if (value.isJsonArray()) {
                JsonArray arr = value.getAsJsonArray();
                net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
                for (JsonElement elem : arr) {
                    if (elem.isJsonObject()) {
                        list.add(jsonToCompoundTag(elem.getAsJsonObject()));
                    } else if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
                        list.add(net.minecraft.nbt.StringTag.valueOf(elem.getAsString()));
                    }
                }
                tag.put(key, list);
            }
        }
        return tag;
    }
}
