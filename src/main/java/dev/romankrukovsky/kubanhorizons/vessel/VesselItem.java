package dev.romankrukovsky.kubanhorizons.vessel;

import dev.romankrukovsky.kubanhorizons.registry.KHDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Базовый класс живого сосуда — предмета с сознанием и привязкой.
 */
public class VesselItem extends Item {
    private final VesselType vesselType;

    public VesselItem(VesselType type, Properties props) {
        super(props.stacksTo(1));
        this.vesselType = type;
    }

    public VesselType getVesselType() {
        return vesselType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            VesselBond bond = getOrCreateBond(stack);

            if (!bond.canBeLiftedBy(player)) {
                player.sendSystemMessage(Component.literal(
                    "§cСосуд отказывается быть поднятым. Он верен своему владельцу."
                ));
                return InteractionResultHolder.fail(stack);
            }

            if (bond.attemptBond(player)) {
                player.sendSystemMessage(Component.literal(
                    "§aСосуд признал вас. Связь установлена."
                ));
                saveBond(stack, bond);
            } else {
                player.sendSystemMessage(Component.literal(
                    "§cСосуд отвергает вас. Он уже связан с другим."
                ));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private VesselBond getOrCreateBond(ItemStack stack) {
        VesselBond bond = stack.get(KHDataComponents.VESSEL_BOND.get());
        return bond != null ? bond : new VesselBond();
    }

    private void saveBond(ItemStack stack, VesselBond bond) {
        stack.set(KHDataComponents.VESSEL_BOND.get(), bond);
    }

    @Override
    public Component getName(ItemStack stack) {
        VesselBond bond = stack.get(KHDataComponents.VESSEL_BOND.get());
        if (bond != null && bond.getOwnerName() != null) {
            return Component.literal("§6" + vesselType.name() + " §7(владелец: " + bond.getOwnerName() + ")");
        }
        return Component.literal("§e" + vesselType.name() + " §7(бесхозный)");
    }
}
