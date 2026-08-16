package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Память контекстных дверей: запоминает, откуда игрок вошёл в покет-измерение,
 * чтобы та же дверь могла вернуть его точно в исходную точку.
 */
public final class ContextualDoorMemory {

    private static final Map<UUID, ReturnPoint> RETURNS = new ConcurrentHashMap<>();

    private record ReturnPoint(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private ContextualDoorMemory() {
    }

    /** Вход из обычного мира в покет-измерение. Возвращает false, если покет недоступен. */
    public static boolean enter(ServerLevel from, ServerPlayer player) {
        ServerLevel pocket = from.getServer().getLevel(PocketDimension.POCKET);
        if (pocket == null) {
            return false;
        }
        RETURNS.put(player.getUUID(),
                new ReturnPoint(from.dimension(), player.blockPosition()));
        pocket.getChunk(4, 4);
        Vec3 destination = new Vec3(0.5D, PocketDimension.FLOOR_Y + 2.0D, 0.5D);
        player.teleport(new TeleportTransition(pocket, destination, Vec3.ZERO,
                player.getYRot(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.door.entered"));
        return true;
    }

    /** Выход из покета обратно в исходную точку. */
    public static void leave(ServerPlayer player) {
        ReturnPoint returnPoint = RETURNS.remove(player.getUUID());
        if (returnPoint == null) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.door.no_return"));
            return;
        }
        ServerLevel target = player.level().getServer().getLevel(returnPoint.dimension());
        if (target == null) {
            return;
        }
        Vec3 destination = Vec3.atBottomCenterOf(returnPoint.pos().above());
        player.teleport(new TeleportTransition(target, destination, Vec3.ZERO,
                player.getYRot(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.door.returned"));
    }

    /** Очистка записей при выходе игрока из мира. */
    public static void forget(UUID playerId) {
        RETURNS.remove(playerId);
    }
}