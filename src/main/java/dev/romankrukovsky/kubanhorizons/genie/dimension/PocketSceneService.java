package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PocketScenePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CPocketPreview;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CPocketResult;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Серверный игровой цикл временной карманной сцены. */
public final class PocketSceneService {
    public static final int DEFAULT_DURATION_TICKS = 1_200;
    private static final Map<UUID, PocketScenePreview> PENDING = new ConcurrentHashMap<>();

    private PocketSceneService() {
    }

    public static Result begin(ServerPlayer player) {
        Result result = preview(player, DEFAULT_DURATION_TICKS);
        if (result.success()) {
            PocketScenePreview preview = PENDING.get(player.getUUID());
            S2CPocketPreview.send(player, preview.changedBlocks(), preview.durationTicks(),
                    preview.risk().name());
        }
        return result;
    }

    /** Готовит серверный preview без привязки к способу его показа. */
    public static Result preview(ServerPlayer player, int durationTicks) {
        if (memory(player.level()).hasActivePocketScene(player.getUUID())) {
            return new Result(false, Component.translatable(
                    "screen.kubanhorizons.pocket.already_active"));
        }
        try {
            WishRuntime runtime = WishRuntime.get(player.level().getServer());
            if (!runtime.ready()) {
                runtime.recover();
            }
            PocketScenePreview preview = runtime.previewPocketScene(
                    player, player.blockPosition(), durationTicks);
            PENDING.put(player.getUUID(), preview);
            return new Result(true, Component.translatable(
                    "message.kubanhorizons.genie.runtime.scene_preview",
                    preview.changedBlocks(), preview.durationTicks(), preview.risk().name()));
        } catch (IOException | RuntimeException exception) {
            return failure(exception);
        }
    }

    public static Result confirm(ServerPlayer player) {
        PocketScenePreview preview = PENDING.remove(player.getUUID());
        if (preview == null) {
            return new Result(false, Component.translatable(
                    "screen.kubanhorizons.pocket.preview_expired"));
        }
        try {
            WishRuntime runtime = WishRuntime.get(player.level().getServer());
            var report = runtime.executePocketScene(player,
                    runtime.confirmPocketScene(player.getUUID(), preview));
            boolean completed = report.outcome() == TransactionOutcome.COMPLETED
                    || report.outcome() == TransactionOutcome.COMPLETED_WITH_WARNINGS;
            if (!completed) {
                return new Result(false, Component.translatable(
                        "message.kubanhorizons.genie.runtime.outcome", report.outcome().name(),
                        report.changedBlocks(), report.transactionId()));
            }
            memory(player.level()).recordPocketScene(player.getUUID(), report.transactionId(),
                    preview.selection().dimension(),
                    player.level().getGameTime() + preview.durationTicks());
            return new Result(true, Component.translatable(
                    "screen.kubanhorizons.pocket.applied", preview.durationTicks() / 20));
        } catch (IOException | RuntimeException exception) {
            return failure(exception);
        }
    }

    public static Result cancel(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        return new Result(true, Component.translatable("screen.kubanhorizons.pocket.cancelled"));
    }

    /** Возвращает истёкшие сцены через retained undo общего рантайма. */
    public static void tick(ServerLevel level) {
        WorldGenieMemory memory = memory(level);
        for (WorldGenieMemory.ActivePocketScene scene : memory.activePocketScenes()) {
            if (!scene.dimension().equals(level.dimension().identifier().toString())
                    || level.getGameTime() < scene.expiresAtTick()) {
                continue;
            }
            try {
                var report = WishRuntime.get(level.getServer())
                        .undo(level, scene.actorId(), scene.transactionId());
                if (report.outcome() == TransactionOutcome.COMPLETED
                        || report.outcome() == TransactionOutcome.COMPLETED_WITH_WARNINGS) {
                    memory.removePocketScene(scene.actorId(), scene.transactionId());
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(scene.actorId());
                    if (player != null) {
                        try {
                            S2CPocketResult.send(player, true, Component.translatable(
                                    "screen.kubanhorizons.pocket.restored"));
                        } catch (RuntimeException unsupportedClient) {
                            // Embedded GameTest-клиент не согласует optional payload.
                            // Мир уже успешно восстановлен, поэтому сбой уведомления
                            // не должен объявлять откат неудачным или повторять его.
                            KubanHorizons.LOGGER.debug(
                                    "Клиент {} не принимает pocket_result: {}",
                                    player.getUUID(), unsupportedClient.getMessage());
                        }
                    }
                }
            } catch (IOException | RuntimeException exception) {
                KubanHorizons.LOGGER.warn("Не удалось вернуть карманную сцену {}: {}",
                        scene.transactionId(), exception.getMessage());
            }
        }
    }

    public static boolean hasPending(UUID actorId) {
        return PENDING.containsKey(actorId);
    }

    public static boolean isActive(ServerLevel level, UUID actorId) {
        return memory(level).hasActivePocketScene(actorId);
    }

    private static Result failure(Exception exception) {
        return new Result(false, Component.translatable(
                "message.kubanhorizons.genie.runtime.failed", exception.getMessage()));
    }

    private static WorldGenieMemory memory(ServerLevel level) {
        ServerLevel pocket = level.getServer().getLevel(KHDimensions.POCKET);
        return WorldGenieMemory.get(pocket != null ? pocket : level.getServer().overworld());
    }

    public record Result(boolean success, Component message) {
    }
}
