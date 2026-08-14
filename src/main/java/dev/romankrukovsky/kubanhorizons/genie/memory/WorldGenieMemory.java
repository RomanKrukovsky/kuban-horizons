package dev.romankrukovsky.kubanhorizons.genie.memory;

import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Долговременная память мира о Кубанской Джиннии: спасения, желания, встречи и места.
 *
 * <p>Здесь же живёт якорь единственности: UUID той сущности, которая является
 * джиннией этого мира, её последнее известное место и снимок личности. Якорь
 * хранится вместе с памятью, потому что это одна и та же сущность: «кто именно
 * джинния» — такой же факт о мире, как «где мы познакомились».</p>
 */
public final class WorldGenieMemory implements ValueIOSerializable {
    public static final int SCHEMA_VERSION = 4;

    private BlockPos firstDiscoveredPos = BlockPos.ZERO;
    private UUID firstOwnerId;
    private int totalWishesGranted;
    private int totalRescuesPerformed;
    private int savedVillagesCount;
    private final List<MemoryEntry> entries = new ArrayList<>();

    private UUID anchoredGenieId;
    private ResourceKey<Level> anchoredGenieDimension;
    private BlockPos anchoredGeniePos = BlockPos.ZERO;
    private GenieStateSnapshot anchoredGenieState;
    private final Map<UUID, ActivePocketScene> activePocketScenes = new LinkedHashMap<>();
    private final Map<UUID, ConditionalRule> conditionalRules = new LinkedHashMap<>();
    private final Map<UUID, Contract> contracts = new LinkedHashMap<>();

    public static WorldGenieMemory get(ServerLevel level) {
        return level.getData(KHAttachments.GENIE_WORLD_MEMORY);
    }

    public void recordFirstDiscovery(BlockPos pos, UUID ownerId) {
        if (firstDiscoveredPos.equals(BlockPos.ZERO)) {
            firstDiscoveredPos = pos;
            firstOwnerId = ownerId;
            recordEvent(pos, "first_discovery", "message.kubanhorizons.genie.memory.first_discovery", 0L);
        }
    }

    public void recordWish(BlockPos pos, String wording, int precision, long gameTime) {
        totalWishesGranted++;
        recordEvent(pos, "wish", wording, gameTime);
    }

    public void recordRescue(BlockPos pos, long gameTime) {
        totalRescuesPerformed++;
        recordEvent(pos, "rescue", "message.kubanhorizons.genie.memory.rescue", gameTime);
    }

    public void recordVillageSaved(BlockPos pos, long gameTime) {
        savedVillagesCount++;
        recordEvent(pos, "village_saved", "message.kubanhorizons.genie.memory.village", gameTime);
    }

    public void recordEvent(BlockPos pos, String type, String note, long gameTime) {
        entries.add(new MemoryEntry(pos, type, note, gameTime));
        if (entries.size() > 500) {
            entries.removeFirst();
        }
    }

    public Optional<MemoryEntry> findNearbyMemory(BlockPos pos, double radiusBlocks) {
        double rSq = radiusBlocks * radiusBlocks;
        for (MemoryEntry entry : entries) {
            if (entry.pos().distSqr(pos) <= rSq) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public BlockPos firstDiscoveredPos() {
        return firstDiscoveredPos;
    }

    public UUID firstOwnerId() {
        return firstOwnerId;
    }

    public int totalWishesGranted() {
        return totalWishesGranted;
    }

    public int totalRescuesPerformed() {
        return totalRescuesPerformed;
    }

    public int savedVillagesCount() {
        return savedVillagesCount;
    }

    public List<MemoryEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    // --- Якорь единственности ---

    /** UUID сущности, которая является джиннией этого мира, либо null. */
    public UUID anchoredGenieId() {
        return anchoredGenieId;
    }

    /** Измерение, в котором джиннию видели последний раз. */
    public Optional<ResourceKey<Level>> anchoredGenieDimension() {
        return Optional.ofNullable(anchoredGenieDimension);
    }

    /** Позиция, в которой джиннию видели последний раз. */
    public BlockPos anchoredGeniePosition() {
        return anchoredGeniePos;
    }

    /** Снимок личности якорной джиннии, если он уже сохранялся. */
    public Optional<GenieStateSnapshot> anchoredGenieState() {
        return Optional.ofNullable(anchoredGenieState);
    }

    /** Назначает якорную джиннию мира. */
    public void anchorGenie(UUID genieId, ResourceKey<Level> dimension, BlockPos pos) {
        anchoredGenieId = genieId;
        anchoredGenieDimension = dimension;
        anchoredGeniePos = pos;
    }

    /** Обновляет последнее известное место якорной джиннии. */
    public void updateAnchorLocation(ResourceKey<Level> dimension, BlockPos pos) {
        anchoredGenieDimension = dimension;
        anchoredGeniePos = pos;
    }

    /**
     * Запоминает личность джиннии отдельно от сущности.
     *
     * <p>Снимок нужен, чтобы восстановить характер и историю, если сущность
     * всё-таки была уничтожена: без него джинния возвращалась бы чистым
     * листом и теряла накопленные отношения.</p>
     */
    public void rememberGenieState(GenieStateSnapshot snapshot) {
        anchoredGenieState = snapshot;
    }

    /** Снимает якорь, позволяя миру привязаться к новой джиннии. */
    public void releaseGenieAnchor() {
        anchoredGenieId = null;
        anchoredGenieDimension = null;
        anchoredGeniePos = BlockPos.ZERO;
    }

    public void recordPocketScene(UUID actorId, UUID transactionId, String dimension,
                                  long expiresAtTick) {
        activePocketScenes.put(actorId,
                new ActivePocketScene(actorId, transactionId, dimension, expiresAtTick));
    }

    public List<ActivePocketScene> activePocketScenes() {
        return List.copyOf(activePocketScenes.values());
    }

    public boolean hasActivePocketScene(UUID actorId) {
        return activePocketScenes.containsKey(actorId);
    }

    public void removePocketScene(UUID actorId, UUID transactionId) {
        activePocketScenes.computeIfPresent(actorId,
                (ignored, scene) -> scene.transactionId().equals(transactionId) ? null : scene);
    }

    public ConditionalRule upsertConditionalRule(UUID ownerId, String condition, String action) {
        for (ConditionalRule rule : conditionalRules.values()) {
            if (rule.ownerId().equals(ownerId) && rule.condition().equals(condition)
                    && rule.action().equals(action)) {
                ConditionalRule enabled = new ConditionalRule(rule.ruleId(), ownerId,
                        condition, action, true, rule.lastTriggeredTick());
                conditionalRules.put(rule.ruleId(), enabled);
                return enabled;
            }
        }
        long ownedRules = conditionalRules.values().stream()
                .filter(rule -> rule.ownerId().equals(ownerId))
                .count();
        if (ownedRules >= 16L) {
            throw new IllegalStateException("conditional rule limit reached");
        }
        ConditionalRule rule = new ConditionalRule(UUID.randomUUID(), ownerId,
                condition, action, true, 0L);
        conditionalRules.put(rule.ruleId(), rule);
        return rule;
    }

    public boolean removeConditionalRule(UUID ownerId, String condition, String action) {
        return conditionalRules.values().removeIf(rule -> rule.ownerId().equals(ownerId)
                && rule.condition().equals(condition) && rule.action().equals(action));
    }

    public List<ConditionalRule> conditionalRules(UUID ownerId) {
        return conditionalRules.values().stream()
                .filter(rule -> rule.ownerId().equals(ownerId))
                .toList();
    }

    public void markConditionalRuleTriggered(UUID ruleId, long gameTime) {
        conditionalRules.computeIfPresent(ruleId, (ignored, rule) ->
                new ConditionalRule(rule.ruleId(), rule.ownerId(), rule.condition(), rule.action(),
                        rule.enabled(), Math.max(0L, gameTime)));
    }

    // --- Contract management ---

    public Contract storeContract(Contract contract) {
        contracts.put(contract.contractId(), contract);
        return contract;
    }

    public Contract getContract(UUID contractId) {
        return contracts.get(contractId);
    }

    public boolean removeContract(UUID contractId) {
        return contracts.remove(contractId) != null;
    }

    public List<Contract> contracts() {
        return List.copyOf(contracts.values());
    }

    public void restoreContracts(List<Contract> restored) {
        contracts.clear();
        if (restored != null) {
            for (Contract c : restored) {
                contracts.put(c.contractId(), c);
            }
        }
    }

    // --- Persistence helpers for JSON round-trip restoration ---

    /** Restores totals from persisted state (used by JSON loader). */
    public void restoreTotals(int wishes, int rescues, int villages) {
        this.totalWishesGranted = Math.max(0, wishes);
        this.totalRescuesPerformed = Math.max(0, rescues);
        this.savedVillagesCount = Math.max(0, villages);
    }

    /** Bulk-restores memory entries (used by JSON loader, bypasses size limit). */
    public void restoreEntries(List<MemoryEntry> restored) {
        entries.clear();
        if (restored != null) {
            entries.addAll(restored);
        }
    }

    /** Bulk-restores active pocket scenes. */
    public void restorePocketScenes(List<ActivePocketScene> scenes) {
        activePocketScenes.clear();
        if (scenes != null) {
            for (ActivePocketScene scene : scenes) {
                activePocketScenes.put(scene.actorId(), scene);
            }
        }
    }

    /** Bulk-restores conditional rules. */
    public void restoreConditionalRules(List<ConditionalRule> rules) {
        conditionalRules.clear();
        if (rules != null) {
            for (ConditionalRule rule : rules) {
                conditionalRules.put(rule.ruleId(), rule);
            }
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putLong("FirstPos", firstDiscoveredPos.asLong());
        if (firstOwnerId != null) {
            output.putString("FirstOwner", firstOwnerId.toString());
        }
        output.putInt("TotalWishes", totalWishesGranted);
        output.putInt("TotalRescues", totalRescuesPerformed);
        output.putInt("SavedVillages", savedVillagesCount);

        if (anchoredGenieId != null) {
            output.putString("AnchoredGenie", anchoredGenieId.toString());
        }
        if (anchoredGenieDimension != null) {
            output.putString("AnchoredGenieDimension", anchoredGenieDimension.identifier().toString());
        }
        output.putLong("AnchoredGeniePos", anchoredGeniePos.asLong());
        if (anchoredGenieState != null) {
            anchoredGenieState.save(output.child("AnchoredGenieState"));
        }

        var list = output.childrenList("Entries");
        for (MemoryEntry entry : entries) {
            var child = list.addChild();
            child.putLong("pos", entry.pos().asLong());
            child.putString("type", entry.type());
            child.putString("note", entry.note());
            child.putLong("time", entry.gameTime());
        }
        var pocketScenes = output.childrenList("ActivePocketScenes");
        for (ActivePocketScene scene : activePocketScenes.values()) {
            var child = pocketScenes.addChild();
            child.putString("Actor", scene.actorId().toString());
            child.putString("Transaction", scene.transactionId().toString());
            child.putString("Dimension", scene.dimension());
            child.putLong("ExpiresAtTick", scene.expiresAtTick());
        }
        var rules = output.childrenList("ConditionalRules");
        for (ConditionalRule rule : conditionalRules.values()) {
            var child = rules.addChild();
            child.putString("Rule", rule.ruleId().toString());
            child.putString("Owner", rule.ownerId().toString());
            child.putString("Condition", rule.condition());
            child.putString("Action", rule.action());
            child.putBoolean("Enabled", rule.enabled());
            child.putLong("LastTriggeredTick", rule.lastTriggeredTick());
        }

        var contractsList = output.childrenList("Contracts");
        for (Contract contract : contracts.values()) {
            var child = contractsList.addChild();
            contract.save(child);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        firstDiscoveredPos = BlockPos.of(input.getLongOr("FirstPos", 0L));
        firstOwnerId = readUuid(input, "FirstOwner");
        totalWishesGranted = input.getIntOr("TotalWishes", 0);
        totalRescuesPerformed = input.getIntOr("TotalRescues", 0);
        savedVillagesCount = input.getIntOr("SavedVillages", 0);

        // Миры версии 1 якоря не содержат: там джинния станет якорной при
        // первой же прогрузке, что и требуется.
        anchoredGenieId = readUuid(input, "AnchoredGenie");
        anchoredGenieDimension = readDimension(input);
        anchoredGeniePos = BlockPos.of(input.getLongOr("AnchoredGeniePos", 0L));
        anchoredGenieState = input.child("AnchoredGenieState")
                .map(GenieStateSnapshot::load)
                .orElse(null);

        entries.clear();
        for (ValueInput child : input.childrenListOrEmpty("Entries")) {
            BlockPos pos = BlockPos.of(child.getLongOr("pos", 0L));
            String type = child.getStringOr("type", "generic");
            String note = child.getStringOr("note", "");
            long time = child.getLongOr("time", 0L);
            entries.add(new MemoryEntry(pos, type, note, time));
        }
        activePocketScenes.clear();
        for (ValueInput child : input.childrenListOrEmpty("ActivePocketScenes")) {
            UUID actor = readUuid(child, "Actor");
            UUID transaction = readUuid(child, "Transaction");
            String dimension = child.getStringOr("Dimension", "");
            long expiresAt = child.getLongOr("ExpiresAtTick", 0L);
            if (actor != null && transaction != null && Identifier.tryParse(dimension) != null
                    && expiresAt > 0L) {
                activePocketScenes.put(actor,
                        new ActivePocketScene(actor, transaction, dimension, expiresAt));
            }
        }
        conditionalRules.clear();
        for (ValueInput child : input.childrenListOrEmpty("ConditionalRules")) {
            UUID ruleId = readUuid(child, "Rule");
            UUID ownerId = readUuid(child, "Owner");
            String condition = child.getStringOr("Condition", "");
            String action = child.getStringOr("Action", "");
            if (ruleId != null && ownerId != null && !condition.isBlank() && !action.isBlank()) {
                conditionalRules.put(ruleId, new ConditionalRule(ruleId, ownerId, condition, action,
                        child.getBooleanOr("Enabled", true),
                        Math.max(0L, child.getLongOr("LastTriggeredTick", 0L))));
            }
        }

        contracts.clear();
        for (ValueInput child : input.childrenListOrEmpty("Contracts")) {
            Contract contract = Contract.load(child);
            contracts.put(contract.contractId(), contract);
        }
    }

    private static UUID readUuid(ValueInput input, String key) {
        String raw = input.getStringOr(key, "");
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ResourceKey<Level> readDimension(ValueInput input) {
        String raw = input.getStringOr("AnchoredGenieDimension", "");
        if (raw.isEmpty()) {
            return null;
        }
        Identifier id = Identifier.tryParse(raw);
        return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
    }

    public record MemoryEntry(BlockPos pos, String type, String note, long gameTime) {
    }

    public record ActivePocketScene(UUID actorId, UUID transactionId, String dimension,
                                    long expiresAtTick) {
    }

    public record ConditionalRule(UUID ruleId, UUID ownerId, String condition, String action,
                                  boolean enabled, long lastTriggeredTick) {
    }
}
