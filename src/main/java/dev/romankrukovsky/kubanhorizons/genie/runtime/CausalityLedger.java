package dev.romankrukovsky.kubanhorizons.genie.runtime;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.genie.wish.ParsedWish;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent ledger of all executed wishes.
 * Survives world saves and restarts.
 */
public final class CausalityLedger extends SavedData {

    public static final Codec<WishExecutionRecord> RECORD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("time").forGetter(WishExecutionRecord::timestamp),
            Codec.STRING.fieldOf("initiator").forGetter(r -> r.initiator.toString()),
            Codec.STRING.fieldOf("raw").forGetter(WishExecutionRecord::rawWish),
            Codec.STRING.fieldOf("op").forGetter(WishExecutionRecord::operation),
            Codec.list(Codec.STRING).fieldOf("entities").forGetter(r -> r.affectedEntities.stream().map(UUID::toString).toList()),
            Codec.list(Codec.STRING).fieldOf("blocks").forGetter(r -> r.affectedBlocks.stream().map(b -> b.getX() + "," + b.getY() + "," + b.getZ()).toList())
    ).apply(instance, (time, initiator, raw, op, entities, blocks) -> new WishExecutionRecord(
            time,
            UUID.fromString(initiator),
            raw,
            op,
            entities.stream().map(UUID::fromString).toList(),
            blocks.stream().map(s -> {
                String[] p = s.split(",");
                return new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            }).toList()
    )));

    public static final Codec<CausalityLedger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(RECORD_CODEC).fieldOf("records").forGetter(l -> l.records)
    ).apply(instance, CausalityLedger::new));

    public static final SavedDataType<CausalityLedger> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KubanHorizons.MOD_ID, "causality_ledger"),
            CausalityLedger::new,
            CODEC
    );

    private final List<WishExecutionRecord> records;

    public CausalityLedger() {
        this(new ArrayList<>());
    }

    public CausalityLedger(List<WishExecutionRecord> records) {
        this.records = new ArrayList<>(records);
    }

    public static CausalityLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
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

    public record WishExecutionRecord(
            long timestamp,
            UUID initiator,
            String rawWish,
            String operation,
            List<UUID> affectedEntities,
            List<BlockPos> affectedBlocks
    ) {}
}