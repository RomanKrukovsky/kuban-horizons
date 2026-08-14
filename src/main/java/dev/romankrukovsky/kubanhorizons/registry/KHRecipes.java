package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.processing.OilPressingRecipe;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация типов рецептов, сериализаторов и категорий книги рецептов.
 */
public final class KHRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, KubanHorizons.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, KubanHorizons.MOD_ID);
    private static final DeferredRegister<RecipeBookCategory> BOOK_CATEGORIES =
            DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, KubanHorizons.MOD_ID);

    /** Тип рецепта «отжим масла». */
    public static final DeferredHolder<RecipeType<?>, RecipeType<OilPressingRecipe>> OIL_PRESSING_TYPE =
            TYPES.register("oil_pressing", () -> RecipeType.simple(KHIds.of("oil_pressing")));

    /** Сериализатор рецепта «отжим масла». */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<OilPressingRecipe>> OIL_PRESSING_SERIALIZER =
            SERIALIZERS.register("oil_pressing",
                    () -> new RecipeSerializer<>(OilPressingRecipe.MAP_CODEC, OilPressingRecipe.STREAM_CODEC));

    /** Категория книги рецептов для устройств переработки мода. */
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> OIL_PRESSING_CATEGORY =
            BOOK_CATEGORIES.register("oil_pressing", RecipeBookCategory::new);

    /** Тип рецепта «сушка». */
    public static final DeferredHolder<RecipeType<?>, RecipeType<dev.romankrukovsky.kubanhorizons.processing.DryingRecipe>> DRYING_TYPE =
            TYPES.register("drying", () -> RecipeType.simple(KHIds.of("drying")));

    /** Сериализатор рецепта «сушка». */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dev.romankrukovsky.kubanhorizons.processing.DryingRecipe>> DRYING_SERIALIZER =
            SERIALIZERS.register("drying",
                    () -> new RecipeSerializer<>(
                            dev.romankrukovsky.kubanhorizons.processing.DryingRecipe.MAP_CODEC,
                            dev.romankrukovsky.kubanhorizons.processing.DryingRecipe.STREAM_CODEC));

    /** Категория книги рецептов «сушка». */
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> DRYING_CATEGORY =
            BOOK_CATEGORIES.register("drying", RecipeBookCategory::new);

    /** Тип рецепта «копчение». */
    public static final DeferredHolder<RecipeType<?>, RecipeType<dev.romankrukovsky.kubanhorizons.processing.SmokingProcessRecipe>> SMOKING_TYPE =
            TYPES.register("smoking_process", () -> RecipeType.simple(KHIds.of("smoking_process")));

    /** Сериализатор рецепта «копчение». */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dev.romankrukovsky.kubanhorizons.processing.SmokingProcessRecipe>> SMOKING_SERIALIZER =
            SERIALIZERS.register("smoking_process",
                    () -> new RecipeSerializer<>(
                            dev.romankrukovsky.kubanhorizons.processing.SmokingProcessRecipe.MAP_CODEC,
                            dev.romankrukovsky.kubanhorizons.processing.SmokingProcessRecipe.STREAM_CODEC));

    /** Категория книги рецептов «копчение». */
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> SMOKING_CATEGORY =
            BOOK_CATEGORIES.register("smoking_process", RecipeBookCategory::new);

    /** Тип рецепта «помол». */
    public static final DeferredHolder<RecipeType<?>, RecipeType<dev.romankrukovsky.kubanhorizons.processing.MillingRecipe>> MILLING_TYPE =
            TYPES.register("milling", () -> RecipeType.simple(KHIds.of("milling")));

    /** Сериализатор рецепта «помол». */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dev.romankrukovsky.kubanhorizons.processing.MillingRecipe>> MILLING_SERIALIZER =
            SERIALIZERS.register("milling",
                    () -> new RecipeSerializer<>(
                            dev.romankrukovsky.kubanhorizons.processing.MillingRecipe.MAP_CODEC,
                            dev.romankrukovsky.kubanhorizons.processing.MillingRecipe.STREAM_CODEC));

    /** Категория книги рецептов «помол». */
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> MILLING_CATEGORY =
            BOOK_CATEGORIES.register("milling", RecipeBookCategory::new);

    /** Тип рецепта «нарезка» (разделочный стол). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<dev.romankrukovsky.kubanhorizons.processing.CuttingRecipe>> CUTTING_TYPE =
            TYPES.register("cutting", () -> RecipeType.simple(KHIds.of("cutting")));

    /** Сериализатор рецепта «нарезка». */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dev.romankrukovsky.kubanhorizons.processing.CuttingRecipe>> CUTTING_SERIALIZER =
            SERIALIZERS.register("cutting",
                    () -> new RecipeSerializer<>(
                            dev.romankrukovsky.kubanhorizons.processing.CuttingRecipe.MAP_CODEC,
                            dev.romankrukovsky.kubanhorizons.processing.CuttingRecipe.STREAM_CODEC));

    /** Категория книги рецептов «нарезка». */
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> CUTTING_CATEGORY =
            BOOK_CATEGORIES.register("cutting", RecipeBookCategory::new);

    /**
     * Тип рецепта «давка сока» (виноградный чан).
     *
     * <p>Отдельный тип, а не {@code oil_pressing}: у давки другой состав полей
     * (выход сока на единицу и стоимость бутылки вместо жмыха и длительности
     * цикла), а общий тип заставил бы маслопресс принимать виноград, а чан —
     * семечки. Подробнее — javadoc {@code PressingRecipe}.</p>
     */
    public static final DeferredHolder<RecipeType<?>, RecipeType<dev.romankrukovsky.kubanhorizons.processing.PressingRecipe>> PRESSING_TYPE =
            TYPES.register("pressing", () -> RecipeType.simple(KHIds.of("pressing")));

    /** Сериализатор рецепта «давка сока». */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<dev.romankrukovsky.kubanhorizons.processing.PressingRecipe>> PRESSING_SERIALIZER =
            SERIALIZERS.register("pressing",
                    () -> new RecipeSerializer<>(
                            dev.romankrukovsky.kubanhorizons.processing.PressingRecipe.MAP_CODEC,
                            dev.romankrukovsky.kubanhorizons.processing.PressingRecipe.STREAM_CODEC));

    /** Категория книги рецептов «давка сока». */
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> PRESSING_CATEGORY =
            BOOK_CATEGORIES.register("pressing", RecipeBookCategory::new);

    private KHRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
        BOOK_CATEGORIES.register(modEventBus);
    }
}
