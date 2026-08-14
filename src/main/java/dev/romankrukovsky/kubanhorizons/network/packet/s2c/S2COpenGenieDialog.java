package dev.romankrukovsky.kubanhorizons.network.packet.s2c;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** S2C: открыть диалог с проверенной сервером джиннией. */
public record S2COpenGenieDialog(int genieId, Component genieName, GenieBehaviorMode mode)
        implements CustomPacketPayload {
    public static final Type<S2COpenGenieDialog> TYPE = new Type<>(KHIds.of("show_genie_dialog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenGenieDialog> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.genieId());
                        ComponentSerialization.STREAM_CODEC.encode(buffer, packet.genieName());
                        buffer.writeEnum(packet.mode());
                    },
                    buffer -> new S2COpenGenieDialog(
                            buffer.readVarInt(),
                            ComponentSerialization.STREAM_CODEC.decode(buffer),
                            buffer.readEnum(GenieBehaviorMode.class)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, KubanGenie genie) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new S2COpenGenieDialog(genie.getId(), genie.getDisplayName(), genie.brain().mode()));
    }
}
