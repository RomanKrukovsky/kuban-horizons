package dev.romankrukovsky.kubanhorizons.network.packet.c2s;

import dev.romankrukovsky.kubanhorizons.genie.GenieConversationService;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CGenieResponse;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: игрок отправил желание джиннии (свободный текст).
 */
public record C2SWishRequest(int genieId, String wishText) implements CustomPacketPayload {

    public static final Type<C2SWishRequest> TYPE =
            new Type<>(KHIds.of("wish_request"));

    public static final StreamCodec<FriendlyByteBuf, C2SWishRequest> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.genieId());
                        buffer.writeUtf(packet.wishText(), 256);
                    },
                    buffer -> new C2SWishRequest(buffer.readVarInt(), buffer.readUtf(256))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SWishRequest packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                // Full MetaRuleEngine integration
                if (!dev.romankrukovsky.kubanhorizons.rules.MetaRuleEngine.get()
                        .evaluateWish(sp, packet.genieId(), packet.wishText())) {
                    S2CGenieResponse.send(sp, packet.genieId(),
                            net.minecraft.network.chat.Component.literal("Meta-rule violation: wish rejected."),
                            0, false);
                    return;
                }
                var response = GenieConversationService.submitWish(
                        sp, packet.genieId(), packet.wishText());
                S2CGenieResponse.send(sp, packet.genieId(), response.message(),
                        response.emotionLevel(), response.confirmationRequired());
            }
        });
    }

    public static void send(int genieId, String text) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new C2SWishRequest(genieId, text));
    }
}
