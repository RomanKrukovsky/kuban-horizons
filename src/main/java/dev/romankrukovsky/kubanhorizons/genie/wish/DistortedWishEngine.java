package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieTransformationController;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Исполнитель искажённых желаний высшего порядка («Я хочу стать всемогущим» и др.).
 */
public final class DistortedWishEngine {
    private DistortedWishEngine() {
    }

    public static WishExecutor.Result execute(ServerLevel level, ServerPlayer player, WishIntent intent) {
        MagicalSignature.cast(level, player.position());

        Component genieWarning = switch (intent.target()) {
            case OMNIPOTENCE -> Component.translatable("message.kubanhorizons.genie.wish.omnipotence_warning");
            case ETERNITY -> Component.translatable("message.kubanhorizons.genie.wish.eternity_warning");
            case SELF_FULFILLMENT -> Component.translatable("message.kubanhorizons.genie.wish.self_fulfillment_warning");
            case MIRROR_TRANSFORMATION -> Component.translatable("message.kubanhorizons.genie.wish.mirror_warning");
            case POWER_TRANSFER -> Component.translatable("message.kubanhorizons.genie.wish.power_transfer_warning");
            case ABS_INTANGIBILITY -> Component.translatable("message.kubanhorizons.genie.wish.intangibility_warning");
            default -> Component.translatable("message.kubanhorizons.genie.wish.higher_order_warning");
        };

        player.sendSystemMessage(genieWarning);
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.wish.higher_order_banner"));

        // Запуск кинематографической трансформации игрока в джиннию
        PlayerGenieTransformationController.startTransformation(level, player, intent.target());

        return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.transformation_started");
    }
}
