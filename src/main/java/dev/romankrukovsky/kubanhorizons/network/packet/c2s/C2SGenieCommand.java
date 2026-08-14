package dev.romankrukovsky.kubanhorizons.network.packet.c2s;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.genie.GenieConversationService;
import dev.romankrukovsky.kubanhorizons.genie.GenieBehaviorMode;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: игрок выбрал режим поведения конкретной джиннии через радиальное меню. */
public record C2SGenieCommand(int genieId, GenieBehaviorMode mode) implements CustomPacketPayload {

    public static final Type<C2SGenieCommand> TYPE =
            new Type<>(KHIds.of("genie_command"));

    public static final StreamCodec<FriendlyByteBuf, C2SGenieCommand> CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeVarInt(packet.genieId());
                        buffer.writeEnum(packet.mode());
                    },
                    buffer -> new C2SGenieCommand(buffer.readVarInt(), buffer.readEnum(GenieBehaviorMode.class))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SGenieCommand packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                if (!GenieConversationService.changeMode(sp, packet.genieId(), packet.mode())) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "message.kubanhorizons.genie.ai.no_companion"));
                    return;
                }
                sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.kubanhorizons.genie.ai.mode",
                        net.minecraft.network.chat.Component.translatable(packet.mode().translationKey())));
            }
        });
    }

    public static void send(int genieId, GenieBehaviorMode mode) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new C2SGenieCommand(genieId, mode));
    }
}
