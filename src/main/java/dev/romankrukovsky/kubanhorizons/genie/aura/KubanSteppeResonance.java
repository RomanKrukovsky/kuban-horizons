package dev.romankrukovsky.kubanhorizons.genie.aura;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Резонанс Джиннии с Кубанской землёй («Хозяйка Вольной Степи»). */
public final class KubanSteppeResonance {
    private static final ResourceKey<Biome> KUBAN_STEPPE =
            ResourceKey.create(Registries.BIOME, KHIds.of("kuban_steppe"));
    private static final ResourceKey<Biome> RIVER_FLOODPLAIN =
            ResourceKey.create(Registries.BIOME, KHIds.of("river_floodplain"));
    private static final ResourceKey<Biome> PLAVNI =
            ResourceKey.create(Registries.BIOME, KHIds.of("plavni"));
    private static final ResourceKey<Biome> LIMAN =
            ResourceKey.create(Registries.BIOME, KHIds.of("liman"));

    private KubanSteppeResonance() {
    }

    public static boolean isKubanBiome(ServerLevel level, BlockPos pos) {
        var holder = level.getBiome(pos);
        return holder.is(KUBAN_STEPPE) || holder.is(RIVER_FLOODPLAIN) || holder.is(PLAVNI) || holder.is(LIMAN);
    }

    public static void tickResonance(KubanGenie genie, ServerLevel level) {
        BlockPos pos = genie.blockPosition();
        if (!isKubanBiome(level, pos)) {
            return;
        }

        // 1. Свечение растительности и стимуляция роста подсолнухов/винограда
        if (level.getRandom().nextInt(4) == 0) {
            BlockPos cropPos = pos.offset(level.getRandom().nextInt(11) - 5, level.getRandom().nextInt(5) - 2, level.getRandom().nextInt(11) - 5);
            BlockState state = level.getBlockState(cropPos);
            if (state.getBlock() instanceof BonemealableBlock growable) {
                if (growable.isValidBonemealTarget(level, cropPos, state)) {
                    growable.performBonemeal(level, level.getRandom(), cropPos, state);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5D, cropPos.getY() + 0.5D,
                            cropPos.getZ() + 0.5D, 6, 0.3D, 0.4D, 0.3D, 0.02D);
                }
            }
        }

        // 2. Умиротворение и успокоение животных в Вольной Степи
        AABB auraArea = genie.getBoundingBox().inflate(12.0D);
        var animals = level.getEntitiesOfClass(Animal.class, auraArea);
        for (Animal animal : animals) {
            if (level.getRandom().nextInt(10) == 0) {
                level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + animal.getBbHeight() + 0.2D,
                        animal.getZ(), 1, 0.1D, 0.1D, 0.1D, 0.0D);
            }
        }
    }
}
