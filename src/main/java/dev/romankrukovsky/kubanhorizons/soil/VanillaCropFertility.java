package dev.romankrukovsky.kubanhorizons.soil;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * Интеграция системы плодородия с ванильными культурами.
 *
 * <p>Ванильные культуры не знают о плодородии, поэтому влияние
 * реализовано событиями NeoForge:</p>
 * <ul>
 *   <li>{@link CropGrowEvent.Pre}: на истощённой почве часть попыток
 *       роста отменяется, на богатой — часть форсируется. Средняя
 *       скорость роста соответствует множителю
 *       {@link SoilFertility#growthMultiplier};</li>
 *   <li>{@link BreakBlockEvent}: сбор зрелой ванильной культуры
 *       игроком регистрируется в истории севооборота.</li>
 * </ul>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class VanillaCropFertility {
    private VanillaCropFertility() {
    }

    @SubscribeEvent
    static void onCropGrowPre(CropGrowEvent.Pre event) {
        if (!KHServerConfig.fertilityEnabled()
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        // Только классические культуры и стебли на грядке; наши культуры
        // учитывают плодородие сами.
        if (!(state.getBlock() instanceof CropBlock)
                && !(state.getBlock() instanceof StemBlock)
                && !(state.getBlock() instanceof AttachedStemBlock)) {
            return;
        }
        BlockPos farmland = event.getPos().below();
        float multiplier = SoilFertility.growthMultiplier(level, farmland);
        if (multiplier >= 1.0F) {
            // Богатая почва: дополнительный шанс форсировать рост.
            if (level.getRandom().nextFloat() < multiplier - 1.0F) {
                event.setResult(CropGrowEvent.Pre.Result.GROW);
            }
        } else {
            // Истощённая: часть попыток роста отменяется.
            if (level.getRandom().nextFloat() > multiplier) {
                event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            }
        }
    }

    @SubscribeEvent
    static void onBlockBreak(BreakBlockEvent event) {
        if (!KHServerConfig.fertilityEnabled()
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        // Зрелая ванильная культура → запись в севооборот.
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            SoilFertility.onHarvest(level, event.getPos().below(), crop);
        }
        // Арбуз/тыква: плод не стоит на грядке. Учитываем только реально
        // привязанный к нему стебель и истощаем грядку под этим стеблем.
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            attachedStemFarmland(level, event.getPos(), state)
                    .ifPresent(farmland -> SoilFertility.onHarvest(level, farmland, state.getBlock()));
        }
    }

    private static java.util.Optional<BlockPos> attachedStemFarmland(
            ServerLevel level, BlockPos fruitPos, BlockState fruitState) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos stemPos = fruitPos.relative(direction);
            BlockState stemState = level.getBlockState(stemPos);
            boolean matchingStem = fruitState.is(Blocks.MELON)
                    ? stemState.is(Blocks.ATTACHED_MELON_STEM)
                    : stemState.is(Blocks.ATTACHED_PUMPKIN_STEM);
            if (matchingStem
                    && stemState.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction.getOpposite()) {
                return java.util.Optional.of(stemPos.below());
            }
        }
        return java.util.Optional.empty();
    }
}
