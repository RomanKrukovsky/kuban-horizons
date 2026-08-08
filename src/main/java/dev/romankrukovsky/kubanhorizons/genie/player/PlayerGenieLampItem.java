package dev.romankrukovsky.kubanhorizons.genie.player;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Лампа-сосуд превращённого игрока-Джиннии.
 */
public class PlayerGenieLampItem extends Item {
    public PlayerGenieLampItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            MagicalSignature.cast(serverLevel, player.position());

            String genieId = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getStringOr("GeniePlayer", "");
            if (genieId.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.lamp.unbound"));
                return InteractionResult.FAIL;
            }
            ServerPlayer geniePlayer;
            try {
                geniePlayer = serverLevel.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(genieId));
            } catch (IllegalArgumentException exception) {
                geniePlayer = null;
            }
            if (geniePlayer != null && geniePlayer != player
                    && geniePlayer.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA).isGenie()) {
                GenieMasterManager.summonGeniePlayer(serverLevel, player, geniePlayer);
                return InteractionResult.SUCCESS;
            }

            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.lamp.no_genie_online"));
        }

        return InteractionResult.SUCCESS;
    }
}
