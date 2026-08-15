package dev.romankrukovsky.kubanhorizons.genie.runtime;

import dev.romankrukovsky.kubanhorizons.genie.wish.ParsedWish;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/**
 * Records every executed wish for potential rollback and auditing.
 */
public final class CausalityLedger {

    private final List<WishExecutionRecord> records = new ArrayList<>();

    public void recordExecution(Player initiator, ParsedWish wish, ServerLevel level,
                                List<Entity> affectedEntities, List<BlockPos> affectedBlocks) {
        WishExecutionRecord record = new WishExecutionRecord(
                System.currentTimeMillis(),
                initiator.getUUID(),
                wish.rawText(),
                wish.operationName(),
                affectedEntities.stream().map(Entity::getUUID).toList(),
                affectedBlocks
        );
        records.add(record);
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
