package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.vessel.VesselBond;
import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;
import dev.romankrukovsky.kubanhorizons.vessel.schools.PersonalMagicSchool;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Кольцо — сосуд школы PERSONAL_MAGIC.
 *
 * <p>Каждое ПКМ переключает личную благодать по кругу: стойкость, стремительность,
 * могущество. Эффект привязан к владельцу и не влияет на окружение.</p>
 */
public class RingVesselItem extends VesselItem {

    public RingVesselItem(Properties props) {
        super(VesselType.RING, props);
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
            player.sendSystemMessage(Component.literal("§dКольцо погасло. Оно не признало вас."));
            return InteractionResult.FAIL;
        }
        new PersonalMagicSchool().cast((ServerLevel) level, serverPlayer, stack);
        return InteractionResult.CONSUME;
    }
}