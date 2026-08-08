package dev.romankrukovsky.kubanhorizons.genie.item;

import dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Магическое зеркало служит безопасным двухточечным выделителем strong-wish runtime. */
public final class MagicMirrorItem extends Item {
    public MagicMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        try {
            WishRuntime runtime = WishRuntime.get(level.getServer());
            var update = runtime.select(player, context.getClickedPos());
            if (update.completedSelection().isPresent()) {
                var selection = update.completedSelection().orElseThrow();
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.runtime.selection_complete",
                        selection.volume(), selection.chunkCount()));
            } else {
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.runtime.selection_first",
                        update.point().getX(), update.point().getY(), update.point().getZ()));
            }
            return InteractionResult.CONSUME;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.runtime.failed", exception.getMessage()));
            return InteractionResult.FAIL;
        }
    }
}
