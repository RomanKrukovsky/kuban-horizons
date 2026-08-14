package dev.romankrukovsky.kubanhorizons.network.packet.c2s;

import dev.romankrukovsky.kubanhorizons.genie.GenieConversationService;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2COpenGenieDialog;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: запрос открыть диалог с ближайшей связанной джиннией. */
public record C2SOpenGenieDialog() implements CustomPacketPayload {
    public static final Type<C2SOpenGenieDialog> TYPE = new Type<>(KHIds.of("open_genie_dialog"));
    public static final StreamCodec<FriendlyByteBuf, C2SOpenGenieDialog> CODEC =
            StreamCodec.unit(new C2SOpenGenieDialog());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SOpenGenieDialog packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            var genie = GenieConversationService.nearestOwned(player);
            if (genie == null) {
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.ai.no_companion"));
                return;
            }
            S2COpenGenieDialog.send(player, genie);
        });
    }

    public static void send() {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new C2SOpenGenieDialog());
    }
}
