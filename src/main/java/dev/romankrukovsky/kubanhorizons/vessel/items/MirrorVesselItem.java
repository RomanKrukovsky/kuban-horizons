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
 * Зеркало — сосуд школы ILLUSION_ALTERNATE.
 * Открывает иллюзии и карманные измерения.
 */
public class MirrorVesselItem extends VesselItem {

    public MirrorVesselItem(Properties props) {
        super(VesselType.MIRROR, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            var result = super.use(level, player, hand);
            if (result.getResult().consumesAction()) {
                player.sendSystemMessage(Component.literal(
                    "§bЗеркало активировано. Отражение реальности..."
                ));
                // TODO: Открыть MirrorSmartphoneScreen (wish map, contracts, messages, history, settlement, archive)
            }
            return result;
        }

        return InteractionResultHolder.sidedSuccess(stack, true);
    }
}
