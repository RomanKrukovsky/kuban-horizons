package dev.romankrukovsky.kubanhorizons.network.packet.s2c;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.client.screen.OwnerDeathChoiceScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** S2C: открывает экран выбора после смерти владельца джиннии. */
public record S2COpenOwnerDeathScreen(UUID genieId) implements CustomPacketPayload {

    public static final Type<S2COpenOwnerDeathScreen> TYPE =
            new Type<>(new net.minecraft.util.ResourceLocation(KubanHorizons.MOD_ID, "open_owner_death_screen"));

    public static final StreamCodec<FriendlyByteBuf, S2COpenOwnerDeathScreen> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeUUID(packet.genieId()),
                    buffer -> new S2COpenOwnerDeathScreen(buffer.readUUID())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2COpenOwnerDeathScreen packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                // Передаём genieId в экран для возможного отображения имени/инфо о джиннии
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new OwnerDeathChoiceScreen(packet.genieId()));
            }
        });
    }

    public static void send(ServerPlayer player, UUID genieId) {
        player.connection.send(new S2COpenOwnerDeathScreen(genieId));
    }
}
```