package dev.romankrukovsky.kubanhorizons.network.packet.c2s;

import dev.romankrukovsky.kubanhorizons.genie.GenieConversationService;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CGenieResponse;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: подтвердить или отклонить показанное глобальное правило. */
public record C2SPolicyDecision(int genieId, boolean confirmed) implements CustomPacketPayload {
    public static final Type<C2SPolicyDecision> TYPE = new Type<>(KHIds.of("policy_decision"));
    public static final StreamCodec<FriendlyByteBuf, C2SPolicyDecision> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.genieId());
                        buffer.writeBoolean(packet.confirmed());
                    },
                    buffer -> new C2SPolicyDecision(buffer.readVarInt(), buffer.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPolicyDecision packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            GenieConversationService.Response response;
            if (packet.confirmed()) {
                response = GenieConversationService.confirmPolicy(player, packet.genieId());
            } else {
                GenieConversationService.cancelPolicy(player);
                response = new GenieConversationService.Response(
                        net.minecraft.network.chat.Component.translatable(
                                "screen.kubanhorizons.genie.policy_cancelled"), 0, false);
            }
            S2CGenieResponse.send(player, packet.genieId(), response.message(),
                    response.emotionLevel(), response.confirmationRequired());
        });
    }

    public static void send(int genieId, boolean confirmed) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new C2SPolicyDecision(genieId, confirmed));
    }
}
