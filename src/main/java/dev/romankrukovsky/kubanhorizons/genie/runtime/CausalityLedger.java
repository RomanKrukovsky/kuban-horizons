package dev.romankrukovsky.kubanhorizons.genie.runtime;

import dev.romankrukovsky.kubanhorizons.genie.wish.ParsedWish;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/**
 * Persistent ledger of all executed wishes.
 * Survives world saves and restarts.
 */
public final class CausalityLedger extends SavedData {

    private static final String DATA_NAME = "kubanhorizons_causality_ledger";

    private final List<WishExecutionRecord> records = new ArrayList<>();

    public static CausalityLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(CausalityLedger::new, CausalityLedger::load),
                DATA_NAME
        );
    }

    public void recordExecution(Player initiator, ParsedWish wish, ServerLevel level,
                                List<UUID> affectedEntities, List<BlockPos> affectedBlocks) {

        WishExecutionRecord record = new WishExecutionRecord(
                System.currentTimeMillis(),
                initiator.getUUID(),
                wish.rawText(),
                wish.operationName(),
                affectedEntities,
                affectedBlocks
        );
        records.add(record);
        setDirty();
    }

    public Optional<WishExecutionRecord> getLastRecord() {
        if (records.isEmpty()) return Optional.empty();
        return Optional.of(records.get(records.size() - 1));
    }

    // === Persistence ===

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (WishExecutionRecord r : records) {
            CompoundTag t = new CompoundTag();
            t.putLong("time", r.timestamp);
            t.putUUID("initiator", r.initiator);
            t.putString("raw", r.rawWish);
            t.putString("op", r.operation);

            ListTag entities = new ListTag();
            for (UUID uuid : r.affectedEntities) {
                CompoundTag e = new CompoundTag();
                e.putUUID("uuid", uuid);
                entities.add(e);
            }
            t.put("entities", entities);

            ListTag blocks = new ListTag();
            for (BlockPos pos : r.affectedBlocks) {
                CompoundTag b = new CompoundTag();
                b.putInt("x", pos.getX());
                b.putInt("y", pos.getY());
                b.putInt("z", pos.getZ());
                blocks.add(b);
            }
            t.put("blocks", blocks);

            list.add(t);
        }
        tag.put("records", list);
        return tag;
    }

    public static CausalityLedger load(CompoundTag tag, HolderLookup.Provider provider) {
        CausalityLedger ledger = new CausalityLedger();
        ListTag list = tag.getList("records", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            long time = t.getLong("time");
            UUID initiator = t.getUUID("initiator");
            String raw = t.getString("raw");
            String op = t.getString("op");

            List<UUID> entities = new ArrayList<>();
            ListTag el = t.getList("entities", Tag.TAG_COMPOUND);
            for (int j = 0; j < el.size(); j++) {
                entities.add(el.getCompound(j).getUUID("uuid"));
            }

            List<BlockPos> blocks = new ArrayList<>();
            ListTag bl = t.getList("blocks", Tag.TAG_COMPOUND);
            for (int j = 0; j < bl.size(); j++) {
                CompoundTag b = bl.getCompound(j);
                blocks.add(new BlockPos(b.getInt("x"), b.getInt("y"), b.getInt("z")));
            }

            ledger.records.add(new WishExecutionRecord(time, initiator, raw, op, entities, blocks));
        }
        return ledger;
    }

    public record WishExecutionRecord(
            long timestamp,
            UUID initiator,
            String rawWish,
            String operation,
            List<UUID> affectedEntities,
            List<BlockPos> affectedBlocks
    ) {}
}
