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
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Рецепт отжима масла в маслопрессе.
 *
 * <p>Вход: {@code inputCount} предметов сырья (например, 8 семян
 * подсолнечника) и одна стеклянная бутылка. Выход: основной продукт
 * (бутылка масла) и побочный продукт (жмых).</p>
 *
 * @param commonInfo     общие свойства рецепта (уведомление о разблокировке)
 * @param group          группа рецептов для recipe book
 * @param input          ингредиент сырья
 * @param inputCount     требуемое количество сырья за одну операцию
 * @param result         основной результат
 * @param byproduct      побочный продукт (может быть пустым)
 * @param workTicks      длительность одного отжима в тиках (для пассивного режима)
 */
public record OilPressingRecipe(
        Recipe.CommonInfo commonInfo,
        String group,
        Ingredient input,
        int inputCount,
        ItemStackTemplate result,
        ItemStackTemplate byproduct,
        int workTicks
) implements Recipe<OilPressInput> {

    public static final MapCodec<OilPressingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(OilPressingRecipe::commonInfo),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(OilPressingRecipe::group),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(OilPressingRecipe::input),
            com.mojang.serialization.Codec.intRange(1, 64).optionalFieldOf("ingredient_count", 1).forGetter(OilPressingRecipe::inputCount),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(OilPressingRecipe::result),
            ItemStackTemplate.CODEC.fieldOf("byproduct").forGetter(OilPressingRecipe::byproduct),
            com.mojang.serialization.Codec.intRange(1, 72000).optionalFieldOf("work_ticks", 300).forGetter(OilPressingRecipe::workTicks)
    ).apply(i, OilPressingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OilPressingRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, recipe) -> {
                        Recipe.CommonInfo.STREAM_CODEC.encode(buffer, recipe.commonInfo());
                        buffer.writeUtf(recipe.group());
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input());
                        buffer.writeVarInt(recipe.inputCount());
                        ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result());
                        ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.byproduct());
                        buffer.writeVarInt(recipe.workTicks());
                    },
                    buffer -> new OilPressingRecipe(
                            Recipe.CommonInfo.STREAM_CODEC.decode(buffer),
                            buffer.readUtf(),
                            Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                            buffer.readVarInt(),
                            ItemStackTemplate.STREAM_CODEC.decode(buffer),
                            ItemStackTemplate.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt()
                    )
            );

    @Override
    public boolean matches(OilPressInput input, Level level) {
        return this.input.test(input.item()) && input.item().getCount() >= this.inputCount;
    }

    @Override
    public ItemStack assemble(OilPressInput input) {
        return this.result.create();
    }

    /** Побочный продукт одной операции. */
    public ItemStack createByproduct() {
        return this.byproduct.create();
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }

    @Override
    public RecipeSerializer<OilPressingRecipe> getSerializer() {
        return KHRecipes.OIL_PRESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<OilPressingRecipe> getType() {
        return KHRecipes.OIL_PRESSING_TYPE.get();
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
        return KHRecipes.OIL_PRESSING_CATEGORY.get();
    }

    @Override
    public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return List.of();
    }
}
