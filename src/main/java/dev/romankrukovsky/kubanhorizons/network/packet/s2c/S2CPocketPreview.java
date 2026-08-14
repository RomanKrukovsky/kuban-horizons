package dev.romankrukovsky.kubanhorizons.network.packet.s2c;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** S2C: превью изменений карманного измерения перед подтверждением. */
public record S2CPocketPreview(int changedBlocks, int durationTicks, String risk)
        implements CustomPacketPayload {

    public static final Type<S2CPocketPreview> TYPE =
            new Type<>(KHIds.of("pocket_preview"));

    public static final StreamCodec<FriendlyByteBuf, S2CPocketPreview> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.changedBlocks());
                        buffer.writeVarInt(packet.durationTicks());
                        buffer.writeUtf(packet.risk(), 32);
                    },
                    buffer -> new S2CPocketPreview(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(32))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, int changedBlocks, int durationTicks, String risk) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new S2CPocketPreview(changedBlocks, durationTicks, risk));
    }
}
