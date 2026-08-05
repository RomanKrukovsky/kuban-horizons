# Datagen в NeoForge 26.2 (MC 26.2) — шпаргалка

> Проверено по исходникам: `mc-src` (Minecraft 26.2) и `neoforge-src` (NeoForge 26.2.0.48-beta).
> Важно: `ResourceLocation` переименован в **`Identifier`** (`net.minecraft.resources.Identifier`).

---

## 1. GatherDataEvent — точка входа

Файл: `net/neoforged/neoforge/data/event/GatherDataEvent.java`.

`GatherDataEvent` — абстрактный, есть два подкласса:
- **`GatherDataEvent.Client`** — клиентский datagen (assets **и** data — NeoForge сам гоняет client+server datagen одним запуском `clientData`, см. комментарий в `ClientNeoForgeMod`);
- `GatherDataEvent.Server` — только серверные данные.

Событие идёт на **мод-шине** (`IModBusEvent`). Подписка:

```java
@EventBusSubscriber(modid = KubanHorizon.MODID)
public class DataGen {
    @SubscribeEvent
    static void onGatherData(GatherDataEvent.Client event) { ... }
}
```

Ключевые методы события:

```java
DataGenerator getGenerator();                                  // классический генератор
CompletableFuture<HolderLookup.Provider> getLookupProvider();  // реестры (с учётом datapack-объектов, если createDatapackRegistryObjects вызван ДО)
ModContainer getModContainer();
ResourceManager getResourceManager(PackType packType);         // существующие ресурсы (CLIENT_RESOURCES / SERVER_DATA)
boolean includeDev(); boolean includeReports(); boolean validate();

// Хелперы регистрации (предпочтительный путь!):
<T extends DataProvider> T addProvider(T provider);            // = getGenerator().addProvider(true, provider)
<T> T createProvider(DataProviderFromOutput<T> b);             // (PackOutput) -> T
<T> T createProvider(DataProviderFromOutputLookup<T> b);       // (PackOutput, CompletableFuture<HolderLookup.Provider>) -> T

// Пара блок+предмет теги (item-провайдер получает contentsGetter блочного):
void createBlockAndItemTags(DataProviderFromOutputLookup<TagsProvider<Block>> blockTags,
                            ItemTagsProvider itemTags); // ItemTagsProvider тут — @FunctionalInterface (output, lookup, blockTagLookup) -> TagsProvider<Item>

// Worldgen/datapack-реестры (регистрирует DatapackBuiltinEntriesProvider И
// подменяет getLookupProvider() на реестры с модовыми записями):
void createDatapackRegistryObjects(RegistrySetBuilder builder);
void createDatapackRegistryObjects(RegistrySetBuilder builder, Set<String> modIds);
void createDatapackRegistryObjects(RegistrySetBuilder builder, Map<ResourceKey<?>, List<ICondition>> conditions);
```

Типовой скелет (реальный пример — `ClientNeoForgeMod#onGatherData`):

```java
@SubscribeEvent
static void onGatherData(GatherDataEvent.Client event) {
    // ВАЖНО: datapack-реестры первыми, чтобы getLookupProvider() их видел
    event.createDatapackRegistryObjects(WORLDGEN_BUILDER);

    event.createProvider(ModModelProvider::new);            // (output) -> ...
    event.createProvider(ModRecipeProvider.Runner::new);    // (output, lookup) -> ...
    event.createProvider(ModLootTableProvider::new);
    event.createBlockAndItemTags(ModBlockTagsProvider::new, ModItemTagsProvider::new);
    event.createProvider(ModLanguageProvider::new);
    event.createProvider(ModAdvancementProvider::new);
    event.createProvider(ModSoundDefinitionsProvider::new);
    event.createProvider(ModDataMapProvider::new);
}
```

---

## 2. Модели: ModelProvider (новый vanilla-путь)

Файл: `mc-src/net/minecraft/client/data/models/ModelProvider.java` (`@OnlyIn(Dist.CLIENT)`).

**Старого NeoForge `BlockStateProvider` больше НЕТ.** В `neoforge-src/.../client/model/generators/` остались только расширения для vanilla-пайплайна: `template/ExtendedModelTemplateBuilder` (кастомные шаблоны, ElementBuilder, RootTransforms), `blockstate/CustomBlockStateModelBuilder`, `loaders/` (Obj/Composite/Conditional — кастомные лоадеры). Вся генерация — через vanilla `ModelProvider` + `BlockModelGenerators` / `ItemModelGenerators`.

```java
public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, KubanHorizon.MODID);   // Neo-конструктор с modId (без modId — @Deprecated)
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialCube(ModBlocks.SALT_BLOCK.get());
        blockModels.createCropBlock(ModBlocks.SUNFLOWER_CROP.get(), SunflowerCropBlock.AGE, 0, 1, 2, 3, 4, 5, 6, 7);
        itemModels.generateFlatItem(ModItems.SALO.get(), ModelTemplates.FLAT_ITEM);
        // инструмент: ModelTemplates.FLAT_HANDHELD_ITEM
    }
}
```

Особенности:
- Провайдер **валидирует полноту**: `getKnownBlocks()` / `getKnownItems()` по умолчанию возвращают все блоки/предметы с namespace == modId; если для чего-то не сгенерирована модель — `IllegalStateException("Missing blockstate definitions / item model definitions")`. Можно переопределить оба метода (например, `Stream.empty()`).
- Для `BlockItem` без явной item-модели автоматически ставится ссылка на блок-модель (см. `ItemInfoCollector.finalizeAndValidate`).
- Пишет сразу три вида файлов: `blockstates/`, `items/` (client items — новый формат!), `models/`.
- NeoForge-расширение `IModelProviderExtension`: `modLocation("path")` и `mcLocation("path")` → `Identifier`.

### BlockModelGenerators — полезные методы

Конструктор: `BlockModelGenerators(Consumer<BlockModelDefinitionGenerator> blockStateOutput, ItemModelOutput itemModelOutput, BiConsumer<Identifier, ModelInstance> modelOutput)` — создаёт сам `ModelProvider`, руками не нужен.

```java
// Крапы (пшеница-стайл): property — AGE, stages — маппинг значения age -> номер текстуры _stageN
public void createCropBlock(Block block, Property<Integer> property, int... stages)
// внутри: registerSimpleFlatItemModel(block.asItem()) + MultiVariantGenerator.dispatch(block)
//         .with(PropertyDispatch.initial(property).generate(...)) с моделями ModelTemplates.CROP
//         текстуры: TextureMapping::crop -> "block/<name>_stageN"
// Требование: property.getPossibleValues().size() == stages.length

// Крестовидные растения:
public void createCrossBlockWithDefaultItem(Block block, PlantType plantType)
public void createCrossBlock(Block block, PlantType plantType)
public void createCrossBlock(Block block, PlantType plantType, Property<Integer> property, int... stages) // многостадийный cross (sweet berry style)
public void createPlantWithDefaultItem(Block standAlone, Block potted, PlantType plantType) // + горшок
public void createDoublePlantWithDefaultItem(Block block, PlantType plantType)              // двойные (item с "_top")
// PlantType: TINTED / NOT_TINTED / EMISSIVE_NOT_TINTED

// Плоские item-модели из текстуры блока/предмета:
public void registerSimpleFlatItemModel(Item item)
public void registerSimpleFlatItemModel(Block block)               // текстура блока
public void registerSimpleFlatItemModel(Block block, String suffix)
public void registerSimpleItemModel(Item item, Identifier model)   // ссылка на готовую модель

// Прочее: createTrivialCube, createTrivialBlock, family(block) (BlockFamilyProvider — лестницы/плиты/...),
// woodProvider(log) (лог+wood), createDoor, createTrapdoor и т.д.
```

### ItemModelGenerators

```java
public void generateFlatItem(Item item, ModelTemplate template)             // FLAT_ITEM / FLAT_HANDHELD_ITEM
public void generateFlatItem(Item item, Item textureDonor, ModelTemplate template)
public void declareCustomModelItem(Item item)                               // «модель есть, не генерируй»
```

---

## 3. Рецепты: RecipeProvider + Runner

Файл: `mc-src/net/minecraft/data/recipes/RecipeProvider.java`. С 1.21.4+ схема двойная: `RecipeProvider` (логика) + вложенный `RecipeProvider.Runner` (собственно `DataProvider`).

```java
// RecipeProvider: НЕ DataProvider!
protected RecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
    this.registries = registries;
    this.items = registries.lookupOrThrow(Registries.ITEM);  // <-- вот HolderGetter<Item>
    this.output = output;
}
protected abstract void buildRecipes();

// Runner:
public abstract static class Runner implements DataProvider {
    protected Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {...}
    protected abstract RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output);
}
```

Пример:

```java
public class ModRecipeProvider extends RecipeProvider {
    ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) { super(registries, output); }

    @Override
    protected void buildRecipes() {
        // protected-хелперы уже прокидывают this.items и this.output:
        this.shaped(RecipeCategory.FOOD, ModItems.BORSCHT.get())
            .pattern("BC")
            .pattern("PW")
            .define('B', Items.BEETROOT)
            .define('C', Items.CABBAGE) // условно
            .define('P', Items.BOWL)
            .define('W', Items.WATER_BUCKET)
            .unlockedBy(getHasName(Items.BEETROOT), this.has(Items.BEETROOT))
            .save(this.output); // save(output, Identifier/ResourceKey) — для кастомного id

        this.shapeless(RecipeCategory.MISC, ModItems.SUNFLOWER_SEEDS.get(), 4)
            .requires(ModBlocks.SUNFLOWER_CROP.get().asItem())
            .unlockedBy("has_sunflower", this.has(ModItems.SUNFLOWER.get()))
            .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) { super(output, registries); }
        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }
    }
}
// Регистрация: event.createProvider(ModRecipeProvider.Runner::new);
```

Статические билдеры, если нужен `HolderGetter<Item>` напрямую (внутри провайдера это `this.items` или `registries.lookupOrThrow(Registries.ITEM)`):

```java
ShapedRecipeBuilder.shaped(HolderGetter<Item> items, RecipeCategory cat, ItemLike result [, int count])
ShapelessRecipeBuilder.shapeless(HolderGetter<Item> items, RecipeCategory cat, ItemLike result [, int count])
```

NeoForge: `RecipeOutput.accept(...)` принимает varargs `ICondition...` — условные рецепты. Плюс `SmeltingRecipeBuilder`/`oreSmelting`, `oneToOneConversionRecipe` и т.п. хелперы в базовом классе.

---

## 4. Loot: LootTableProvider + BlockLootSubProvider

Файлы: `mc-src/net/minecraft/data/loot/LootTableProvider.java`, `BlockLootSubProvider.java`.

```java
// LootTableProvider — конструктор:
public LootTableProvider(PackOutput output,
                         Set<ResourceKey<LootTable>> requiredTables,       // обычно Set.of()
                         List<LootTableProvider.SubProviderEntry> subProviders,
                         CompletableFuture<HolderLookup.Provider> registries)

// SubProviderEntry:
public record SubProviderEntry(Function<HolderLookup.Provider, LootTableSubProvider> provider, ContextKeySet paramSet)
// paramSet: LootContextParamSets.BLOCK (тип теперь ContextKeySet, не LootContextParamSet!)
```

```java
public class ModBlockLoot extends BlockLootSubProvider {
    protected ModBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        // сигнатура: (Set<Item> explosionResistant, FeatureFlagSet enabledFeatures, HolderLookup.Provider registries)
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.SALT_BLOCK.get());

        // Крапы:
        LootItemCondition.Builder maxAge = LootItemBlockStatePropertyCondition
            .hasBlockStateProperties(ModBlocks.SUNFLOWER_CROP.get())
            .setProperties(StatePropertiesPredicate.Builder.properties()
                .hasProperty(SunflowerCropBlock.AGE, SunflowerCropBlock.MAX_AGE));
        add(ModBlocks.SUNFLOWER_CROP.get(),
            createCropDrops(ModBlocks.SUNFLOWER_CROP.get(), ModItems.SUNFLOWER.get(), ModItems.SUNFLOWER_SEEDS.get(), maxAge));
        // сигнатура: createCropDrops(Block original, Item cropDrop, Item seedDrop, LootItemCondition.Builder isMaxAge)
        // внутри использует Fortune (addBonusBinomialDistributionCount) + explosion decay
    }

    // ОБЯЗАТЕЛЬНО сузить, иначе валидация потребует таблицы для ВСЕХ блоков (default = весь реестр):
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.entrySet().stream()
            .filter(e -> e.getKey().identifier().getNamespace().equals(KubanHorizon.MODID))
            .map(Map.Entry::getValue).toList();
    }
}

public class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(),
              List.of(new SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)),
              registries);
    }
}
```

Прочее в `BlockLootSubProvider`: `createSilkTouchOnlyTable`, `createOreDrop`, `createLeavesDrops`, `createDoorTable`, `createSlabItemTable`, `otherWhenSilkTouch`, `noDrop`. Блок сам знает свою таблицу через `block.getLootTable()` (Optional).

---

## 5. Теги: BlockTagsProvider / ItemTagsProvider / copy

NeoForge-классы в `net/neoforged/neoforge/common/data/`:

```java
// BlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId)
public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, KubanHorizon.MODID);
    }
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SALT_BLOCK.get());
        tag(Tags.Blocks.STORAGE_BLOCKS).add(ModBlocks.SALT_BLOCK.get());
    }
}
```

Для **копирования блок-тегов в item-теги** используется `BlockTagCopyingItemTagProvider` (у простого `ItemTagsProvider` метода `copy` НЕТ — он просто `TagsProvider<Item>`):

```java
public class ModItemTagsProvider extends BlockTagCopyingItemTagProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                               CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
        super(output, lookup, blockTags, KubanHorizon.MODID);
    }
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        copy(Tags.Blocks.STORAGE_BLOCKS, Tags.Items.STORAGE_BLOCKS); // copy(TagKey<Block>, TagKey<Item>)
        tag(ItemTags.VILLAGER_PLANTABLE_SEEDS).add(ModItems.SUNFLOWER_SEEDS.get());
    }
}

// Регистрация ПАРОЙ (contentsGetter пробрасывается автоматически):
event.createBlockAndItemTags(ModBlockTagsProvider::new, ModItemTagsProvider::new);
```

Прочие теги (fluid, entity, biome...) — vanilla `TagsProvider<T>`/`KeyTagProvider` с `super(output, Registries.XXX, lookup, modId)`, регистрируются через `event.createProvider(...)`.

---

## 6. Язык: LanguageProvider

`net/neoforged/neoforge/common/data/LanguageProvider.java`:

```java
public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, KubanHorizon.MODID, "en_us"); // (PackOutput output, String modid, String locale)
        // для русского — второй провайдер с "ru_ru"
    }
    @Override
    protected void addTranslations() {
        add(ModBlocks.SALT_BLOCK.get(), "Salt Block");     // add(Block, String)
        add(ModItems.SALO.get(), "Salo");                  // add(Item, String)
        addBlock(ModBlocks.SALT_BLOCK, "Salt Block");      // addBlock(Supplier<? extends Block>, String)
        addItem(ModItems.SALO, "Salo");
        add("itemGroup.kuban_horizon", "Kuban Horizon");   // произвольный ключ
        // также: addEffect, addEntityType, addTag(TagKey), addComponent(Component, String)...
    }
}
```

Пишет `assets/<modid>/lang/<locale>.json`. Внутри `Map<String, Component>` (TreeMap — сортировка ключей).

---

## 7. Advancements: AdvancementProvider

Vanilla `net/minecraft/data/advancements/AdvancementProvider.java` — **не абстрактный**, принимает список саб-провайдеров:

```java
// AdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, List<AdvancementSubProvider> subProviders)
// AdvancementSubProvider: void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output)

public class ModAdvancementProvider extends AdvancementProvider {
    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, List.of(new ModAdvancements()));
    }

    static class ModAdvancements implements AdvancementSubProvider {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
            AdvancementHolder root = Advancement.Builder.advancement()
                .display(ModItems.SALO.get(), Component.translatable("advancement.kuban_horizon.root.title"),
                         Component.translatable("advancement.kuban_horizon.root.desc"),
                         Identifier.fromNamespaceAndPath("minecraft", "textures/block/hay_block_side.png"),
                         AdvancementType.TASK, true, true, false)
                .addCriterion("has_salo", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SALO.get()))
                .save(output, Identifier.fromNamespaceAndPath(KubanHorizon.MODID, "root").toString());
        }
    }
}
```

Дубликаты id → `IllegalStateException`. (Рецепт-advancement'ы генерирует сам RecipeProvider.)

---

## 8. Звуки: SoundDefinitionsProvider

`net/neoforged/neoforge/common/data/SoundDefinitionsProvider.java`:

```java
public class ModSounds extends SoundDefinitionsProvider {
    public ModSounds(PackOutput output) {
        super(output, KubanHorizon.MODID); // (PackOutput output, String modId)
    }
    @Override
    public void registerSounds() {
        add(ModSoundEvents.GARMOSHKA.get(), definition()   // add(Holder<SoundEvent>/SoundEvent/Identifier/String, SoundDefinition)
            .subtitle("subtitles.kuban_horizon.garmoshka")
            .with(sound(Identifier.fromNamespaceAndPath(KubanHorizon.MODID, "garmoshka_1")).volume(0.8f),
                  sound(KubanHorizon.MODID + ":garmoshka_2").pitch(1.1f))
        );
        // sound(name, SoundDefinition.SoundType.EVENT) — ссылка на другой sound event
    }
}
```

Пишет `assets/<modid>/sounds.json`. Валидирует существование `.ogg` в известных ресурсах.

---

## 9. Worldgen: DatapackBuiltinEntriesProvider

Проще всего — через событие (провайдер создаётся и регистрируется сам, а `getLookupProvider()` начинает включать ваши записи для последующих провайдеров — тегов биомов и т.п.):

```java
public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
    .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
    .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
    .add(Registries.BIOME, ModBiomes::bootstrap);

// в обработчике (ПЕРВЫМ, до тегов/лута):
event.createDatapackRegistryObjects(BUILDER);                       // modIds = Set.of(modId)
event.createDatapackRegistryObjects(BUILDER, Set.of("kuban_horizon", "minecraft")); // если патчим ваниль
// перегрузки с Map<ResourceKey<?>, List<ICondition>> / Consumer<BiConsumer<...>> — условные объекты
```

Вручную: `new DatapackBuiltinEntriesProvider(output, lookupProvider, RegistrySetBuilder, Map<ResourceKey<?>,List<ICondition>>, Set<String> modIds)` (наследует `RegistriesDatapackGenerator`; `getRegistryProvider()` → полные реестры).

---

## 10. Data Maps: DataMapProvider (компост и др.)

`net/neoforged/neoforge/common/data/DataMapProvider.java` + `net/neoforged/neoforge/registries/datamaps/builtin/NeoForgeDataMaps.java`.

Встроенные data maps NeoForge заменяют vanilla-мапы: `COMPOSTABLES` (вместо `ComposterBlock.COMPOSTABLES`), `FURNACE_FUELS`, `STRIPPABLES`, `PARROT_IMITATIONS`, `VIBRATION_FREQUENCIES`, `RAID_HERO_GIFTS` и др.

```java
public class ModDataMapProvider extends DataMapProvider {
    public ModDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup); // (PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider)
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        var compostables = builder(NeoForgeDataMaps.COMPOSTABLES); // DataMapType<Item, Compostable>
        // Compostable(float chance) или Compostable(float chance, boolean canVillagerCompost)
        compostables.add(ModItems.SUNFLOWER_SEEDS.get().builtInRegistryHolder(),
                         new Compostable(0.3F), false);            // add(Holder<R>, T value, boolean replace, ICondition...)
        compostables.add(Identifier.fromNamespaceAndPath("kuban_horizon", "sunflower"),
                         new Compostable(0.65F, true), false);     // add(Identifier, ...)
        compostables.add(ModItemTags.CROPS, new Compostable(0.5F), false); // add(TagKey<R>, ...) — целым тегом!
        // .remove(...) / .replace(true) / .conditions(...) тоже есть
    }
}
// Регистрация: event.createProvider(ModDataMapProvider::new);
// Вывод: data/<modid>/data_maps/item/compostables.json (папка = реестр)
```

---

## Мини-чеклист регистрации (порядок имеет значение)

```java
@SubscribeEvent
static void onGatherData(GatherDataEvent.Client event) {
    event.createDatapackRegistryObjects(ModWorldgen.BUILDER);            // 1. worldgen (первым!)
    event.createBlockAndItemTags(ModBlockTags::new, ModItemTags::new);   // 2. теги
    event.createProvider(ModModelProvider::new);                         // 3. модели+blockstates+client items
    event.createProvider(ModRecipeProvider.Runner::new);                 // 4. рецепты
    event.createProvider(ModLootTableProvider::new);                     // 5. лут
    event.createProvider(ModLanguageProvider::new);                      // 6. язык
    event.createProvider(ModAdvancementProvider::new);                   // 7. ачивки
    event.createProvider(ModSoundDefinitionsProvider::new);              // 8. звуки
    event.createProvider(ModDataMapProvider::new);                       // 9. data maps (компост)
}
```
