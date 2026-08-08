package dev.romankrukovsky.kubanhorizons.genie.player;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

            // Ищем онлайн игроков-джинний для вызова
            for (ServerPlayer onlinePlayer : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (onlinePlayer != player && onlinePlayer.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA).isGenie()) {
                    GenieMasterManager.summonGeniePlayer(serverLevel, player, onlinePlayer);
                    return InteractionResult.SUCCESS;
                }
            }

            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.lamp.no_genie_online"));
        }

        return InteractionResult.SUCCESS;
    }
}
