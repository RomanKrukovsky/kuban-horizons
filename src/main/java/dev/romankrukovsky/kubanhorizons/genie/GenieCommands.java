package dev.romankrukovsky.kubanhorizons.genie;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.wish.LiteralWishEngine;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishExecutor;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishParser;
import java.util.Comparator;
import java.util.Locale;
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
    private GenieCommands() {
    }

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("genie")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> converse(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "message")))));
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
