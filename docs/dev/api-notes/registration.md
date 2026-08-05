# Регистрация контента: NeoForge 26.2.0.48-beta / Minecraft 26.2

> Источник: реальные исходники `~/mc-sources/26.2/mc-src` и `~/mc-sources/26.2/neoforge-src`.
> **Главное:** `ResourceLocation` переименован в `net.minecraft.resources.Identifier`.
> `Item.Properties` и `BlockBehaviour.Properties` **обязаны** знать свой ID (`setId(ResourceKey)`) —
> но `registerBlock`/`registerItem`/`registerSimple*` в `DeferredRegister` вызывают `setId` **автоматически**.

---

## 1. DeferredRegister

`net.neoforged.neoforge.registries.DeferredRegister<T>`

### Фабрики
```java
public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace)
public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String namespace)
public static <B> DeferredRegister<B> create(Identifier registryName, String modid)   // Identifier, не ResourceLocation!
public static DeferredRegister.Items  createItems(String modid)
public static DeferredRegister.Blocks createBlocks(String modid)
public static DeferredRegister.DataComponents createDataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey, String modid)
public static DeferredRegister.Entities createEntities(String modid)
```

### Базовые методы
```java
public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup)
public <I extends T> DeferredHolder<T, I> register(String name, Function<Identifier, ? extends I> func) // передаёт полный Identifier
public void register(IEventBus bus)                       // подписка на мод-бас в конструкторе мода
public TagKey<T> createTagKey(String path)
public TagKey<T> createTagKey(Identifier location)
public void addAlias(Identifier from, Identifier to)
public Registry<T> makeRegistry(Consumer<RegistryBuilder<T>> consumer)   // своя custom-registry
public ResourceKey<? extends Registry<T>> getRegistryKey()
public Identifier getRegistryName()
```

### DeferredRegister.Blocks (extends DeferredRegister\<Block\>)
```java
public <B extends Block> DeferredBlock<B> register(String name, Function<Identifier, ? extends B> func)
public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> sup)

// setId(ResourceKey.create(Registries.BLOCK, key)) вызывается АВТОМАТИЧЕСКИ:
public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> func, Supplier<BlockBehaviour.Properties> properties)
public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> func, UnaryOperator<BlockBehaviour.Properties> properties)
public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> func)

public DeferredBlock<Block> registerSimpleBlock(String name, Supplier<BlockBehaviour.Properties> properties)
public DeferredBlock<Block> registerSimpleBlock(String name, UnaryOperator<BlockBehaviour.Properties> properties)
public DeferredBlock<Block> registerSimpleBlock(String name)
```

### DeferredRegister.Items (extends DeferredRegister\<Item\>)
```java
public <I extends Item> DeferredItem<I> register(String name, Function<Identifier, ? extends I> func)
public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> sup)

// setId(ResourceKey.create(Registries.ITEM, key)) вызывается АВТОМАТИЧЕСКИ:
public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, Supplier<Item.Properties> properties)
public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, UnaryOperator<Item.Properties> properties)
public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func)

public DeferredItem<Item> registerSimpleItem(String name, Supplier<Item.Properties> properties)
public DeferredItem<Item> registerSimpleItem(String name, UnaryOperator<Item.Properties> properties)
public DeferredItem<Item> registerSimpleItem(String name)

// BlockItem; автоматически вызывает properties.useBlockDescriptionPrefix():
public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Supplier<Item.Properties> properties)
public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, UnaryOperator<Item.Properties> properties)
public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block)
public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, Supplier<Item.Properties> properties)   // имя берётся из блока
public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, UnaryOperator<Item.Properties> properties)
public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block)
```

### DeferredBlock / DeferredItem
```java
// DeferredBlock<T extends Block> extends DeferredHolder<Block, T> implements ItemLike
public ItemStack toStack();  public ItemStack toStack(int count);  public Item asItem();
public static <T extends Block> DeferredBlock<T> createBlock(Identifier key)
public static <T extends Block> DeferredBlock<T> createBlock(ResourceKey<Block> key)
// DeferredItem — аналогично: toStack(), toStack(int), asItem(), createItem(Identifier | ResourceKey<Item>)
```

### Пример
```java
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("kuban_horizon");
public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems("kuban_horizon");

public static final DeferredBlock<Block> SALT_BLOCK = BLOCKS.registerSimpleBlock("salt_block",
        p -> p.mapColor(MapColor.SNOW).strength(1.5F, 6.0F).sound(SoundType.STONE));

public static final DeferredItem<BlockItem> SALT_BLOCK_ITEM =
        ITEMS.registerSimpleBlockItem(SALT_BLOCK);   // Holder-перегрузка, имя и prefix — автоматом

public static final DeferredItem<Item> KUBAN_APPLE = ITEMS.registerSimpleItem("kuban_apple",
        p -> p.food(KUBAN_APPLE_FOOD).stacksTo(16));

// В конструкторе мода:
public KubanHorizon(IEventBus modBus) {
    BLOCKS.register(modBus);
    ITEMS.register(modBus);
}
```

---

## 2. Item.Properties и BlockBehaviour.Properties

### setId — ОБЯЗАТЕЛЕН (при ручной регистрации)

Оба Properties хранят `@Nullable ResourceKey<...> id`; без него `effectiveDescriptionId()` кидает
`NullPointerException("Item id not set" / "Block id not set")`. При использовании
`registerBlock/registerItem/registerSimple*` из `DeferredRegister` — **id ставится автоматически**,
вручную вызывать не нужно. Нужен только при «сыром» `register(name, Supplier)`:

```java
ITEMS.register("thing", () -> new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("kuban_horizon", "thing")))));
```

### Item.Properties (mc-src `net/minecraft/world/item/Item.java`, реализует `IItemPropertiesExtensions`)
```java
public Item.Properties setId(ResourceKey<Item> id)
public Item.Properties overrideDescription(String descriptionId)
public Item.Properties useBlockDescriptionPrefix()     // "block.<ns>.<path>"
public Item.Properties useItemDescriptionPrefix()      // "item.<ns>.<path>" (по умолчанию)

public Item.Properties food(FoodProperties foodProperties)               // = food(fp, Consumables.DEFAULT_FOOD)
public Item.Properties food(FoodProperties foodProperties, Consumable consumable)
public Item.Properties usingConvertsTo(Item item)                        // компонент USE_REMAINDER
public Item.Properties useCooldown(float seconds)
public Item.Properties stacksTo(int max)
public Item.Properties durability(int maxDamage)
public Item.Properties craftRemainder(Item craftingRemainingItem)
public Item.Properties craftRemainder(ItemStackTemplate craftingRemainingItem)   // НОВОЕ: ItemStackTemplate
public Item.Properties rarity(Rarity rarity)
public Item.Properties fireResistant()
public Item.Properties jukeboxPlayable(ResourceKey<JukeboxSong> song)
public Item.Properties enchantable(int value)
public Item.Properties repairable(Item repairItem)
public Item.Properties repairable(TagKey<Item> repairItems)
public Item.Properties setNoCombineRepair()
public Item.Properties equippable(EquipmentSlot slot)
public Item.Properties equippableUnswappable(EquipmentSlot slot)
// Инструменты — данные, а не подклассы (SwordItem и т.п. больше не нужны):
public Item.Properties tool(ToolMaterial, TagKey<Block>, float attackDamage, float attackSpeed, float disableBlockingForSeconds) // сигнатура многострочная
public Item.Properties pickaxe(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline)
public Item.Properties axe(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline)
public Item.Properties hoe(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline)
public Item.Properties shovel(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline)
public Item.Properties sword(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline)
public Item.Properties spear(...)                                        // НОВОЕ в 26.x
public Item.Properties spawnEgg(EntityType<?> type)
public Item.Properties humanoidArmor(ArmorMaterial material, ArmorType type)
public Item.Properties wolfArmor(ArmorMaterial material)
public Item.Properties horseArmor(ArmorMaterial material)
public Item.Properties nautilusArmor(ArmorMaterial material)             // НОВОЕ
public Item.Properties trimMaterial(ResourceKey<TrimMaterial> material)
public Item.Properties requiredFeatures(FeatureFlag... flags)
public Item.Properties attributes(ItemAttributeModifiers attributes)
public <T> Item.Properties component(DataComponentType<T> type, T value)
```

### BlockBehaviour.Properties (mc-src `net/minecraft/world/level/block/state/BlockBehaviour.java`)
```java
public static BlockBehaviour.Properties of()
public static BlockBehaviour.Properties ofFullCopy(BlockBehaviour block)
public static BlockBehaviour.Properties ofLegacyCopy(BlockBehaviour block)

public BlockBehaviour.Properties setId(ResourceKey<Block> id)
public BlockBehaviour.Properties overrideDescription(String descriptionId)

public BlockBehaviour.Properties mapColor(DyeColor dyeColor)
public BlockBehaviour.Properties mapColor(MapColor mapColor)
public BlockBehaviour.Properties mapColor(Function<BlockState, MapColor> mapColor)
public BlockBehaviour.Properties noCollision()        // БЕЗ двойной s! (в 1.21.1 было noCollission)
public BlockBehaviour.Properties noOcclusion()
public BlockBehaviour.Properties friction(float friction)
public BlockBehaviour.Properties speedFactor(float speedFactor)
public BlockBehaviour.Properties jumpFactor(float jumpFactor)
public BlockBehaviour.Properties bounceRestitution(float bounceRestitution)   // НОВОЕ
public BlockBehaviour.Properties sound(SoundType soundType)
public BlockBehaviour.Properties lightLevel(ToIntFunction<BlockState> lightEmission)
public BlockBehaviour.Properties strength(float destroyTime, float explosionResistance)
public BlockBehaviour.Properties strength(float destroyTime)
public BlockBehaviour.Properties instabreak()
public BlockBehaviour.Properties randomTicks()
public BlockBehaviour.Properties dynamicShape()
public BlockBehaviour.Properties noLootTable()
public BlockBehaviour.Properties overrideLootTable(Optional<ResourceKey<LootTable>> table)
public BlockBehaviour.Properties ignitedByLava()
public BlockBehaviour.Properties liquid()
public BlockBehaviour.Properties forceSolidOn()
public BlockBehaviour.Properties forceSolidOff()
public BlockBehaviour.Properties pushReaction(PushReaction pushReaction)
public BlockBehaviour.Properties air()
public BlockBehaviour.Properties isValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> p)
public BlockBehaviour.Properties isRedstoneConductor(BlockBehaviour.StatePredicate p)
public BlockBehaviour.Properties isSuffocating(BlockBehaviour.StatePredicate p)
public BlockBehaviour.Properties isViewBlocking(BlockBehaviour.StatePredicate p)
public BlockBehaviour.Properties postProcess(BlockBehaviour.PostProcess postProcess)
public BlockBehaviour.Properties emissiveRendering(Predicate<BlockState> emissiveRendering)
public BlockBehaviour.Properties requiresCorrectToolForDrops()
public BlockBehaviour.Properties destroyTime(float destroyTime)
public BlockBehaviour.Properties explosionResistance(float explosionResistance)
public BlockBehaviour.Properties offsetType(BlockBehaviour.OffsetType offsetType)
public BlockBehaviour.Properties noTerrainParticles()
public BlockBehaviour.Properties requiredFeatures(FeatureFlag... flags)
public BlockBehaviour.Properties instrument(NoteBlockInstrument instrument)
public BlockBehaviour.Properties replaceable()
```

---

## 3. Еда: FoodProperties + Consumable

### FoodProperties (mc-src `net/minecraft/world/food/FoodProperties.java`)
```java
public record FoodProperties(int nutrition, float saturation, boolean canAlwaysEat) implements ConsumableListener

// Builder:
public FoodProperties.Builder nutrition(int nutrition)
public FoodProperties.Builder saturationModifier(float saturationModifier)  // как в 1.21: модификатор, сатурация считается через FoodConstants.saturationByModifier
public FoodProperties.Builder alwaysEdible()
public FoodProperties build()
```
Эффектов у FoodProperties больше **нет** (нет `effect(...)`) — эффекты живут в `Consumable.onConsume(ConsumeEffect)`.

### Consumable (mc-src `net/minecraft/world/item/component/Consumable.java`)
```java
public record Consumable(float consumeSeconds, ItemUseAnimation animation, Holder<SoundEvent> sound,
                         boolean hasConsumeParticles, List<ConsumeEffect> onConsumeEffects)

public static Consumable.Builder builder()
// Builder:
public Consumable.Builder consumeSeconds(float consumeSeconds)   // default 1.6F
public Consumable.Builder animation(ItemUseAnimation animation)
public Consumable.Builder sound(Holder<SoundEvent> sound)
public Consumable.Builder soundAfterConsume(Holder<SoundEvent> soundAfterConsume)
public Consumable.Builder hasConsumeParticles(boolean hasConsumeParticles)
public Consumable.Builder onConsume(ConsumeEffect effect)        // ApplyStatusEffectsConsumeEffect и др.
public Consumable build()
```

### Consumables — готовые пресеты (mc-src `net/minecraft/world/item/component/Consumables.java`)
```java
public static Consumable.Builder defaultFood()    // 1.6s, EAT, GENERIC_EAT, particles
public static Consumable.Builder defaultDrink()
public static final Consumable DEFAULT_FOOD, DEFAULT_DRINK, HONEY_BOTTLE, OMINOUS_BOTTLE,
        DRIED_KELP, CHICKEN, ENCHANTED_GOLDEN_APPLE, GOLDEN_APPLE, POISONOUS_POTATO,
        PUFFERFISH, ROTTEN_FLESH, SPIDER_EYE, MILK_BUCKET, CHORUS_FRUIT;
```

### Пример еды с эффектом
```java
public static final FoodProperties BORSCHT_FOOD =
        new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).build();

public static final Consumable BORSCHT_CONSUMABLE = Consumables.defaultFood()
        .consumeSeconds(2.0F)
        .onConsume(new ApplyStatusEffectsConsumeEffect(
                new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0F))
        .build();

public static final DeferredItem<Item> BORSCHT = ITEMS.registerSimpleItem("borscht",
        p -> p.food(BORSCHT_FOOD, BORSCHT_CONSUMABLE).stacksTo(1).usingConvertsTo(Items.BOWL));
```

---

## 4. CreativeModeTab

Регистрируется через `DeferredRegister<CreativeModeTab>` по ключу `Registries.CREATIVE_MODE_TAB`.

```java
public static CreativeModeTab.Builder builder()                                  // NeoForge-перегрузка (в ваниле builder(Row, int))
public static CreativeModeTab.Builder builder(CreativeModeTab.Row row, int column)

// Builder:
public CreativeModeTab.Builder title(Component displayName)
public CreativeModeTab.Builder icon(Supplier<ItemStack> iconGenerator)
public CreativeModeTab.Builder displayItems(CreativeModeTab.DisplayItemsGenerator displayItemsGenerator)
public CreativeModeTab.Builder displayItems(Collection<? extends Holder<? extends ItemLike>> collection) // NeoForge: можно скормить BLOCKS/ITEMS.getEntries()
public CreativeModeTab.Builder alignedRight()
public CreativeModeTab.Builder hideTitle()
public CreativeModeTab.Builder noScrollBar()
public CreativeModeTab.Builder backgroundTexture(Identifier backgroundTexture)   // Identifier!
public CreativeModeTab.Builder withSearchBar()
public CreativeModeTab.Builder withSearchBar(int searchBarWidth)                 // NeoForge
public CreativeModeTab.Builder withScrollBarSpriteLocation(Identifier loc)       // NeoForge
public CreativeModeTab.Builder withTabsImage(Identifier tabsImage)               // NeoForge
public CreativeModeTab.Builder withLabelColor(int labelColor)                    // NeoForge
public CreativeModeTab.Builder withTabFactory(Function<CreativeModeTab.Builder, CreativeModeTab> f) // NeoForge
public CreativeModeTab.Builder withTabsBefore(Identifier... tabs)                // NeoForge
public CreativeModeTab.Builder withTabsAfter(Identifier... tabs)                 // NeoForge
public CreativeModeTab build()
```

```java
public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "kuban_horizon");

public static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register("main",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.kuban_horizon.main"))
                .icon(() -> BORSCHT.toStack())
                .displayItems((params, output) -> {
                    output.accept(KUBAN_APPLE.get());
                    output.accept(SALT_BLOCK_ITEM.get());
                })
                .build());
```

---

## 5. DataComponents для еды (mc-src `net/minecraft/core/component/DataComponents.java`)

```java
public static final DataComponentType<FoodProperties> FOOD        // "food"
public static final DataComponentType<Consumable>     CONSUMABLE  // "consumable"
```

`Item.Properties.food(fp, consumable)` — это просто
`component(DataComponents.FOOD, fp).component(DataComponents.CONSUMABLE, consumable)`.
Также рядом: `DataComponents.USE_REMAINDER` (заполняется `usingConvertsTo(Item)`).

Чтение в рантайме: `stack.get(DataComponents.FOOD)`, `stack.has(DataComponents.CONSUMABLE)`.

---

## Ловушки при миграции с 1.21.1

- `ResourceLocation` → `Identifier` (`Identifier.fromNamespaceAndPath(ns, path)`, `Identifier.parse(...)`, `Identifier.withDefaultNamespace(...)`).
- `noCollission()` → `noCollision()` (исправлена опечатка ванилы).
- `new Item(new Item.Properties())` без `setId` → NPE при первом обращении к описанию/модели. Используйте `registerItem`/`registerSimpleItem`.
- Модель предмета берётся из id (`effectiveModel()`), кастомный сеттер модели отсутствует (поле `model` финальное — `ResourceKey::identifier`).
- `SwordItem`/`PickaxeItem` и т.д. — заменены на `Item.Properties.sword(...)/pickaxe(...)/...`.
- `craftRemainder` теперь принимает и `ItemStackTemplate`.
