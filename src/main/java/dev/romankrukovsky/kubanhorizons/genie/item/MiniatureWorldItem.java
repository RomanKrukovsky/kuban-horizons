package dev.romankrukovsky.kubanhorizons.genie.item;

import dev.romankrukovsky.kubanhorizons.genie.spatial.MiniaturizationEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Разворачивает сохранённую область в пустом месте над выбранным блоком. */
public final class MiniatureWorldItem extends Item {
    public MiniatureWorldItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        boolean restored = MiniaturizationEngine.uncompressRegion(level,
                context.getClickedPos().relative(context.getClickedFace()), context.getItemInHand());
        if (!restored && context.getPlayer() != null) {
            context.getPlayer().sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.miniature.blocked"));
        }
        return restored ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }
}
