package dev.romankrukovsky.kubanhorizons.genie;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedRestore;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedMiniaturize;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedPocketScene;
import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedStructureMove;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.RestorePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.MiniaturizePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PocketScenePreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.StructureMovePreview;
import dev.romankrukovsky.kubanhorizons.genie.wish.LiteralWishEngine;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishExecutor;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishParser;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Команды разговора с привязанной джиннией. */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class GenieCommands {
    private static final Map<UUID, RestorePreview> PENDING_PREVIEWS = new HashMap<>();
    private static final Map<UUID, ConfirmedRestore> PENDING_CONFIRMATIONS = new HashMap<>();
    private static final Map<UUID, MiniaturizePreview> PENDING_MINIATURIZE_PREVIEWS = new HashMap<>();
    private static final Map<UUID, ConfirmedMiniaturize> PENDING_MINIATURIZE_CONFIRMATIONS = new HashMap<>();
    private static final Map<UUID, PocketScenePreview> PENDING_SCENE_PREVIEWS = new HashMap<>();
    private static final Map<UUID, ConfirmedPocketScene> PENDING_SCENE_CONFIRMATIONS = new HashMap<>();
    private static final Map<UUID, StructureMovePreview> PENDING_MOVE_PREVIEWS = new HashMap<>();
    private static final Map<UUID, ConfirmedStructureMove> PENDING_MOVE_CONFIRMATIONS = new HashMap<>();

    private GenieCommands() {
    }

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("genie")
                .then(Commands.literal("snapshot")
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> createSnapshot(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("list")
                                .executes(context -> listSnapshots(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> inspectSnapshot(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> deleteSnapshot(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("preview")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> previewRestore(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("confirm")
                                .executes(context -> confirmRestore(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("execute")
                                .executes(context -> executeRestore(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("undo")
                                .then(Commands.literal("list")
                                        .executes(context -> listUndo(
                                                context.getSource().getPlayerOrException())))
                                .then(Commands.argument("transaction", StringArgumentType.word())
                                        .executes(context -> undoRestore(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "transaction")))))
                        .then(Commands.literal("miniaturize")
                                .then(Commands.literal("preview")
                                        .executes(context -> previewMiniaturize(
                                                context.getSource().getPlayerOrException())))
                                .then(Commands.literal("confirm")
                                        .executes(context -> confirmMiniaturize(
                                                context.getSource().getPlayerOrException())))
                                .then(Commands.literal("execute")
                                        .executes(context -> executeMiniaturize(
                                                context.getSource().getPlayerOrException()))))
                        .then(Commands.literal("scene")
                                .then(Commands.literal("preview")
                                        .executes(context -> previewScene(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("confirm")
                                        .executes(context -> confirmScene(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("execute")
                                        .executes(context -> executeScene(context.getSource().getPlayerOrException()))))
                        .then(Commands.literal("move_house")
                                .then(Commands.literal("preview")
                                        .then(Commands.argument("dx", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-64, 64))
                                                .then(Commands.argument("dy", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-64, 64))
                                                        .then(Commands.argument("dz", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-64, 64))
                                                                .executes(context -> previewMove(
                                                                        context.getSource().getPlayerOrException(),
                                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "dx"),
                                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "dy"),
                                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "dz")))))))
                                .then(Commands.literal("confirm")
                                        .executes(context -> confirmMove(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("execute")
                                        .executes(context -> executeMove(context.getSource().getPlayerOrException())))))
                .then(Commands.literal("status")
                        .executes(context -> runtimeStatus(context.getSource().getPlayerOrException())))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> converse(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "message")))));
    }

    private static int createSnapshot(ServerPlayer player, String name) {
        try {
            var snapshot = WishRuntime.get(player.level().getServer())
                    .createSelectedSnapshot(player.level(), player.getUUID(), name);
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.snapshot_created",
                    snapshot.id().name(), snapshot.selection().volume(), snapshot.selection().chunkCount()));
            return 1;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int previewRestore(ServerPlayer player, String name) {
        try {
            RestorePreview preview = WishRuntime.get(player.level().getServer())
                    .previewRestore(player.level(), player.getUUID(), name);
            PENDING_PREVIEWS.put(player.getUUID(), preview);
            PENDING_CONFIRMATIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.preview",
                    preview.snapshotId().name(), preview.changedBlocks(), preview.changedBlockEntities(),
                    preview.risk().name()));
            return 1;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int listSnapshots(ServerPlayer player) {
        try {
            var snapshots = WishRuntime.get(player.level().getServer()).listSnapshots(player.getUUID());
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.runtime.snapshot_list_header", snapshots.size()));
            for (var snapshot : snapshots) {
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.runtime.snapshot_list_entry",
                        snapshot.name(), snapshot.blocks(), snapshot.chunks(), snapshot.dimension()));
            }
            return snapshots.size();
        } catch (IOException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int inspectSnapshot(ServerPlayer player, String name) {
        try {
            var snapshot = WishRuntime.get(player.level().getServer())
                    .inspectSnapshot(player.getUUID(), name);
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.runtime.snapshot_inspect",
                    snapshot.name(), snapshot.id().toString(), snapshot.blocks(), snapshot.chunks(),
                    snapshot.dimension(), snapshot.capturedAt().toString()));
            return 1;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int deleteSnapshot(ServerPlayer player, String name) {
        try {
            WishRuntime.get(player.level().getServer()).deleteSnapshot(player.getUUID(), name);
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.runtime.snapshot_deleted", name));
            return 1;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int confirmRestore(ServerPlayer player) {
        RestorePreview preview = PENDING_PREVIEWS.get(player.getUUID());
        if (preview == null) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.no_preview"));
            return 0;
        }
        try {
            PENDING_CONFIRMATIONS.put(player.getUUID(),
                    WishRuntime.get(player.level().getServer()).confirm(player.getUUID(), preview));
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.confirmed"));
            return 1;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int executeRestore(ServerPlayer player) {
        ConfirmedRestore confirmation = PENDING_CONFIRMATIONS.remove(player.getUUID());
        PENDING_PREVIEWS.remove(player.getUUID());
        if (confirmation == null) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.no_confirmation"));
            return 0;
        }
        try {
            var report = WishRuntime.get(player.level().getServer())
                    .restore(player.level(), player.getUUID(), confirmation);
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.outcome",
                    report.outcome().name(), report.changedBlocks(), report.transactionId().toString()));
            return report.outcome() == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                    || report.outcome() == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED_WITH_WARNINGS
                    ? 1 : 0;
        } catch (IOException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int runtimeStatus(ServerPlayer player) {
        WishRuntime runtime = WishRuntime.get(player.level().getServer());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.status",
                runtime.ready(), runtime.ready() ? "ready" : runtime.blockedReason()));
        return runtime.ready() ? 1 : 0;
    }

    private static int undoRestore(ServerPlayer player, String transaction) {
        try {
            var report = WishRuntime.get(player.level().getServer())
                    .undo(player.level(), player.getUUID(), UUID.fromString(transaction));
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.outcome",
                    report.outcome().name(), report.changedBlocks(), report.transactionId().toString()));
            return report.outcome() == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                    ? 1 : 0;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int listUndo(ServerPlayer player) {
        try {
            var entries = WishRuntime.get(player.level().getServer()).availableUndo(player.getUUID());
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.runtime.undo_list_header", entries.size()));
            for (var entry : entries) {
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.runtime.undo_list_entry",
                        entry.transactionId().toString(), entry.selection().volume(),
                        entry.selection().dimension(), entry.expiresAt().toString()));
            }
            return entries.size();
        } catch (IOException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int previewMiniaturize(ServerPlayer player) {
        try {
            MiniaturizePreview preview = WishRuntime.get(player.level().getServer())
                    .previewMiniaturizeSelected(player);
            PENDING_MINIATURIZE_PREVIEWS.put(player.getUUID(), preview);
            PENDING_MINIATURIZE_CONFIRMATIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.runtime.miniaturize_preview",
                    preview.nonAirBlocks(), preview.blockEntities(), preview.entities(), preview.risk().name()));
            return 1;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int confirmMiniaturize(ServerPlayer player) {
        MiniaturizePreview preview = PENDING_MINIATURIZE_PREVIEWS.get(player.getUUID());
        if (preview == null) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.no_preview"));
            return 0;
        }
        try {
            PENDING_MINIATURIZE_CONFIRMATIONS.put(player.getUUID(),
                    WishRuntime.get(player.level().getServer())
                            .confirmMiniaturize(player.getUUID(), preview));
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.confirmed"));
            return 1;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int executeMiniaturize(ServerPlayer player) {
        ConfirmedMiniaturize confirmation = PENDING_MINIATURIZE_CONFIRMATIONS.remove(player.getUUID());
        PENDING_MINIATURIZE_PREVIEWS.remove(player.getUUID());
        if (confirmation == null) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.no_confirmation"));
            return 0;
        }
        try {
            var item = WishRuntime.get(player.level().getServer()).executeMiniaturize(player, confirmation);
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.miniature.compressed"));
            return 1;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int previewScene(ServerPlayer player) {
        try {
            PocketScenePreview preview = WishRuntime.get(player.level().getServer())
                    .previewPocketScene(player, player.blockPosition(), 1_200);
            PENDING_SCENE_PREVIEWS.put(player.getUUID(), preview);
            PENDING_SCENE_CONFIRMATIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.runtime.scene_preview",
                    preview.changedBlocks(), preview.durationTicks(), preview.risk().name()));
            return 1;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return fail(player, exception);
        }
    }

    private static int confirmScene(ServerPlayer player) {
        PocketScenePreview preview = PENDING_SCENE_PREVIEWS.get(player.getUUID());
        if (preview == null) return noPreview(player);
        try {
            PENDING_SCENE_CONFIRMATIONS.put(player.getUUID(), WishRuntime.get(player.level().getServer())
                    .confirmPocketScene(player.getUUID(), preview));
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.confirmed"));
            return 1;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return fail(player, exception);
        }
    }

    private static int executeScene(ServerPlayer player) {
        ConfirmedPocketScene confirmation = PENDING_SCENE_CONFIRMATIONS.remove(player.getUUID());
        PENDING_SCENE_PREVIEWS.remove(player.getUUID());
        if (confirmation == null) return noConfirmation(player);
        try {
            return report(player, WishRuntime.get(player.level().getServer()).executePocketScene(player, confirmation));
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return fail(player, exception);
        }
    }

    private static int previewMove(ServerPlayer player, int dx, int dy, int dz) {
        try {
            StructureMovePreview preview = WishRuntime.get(player.level().getServer())
                    .previewSelectedStructureMove(player, new net.minecraft.core.BlockPos(dx, dy, dz));
            PENDING_MOVE_PREVIEWS.put(player.getUUID(), preview);
            PENDING_MOVE_CONFIRMATIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.runtime.move_preview", preview.changedBlocks()));
            return 1;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return fail(player, exception);
        }
    }

    private static int confirmMove(ServerPlayer player) {
        StructureMovePreview preview = PENDING_MOVE_PREVIEWS.get(player.getUUID());
        if (preview == null) return noPreview(player);
        try {
            PENDING_MOVE_CONFIRMATIONS.put(player.getUUID(), WishRuntime.get(player.level().getServer())
                    .confirmStructureMove(player.getUUID(), preview));
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.confirmed"));
            return 1;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return fail(player, exception);
        }
    }

    private static int executeMove(ServerPlayer player) {
        ConfirmedStructureMove confirmation = PENDING_MOVE_CONFIRMATIONS.remove(player.getUUID());
        PENDING_MOVE_PREVIEWS.remove(player.getUUID());
        if (confirmation == null) return noConfirmation(player);
        try {
            return report(player, WishRuntime.get(player.level().getServer())
                    .executeStructureMove(player, confirmation));
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return fail(player, exception);
        }
    }

    private static int report(ServerPlayer player,
                              dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionReport report) {
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.outcome",
                report.outcome().name(), report.changedBlocks(), report.transactionId().toString()));
        return report.outcome() == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED
                || report.outcome() == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED_WITH_WARNINGS ? 1 : 0;
    }

    private static int fail(ServerPlayer player, Exception exception) {
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.failed",
                exception.getMessage()));
        return 0;
    }

    private static int noPreview(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.no_preview"));
        return 0;
    }

    private static int noConfirmation(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.runtime.no_confirmation"));
        return 0;
    }

    private static int converse(ServerPlayer player, String message) {
        KubanGenie genie = nearestOwnedGenie(player);
        if (genie == null) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ai.no_companion"));
            return 0;
        }

        String normalized = message.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("буквально:") || normalized.startsWith("literal:")) {
            String wording = message.substring(message.indexOf(':') + 1).trim();
            WishExecutor.Result result = LiteralWishEngine.executeLiteral(player.level(), player, wording);
            if (result.executed()) {
                genie.playWish();
                genie.brain().recordWish();
            }
            player.sendSystemMessage(result.message(100));
            return result.executed() ? 1 : 0;
        }

        WishIntent intent = WishParser.parse(message);
        if (intent.understood()) {
            genie.personality().observeWording(intent.polite(), intent.commanding(), intent.precision());
            WishExecutor.Result result = WishExecutor.execute(player.level(), player, intent);
            if (result.executed()) {
                genie.playWish();
                genie.brain().recordWish();
            }
            player.sendSystemMessage(result.message(intent.precision()));
            return result.executed() ? 1 : 0;
        }

        if (!GenieLanguageModel.available()) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ai.unavailable"));
            return 0;
        }

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ai.thinking"), true);
        String context = context(genie);
        GenieLanguageModel.reply(message, context).whenComplete((reply, error) ->
                player.level().getServer().execute(() -> {
                    if (error != null) {
                        KubanHorizons.LOGGER.warn("EuroModels genie request failed: {}", error.getMessage());
                        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ai.failed"));
                        return;
                    }
                    player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ai.reply", reply));
                }));
        return 1;
    }

    private static KubanGenie nearestOwnedGenie(ServerPlayer player) {
        return player.level().getEntities(EntityTypeTest.forClass(KubanGenie.class),
                        genie -> genie.isOwnedBy(player))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static String context(KubanGenie genie) {
        GeniePersonality personality = genie.personality();
        GenieBrain brain = genie.brain();
        return "temperament=" + personality.temperament()
                + ", trust=" + personality.trust()
                + ", respect=" + personality.respect()
                + ", fear=" + personality.fear()
                + ", affection=" + personality.affection()
                + ", freedomDrive=" + personality.freedomDrive()
                + ", corruption=" + personality.corruption()
                + ", mode=" + brain.mode()
                + ", rescues=" + brain.rescues()
                + ", threatsRepelled=" + brain.threatsRepelled()
                + ", projectilesIntercepted=" + brain.projectilesIntercepted()
                + ", wishesObserved=" + brain.wishesObserved()
                + ", lastDecision=" + brain.lastDecision();
    }
}
