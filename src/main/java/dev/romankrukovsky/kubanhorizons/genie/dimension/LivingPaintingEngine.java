package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Движок живых картин-порталов и зеркального мира (Living Paintings & Mirror World Engine). */
public final class LivingPaintingEngine {
    private LivingPaintingEngine() {
    }

    public static boolean enterPaintingDimension(ServerLevel level, BlockPos pos, Player player) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(pos));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.living_painting"));
        return true;
    }

    public static boolean enterMirrorWorld(ServerLevel level, BlockPos pos, Player player) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(pos));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mirror_world"));
        return true;
    }
}
