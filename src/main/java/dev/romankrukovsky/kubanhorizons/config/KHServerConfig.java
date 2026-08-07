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

    // --- Давление на хозяйство ---

    private static final ModConfigSpec.BooleanValue PRESSURE_ENABLED = BUILDER
            .comment("Master switch for farm pressure: pests, trampling, drought and floods.",
                    "Главный выключатель давления на хозяйство: вредители, вытаптывание,",
                    "суховей и половодье. Выключенный — мод остаётся мирным садоводством.")
            .define("pressure.enabled", true);

    private static final ModConfigSpec.DoubleValue PRESSURE_SEVERITY = BUILDER
            .comment("Scales every pressure effect: trampling, gnawing, drought and locust damage.",
                    "Масштабирует силу всех видов давления: вытаптывания, порчи желобов,",
                    "суховея и потрав саранчи. 0.5 — мягко, 2.0 — жёстко.")
            .defineInRange("pressure.severity", 1.0D, 0.0D, 4.0D);

    private static final ModConfigSpec.BooleanValue LOCUST_SWARMS = BUILDER
            .comment("Enable periodic locust swarms over farmland.",
                    "Включает периодические налёты саранчи на посевы.")
            .define("pressure.enableLocustSwarms", true);

    private static final ModConfigSpec.IntValue LOCUST_SWARM_INTERVAL = BUILDER
            .comment("Average ticks between locust swarm attempts per player.",
                    "Средний интервал в тиках между попытками налёта саранчи на игрока.")
            .defineInRange("pressure.locustSwarmInterval", 24000, 1200, 720000);

    private static final ModConfigSpec.BooleanValue DRY_WIND_ENABLED = BUILDER
            .comment("Enable sukhovey: a dry steppe wind that removes farmland moisture.",
                    "Включает суховей — сухой степной ветер, сушащий грядки.")
            .define("pressure.enableDryWind", true);

    private static final ModConfigSpec.BooleanValue FLOODING_ENABLED = BUILDER
            .comment("Enable floodplain high water: floods low farmland, enriches meadow soil.",
                    "Включает половодье в пойме: заливает низкие грядки, обогащая луг.")
            .define("pressure.enableFlooding", true);

    private static final ModConfigSpec.BooleanValue POLLINATION_ENABLED = BUILDER
            .comment("Enable Caucasian bee pollination yield bonus.",
                    "Включает бонус к урожаю от опыления кавказской пчелой.")
            .define("wildlife.enablePollination", true);

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

    /** Главный выключатель давления на хозяйство. */
    public static boolean pressureEnabled() {
        return PRESSURE_ENABLED.get();
    }

    /** Множитель силы давления; 0 равносилен выключению эффектов. */
    public static double pressureSeverity() {
        return PRESSURE_SEVERITY.get();
    }

    public static boolean locustSwarmsEnabled() {
        return PRESSURE_ENABLED.get() && LOCUST_SWARMS.get();
    }

    public static int locustSwarmInterval() {
        return LOCUST_SWARM_INTERVAL.get();
    }

    public static boolean dryWindEnabled() {
        return PRESSURE_ENABLED.get() && DRY_WIND_ENABLED.get();
    }

    public static boolean floodingEnabled() {
        return PRESSURE_ENABLED.get() && FLOODING_ENABLED.get();
    }

    public static boolean pollinationEnabled() {
        return POLLINATION_ENABLED.get();
    }

    public static boolean worldgenEnabled() {
        return WORLDGEN_ENABLED.get();
    }

    public static boolean tradeEnabled() {
        return TRADE_ENABLED.get();
    }
}
