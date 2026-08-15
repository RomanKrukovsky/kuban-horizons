package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselBond;
import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;
import dev.romankrukovsky.kubanhorizons.vessel.music.MusicBoxSchool;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Музыкальная шкатулка — сосуд школы EMOTION_ATMOSPHERE.
 *
 * <p>Каждое ПКМ переключает настроение по кругу и применяет его ауру
 * ({@link MusicBoxSchool.Mood}). Шкатулка слушает только настоящего владельца.</p>
 */
public class MusicBoxVesselItem extends VesselItem {

    public MusicBoxVesselItem(Properties props) {
        super(VesselType.MUSIC_BOX, props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        VesselBond bond = stack.get(dev.romankrukovsky.kubanhorizons.registry.KHDataComponents.VESSEL_BOND.get());
        if (bond == null || !bond.canBeLiftedBy(player)) {
            player.sendSystemMessage(Component.literal(
                    "§cШкатулка молчит. Она не признала вас."));
            return InteractionResult.FAIL;
        }
        MusicBoxSchool.Mood mood = MusicBoxSchool.nextMood(stack);
        MusicBoxSchool.storeMood(stack, mood);
        MusicBoxSchool.play((net.minecraft.server.level.ServerLevel) level, serverPlayer, mood);
        return InteractionResult.CONSUME;
    }
}