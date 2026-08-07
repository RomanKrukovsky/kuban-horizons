package dev.romankrukovsky.kubanhorizons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Серверная конфигурация мода.
 *
 * <p>Все значения ограничены диапазонами: некорректный ввод в файле
 * конфигурации откатывается NeoForge к значению по умолчанию и не может
 * привести к падению игры. Читать значения следует только через геттеры
 * этого класса.</p>
 */
public final class KHServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Культуры ---

    private static final ModConfigSpec.DoubleValue CROP_GROWTH_SPEED = BUILDER
            .comment("Global growth speed multiplier for Kuban Horizons crops.",
                    "Общий множитель скорости роста культур мода.")
            .defineInRange("crops.growthSpeed", 1.0D, 0.1D, 10.0D);

    // --- Плодородие ---

    private static final ModConfigSpec.BooleanValue FERTILITY_ENABLED = BUILDER
            .comment("Enable the soil fertility system. When disabled, all soil behaves like vanilla.",
                    "Включает систему плодородия почвы. При отключении почва ведёт себя как в ванили.")
            .define("fertility.enabled", true);

    private static final ModConfigSpec.DoubleValue FERTILITY_DEPLETION_RATE = BUILDER
            .comment("How strongly repeated harvests of the same crop deplete fertility (multiplier).",
                    "Насколько сильно повторные сборы одной культуры истощают плодородие (множитель).")
            .defineInRange("fertility.depletionRate", 1.0D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue FERTILITY_RECOVERY_RATE = BUILDER
            .comment("How fast fallow land and compost restore fertility (multiplier).",
                    "Насколько быстро пар и компост восстанавливают плодородие (множитель).")
            .defineInRange("fertility.recoveryRate", 1.0D, 0.0D, 5.0D);

    // --- Орошение ---

    private static final ModConfigSpec.BooleanValue IRRIGATION_ENABLED = BUILDER
            .comment("Enable the irrigation system.",
                    "Включает систему орошения.")
            .define("irrigation.enabled", true);

    private static final ModConfigSpec.IntValue IRRIGATION_RANGE = BUILDER
            .comment("Radius (in blocks) around a filled channel that counts as irrigated farmland.",
                    "Радиус (в блоках) вокруг заполненного желоба, в котором грядки считаются орошаемыми.")
            .defineInRange("irrigation.range", 4, 1, 8);

    // --- Переработка ---

    private static final ModConfigSpec.BooleanValue OIL_PRESS_AUTO = BUILDER
            .comment("Allow the oil press to work passively without player interaction (slower).",
                    "Разрешает маслопрессу работать пассивно без участия игрока (медленнее).")
            .define("automation.oilPressAuto", true);

    private static final ModConfigSpec.IntValue OIL_PRESS_WORK_TICKS = BUILDER
            .comment("Ticks of work required for one pressing operation in passive mode.",
                    "Число тиков пассивной работы для одной операции отжима.")
            .defineInRange("automation.oilPressWorkTicks", 300, 20, 6000);

    // --- Дикая природа ---

    private static final ModConfigSpec.BooleanValue GROUND_BIRD_SPAWNS = BUILDER
            .comment("Enable natural pheasant and quail spawning. Requires a server restart.",
                    "Включает естественный спавн фазанов и перепелов. Требует перезапуска сервера.")
            .define("wildlife.enableGroundBirdSpawns", true);

    // --- Мир ---

    private static final ModConfigSpec.BooleanValue WORLDGEN_ENABLED = BUILDER
            .comment("Master switch for Kuban Horizons world generation (biomes, structures).",
                    "Главный выключатель генерации мира мода (биомы, структуры).")
            .define("worldgen.enabled", true);

    // --- Торговля ---

    private static final ModConfigSpec.BooleanValue TRADE_ENABLED = BUILDER
            .comment("Enable Kuban Horizons villager professions and trades.",
                    "Включает региональные профессии и сделки поселенцев.")
            .define("trade.enabled", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private KHServerConfig() {
    }

    public static double cropGrowthSpeed() {
        return CROP_GROWTH_SPEED.get();
    }

    public static boolean fertilityEnabled() {
        return FERTILITY_ENABLED.get();
    }

    public static double fertilityDepletionRate() {
        return FERTILITY_DEPLETION_RATE.get();
    }

    public static double fertilityRecoveryRate() {
        return FERTILITY_RECOVERY_RATE.get();
    }

    public static boolean irrigationEnabled() {
        return IRRIGATION_ENABLED.get();
    }

    public static int irrigationRange() {
        return IRRIGATION_RANGE.get();
    }

    public static boolean oilPressAuto() {
        return OIL_PRESS_AUTO.get();
    }

    public static int oilPressWorkTicks() {
        return OIL_PRESS_WORK_TICKS.get();
    }

    public static boolean groundBirdSpawnsEnabled() {
        return GROUND_BIRD_SPAWNS.get();
    }

    public static boolean worldgenEnabled() {
        return WORLDGEN_ENABLED.get();
    }

    public static boolean tradeEnabled() {
        return TRADE_ENABLED.get();
    }
}
