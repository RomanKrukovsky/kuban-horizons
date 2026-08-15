package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselBond;
import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;
import dev.romankrukovsky.kubanhorizons.vessel.schools.IllusionSchool;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Зеркало — сосуд школы ILLUSION_ALTERNATE.
 *
 * <p>Каждое ПКМ переключает иллюзию по кругу: призрачный двойник, невидимость,
 * успокоение враждебных мобов. Только для настоящего владельца.</p>
 */
public class MirrorVesselItem extends VesselItem {

    public MirrorVesselItem(Properties props) {
        super(VesselType.MIRROR, props);
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
            player.sendSystemMessage(Component.literal("§bЗеркало затуманилось. Оно не признало вас."));
            return InteractionResult.FAIL;
        }
        new IllusionSchool().cast((ServerLevel) level, serverPlayer, stack);
        return InteractionResult.CONSUME;
    }
}