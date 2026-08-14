package dev.romankrukovsky.kubanhorizons.genie.item;

import dev.romankrukovsky.kubanhorizons.genie.dimension.LivingPaintingEngine;
import dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Магическое зеркало служит безопасным двухточечным выделителем strong-wish runtime. */
public final class MagicMirrorItem extends Item {
    public MagicMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (LivingPaintingEngine.isInside(player)) {
            return LivingPaintingEngine.leave(serverPlayer)
                    ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
        }
        boolean entered = player.isShiftKeyDown()
                ? LivingPaintingEngine.enterPaintingDimension(
                        serverLevel, player.blockPosition(), player)
                : LivingPaintingEngine.enterMirrorWorld(
                        serverLevel, player.blockPosition(), player);
        return entered ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (LivingPaintingEngine.isInside(player)) {
            return LivingPaintingEngine.leave(player)
                    ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            return LivingPaintingEngine.enterPaintingDimension(
                    level, context.getClickedPos(), player)
                    ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
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
