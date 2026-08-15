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

    // Настройки irrigation.range здесь больше нет, и это не упущение.
    // Она обещала «радиус вокруг заполненного желоба, в котором грядки
    // считаются орошаемыми», но задать этот радиус мод не может: увлажнение
    // делает ванильный FarmlandBlock.isNearWater, а радиус 4 записан прямо
    // в его теле (private static, betweenClosed(pos.offset(-4,0,-4), ...)).
    // Желоб лишь отдаёт FluidState воды — дальше решает ванильный код.
    // Число в конфиге читалось бы игроком как настройка, ничего при этом не
    // меняя: соврать в описании хуже, чем не дать ручку вовсе.

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

    private static final ModConfigSpec.BooleanValue MANUL_SPAWNS = BUILDER
            .comment("Enable natural manul spawning. Requires a server restart.",
                    "Включает естественный спавн манула. Требует перезапуска сервера.")
            .define("wildlife.enableManulSpawns", true);

    private static final ModConfigSpec.BooleanValue MANUL_NOCTURNAL = BUILDER
            .comment("Restrict natural manul spawning to dusk, night and dawn.",
                    "Ограничивает естественный спавн манула сумерками, ночью и рассветом.",
                    "Выключенный — манул может появиться и днём, встреча становится частой.")
            .define("wildlife.manulNocturnalSpawns", true);

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

    // --- Мир и торговля ---

    // Настроек worldgen.enabled и trade.enabled здесь больше нет.
    //
    // Обе обещали быть «главным выключателем», но выключать им нечего:
    // и биомы с пресетом мира, и профессии со сделками задаются
    // datapack-реестрами (Registries.BIOME, WORLD_PRESET, VILLAGER_TRADE,
    // TRADE_SET) — их содержимое запечено в JSON на этапе датагена и
    // читается при загрузке мира, ДО того как серверный конфиг вообще
    // существует. Прочитать флаг в bootstrap-методе невозможно.
    //
    // Для генерации мира выключатель к тому же излишен: мир Kuban Horizons
    // выбирается игроком как отдельный пресет в стандартном экране создания
    // мира (см. KHWorldPresetTagsProvider), то есть он уже opt-in. Кто не
    // хочет кубанских биомов — просто не выбирает этот пресет.
    //
    // Кому нужно отключить содержимое на самом деле, тот отключает его тем
    // механизмом, которым такие вещи и отключаются, — датапаком. Галочка,
    // которая переживает перезапуск и не делает ничего, вводит в заблуждение
    // сильнее, чем её отсутствие.

    // --- Джинния: изменения мира ---

    private static final ModConfigSpec.IntValue GENIE_MAX_REGION_VOLUME = BUILDER
            .comment("Maximum number of blocks the Genie may capture or move in one operation.",
                    "Максимальное число блоков, которое джинния может захватить или перенести за одну операцию.")
            .defineInRange("genie.maxRegionVolume", 32768, 64, 1048576);

    private static final ModConfigSpec.IntValue POCKET_SCENE_MAX_DURATION_TICKS = BUILDER
            .comment("Maximum lifetime of a pocket scene in ticks before automatic exit.",
                    "Максимальное время жизни карманной сцены в тиках до автоматического возврата.",
                    "12000 = 10 минут. Переживает перезапуск сервера: таймер хранится в SavedData.")
            .defineInRange("genie.pocketSceneMaxDurationTicks", 12000, 20, 720000);

    private static final ModConfigSpec.IntValue GENIE_LITERAL_MAX_ENTITIES = BUILDER
            .comment("Maximum entities a single `literal:` wish may spawn (Закон буквальности).",
                    "Максимальное число существ, которое одно `literal:` желание может породить (Закон буквальности).",
                    "Защищает сервер от лагов при буквальных «40 000 куриц».")
            .defineInRange("genie.literalMaxEntities", 40000, 0, 200000);

    private static final ModConfigSpec.IntValue GENIE_LITERAL_MAX_ENTITIES_PER_CHUNK = BUILDER
            .comment("Maximum entities of one wish that may occupy a single chunk after a `literal:` spawn.",
                    "Максимальное число существ одного желания в одном чанке после `literal:` спавна.",
                    "Предотвращает концентрацию существ в одной точке.")
            .defineInRange("genie.literalMaxEntitiesPerChunk", 200, 0, 10000);

    private static final ModConfigSpec.IntValue GENIE_FLYING_HOUSE_DURATION_TICKS = BUILDER
            .comment("Duration of a flying house flight in ticks before it lands (1200 = 1 minute).",
                    "Длительность полёта летающего дома в тиках до посадки (1200 = 1 минута).")
            .defineInRange("genie.flyingHouseDurationTicks", 1200, 40, 720000);

    // --- Гибриды и эволюция ---

    private static final ModConfigSpec.IntValue HYBRID_POPULATION_CAP_PER_CHUNK = BUILDER
            .comment("Maximum number of hybrids that may live in a single chunk before reproduction is refused.",
                    "Максимальное число гибридов в одном чанке, после которого размножение запрещается.")
            .defineInRange("genie.hybridPopulationCapPerChunk", 16, 1, 1000);

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

    public static boolean oilPressAuto() {
        return OIL_PRESS_AUTO.get();
    }

    public static int oilPressWorkTicks() {
        return OIL_PRESS_WORK_TICKS.get();
    }

    public static boolean groundBirdSpawnsEnabled() {
        return GROUND_BIRD_SPAWNS.get();
    }

    /** Включён ли естественный спавн манула. */
    public static boolean manulSpawnsEnabled() {
        return MANUL_SPAWNS.get();
    }

    /**
     * Ограничен ли спавн манула сумерками и ночью.
     *
     * <p>Отдельный флаг, а не константа: «редкая ночная встреча» — вопрос
     * вкуса, и сервер должен уметь сделать манула обычным дневным зверем,
     * не отключая его целиком.</p>
     */
    public static boolean manulNocturnalSpawns() {
        return MANUL_NOCTURNAL.get();
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

    /** Лимит объёма региона для операций джиннии (Закон сохранности). */
    public static int genieMaxRegionVolume() {
        return GENIE_MAX_REGION_VOLUME.get();
    }

    /** Максимальная длительность карманной сцены в тиках. */
    public static int pocketSceneMaxDurationTicks() {
        return POCKET_SCENE_MAX_DURATION_TICKS.get();
    }

    /** Максимальное число существ на одно `literal:` желание (Закон буквальности). */
    public static int genieLiteralMaxEntities() {
        return GENIE_LITERAL_MAX_ENTITIES.get();
    }

    /** Максимальное число существ одного желания в одном чанке после `literal:` спавна. */
    public static int genieLiteralMaxEntitiesPerChunk() {
        return GENIE_LITERAL_MAX_ENTITIES_PER_CHUNK.get();
    }

    /** Длительность полёта летающего дома в тиках. */
    public static int genieFlyingHouseDurationTicks() {
        return GENIE_FLYING_HOUSE_DURATION_TICKS.get();
    }

    /** Лимит гибридов на один чанк до запрета размножения (Wishborne Ecology). */
    public static int hybridPopulationCapPerChunk() {
        return HYBRID_POPULATION_CAP_PER_CHUNK.get();
    }
}
