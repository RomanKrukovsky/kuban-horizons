package dev.romankrukovsky.kubanhorizons.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.registry.KHRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Рецепт нарезки на разделочном столе.
 *
 * <p>Отличается от помола двумя вещами, и обе — механические, а не
 * косметические: результатов может быть до трёх (нарезка даёт несколько частей),
 * а инструмент задаётся тегом — нож режет иначе, чем топор. Требование
 * конкретного инструмента и есть то, что делает разделочный стол отдельным
 * устройством, а не вторым названием мельницы.</p>
 *
 * @param commonInfo общие свойства рецепта
 * @param group      группа рецептов
 * @param input      что режем
 * @param tool       тег требуемого инструмента; пусто — годится любой предмет
 * @param results    результаты нарезки, 1..3
 */
public record CuttingRecipe(
        Recipe.CommonInfo commonInfo,
        String group,
        Ingredient input,
        Optional<TagKey<Item>> tool,
        List<ItemStackTemplate> results
) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<CuttingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(CuttingRecipe::commonInfo),
            Codec.STRING.optionalFieldOf("group", "").forGetter(CuttingRecipe::group),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(CuttingRecipe::input),
            TagKey.codec(net.minecraft.core.registries.Registries.ITEM)
                    .optionalFieldOf("tool").forGetter(CuttingRecipe::tool),
            ItemStackTemplate.CODEC.sizeLimitedListOf(3).fieldOf("results")
                    .forGetter(CuttingRecipe::results)
    ).apply(i, CuttingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CuttingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC, CuttingRecipe::commonInfo,
                    ByteBufCodecs.STRING_UTF8, CuttingRecipe::group,
                    Ingredient.CONTENTS_STREAM_CODEC, CuttingRecipe::input,
                    ByteBufCodecs.optional(TagKey.streamCodec(
                            net.minecraft.core.registries.Registries.ITEM)), CuttingRecipe::tool,
                    ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list(3)),
                    CuttingRecipe::results,
                    CuttingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    /** Подходит ли инструмент в руке игрока. */
    public boolean toolMatches(ItemStack held) {
        return this.tool.map(held::is).orElse(true);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        // Первый результат — основной; остальные выдаёт блок-энтити.
        return this.results.getFirst().create();
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }

    @Override
    public RecipeSerializer<CuttingRecipe> getSerializer() {
        return KHRecipes.CUTTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CuttingRecipe> getType() {
        return KHRecipes.CUTTING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(this.input);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return KHRecipes.CUTTING_CATEGORY.get();
    }

    @Override
    public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return List.of();
    }
}
