package dev.romankrukovsky.kubanhorizons.registry;

import net.minecraft.world.food.FoodProperties;

/**
 * Свойства еды мода. Баланс — см. GAME_DESIGN.md §11.
 */
public final class KHFoods {
    /** Сырые семечки: перекус, слабое питание. */
    public static final FoodProperties SUNFLOWER_SEEDS = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.2F)
            .build();

    /** Жареные семечки: полноценный перекус. */
    public static final FoodProperties ROASTED_SUNFLOWER_SEEDS = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.5F)
            .build();

    /** Сырой початок: скромное питание. */
    public static final FoodProperties CORN_COB = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.3F)
            .build();

    /** Печёная кукуруза: полноценная еда. */
    public static final FoodProperties GRILLED_CORN = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.6F)
            .build();

    /** Сушёные фрукты: концентрированная походная еда. */
    public static final FoodProperties DRIED_FRUIT = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.6F)
            .build();

    /** Томат: свежий овощ. */
    public static final FoodProperties TOMATO = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3F)
            .build();

    /** Гроздь винограда: лёгкая сладкая еда. */
    public static final FoodProperties GRAPES = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.3F)
            .build();

    /** Сырая рисовая крупа: съедобна, но почти бесполезна. */
    public static final FoodProperties RICE = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1F)
            .build();

    /** Отварной рис: сытная основа. */
    public static final FoodProperties COOKED_RICE = new FoodProperties.Builder()
            .nutrition(7)
            .saturationModifier(0.55F)
            .build();

    /** Персик: сочный фрукт. */
    public static final FoodProperties PEACH = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.4F)
            .build();

    /** Абрикос. */
    public static final FoodProperties APRICOT = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.4F)
            .build();

    /** Слива. */
    public static final FoodProperties PLUM = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.4F)
            .build();

    /** Грецкий орех: калорийный перекус. */
    public static final FoodProperties WALNUT = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3F)
            .build();

    private KHFoods() {
    }
}
