# Шпаргалка: GameTest, ModConfigSpec, Data Attachments, SavedData (MC 26.2 + NeoForge 26.2.0.48-beta)

Проверено по исходникам: `mc-src/net/minecraft/gametest/framework/`, `neoforge-src/net/neoforged/neoforge/`.
Напоминание: `ResourceLocation` теперь называется `Identifier`.

---

## 1. GameTest в 26.2 — registry-based, НЕ аннотации

Старых аннотаций `@GameTest`/`@GameTestHolder` **больше нет**. Начиная с 1.21.5 GameTest — это
**datapack-реестры** (см. `Registries.java`):

| Реестр | Ключ | Содержимое |
|---|---|---|
| `Registries.TEST_FUNCTION` | `test_function` | `Consumer<GameTestHelper>` — сам код теста |
| `Registries.TEST_INSTANCE` | `test_instance` | `GameTestInstance` (datapack-элемент: функция + `TestData`) |
| `Registries.TEST_ENVIRONMENT` | `test_environment` | `TestEnvironmentDefinition<?>` (погода, гейм-рулы, время...) |
| `Registries.TEST_INSTANCE_TYPE` | `test_instance_type` | `MapCodec<? extends GameTestInstance>` (типы: `function`, `block_based`) |

- `GameTestInstance` (`gametest/framework/GameTestInstance.java`) — абстрактный класс с
  `DIRECT_CODEC` (dispatch по `TEST_INSTANCE_TYPE`), методами `run(GameTestHelper)` и `codec()`.
  Ванильная реализация — `FunctionGameTestInstance` (ссылается на `ResourceKey` из `TEST_FUNCTION`).
- Ванильный пример регистрации через datagen: `GameTestInstances.bootstrap(BootstrapContext<GameTestInstance>)`.
- Ванильный код тестов регистрируется через `TestFunctionLoader` / `BuiltinTestFunctions.bootstrap(registry)`.

### TestData (описание теста, `TestData.java`)

```java
public record TestData<EnvironmentType>(
    EnvironmentType environment,   // Holder<TestEnvironmentDefinition<?>>
    Identifier structure,          // id структуры-шаблона
    int maxTicks,                  // "max_ticks", обязателен, > 0
    int setupTicks,                // "setup_ticks", по умолч. 0
    boolean required,              // по умолч. true
    Rotation rotation,             // по умолч. NONE
    boolean manualOnly,            // по умолч. false
    int maxAttempts,               // по умолч. 1
    int requiredSuccesses,         // по умолч. 1
    boolean skyAccess,             // по умолч. false
    int padding                    // 0..128, по умолч. 0
) {}
// Удобные конструкторы: TestData(env, structure, maxTicks, setupTicks, required[, rotation])
```

### Регистрация в NeoForge — `RegisterGameTestsEvent`

Файл: `neoforge-src/net/neoforged/neoforge/event/RegisterGameTestsEvent.java`.
Событие **мод-баса** (`IModBusEvent`), даёт прямой доступ к `WritableRegistry`:

```java
@SubscribeEvent // на мод-басе
static void registerTests(RegisterGameTestsEvent event) {
    // среда (опционально; можно взять ванильную "default")
    Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
        Identifier.fromNamespaceAndPath(MODID, "my_env"),
        new TestEnvironmentDefinition.SetGameRules(...));

    // сам тест
    event.registerTest(
        Identifier.fromNamespaceAndPath(MODID, "my_test"),
        new FunctionGameTestInstance(
            MY_TEST_FUNCTION_KEY, // ResourceKey<Consumer<GameTestHelper>> из TEST_FUNCTION
            new TestData<>(env, Identifier.fromNamespaceAndPath(MODID, "my_structure"),
                           100 /*maxTicks*/, 0 /*setupTicks*/, true /*required*/)));
}
```

Событие стреляет при загрузке datapack-реестров, **только** если
`GameTestHooks.isGametestEnabled()` == true: не production И (запуск в IDE, GameTest-сервер
или `-Dneoforge.enableGameTest=true`).

Ванильная среда по умолчанию: `GameTestEnvironments.DEFAULT_KEY` = `minecraft:default`
(пустой `AllOf`). Через неё удобно брать `HolderGetter`-ом в bootstrap/datagen.

### Структура-шаблон

- Задаётся полем `structure` в `TestData` — id NBT-структуры (обычная structure template,
  `data/<ns>/structure/...`).
- **Встроенный пустой шаблон есть**: `minecraft:empty` — ванильный тест `always_pass`
  использует именно `Identifier.withDefaultNamespace("empty")`. Для тестов, где арена
  строится кодом, указывайте его.
- Размещением в мире управляет `TestInstanceBlockEntity` (блок test_instance) +
  `StructureUtils` / `StructureGridSpawner`.

### GameTestHelper — основные методы (`GameTestHelper.java`)

Все координаты — **относительные** внутри структуры (`absolutePos`/`relativePos` для конверсии).

- **Блоки:** `setBlock(pos|x,y,z, Block|BlockState)`, `placeBlock(...)`, `destroyBlock(pos)`,
  `getBlockState(pos)`, `getBlockEntity(pos, Class)`, `pulseRedstone(pos, duration)`.
- **Взаимодействие:** `pressButton(pos)`, `pullLever(pos)`, `useBlock(pos[, player])`,
  `makeMockPlayer(GameType)`, `makeMockServerPlayerInLevel()`.
- **Сущности:** `spawn(EntityType, pos)`, `spawnWithNoFreeWill(...)`, `spawnItem(item, pos)`,
  `spawnEntity(...)`/`spawnMob(...)` (билдеры), `killAllEntities()`, `walkTo(mob, pos, speed)`.
- **Ассерты:** `assertBlockPresent/NotPresent(block, pos)`, `assertBlockState(pos, ...)`,
  `assertBlockProperty(pos, prop, value)`, `assertEntityPresent/NotPresent(...)`,
  `assertEntitiesPresent(type, count)`, `assertContainerContains/Empty(pos, ...)`,
  `assertTrue/assertFalse(cond, msg)`, `assertValueEqual(actual, expected, name)`,
  `assertRedstoneSignal(...)`, `fail(msg[, pos|entity])`, `assertionException(...)`.
- **Завершение:** `succeed()`, `succeedIf(runnable)` (сразу), `succeedWhen(runnable)`
  (поллинг каждый тик до таймаута), `succeedOnTickWhen(tick, runnable)`,
  `succeedWhenBlockPresent(block, pos)`, `succeedWhenEntityPresent/NotPresent(...)`.
- **Тайминг:** `runAtTickTime(time, r)`, `runAfterDelay(ticks, r)`, `onEachTick(r)`,
  `startSequence()` → `GameTestSequence`: `.thenExecute(r)`, `.thenExecuteAfter(ticks, r)`,
  `.thenIdle(n)`, `.thenWaitUntil(assertion)`, `.thenTrigger()`, `.thenSucceed()`, `.thenFail(...)`.
- **Геометрия:** `getBounds()`, `getBoundsWithPadding()`, `forEveryBlockInStructure(consumer)`,
  `getLevel()` → `ServerLevel`.

NeoForge добавляет `GameTestHelperExtension`: `fail(String, pos|entity)`,
`getCapability(BlockCapability, pos, ctx)`, `requireCapability(...)`.

---

## 2. ModConfigSpec (`neoforge/common/ModConfigSpec.java`)

Паттерн — статический билдер:

```java
public final class MyConfig {
    public static final ModConfigSpec SPEC;
    public static final MyConfig INSTANCE;

    public final ModConfigSpec.BooleanValue enableFeature;
    public final ModConfigSpec.IntValue radius;
    public final ModConfigSpec.EnumValue<Mode> mode;

    private MyConfig(ModConfigSpec.Builder builder) {
        builder.push("general"); // секция
        enableFeature = builder
            .comment("Включает фичу")
            .translation("mymod.configgui.enableFeature")
            .define("enableFeature", true);
        radius = builder
            .worldRestart()                     // или .gameRestart() — не для SERVER-типа
            .defineInRange("radius", 8, 1, 64); // IntValue/DoubleValue/LongValue
        mode = builder.defineEnum("mode", Mode.NORMAL);
        builder.pop();
    }

    static {
        var pair = new ModConfigSpec.Builder().configure(MyConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }
}
```

Методы билдера: `define(path, default[, validator])` (в т.ч. `BooleanValue`-перегрузка),
`defineInRange(path, default, min, max)` (int/long/double), `defineInList`, `defineList`,
`defineListAllowEmpty`, `defineEnum`, `comment(...)`, `translation(key)`, `push`/`pop`,
`worldRestart()`, `gameRestart()`. Чтение: `value.get()` / `getAsBoolean()` / `getAsInt()`;
запись: `value.set(v)` + `SPEC.save()`.

`RestartType`: `NONE` / `WORLD` / `GAME` (GAME запрещён для SERVER-конфигов).

**Регистрация** — в конструкторе мода через `ModContainer`:

```java
@Mod(MODID)
public class MyMod {
    public MyMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, MyConfig.SPEC);
        // ModConfig.Type: STARTUP / CLIENT / COMMON / SERVER (net.neoforged.fml.config.ModConfig)
    }
}
```

- `CLIENT` → `<mod>-client.toml` (только клиент), `COMMON` → оба, `SERVER` → per-world,
  синхронизируется на клиент (`ConfigSync`), `STARTUP` → грузится максимально рано.
- **События** (мод-бас, `net.neoforged.fml.event.config.ModConfigEvent`):
  `ModConfigEvent.Loading` (первая загрузка) и `ModConfigEvent.Reloading` (изменение файла /
  синхронизация). Внутри проверять `event.getConfig().getSpec() == SPEC` и кэшировать значения
  (образец — `NeoForgeClientConfig.onLoad/onFileChange`).

---

## 3. Data Attachments (`neoforge/attachment/AttachmentType.java`)

Реестр: `NeoForgeRegistries.Keys.ATTACHMENT_TYPES` (`neoforge:attachment_types`).
Холдеры (`IAttachmentHolder`): `Entity`, `BlockEntity`, `Level`, `ChunkAccess` (chunk/protochunk).

### Регистрация

```java
private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
    DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

public static final Supplier<AttachmentType<MyData>> MY_DATA = ATTACHMENTS.register(
    "my_data",
    () -> AttachmentType.builder(() -> new MyData())      // или builder(holder -> ...)
        .serialize(MyData.CODEC)                          // MapCodec<T>; есть перегрузка (codec, shouldSerializePredicate)
        // .serialize(IAttachmentSerializer) — ручная ValueInput/ValueOutput-сериализация
        // AttachmentType.serializable(MyData::new) — если T implements ValueIOSerializable
        // .copyOnDeath()   — для сущностей: копировать при смерти/конверсии (требует serializer)
        // .copyHandler(...) — свой копировщик (по умолчанию serialize→deserialize)
        // .sync(streamCodec) / .sync(sendToPlayerPredicate, streamCodec) — синк на клиент
        .build());
// в конструкторе мода: ATTACHMENTS.register(modBus);
```

Без `serialize(...)` — attachment временный (не сохраняется на диск).

### Использование (методы `IAttachmentHolder`)

```java
MyData d = chunk.getData(MY_DATA);          // создаёт default, если нет
chunk.setData(MY_DATA, newValue);           // возвращает старое значение или null
chunk.hasData(MY_DATA);
chunk.getExistingData(MY_DATA);             // Optional<T>, НЕ создаёт default
chunk.getExistingDataOrNull(MY_DATA);
chunk.removeData(MY_DATA);
// Все методы принимают и Supplier<AttachmentType<T>> (т.е. DeferredHolder напрямую).
```

### Пометка dirty (из javadoc AttachmentType)

- **LevelChunk / ChunkAccess:** после изменения attachment вызвать `chunk.markUnsaved()`
  (бывший `setUnsaved(true)`; `setData` на чанке это не делает автоматически при мутации
  уже полученного объекта). Serializable-attachment'ы копируются с `ProtoChunk` на
  `LevelChunk` при промоушене.
- **BlockEntity:** вызвать `blockEntity.setChanged()`.
- **Entity:** serializable-attachment'ы по умолчанию НЕ копируются при смерти (но копируются
  при возврате из Энда); включается через `.copyOnDeath()`.
- **Level:** сохраняется через `LevelAttachmentsSavedData`, отдельных требований нет.

---

## 4. SavedData — новый codec-подход (`mc-src/.../saveddata/SavedData.java`)

`SavedData` теперь минимален — только dirty-флаг (`setDirty()`, `isDirty()`).
Методы `save(CompoundTag)` **нет** — вся сериализация через `Codec` в `SavedDataType`:

```java
public record/class MyData extends SavedData {
    public static final Codec<MyData> CODEC = RecordCodecBuilder.create(...);
    public static final SavedDataType<MyData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(MODID, "my_data"),
        MyData::new,      // Supplier<T> — конструктор пустого
        CODEC             // Codec<T>
        /* , DataFixTypes — опционально; NeoForge разрешает null */);
}
```

`SavedDataType` — record `(Identifier id, Factory<T> factory, Factory<Codec<T>> codecFactory,
@Nullable DataFixTypes dataFixType)`. NeoForge добавил level-зависимую фабрику:
`Factory<T> { T create(@Nullable ServerLevel level); }` — можно строить данные/кодек с учётом уровня.
`equals/hashCode` — только по `id`.

Доступ — через `SavedDataStorage` (бывший `DimensionDataStorage`):

```java
MyData data = serverLevel.getDataStorage().computeIfAbsent(MyData.TYPE);
data.setDirty(); // после изменений — иначе не сохранится
// также: get(TYPE) -> @Nullable, set(TYPE, data), scheduleSave(), saveAndJoin()
```

Ванильные примеры: `WeatherData`, `WanderingTraderData`, `Raids.TYPE`, `WorldBorder.TYPE`.

---

## 5. Клиентский мод-класс и клиентские подписчики в NeoForge

**Отдельный клиентский entrypoint** — второй `@Mod` с тем же id и параметром `dist`
(образец — `ClientNeoForgeMod`):

```java
@Mod(value = MODID, dist = Dist.CLIENT)   // класс загружается ТОЛЬКО на клиенте
public class MyModClient {
    public MyModClient(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.CLIENT, MyClientConfig.SPEC);
    }
}
```

**Статические подписчики** — `@EventBusSubscriber` с фильтром по dist (регистрирует
все `@SubscribeEvent`-методы класса; шина выбирается автоматически по типу события):

```java
@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class MyClientEvents {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent e) { ... }
}
```

Ванильные образцы: `NeoForgeRenderPipelines`, `ClientPayloadHandler` —
`@EventBusSubscriber(value = Dist.CLIENT, modid = "neoforge")`.
Проверка стороны в коде: `FMLEnvironment.dist` / `FMLEnvironment.isProduction()`.
