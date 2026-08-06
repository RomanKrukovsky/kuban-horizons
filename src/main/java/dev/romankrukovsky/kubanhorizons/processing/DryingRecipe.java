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
 * Рецепт сушки на сушильной раме.
 *
 * @param commonInfo общие свойства рецепта
 * @param group      группа рецептов
 * @param input      сырьё (один предмет на слот рамы)
 * @param result     результат сушки
 * @param dryTicks   время сушки в тиках (день ≈ 12000)
 */
public record DryingRecipe(
        Recipe.CommonInfo commonInfo,
        String group,
        Ingredient input,
        ItemStackTemplate result,
        int dryTicks
) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<DryingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(DryingRecipe::commonInfo),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(DryingRecipe::group),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(DryingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(DryingRecipe::result),
            com.mojang.serialization.Codec.intRange(1, 72000).optionalFieldOf("dry_ticks", 1200).forGetter(DryingRecipe::dryTicks)
    ).apply(i, DryingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DryingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, DryingRecipe::commonInfo,
            ByteBufCodecs.STRING_UTF8, DryingRecipe::group,
            Ingredient.CONTENTS_STREAM_CODEC, DryingRecipe::input,
            ItemStackTemplate.STREAM_CODEC, DryingRecipe::result,
            ByteBufCodecs.VAR_INT, DryingRecipe::dryTicks,
            DryingRecipe::new);

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
    public RecipeSerializer<DryingRecipe> getSerializer() {
        return KHRecipes.DRYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<DryingRecipe> getType() {
        return KHRecipes.DRYING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(this.input);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return KHRecipes.DRYING_CATEGORY.get();
    }

    @Override
    public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return List.of();
    }
}
