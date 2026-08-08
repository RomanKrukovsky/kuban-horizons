package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Движок Театра Реальности и визуальной реконструкции прошлых событий (Visual History Reenactment Engine). */
public final class VisualReenactmentEngine {
    private VisualReenactmentEngine() {
    }

    public static boolean reenactPastEvent(ServerLevel level, BlockPos origin, Player player) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(origin));

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, origin.getX() + 0.5D, origin.getY() + 1.2D, origin.getZ() + 0.5D,
                50, 0.8D, 1.2D, 0.8D, 0.05D);
        level.sendParticles(ParticleTypes.PORTAL, origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D,
                30, 0.6D, 0.8D, 0.6D, 0.03D);

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.theater_reenactment"));
        return true;
    }
}
