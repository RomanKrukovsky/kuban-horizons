package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import net.minecraft.data.BlockFamily;

/**
 * Семейства строительных блоков мода.
 *
 * <p>Один и тот же {@link BlockFamily} питает и генератор моделей
 * ({@link KHModelProvider}), и генератор рецептов ({@link KHRecipeProvider}) —
 * набор вариантов описан ровно один раз, поэтому модель и рецепт не могут
 * разойтись.</p>
 *
 * <p>Ванильный {@code BlockFamilies.familyBuilder} закрыт для мода (приватен и
 * бросает исключение на повторной регистрации базового блока), поэтому
 * {@link BlockFamily.Builder} создаётся напрямую. Побочный эффект приятный:
 * модовые семейства не попадают в {@code BlockFamilies.getAllFamilies()} и не
 * влияют на ванильную генерацию.</p>
 *
 * <p>Рецепты камнереза семейством не генерируются: ванильный
 * {@code stonecutterResultFromBase} сохраняет рецепт по строковому имени без
 * пространства имён, и {@code Identifier.parse} отнёс бы его к
 * {@code minecraft}. Такие рецепты пишутся вручную в
 * {@link KHRecipeProvider} с явным {@code kubanhorizons:}.</p>
 */
final class KHBlockFamilies {
    /** Саман: по образцу ванильного грязевого кирпича (ступени, плита, стенка). */
    static final BlockFamily ADOBE_BRICKS =
            new BlockFamily.Builder(KHBlocks.ADOBE_BRICKS.get())
                    .stairs(KHBlocks.ADOBE_BRICK_STAIRS.get())
                    .slab(KHBlocks.ADOBE_BRICK_SLAB.get())
                    .wall(KHBlocks.ADOBE_BRICK_WALL.get())
                    .getFamily();

    /** Ракушечник: тот же набор вариантов, режется камнерезом. */
    static final BlockFamily SHELL_ROCK =
            new BlockFamily.Builder(KHBlocks.SHELL_ROCK.get())
                    .stairs(KHBlocks.SHELL_ROCK_STAIRS.get())
                    .slab(KHBlocks.SHELL_ROCK_SLAB.get())
                    .wall(KHBlocks.SHELL_ROCK_WALL.get())
                    .getFamily();

    /** Белёная штукатурка: полный блок, ступеньки и плита. */
    static final BlockFamily WHITEWASHED_PLASTER =
            new BlockFamily.Builder(KHBlocks.WHITEWASHED_PLASTER.get())
                    .stairs(KHBlocks.WHITEWASHED_PLASTER_STAIRS.get())
                    .slab(KHBlocks.WHITEWASHED_PLASTER_SLAB.get())
                    .getFamily();

    /**
     * Плетень: базовый блок — сама ограда, полноценного куба нет.
     *
     * <p>Семейство используется только для моделей: варианты
     * {@code CUSTOM_FENCE}/{@code CUSTOM_FENCE_GATE} рисуются по схеме
     * бамбукового плетня. Рецепты пишутся вручную в {@link KHRecipeProvider},
     * потому что {@code getBaseBlockForCrafting} вернул бы саму ограду как
     * сырьё для самой себя.</p>
     */
    static final BlockFamily WATTLE =
            new BlockFamily.Builder(KHBlocks.WATTLE.get())
                    .customFence(KHBlocks.WATTLE.get())
                    .customFenceGate(KHBlocks.WATTLE_GATE.get())
                    .dontGenerateCraftingRecipe()
                    .getFamily();

    private KHBlockFamilies() {
    }
}
