package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.crop.SunflowerCropBlock;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
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

        // Маслопресс: ориентируемый куб с уникальными текстурами.
        blockModels.createHorizontallyRotatedBlock(KHBlocks.OIL_PRESS.get(), TexturedModel.ORIENTABLE);

        // Плоские предметы.
        itemModels.generateFlatItem(KHItems.SUNFLOWER_SEEDS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.SUNFLOWER_HEAD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.SUNFLOWER_OIL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.OIL_CAKE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.ROASTED_SUNFLOWER_SEEDS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(KHItems.SOIL_PROBE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
    }

    private void registerSunflowerCrop(BlockModelGenerators blockModels) {
        Block crop = KHBlocks.SUNFLOWER_CROP.get();

        // Пустая модель для верхней половины одноблочных стадий не нужна:
        // верхний блок существует только при AGE >= 3. Однако blockstate
        // обязан покрыть все комбинации свойств, поэтому для UPPER при
        // AGE 0..2 используем модель воздуха быть не может — вместо этого
        // отдаём модель верхушки стадии 3 (состояние недостижимо в игре).
        Identifier[] bottom = new Identifier[SunflowerCropBlock.MAX_AGE + 1];
        Identifier[] top = new Identifier[SunflowerCropBlock.MAX_AGE + 1];

        for (int age = 0; age <= 2; age++) {
            // Одноблочные стадии: cross-модель.
            bottom[age] = ModelTemplates.CROSS.createWithSuffix(crop, "_stage" + age,
                    TextureMapping.cross(TextureMapping.getBlockTexture(crop, "_stage" + age)),
                    blockModels.modelOutput);
        }
        for (int age = 3; age <= SunflowerCropBlock.MAX_AGE; age++) {
            bottom[age] = ModelTemplates.CROSS.createWithSuffix(crop, "_bottom_stage" + age,
                    TextureMapping.cross(TextureMapping.getBlockTexture(crop, "_bottom_stage" + age)),
                    blockModels.modelOutput);
            top[age] = ModelTemplates.CROSS.createWithSuffix(crop, "_top_stage" + age,
                    TextureMapping.cross(TextureMapping.getBlockTexture(crop, "_top_stage" + age)),
                    blockModels.modelOutput);
        }
        // Недостижимые состояния UPPER для стадий 0..2 — модель бутона.
        top[0] = top[1] = top[2] = top[3];

        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(crop)
                .with(PropertyDispatch.initial(SunflowerCropBlock.AGE, BlockStateProperties.DOUBLE_BLOCK_HALF)
                        .generate((age, half) -> {
                            Identifier model = half == DoubleBlockHalf.UPPER ? top[age] : bottom[age];
                            return plainVariant(model);
                        })));
    }
}
