package dev.romankrukovsky.kubanhorizons.processing;

import com.mojang.serialization.Codec;
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

/**
 * Рецепт копчения в коптильне.
 *
 * <p>Отличается от ванильной коптильни и от сушильной рамы мода, и оба отличия
 * механические, иначе устройство было бы третьим названием печи:</p>
 *
 * <ul>
 *   <li><b>Нужны дрова.</b> Копчение расходует топливо-щепу
 *   ({@code fuelTicks} на порцию), а не просто ждёт время, как сушка. Поэтому
 *   коптильня требует снабжения, а рама — только терпения.</li>
 *   <li><b>Результат хранится.</b> Копчёность — не «жареное мясо», а отдельный
 *   продукт с бо́льшим насыщением: за долгое приготовление и расход дров игрок
 *   получает то, чего печь не даёт.</li>
 * </ul>
 *
 * @param commonInfo общие свойства рецепта
 * @param group      группа рецептов
 * @param input      сырьё: рыба или мясо
 * @param result     копчёность
 * @param smokeTicks время копчения в тиках
 */
public record SmokingProcessRecipe(
        Recipe.CommonInfo commonInfo,
        String group,
        Ingredient input,
        ItemStackTemplate result,
        int smokeTicks
) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<SmokingProcessRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(SmokingProcessRecipe::commonInfo),
                    Codec.STRING.optionalFieldOf("group", "")
                            .forGetter(SmokingProcessRecipe::group),
                    Ingredient.CODEC.fieldOf("ingredient")
                            .forGetter(SmokingProcessRecipe::input),
                    ItemStackTemplate.CODEC.fieldOf("result")
                            .forGetter(SmokingProcessRecipe::result),
                    Codec.intRange(1, 72000).optionalFieldOf("smoke_ticks", 2400)
                            .forGetter(SmokingProcessRecipe::smokeTicks)
            ).apply(i, SmokingProcessRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SmokingProcessRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, recipe) -> {
                        Recipe.CommonInfo.STREAM_CODEC.encode(buffer, recipe.commonInfo());
                        buffer.writeUtf(recipe.group());
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input());
                        ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result());
                        buffer.writeVarInt(recipe.smokeTicks());
                    },
                    buffer -> new SmokingProcessRecipe(
                            Recipe.CommonInfo.STREAM_CODEC.decode(buffer),
                            buffer.readUtf(),
                            Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
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

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public RecipeSerializer<SmokingProcessRecipe> getSerializer() {
        return KHRecipes.SMOKING_SERIALIZER.get();
    }

    @Override
    public RecipeType<SmokingProcessRecipe> getType() {
        return KHRecipes.SMOKING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(this.input);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return KHRecipes.SMOKING_CATEGORY.get();
    }

    @Override
    public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return java.util.List.of();
    }
}
