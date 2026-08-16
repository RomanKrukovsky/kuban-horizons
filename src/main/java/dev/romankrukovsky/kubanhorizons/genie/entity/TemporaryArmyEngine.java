package dev.romankrukovsky.kubanhorizons.genie.entity;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;

import java.util.ArrayList;
import java.util.List;

/**
 * Временные защитники (GENIE_VISION §Существа): джинния материализует
 * маленькую армию железных големов вокруг игрока. Создания живут, пока их
 * не ранят, и охраняют своего хозяина — «временные люди и армии» без
 * постоянного поселения.
 */
public final class TemporaryArmyEngine {

    private static final int ARMY_SIZE = 3;

    private TemporaryArmyEngine() {
    }

    /** Спавнит армию големов вокруг игрока. Возвращает число созданных. */
    public static int summonArmy(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        List<IronGolem> spawned = new ArrayList<>();
        for (int i = 0; i < ARMY_SIZE; i++) {
            IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
            if (golem == null) {
                continue;
            }
            BlockPos pos = center.offset(2 - i % 3, 0, (i / 3) * 2 - 1);
            golem.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            golem.setTarget(null);
            level.addFreshEntity(golem);
            spawned.add(golem);
        }
        if (!spawned.isEmpty()) {
            MagicalSignature.cast(level, player.position());
        }
        return spawned.size();
    }
}