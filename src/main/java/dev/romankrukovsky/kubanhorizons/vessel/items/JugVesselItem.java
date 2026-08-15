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
 * Кувшин — сосуд школы CREATURE_CREATION.
 * Порождает и связывает существ.
 */
public class JugVesselItem extends VesselItem {

    public JugVesselItem(Properties props) {
        super(VesselType.JUG, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            var result = super.use(level, player, hand);
            if (result.getResult().consumesAction()) {
                player.sendSystemMessage(Component.literal(
                    "§2Кувшин активирован. Кого вы хотите призвать?"
                ));
                // TODO: Открыть CreatureCreationScreen
            }
            return result;
        }

        return InteractionResultHolder.sidedSuccess(stack, true);
    }
}
