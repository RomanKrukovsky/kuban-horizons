package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Музыкальная шкатулка — сосуд школы EMOTION_ATMOSPHERE.
 * Управляет эмоциональным состоянием и атмосферой.
 */
public class MusicBoxVesselItem extends VesselItem {

    public MusicBoxVesselItem(Properties props) {
        super(VesselType.MUSIC_BOX, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            var result = super.use(level, player, hand);
            if (result.getResult().consumesAction()) {
                player.sendSystemMessage(Component.literal(
                    "§9Шкатулка активирована. Атмосфера меняется..."
                ));
                // TODO: Открыть EmotionAtmosphereScreen / проиграть ambient sound
            }
            return result;
        }

        return InteractionResultHolder.sidedSuccess(stack, true);
    }
}
