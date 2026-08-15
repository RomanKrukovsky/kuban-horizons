package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TimelineTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/**
 * Отдельное измерение для карманных сцен джиннии.
 *
 * <p>Карманные сцены строятся здесь, а не в оверворлде: каждая сцена занимает
 * свою область чанков, изолированную от обычного мира, и не может затронуть
 * региональный survival-контур. Пол — ровный, без шума и пещер: генератор
 * плоский (один слой бедрока), потому что содержимое сцены строит движок, а
 * не worldgen.</p>
 *
 * <p>Регистрация — обычный datapack-JSON через {@link BootstrapContext}, как
 * у «Вечной Кубани» и живых картин, поэтому измерение появляется в любом мире,
 * включая существующие сохранения, и не требует opt-in world preset.</p>
 */
public final class PocketDimension {
    public static final ResourceKey<Level> POCKET =
            ResourceKey.create(Registries.DIMENSION, KHIds.of("pocket"));

    public static final ResourceKey<DimensionType> POCKET_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, KHIds.of("pocket"));

    public static final ResourceKey<LevelStem> POCKET_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, KHIds.of("pocket"));

    public static final int FLOOR_Y = 64;

    private PocketDimension() {
    }

    public static void bootstrapType(BootstrapContext<DimensionType> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        context.register(POCKET_TYPE, new DimensionType(
                true,
                true,
                false,
                false,
                1.0D,
                0,
                256,
                256,
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.4F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.END,
                CardinalLighting.Type.DEFAULT,
                atmosphere(),
                context.lookup(Registries.TIMELINE).getOrThrow(TimelineTags.UNIVERSAL),
                Optional.empty()));
    }

    private static EnvironmentAttributeMap atmosphere() {
        return EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.FOG_COLOR, 0x1B1140)
                .set(EnvironmentAttributes.SKY_COLOR, 0x4B287D)
                .set(EnvironmentAttributes.FOG_START_DISTANCE, 64.0F)
                .set(EnvironmentAttributes.FOG_END_DISTANCE, 192.0F)
                .set(EnvironmentAttributes.SKY_FOG_END_DISTANCE, 192.0F)
                .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.6F)
                .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, 0xDCCBFF)
                .set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.5F)
                .set(EnvironmentAttributes.BED_RULE, new BedRule(
                        BedRule.Rule.NEVER,
                        BedRule.Rule.NEVER,
                        false,
                        Optional.of(Component.translatable(
                                "wish.kubanhorizons.pocket.no_sleep"))))
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .set(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, false)
                .build();
    }

    public static void bootstrapStem(BootstrapContext<LevelStem> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = context.lookup(Registries.DIMENSION_TYPE);
        context.register(POCKET_STEM, new LevelStem(
                types.getOrThrow(POCKET_TYPE),
                new FlatLevelSource(flatSettings(biomes))));
    }

    private static FlatLevelGeneratorSettings flatSettings(HolderGetter<Biome> biomes) {
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
                Optional.empty(), biomes.getOrThrow(Biomes.THE_VOID), java.util.List.of());
        settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BEDROCK));
        settings.updateLayers();
        return settings;
    }
}