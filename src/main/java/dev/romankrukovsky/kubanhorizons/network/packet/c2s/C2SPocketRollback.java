package dev.romankrukovsky.kubanhorizons.network.packet.c2s;

import dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CPocketResult;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: игрок отклонил изменения карманного измерения (откат). */
public record C2SPocketRollback() implements CustomPacketPayload {

    public static final Type<C2SPocketRollback> TYPE =
            new Type<>(KHIds.of("pocket_rollback"));

    public static final StreamCodec<FriendlyByteBuf, C2SPocketRollback> CODEC =
            StreamCodec.unit(new C2SPocketRollback());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPocketRollback packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                PocketSceneService.Result result = PocketSceneService.cancel(sp);
                S2CPocketResult.send(sp, result.success(), result.message());
            }
        });
    }

    public static void send() {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new C2SPocketRollback());
    }
}
