package dev.romankrukovsky.kubanhorizons.network.packet.s2c;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C: синхронизация клиентского UX трансформации игрока в джиннию.
 *
 * <p>Серверная {@code PlayerGenieAttachment} уже везёт флаг и стадию через свой
 * {@code sync}-кодек, но прогресс желания туда не входит — он серверная
 * игровая логика. Этот пакет везёт недостающую пару: упрощённую клиентскую
 * стадию (0..3) и прогресс 0..100, чтобы HUD и экран трансформации рисовали
 * актуальное состояние, а не выдумку.</p>
 */
public record S2CTransformationSync(int stageIndex, float progress) implements CustomPacketPayload {

    public static final Type<S2CTransformationSync> TYPE =
            new Type<>(KHIds.of("transformation_sync"));

    public static final StreamCodec<FriendlyByteBuf, S2CTransformationSync> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.stageIndex());
                        buffer.writeFloat(packet.progress());
                    },
                    buffer -> new S2CTransformationSync(buffer.readVarInt(), buffer.readFloat())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, int stageIndex, float progress) {
        try {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new S2CTransformationSync(stageIndex, progress));
        } catch (RuntimeException ignored) {
        }
    }
}