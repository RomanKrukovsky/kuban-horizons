package dev.romankrukovsky.kubanhorizons.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.registry.KHRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

/**
 * Рецепт помола на ручной мельнице.
 *
 * @param commonInfo общие свойства
 * @param group      группа рецептов
 * @param input      сырьё
 * @param result     результат помола
 * @param turns      число оборотов жёрнова
 */
public record MillingRecipe(
        Recipe.CommonInfo commonInfo,
        String group,
        Ingredient input,
        ItemStackTemplate result,
        int turns
) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<MillingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(MillingRecipe::commonInfo),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(MillingRecipe::group),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(MillingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(MillingRecipe::result),
            com.mojang.serialization.Codec.intRange(1, 64).optionalFieldOf("turns", 3).forGetter(MillingRecipe::turns)
    ).apply(i, MillingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MillingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, MillingRecipe::commonInfo,
            ByteBufCodecs.STRING_UTF8, MillingRecipe::group,
            Ingredient.CONTENTS_STREAM_CODEC, MillingRecipe::input,
            ItemStackTemplate.STREAM_CODEC, MillingRecipe::result,
            ByteBufCodecs.VAR_INT, MillingRecipe::turns,
            MillingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }

    @Override
    public RecipeSerializer<MillingRecipe> getSerializer() {
        return KHRecipes.MILLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<MillingRecipe> getType() {
        return KHRecipes.MILLING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(this.input);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return KHRecipes.MILLING_CATEGORY.get();
    }

    @Override
    public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return List.of();
    }
}
