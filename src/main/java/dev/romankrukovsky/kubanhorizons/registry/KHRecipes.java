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

    private KHRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
        BOOK_CATEGORIES.register(modEventBus);
    }
}
