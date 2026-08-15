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
 * Кольцо — сосуд школы PERSONAL_MAGIC.
 * Усиливает личные способности владельца.
 */
public class RingVesselItem extends VesselItem {

    public RingVesselItem(Properties props) {
        super(VesselType.RING, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            var result = super.use(level, player, hand);
            if (result.getResult().consumesAction()) {
                player.sendSystemMessage(Component.literal(
                    "§dКольцо активировано. Личная сила течёт."
                ));
                // TODO: Применить персональные баффы / открыть PersonalMagicScreen
            }
            return result;
        }

        return InteractionResultHolder.sidedSuccess(stack, true);
    }
}
