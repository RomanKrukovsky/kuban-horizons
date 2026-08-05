package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.crop.CornCropBlock;
import dev.romankrukovsky.kubanhorizons.crop.DoubleCropBlock;
import dev.romankrukovsky.kubanhorizons.crop.SunflowerCropBlock;
import dev.romankrukovsky.kubanhorizons.crop.TeaBushBlock;
import dev.romankrukovsky.kubanhorizons.irrigation.IrrigationChannelBlock;
import dev.romankrukovsky.kubanhorizons.irrigation.WaterIntakeBlock;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

/**
 * Генерация blockstates, блок-моделей и item-моделей.
 *
 * <p>Подсолнечник моделируется по схеме pitcher crop: одноблочные стадии
 * 0–2 используют cross-модели с пустой моделью верхней половины, двухблочные
 * стадии 3–4 — пары bottom/top.</p>
 */
public final class KHModelProvider extends ModelProvider {
    public KHModelProvider(PackOutput output) {
        super(output, KubanHorizons.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        registerSunflowerCrop(blockModels);
        registerDoubleCrop(blockModels, KHBlocks.CORN_CROP.get(), CornCropBlock.MAX_AGE, 2);
        registerTeaBush(blockModels);

        // Маслопресс: ориентируемый куб с уникальными текстурами.
        blockModels.createHorizontallyRotatedBlock(KHBlocks.OIL_PRESS.get(), TexturedModel.ORIENTABLE);

        registerIrrigation(blockModels);

        // Плоские предметы.
        itemModels.generateFlatItem(KHItems.SUNFLOWER_SEEDS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.SUNFLOWER_HEAD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.SUNFLOWER_OIL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.OIL_CAKE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.ROASTED_SUNFLOWER_SEEDS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.SOIL_PROBE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(KHItems.CORN_KERNELS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.CORN_COB.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.GRILLED_CORN.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.TEA_SAPLING.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.TEA_LEAVES.get(), ModelTemplates.FLAT_ITEM);
    }

    /**
     * Блоки орошения: модели написаны вручную (нестандартная геометрия
     * желоба — допустимое исключение AD-005), blockstates и item-модели
     * генерируются здесь.
     */
    private void registerIrrigation(BlockModelGenerators blockModels) {
        Block channel = KHBlocks.IRRIGATION_CHANNEL.get();
        Identifier dry = KHIds.of("block/irrigation_channel_dry");
        Identifier filled = KHIds.of("block/irrigation_channel_filled");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(channel)
                .with(PropertyDispatch.initial(IrrigationChannelBlock.DISTANCE)
                        .generate(distance -> plainVariant(distance > 0 ? filled : dry))));
        blockModels.registerSimpleItemModel(channel.asItem(), dry);

        Block intake = KHBlocks.WATER_INTAKE.get();
        Identifier idle = KHIds.of("block/water_intake");
        Identifier active = KHIds.of("block/water_intake_active");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(intake)
                .with(PropertyDispatch.initial(WaterIntakeBlock.ACTIVE)
                        .generate(isActive -> plainVariant(isActive ? active : idle))));
        blockModels.registerSimpleItemModel(intake.asItem(), idle);
    }

    /** Двухблочная культура по схеме pitcher crop: cross-модели стадий. */
    private void registerDoubleCrop(BlockModelGenerators blockModels, Block crop, int maxAge, int doubleAge) {
        Identifier[] bottom = new Identifier[maxAge + 1];
        Identifier[] top = new Identifier[maxAge + 1];

        for (int age = 0; age < doubleAge; age++) {
            bottom[age] = ModelTemplates.CROSS.createWithSuffix(crop, "_stage" + age,
                    TextureMapping.cross(TextureMapping.getBlockTexture(crop, "_stage" + age)),
                    blockModels.modelOutput);
        }
        for (int age = doubleAge; age <= maxAge; age++) {
            bottom[age] = ModelTemplates.CROSS.createWithSuffix(crop, "_bottom_stage" + age,
                    TextureMapping.cross(TextureMapping.getBlockTexture(crop, "_bottom_stage" + age)),
                    blockModels.modelOutput);
            top[age] = ModelTemplates.CROSS.createWithSuffix(crop, "_top_stage" + age,
                    TextureMapping.cross(TextureMapping.getBlockTexture(crop, "_top_stage" + age)),
                    blockModels.modelOutput);
        }
        // Недостижимые состояния UPPER для одноблочных стадий — модель
        // первой двухблочной верхушки (в игре не встречаются).
        for (int age = 0; age < doubleAge; age++) {
            top[age] = top[doubleAge];
        }

        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(crop)
                .with(PropertyDispatch.initial(DoubleCropBlock.AGE, BlockStateProperties.DOUBLE_BLOCK_HALF)
                        .generate((age, half) -> plainVariant(
                                half == DoubleBlockHalf.UPPER ? top[age] : bottom[age]))));
    }

    private void registerSunflowerCrop(BlockModelGenerators blockModels) {
        registerDoubleCrop(blockModels, KHBlocks.SUNFLOWER_CROP.get(), SunflowerCropBlock.MAX_AGE, 3);
    }

    /** Чайный куст: cross-модели четырёх стадий. */
    private void registerTeaBush(BlockModelGenerators blockModels) {
        Block bush = KHBlocks.TEA_BUSH.get();
        Identifier[] models = new Identifier[TeaBushBlock.MAX_AGE + 1];
        for (int age = 0; age <= TeaBushBlock.MAX_AGE; age++) {
            models[age] = ModelTemplates.CROSS.createWithSuffix(bush, "_stage" + age,
                    TextureMapping.cross(TextureMapping.getBlockTexture(bush, "_stage" + age)),
                    blockModels.modelOutput);
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(bush)
                .with(PropertyDispatch.initial(TeaBushBlock.AGE)
                        .generate(age -> plainVariant(models[age]))));
    }
}
