package dev.romankrukovsky.kubanhorizons.genie.expression;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Движок переписывания свойств биома в реальном времени (Biome Rewriter Engine). */
public final class BiomeRewriterEngine {
    private BiomeRewriterEngine() {
    }

    public static void rewriteLocalBiome(ServerLevel level, BlockPos center) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(center));
    }
}
