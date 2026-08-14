package dev.romankrukovsky.kubanhorizons.network.packet.s2c;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** S2C: результат выполнения wish (успех/ошибка + сообщение). */
public record S2CPocketResult(boolean success, Component message) implements CustomPacketPayload {

    public static final Type<S2CPocketResult> TYPE =
            new Type<>(KHIds.of("pocket_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPocketResult> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeBoolean(packet.success());
                        ComponentSerialization.STREAM_CODEC.encode(buffer, packet.message());
                    },
                    buffer -> new S2CPocketResult(
                            buffer.readBoolean(),
                            ComponentSerialization.STREAM_CODEC.decode(buffer)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, boolean success, Component message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new S2CPocketResult(success, message));
    }
}
