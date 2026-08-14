package dev.romankrukovsky.kubanhorizons.worldgen.dimension;

import dev.romankrukovsky.kubanhorizons.util.KHIds;
import java.util.List;
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

/** Два настоящих карманных мира: живая картина и зазеркалье. */
public final class KHMagicDimensions {
    public static final ResourceKey<Level> PAINTING_WORLD =
            ResourceKey.create(Registries.DIMENSION, KHIds.of("painting_world"));
    public static final ResourceKey<Level> MIRROR_WORLD =
            ResourceKey.create(Registries.DIMENSION, KHIds.of("mirror_world"));
    public static final ResourceKey<DimensionType> MAGIC_REALM_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, KHIds.of("magic_realm"));
    public static final ResourceKey<LevelStem> PAINTING_WORLD_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, KHIds.of("painting_world"));
    public static final ResourceKey<LevelStem> MIRROR_WORLD_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, KHIds.of("mirror_world"));
    public static final int FLOOR_Y = 64;

    private KHMagicDimensions() {
    }

    public static boolean isPocketDimension(ResourceKey<Level> dimension) {
        return dimension.equals(PAINTING_WORLD) || dimension.equals(MIRROR_WORLD) || dimension.equals(KHDimensions.POCKET);
    }

    public static void bootstrapType(BootstrapContext<DimensionType> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        context.register(MAGIC_REALM_TYPE, new DimensionType(
                true,
                true,
                false,
                false,
                1.0D,
                0,
                256,
                256,
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.3F,
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.END,
                CardinalLighting.Type.DEFAULT,
                atmosphere(),
                context.lookup(Registries.TIMELINE).getOrThrow(TimelineTags.UNIVERSAL),
                Optional.empty()));
    }

    private static EnvironmentAttributeMap atmosphere() {
        return EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.FOG_COLOR, 0x24143D)
                .set(EnvironmentAttributes.SKY_COLOR, 0x4B287D)
                .set(EnvironmentAttributes.FOG_START_DISTANCE, 80.0F)
                .set(EnvironmentAttributes.FOG_END_DISTANCE, 240.0F)
                .set(EnvironmentAttributes.SKY_FOG_END_DISTANCE, 240.0F)
                .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.8F)
                .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, 0xDCCBFF)
                .set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.55F)
                .set(EnvironmentAttributes.BED_RULE, new BedRule(
                        BedRule.Rule.NEVER,
                        BedRule.Rule.NEVER,
                        false,
                        Optional.of(Component.translatable(
                                "message.kubanhorizons.genie.magic_realm.no_sleep"))))
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .set(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, false)
                .build();
    }

    public static void bootstrapStem(BootstrapContext<LevelStem> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = context.lookup(Registries.DIMENSION_TYPE);
        context.register(PAINTING_WORLD_STEM, new LevelStem(
                types.getOrThrow(MAGIC_REALM_TYPE),
                new FlatLevelSource(flatSettings(biomes, Blocks.SANDSTONE))));
        context.register(MIRROR_WORLD_STEM, new LevelStem(
                types.getOrThrow(MAGIC_REALM_TYPE),
                new FlatLevelSource(flatSettings(biomes, Blocks.SMOOTH_QUARTZ))));
    }

    private static FlatLevelGeneratorSettings flatSettings(HolderGetter<Biome> biomes,
                                                            Block surface) {
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
                Optional.empty(), biomes.getOrThrow(Biomes.THE_VOID), List.of());
        settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BEDROCK));
        settings.getLayersInfo().add(new FlatLayerInfo(FLOOR_Y - 1, Blocks.POLISHED_BLACKSTONE));
        settings.getLayersInfo().add(new FlatLayerInfo(1, surface));
        settings.updateLayers();
        return settings;
    }
}
