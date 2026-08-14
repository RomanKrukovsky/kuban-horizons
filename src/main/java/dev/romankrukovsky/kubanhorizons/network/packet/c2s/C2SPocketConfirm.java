package dev.romankrukovsky.kubanhorizons.network.packet.c2s;

import dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CPocketResult;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: игрок подтвердил изменения карманного измерения. */
public record C2SPocketConfirm() implements CustomPacketPayload {

    public static final Type<C2SPocketConfirm> TYPE =
            new Type<>(KHIds.of("pocket_confirm"));

    public static final StreamCodec<FriendlyByteBuf, C2SPocketConfirm> CODEC =
            StreamCodec.unit(new C2SPocketConfirm());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPocketConfirm packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                PocketSceneService.Result result = PocketSceneService.confirm(sp);
                S2CPocketResult.send(sp, result.success(), result.message());
            }
        });
    }

    public static void send() {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new C2SPocketConfirm());
    }
}
