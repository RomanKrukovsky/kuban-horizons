package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselBond;
import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;
import dev.romankrukovsky.kubanhorizons.vessel.schools.CreatureCreationSchool;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Кувшин — сосуд школы CREATURE_CREATION.
 *
 * <p>Каждое ПКМ призывает спутника по кругу: ручной волк, светлячок-эллай,
 * овца-компаньон. Существа появляются перед владельцем и не враждебны.</p>
 */
public class JugVesselItem extends VesselItem {

    public JugVesselItem(Properties props) {
        super(VesselType.JUG, props);
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
            player.sendSystemMessage(Component.literal("§2Кувшин запечатан. Он не признал вас."));
            return InteractionResult.FAIL;
        }
        new CreatureCreationSchool().cast((ServerLevel) level, serverPlayer, stack);
        return InteractionResult.CONSUME;
    }
}