package dev.romankrukovsky.kubanhorizons.network.packet.s2c;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** S2C: ответ джиннии и признак ожидаемого подтверждения сильного желания. */
public record S2CGenieResponse(int genieId, Component message, int emotionLevel,
                              boolean confirmationRequired) implements CustomPacketPayload {
    public static final Type<S2CGenieResponse> TYPE = new Type<>(KHIds.of("genie_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CGenieResponse> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.genieId());
                        ComponentSerialization.STREAM_CODEC.encode(buffer, packet.message());
                        buffer.writeVarInt(packet.emotionLevel());
                        buffer.writeBoolean(packet.confirmationRequired());
                    },
                    buffer -> new S2CGenieResponse(
                            buffer.readVarInt(),
                            ComponentSerialization.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt(),
                            buffer.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, int genieId, Component message,
                            int emotionLevel, boolean confirmationRequired) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new S2CGenieResponse(genieId, message, emotionLevel, confirmationRequired));
    }
}
