package dev.romankrukovsky.kubanhorizons.network.packet.c2s;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.genie.vessel.OwnerDeathProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: игрок выбрал вариант после смерти владельца. */
public record C2SOwnerDeathChoice(OwnerDeathProtocol.DeathChoice choice) implements CustomPacketPayload {

    public static final Type<C2SOwnerDeathChoice> TYPE =
            new Type<>(new net.minecraft.util.ResourceLocation(KubanHorizons.MOD_ID, "owner_death_choice"));

    public static final StreamCodec<FriendlyByteBuf, C2SOwnerDeathChoice> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeEnum(packet.choice()),
                    buffer -> new C2SOwnerDeathChoice(buffer.readEnum(OwnerDeathProtocol.DeathChoice.class))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SOwnerDeathChoice packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                OwnerDeathProtocol.executeChoice(sp, packet.choice());
            }
        });
    }

    public static void send(OwnerDeathProtocol.DeathChoice choice) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new C2SOwnerDeathChoice(choice));
    }
}
```