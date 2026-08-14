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
 * Рецепт давки сока (виноградный пресс).
 *
 * <p><b>Почему это отдельный тип, а не {@link OilPressingRecipe}.</b> Отжим
 * масла описывает <i>партию</i>: «столько-то сырья + бутылка за один цикл
 * длиной N тиков, на выходе продукт и жмых». Давка сока описывает
 * <i>накопление</i>: сколько сока даёт одна ягодная единица и сколько сока
 * составляет бутылку. Полей у двух процессов не просто разные значения —
 * разный состав: здесь нет ни {@code byproduct}, ни {@code work_ticks}
 * (пассивного режима у чана нет), зато есть {@link #juicePerItem} и
 * {@link #juicePerBottle}, которых нет у масла.</p>
 *
 * <p>Переиспользование {@code oil_pressing} было бы не экономией, а ошибкой:
 * оба устройства ищут рецепты по типу, поэтому общий тип означал бы, что
 * маслопресс начнёт принимать виноград, а чан — семечки, и каждый из них
 * трактовал бы чужие поля по-своему. Разделение типов — это и есть граница
 * между двумя механиками.</p>
 *
 * @param commonInfo     общие свойства рецепта
 * @param group          группа рецептов для recipe book
 * @param input          сырьё (одна ягодная единица за одну давку)
 * @param juicePerItem   сколько единиц сока даёт одна единица сырья
 * @param result         что представляет собой одна бутылка этого сока
 * @param juicePerBottle сколько единиц сока нужно, чтобы налить бутылку
 */
public record PressingRecipe(
        Recipe.CommonInfo commonInfo,
        String group,
        Ingredient input,
        int juicePerItem,
        ItemStackTemplate result,
        int juicePerBottle
) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<PressingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(PressingRecipe::commonInfo),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "")
                    .forGetter(PressingRecipe::group),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(PressingRecipe::input),
            com.mojang.serialization.Codec.intRange(1, 64).optionalFieldOf("juice_per_item", 1)
                    .forGetter(PressingRecipe::juicePerItem),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(PressingRecipe::result),
            com.mojang.serialization.Codec.intRange(1, 64).optionalFieldOf("juice_per_bottle", 2)
                    .forGetter(PressingRecipe::juicePerBottle)
    ).apply(i, PressingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, recipe) -> {
                        Recipe.CommonInfo.STREAM_CODEC.encode(buffer, recipe.commonInfo());
                        buffer.writeUtf(recipe.group());
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input());
                        buffer.writeVarInt(recipe.juicePerItem());
                        ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result());
                        buffer.writeVarInt(recipe.juicePerBottle());
                    },
                    buffer -> new PressingRecipe(
                            Recipe.CommonInfo.STREAM_CODEC.decode(buffer),
                            buffer.readUtf(),
                            Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                            buffer.readVarInt(),
                            ItemStackTemplate.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt()
                    )
            );

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
    }

    /** Бутылка готового сока — единственная точка создания результата. */
    public ItemStack createBottle() {
        return this.result.create();
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }

    @Override
    public RecipeSerializer<PressingRecipe> getSerializer() {
        return KHRecipes.PRESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<PressingRecipe> getType() {
        return KHRecipes.PRESSING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        // Полноценный PlacementInfo нужен, чтобы RecipeManager не считал
        // рецепт «пустым» (ванильное предупреждение при загрузке).
        return PlacementInfo.create(this.input);
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return KHRecipes.PRESSING_CATEGORY.get();
    }

    @Override
    public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return List.of();
    }
}
