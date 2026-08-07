package dev.romankrukovsky.kubanhorizons.registry;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

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

    /** Сырое мясо фазана. */
    public static final FoodProperties RAW_PHEASANT = new FoodProperties.Builder()
            .nutrition(3).saturationModifier(0.3F).build();
    /** Запечённое мясо фазана. */
    public static final FoodProperties COOKED_PHEASANT = new FoodProperties.Builder()
            .nutrition(7).saturationModifier(0.75F).build();
    /** Сырое мясо перепела. */
    public static final FoodProperties RAW_QUAIL = new FoodProperties.Builder()
            .nutrition(2).saturationModifier(0.25F).build();
    /** Запечённое мясо перепела. */
    public static final FoodProperties COOKED_QUAIL = new FoodProperties.Builder()
            .nutrition(5).saturationModifier(0.65F).build();

    // --- Кухня ---

    /** Домашний хлеб: сытнее ванильного. */
    public static final FoodProperties HOMEMADE_BREAD = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.75F)
            .build();

    /** Кубанский борщ: главное блюдо — сильное насыщение и регенерация. */
    public static final FoodProperties BORSCHT = new FoodProperties.Builder()
            .nutrition(10)
            .saturationModifier(0.85F)
            .build();

    /** Борщ подаётся горячим: короткая регенерация. */
    public static final Consumable BORSCHT_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(2.4F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(
                    new MobEffectInstance(MobEffects.REGENERATION, 100, 0)))
            .build();

    /** Мамалыга: простая сытная каша. */
    public static final FoodProperties MAMALYGA = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(0.6F)
            .build();

    /** Чайный напиток: бодрость (скорость). */
    public static final FoodProperties TEA_DRINK = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.2F)
            .alwaysEdible()
            .build();

    public static final Consumable TEA_DRINK_CONSUMABLE = Consumable.builder()
            .consumeSeconds(1.6F)
            .animation(ItemUseAnimation.DRINK)
            .sound(SoundEvents.GENERIC_DRINK)
            .hasConsumeParticles(false)
            .onConsume(new ApplyStatusEffectsConsumeEffect(
                    new MobEffectInstance(MobEffects.SPEED, 1200, 0)))
            .build();

    /** Мёд с орехами: десерт с поглощением. */
    public static final FoodProperties HONEY_WALNUTS = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.7F)
            .build();

    public static final Consumable HONEY_WALNUTS_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(
                    new MobEffectInstance(MobEffects.ABSORPTION, 600, 0)))
            .build();

    /** Овощная закуска: лёгкое питание + насыщение. */
    public static final FoodProperties VEGETABLE_SPREAD = new FoodProperties.Builder()
            .nutrition(7)
            .saturationModifier(0.65F)
            .build();

    private KHFoods() {
    }
}
