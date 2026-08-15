package dev.romankrukovsky.kubanhorizons.vessel.items;

import dev.romankrukovsky.kubanhorizons.genie.wish.WishParser;
import dev.romankrukovsky.kubanhorizons.vessel.VesselBond;
import dev.romankrukovsky.kubanhorizons.vessel.VesselItem;
import dev.romankrukovsky.kubanhorizons.vessel.VesselType;
import dev.romankrukovsky.kubanhorizons.vessel.schools.WishExecutionSchool;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Лампа — сосуд школы WISH_EXECUTION.
 *
 * <p>Владелец держит в другой руке переименованную бумагу с желанием и жмёт
 * ПКМ лампой: лампа разбирает текст и исполняет его через общий wish-рантайм.</p>
 */
public class LampVesselItem extends VesselItem {

    public LampVesselItem(Properties props) {
        super(VesselType.LAMP, props);
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
            player.sendSystemMessage(Component.literal("§cЛампа молчит. Она не признала вас."));
            return InteractionResult.FAIL;
        }
        ItemStack paper = player.getOffhandItem();
        String wording = paper.getItem().toString();
        if (paper.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            wording = paper.getHoverName().getString();
        } else {
            wording = null;
        }
        if (wording == null || wording.isBlank()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.lamp.unbound"));
            return InteractionResult.FAIL;
        }
        // Желание уходит в общий парсер, как если бы его произнесли джиннии.
        var intent = WishParser.parse(wording);
        var result = dev.romankrukovsky.kubanhorizons.genie.wish.WishExecutor
                .execute((net.minecraft.server.level.ServerLevel) level, serverPlayer, intent);
        player.sendSystemMessage(result.message(intent.precision()));
        return InteractionResult.CONSUME;
    }
}