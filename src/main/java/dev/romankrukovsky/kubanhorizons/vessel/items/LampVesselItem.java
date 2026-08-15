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
 * Лампа — сосуд школы WISH_EXECUTION.
 * Исполняет желания через зарегистрированные capability.
 */
public class LampVesselItem extends VesselItem {

    public LampVesselItem(Properties props) {
        super(VesselType.LAMP, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Базовая проверка связи
            var result = super.use(level, player, hand);
            if (result.getResult().consumesAction()) {
                // Открываем интерфейс исполнения желаний
                player.sendSystemMessage(Component.literal(
                    "§eЛампа активирована. Сформулируйте желание."
                ));
                // TODO: Открыть WishExecutionScreen
            }
            return result;
        }

        return InteractionResultHolder.sidedSuccess(stack, true);
    }
}
