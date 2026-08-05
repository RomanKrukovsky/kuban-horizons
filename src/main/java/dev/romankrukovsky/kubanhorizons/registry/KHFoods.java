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

    private KHFoods() {
    }
}
