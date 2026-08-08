package dev.romankrukovsky.kubanhorizons.genie.expression;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import java.util.ArrayList;
import java.util.List;

/** Движок переписывания свойств биома в реальном времени (Biome Rewriter Engine). */
public final class BiomeRewriterEngine {
    private BiomeRewriterEngine() {
    }

    public static boolean rewriteLocalBiome(ServerLevel level, BlockPos center) {
        var biomes = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var target = biomes.get(KHBiomes.KUBAN_STEPPE).orElse(null);
        if (target == null) {
            return false;
        }
        int radius = 16;
        ChunkPos min = ChunkPos.containing(center.offset(-radius, 0, -radius));
        ChunkPos max = ChunkPos.containing(center.offset(radius, 0, radius));
        List<net.minecraft.world.level.chunk.LevelChunk> changedChunks = new ArrayList<>();
        for (int chunkX = min.x(); chunkX <= max.x(); chunkX++) {
            for (int chunkZ = min.z(); chunkZ <= max.z(); chunkZ++) {
                var chunk = level.getChunk(chunkX, chunkZ);
                int quartMinX = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
                int quartMinZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
                for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
                    var section = chunk.getSection(sectionIndex);
                    int quartMinY = QuartPos.fromSection(chunk.getMinSectionY() + sectionIndex);
                    section.fillBiomesFromNoise((quartX, quartY, quartZ, sampler) -> {
                        int blockX = QuartPos.toBlock(quartX) + 2;
                        int blockZ = QuartPos.toBlock(quartZ) + 2;
                        int dx = blockX - center.getX();
                        int dz = blockZ - center.getZ();
                        return dx * dx + dz * dz <= radius * radius ? target
                                : section.getNoiseBiome(quartX & 3, quartY & 3, quartZ & 3);
                    }, null, quartMinX, quartMinY, quartMinZ);
                }
                chunk.markUnsaved();
                changedChunks.add(chunk);
            }
        }
        ClientboundChunksBiomesPacket packet = ClientboundChunksBiomesPacket.forChunks(changedChunks);
        for (var player : level.players()) {
            player.connection.send(packet);
        }
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(center));
        return true;
    }
}
