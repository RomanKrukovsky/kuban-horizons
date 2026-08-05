# Шпаргалка: меню, рецепты, сеть — Minecraft 26.2 + NeoForge 26.2.0.48-beta

> Проверено по исходникам: `/Users/romanmolodyko/mc-sources/26.2/mc-src` и `.../neoforge-src`.
> Главное глобальное переименование: `ResourceLocation` → **`Identifier`** (`net.minecraft.resources.Identifier`).

---

## 1. Меню (AbstractContainerMenu, ContainerData, MenuType)

### AbstractContainerMenu (`mc-src/net/minecraft/world/inventory/AbstractContainerMenu.java`)

Ключевые члены:

```java
public final int containerId;
protected AbstractContainerMenu(@Nullable MenuType<?> menuType, int containerId)

protected Slot addSlot(Slot slot)
protected DataSlot addDataSlot(DataSlot dataSlot)
protected void addDataSlots(ContainerData container)      // добавляет DataSlot.forContainer на каждый индекс

// Обязательные абстрактные методы:
public abstract ItemStack quickMoveStack(Player player, int slotIndex);  // shift-click
public abstract boolean stillValid(Player player);

// Полезные:
protected static boolean stillValid(ContainerLevelAccess access, Player player, Block block)
public void slotsChanged(Container container)
public void removed(Player player)
public void broadcastChanges()
public void setData(int id, int value)   // клиентская сторона получает синк ContainerData
```

Обратить внимание: `clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player)` —
третий параметр теперь **`ContainerInput`** (не `ClickType`).

### ContainerData / DataSlot / SimpleContainerData

```java
public interface ContainerData {
    int get(int dataId);
    void set(int dataId, int value);
    int getCount();
}
```

- `SimpleContainerData implements ContainerData` — простой int-массив (для standalone-меню).
- `DataSlot.forContainer(ContainerData, int)`, `DataSlot.shared(int[], int)`, `DataSlot.standalone()`.
- Синхронизируются только int'ы, сервер → клиент, через `broadcastChanges()`.

### Slot (`world/inventory/Slot.java`)

```java
public Slot(Container container, int slot, int x, int y)
public boolean mayPlace(ItemStack itemStack)
public boolean mayPickup(Player player)
public void onTake(Player player, ItemStack carried)
public boolean isActive()   // false = слот не рендерится и не кликается
```

### MenuType и регистрация (NeoForge)

`MenuType<T extends AbstractContainerMenu> implements FeatureElement, IMenuTypeExtension<T>`.
Реестр: `BuiltInRegistries.MENU` (`Registries.MENU`).

**IMenuTypeExtension** (`neoforge-src/net/neoforged/neoforge/common/extensions/IMenuTypeExtension.java`):

```java
static <T extends AbstractContainerMenu> MenuType<T> create(IContainerFactory<T> factory) {
    return new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS);
}
T create(int windowId, Inventory playerInv, RegistryFriendlyByteBuf extraData);
```

**IContainerFactory** (`neoforge-src/net/neoforged/neoforge/network/IContainerFactory.java`)
расширяет `MenuType.MenuSupplier<T>`:

```java
T create(int windowId, Inventory inv, RegistryFriendlyByteBuf data); // data может быть null (обычный путь)
```

Регистрация через DeferredRegister:

```java
public static final DeferredRegister<MenuType<?>> MENUS =
    DeferredRegister.create(Registries.MENU, MODID);

public static final Supplier<MenuType<MyMenu>> MY_MENU =
    MENUS.register("my_menu", () -> IMenuTypeExtension.create(MyMenu::new));
    // MyMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf buf)
```

### Открытие меню с extra-данными (сервер)

`IPlayerExtension` (neoforge):

```java
OptionalInt openMenu(@Nullable MenuProvider menuProvider, BlockPos pos)   // пишет buf.writeBlockPos(pos)
OptionalInt openMenu(@Nullable MenuProvider menuProvider, @Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter)
// лимит extraData — 32600 байт
```

Плюс `IMenuProviderExtension.writeClientSideData(AbstractContainerMenu, RegistryFriendlyByteBuf)` —
данные пишутся ПЕРЕД extraDataWriter.

---

## 2. Кастомные рецепты

### Интерфейс Recipe (`mc-src/net/minecraft/world/item/crafting/Recipe.java`)

```java
public interface Recipe<T extends RecipeInput> {
    boolean matches(T input, Level level);          // обязателен
    ItemStack assemble(T input);                    // обязателен; БЕЗ HolderLookup.Provider в 26.2!
    default boolean isSpecial() { return false; }
    boolean showNotification();                     // обязателен (обычно из Recipe.CommonInfo)
    String group();                                 // обязателен
    RecipeSerializer<? extends Recipe<T>> getSerializer();
    RecipeType<? extends Recipe<T>> getType();
    PlacementInfo placementInfo();                  // обязателен (для recipe book / авто-раскладки)
    default List<RecipeDisplay> display() { return List.of(); }
    RecipeBookCategory recipeBookCategory();        // обязателен
}
```

Изменения относительно 1.21.x:
- `assemble(input)` — **без** `HolderLookup.Provider`.
- **Нет** `getResultItem()` / `canCraftInDimensions()` — вместо них `display()` (список `RecipeDisplay`).
- Появился вложенный **`Recipe.CommonInfo(boolean showNotification)`** с готовыми
  `MAP_CODEC` и `STREAM_CODEC` — принято хранить его полем и делегировать `showNotification()`.
- `Recipe.BookInfo<CategoryType>` — обёртка category+group с фабриками `mapCodec`/`streamCodec`.
- NeoForge добавляет `Recipe.CONDITIONAL_CODEC` (условия загрузки).

Результат рецепта теперь **`ItemStackTemplate`** (не ItemStack): `result.create()` → ItemStack.
Кодеки: `ItemStackTemplate.CODEC`, `ItemStackTemplate.STREAM_CODEC`.

### RecipeSerializer — теперь record!

```java
public record RecipeSerializer<T extends Recipe<?>>(
    MapCodec<T> codec,
    @Deprecated StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {}
```

Не нужно имплементить интерфейс — просто `new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC)`.
Реестр: `BuiltInRegistries.RECIPE_SERIALIZER`.

### RecipeType

```java
public interface RecipeType<T extends Recipe<?>> {
    static <T extends Recipe<?>> RecipeType<T> simple(Identifier name)  // для модов
}
```

Реестр: `BuiltInRegistries.RECIPE_TYPE` (`Registries.RECIPE_TYPE`).

### RecipeInput

```java
public interface RecipeInput {
    ItemStack getItem(int index);
    int size();
    default boolean isEmpty();
}
```

Готовые: `SingleRecipeInput(ItemStack item)`, `CraftingInput`, `SmithingRecipeInput`.

### PlacementInfo

```java
PlacementInfo.create(Ingredient)                 // один ингредиент
PlacementInfo.create(List<Ingredient>)
PlacementInfo.createFromOptionals(List<Optional<Ingredient>>)  // shaped с пустыми слотами
PlacementInfo.NOT_PLACEABLE                      // если рецепт нельзя разложить (isSpecial)
```

Ленивая инициализация в поле `@Nullable PlacementInfo placementInfo` — как в `SingleItemRecipe`.

### Пример — SingleItemRecipe (эталон минимального рецепта)

```java
public abstract class SingleItemRecipe implements Recipe<SingleRecipeInput> {
    protected final Recipe.CommonInfo commonInfo;
    private final Ingredient input;
    private final ItemStackTemplate result;
    private @Nullable PlacementInfo placementInfo;

    public boolean matches(SingleRecipeInput input, Level level) { return this.input.test(input.item()); }
    public ItemStack assemble(SingleRecipeInput input) { return this.result.create(); }
    public boolean showNotification() { return this.commonInfo.showNotification(); }
    public PlacementInfo placementInfo() {
        if (placementInfo == null) placementInfo = PlacementInfo.create(this.input);
        return placementInfo;
    }
    // MapCodec через RecordCodecBuilder:
    //   Recipe.CommonInfo.MAP_CODEC.forGetter(...),
    //   Ingredient.CODEC.fieldOf("ingredient"),
    //   ItemStackTemplate.CODEC.fieldOf("result")
    // StreamCodec.composite(Recipe.CommonInfo.STREAM_CODEC, ..., Ingredient.CONTENTS_STREAM_CODEC, ...,
    //   ItemStackTemplate.STREAM_CODEC, ..., factory::create)
}
```

`AbstractCookingRecipe extends SingleItemRecipe` добавляет `CookingBookInfo` (category+group),
`experience`, `cookingTime` и реализует `display()`:

```java
public List<RecipeDisplay> display() {
    return List.of(new FurnaceRecipeDisplay(
        this.input().display(), SlotDisplay.AnyFuel.INSTANCE,
        new SlotDisplay.ItemStackSlotDisplay(this.result()),
        new SlotDisplay.ItemSlotDisplay(this.furnaceIcon()),
        this.cookingTime, this.experience));
}
```

Пакет display: `crafting/display/` — `RecipeDisplay`, `SlotDisplay` (варианты `ItemSlotDisplay`,
`ItemStackSlotDisplay`, `TagSlotDisplay`, `AnyFuel`, `Composite`...), `ShapedCraftingRecipeDisplay`,
`FurnaceRecipeDisplay`, `StonecutterRecipeDisplay`, `SmithingRecipeDisplay`.

---

## 3. Поиск рецептов на сервере (RecipeManager)

`RecipeManager extends SimplePreparableReloadListener<RecipeMap> implements RecipeAccess` —
рецепты живут **только на сервере**; клиент получает лишь `RecipePropertySet` и display-данные.

```java
// Основной поиск:
<I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level)
// С подсказкой (кэш последнего рецепта):
Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level, @Nullable ResourceKey<Recipe<?>> recipeHint)
Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level, @Nullable RecipeHolder<T> recipeHint)

Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> recipeId)
Collection<RecipeHolder<?>> getRecipes()
RecipeMap recipeMap()                                // весь индекс

// Кэшированная проверка для BlockEntity (как у печки):
static <I,T> RecipeManager.CachedCheck<I,T> createCheck(RecipeType<T> type)
//   check.getRecipeFor(input, serverLevel) — запоминает lastRecipe (ResourceKey)
```

Получение RecipeManager: `serverLevel.recipeAccess()` (в `CachedCheck` именно так).

### RecipeMap (`crafting/RecipeMap.java`)

```java
static RecipeMap create(Iterable<RecipeHolder<?>> recipes)
<I,T> Collection<RecipeHolder<T>> byType(RecipeType<T> type)
@Nullable RecipeHolder<?> byKey(ResourceKey<Recipe<?>> recipeId)
<I,T> Stream<RecipeHolder<T>> getRecipesFor(RecipeType<T> type, I input, Level level)  // фильтр по matches()
Collection<RecipeHolder<?>> values()
// NeoForge: order(Object2IntMap<...>) — приоритеты рецептов (data map)
```

### RecipePropertySet — клиентские предикаты входов

Клиент не знает рецептов, но знает, какие предметы «влезают» в слоты:

```java
RecipePropertySet.FURNACE_INPUT / BLAST_FURNACE_INPUT / SMOKER_INPUT / CAMPFIRE_INPUT
RecipePropertySet.SMITHING_BASE / SMITHING_TEMPLATE / SMITHING_ADDITION

recipeManager.propertySet(ResourceKey<RecipePropertySet> id)  // .test(ItemStack)
recipeManager.getSynchronizedItemProperties()                 // синкается на клиент
```

В меню (напр. слот печки) — `level.recipeAccess().propertySet(...).test(stack)` вместо поиска рецепта.
Стоункаттер: `recipeManager.stonecutterRecipes()` → `SelectableRecipe.SingleInputSet<StonecutterRecipe>`.

Ключ рецепта — `ResourceKey<Recipe<?>>` в реестре `Registries.RECIPE` (`Recipe.KEY_CODEC`).

---

## 4. Сетевые payloads (NeoForge)

Пакет: `neoforge-src/net/neoforged/neoforge/network/`.

### CustomPacketPayload (vanilla, `network/protocol/common/custom/CustomPacketPayload.java`)

```java
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();
    record Type<T extends CustomPacketPayload>(Identifier id) {}
    static <T> Type<T> createType(String id)   // Identifier.parse(id)
    static <B extends ByteBuf, T> StreamCodec<B,T> codec(StreamMemberEncoder<B,T> writer, StreamDecoder<B,T> reader)
}
```

Типовой payload:

```java
public record MyPayload(BlockPos pos, int value) implements CustomPacketPayload {
    public static final Type<MyPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MODID, "my_payload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MyPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, MyPayload::pos,
            ByteBufCodecs.VAR_INT, MyPayload::value,
            MyPayload::new);
    @Override public Type<MyPayload> type() { return TYPE; }
}
```

### RegisterPayloadHandlersEvent (мод-бас)

`network/event/RegisterPayloadHandlersEvent.java`:

```java
public PayloadRegistrar registrar(String version)   // версия канала, не пустая
```

### PayloadRegistrar — точные сигнатуры (`network/registration/PayloadRegistrar.java`)

```java
// play-фаза (RegistryFriendlyByteBuf):
<T extends CustomPacketPayload> PayloadRegistrar playToClient(
    CustomPacketPayload.Type<T> type,
    StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
    IPayloadHandler<T> handler)
<T> PayloadRegistrar playToClient(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) // без хендлера (только отправка)
<T> PayloadRegistrar playToServer(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler)
<T> PayloadRegistrar playBidirectional(Type<T> type, StreamCodec<...> codec,
    IPayloadHandler<T> serverHandler, @Nullable IPayloadHandler<T> clientHandler)

// configuration-фаза (FriendlyByteBuf):
configurationToClient / configurationToServer / configurationBidirectional (аналогично, FriendlyByteBuf)
// обе фазы сразу:
commonToClient / commonToServer / commonBidirectional (FriendlyByteBuf)

// модификаторы (builder-стиль, влияют на последующие регистрации):
PayloadRegistrar executesOn(HandlerThread thread)  // MAIN (дефолт) | NETWORK
PayloadRegistrar versioned(String version)
PayloadRegistrar optional()                        // канал не обязателен у второй стороны
```

### IPayloadHandler / IPayloadContext (`network/handling/`)

```java
@FunctionalInterface
public interface IPayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload, IPayloadContext context);
}

public interface IPayloadContext {
    Player player();                                     // ServerPlayer на сервере
    default void reply(CustomPacketPayload payload)
    default void disconnect(Component reason)
    CompletableFuture<Void> enqueueWork(Runnable task);  // на главный поток
    <T> CompletableFuture<T> enqueueWork(Supplier<T> task);
    PacketFlow flow();
    default ConnectionProtocol protocol()
    default Connection connection()
}
```

По умолчанию хендлеры уже оборачиваются на MAIN-поток (`MainThreadPayloadHandler`),
`enqueueWork` нужен только при `executesOn(HandlerThread.NETWORK)`.

### Отправка

`PacketDistributor` (сервер → клиент), все static void:

```java
sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads)
sendToPlayersInDimension(ServerLevel level, ...)
sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, y, z, radius, ...)
sendToAllPlayers(...)
sendToPlayersTrackingEntity(Entity entity, ...)
sendToPlayersTrackingEntityAndSelf(Entity entity, ...)
sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, ...)
```

Клиент → сервер: **`ClientPacketDistributor.sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads)`**
(`neoforge-src/net/neoforged/neoforge/client/network/ClientPacketDistributor.java`).

Правила: id payload'а не писать в StreamCodec (пишется автоматически); payload сериализуется
всегда, даже в singleplayer (memory connection).

---

## 5. Screens (26.2 — КРУПНЫЕ изменения рендера!)

### GuiGraphics → GuiGraphicsExtractor

В 26.2 класса `GuiGraphics` нет — вместо него **`GuiGraphicsExtractor`**
(`mc-src/net/minecraft/client/gui/GuiGraphicsExtractor.java`). Рендер стал «экстракцией»
в `GuiRenderState` (retained-режим). Слои — через `graphics.nextStratum()`.
`pose()` возвращает `Matrix3x2fStack` (2D: `pushMatrix()/translate(x,y)/popMatrix()`).

### Методы рендера Screen / AbstractContainerScreen переименованы

| Было (1.21.x)                | Стало (26.2)                                                  |
|------------------------------|---------------------------------------------------------------|
| `render(GuiGraphics,...)`    | `extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a)` |
| `renderBg(...)`              | `extractBackground(GuiGraphicsExtractor g, int mx, int my, float a)` (метод Screen, переопределяется) |
| `renderLabels(...)`          | `extractLabels(GuiGraphicsExtractor g, int xm, int ym)`       |
| `renderTooltip(...)`         | `extractTooltip(GuiGraphicsExtractor g, int mx, int my)`      |
| `drawString(font,...)`       | `graphics.text(Font, String/Component, x, y, color, boolean dropShadow)` |
| `drawCenteredString`         | `graphics.centeredText(...)`                                  |
| `renderItem(...)`            | `graphics.item(ItemStack, x, y)`                              |

Пример из `AbstractFurnaceScreen`:

```java
@Override
public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    super.extractBackground(graphics, mouseX, mouseY, a);
    graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, xo, yo,
                  0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.burnProgressSprite,
                        24, 16, 0, 0, xo + 79, yo + 34, burnProgressWidth, 16);
}
```

Labels (дефолт из AbstractContainerScreen):

```java
protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
    graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
}
```

### Сигнатуры blit (все требуют RenderPipeline первым аргументом, кроме одной)

```java
// основная (текстура-атлас 256x256 и т.п.):
blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
     int width, int height, int textureWidth, int textureHeight)
blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight, int color)
blit(pipeline, texture, x, y, u, v, width, height, int srcWidth, int srcHeight, int texW, int texH [, int color])
// UV напрямую:
blit(Identifier location, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1)

// GUI-спрайты (из атласа gui):
blitSprite(RenderPipeline pipeline, Identifier location, int x, int y, int width, int height)
blitSprite(pipeline, location, x, y, width, height, float alpha)
blitSprite(pipeline, location, x, y, width, height, int color)
blitSprite(pipeline, location, int spriteW, int spriteH, int uOff, int vOff, int x, int y, int w, int h) // частичный (прогресс-бары)
blitSprite(pipeline, TextureAtlasSprite sprite, x, y, width, height [, color])

fill(int x0, int y0, int x1, int y1, int col)
```

Стандартный pipeline: `net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED`.

### Конструктор AbstractContainerScreen

```java
AbstractContainerScreen(T menu, Inventory inventory, Component title)                      // 176x166
AbstractContainerScreen(T menu, Inventory inventory, Component title, int imageWidth, int imageHeight)
// поля: leftPos, topPos, imageWidth, imageHeight, titleLabelX/Y, inventoryLabelX/Y, hoveredSlot, menu
// init(): leftPos = (width - imageWidth)/2; topPos = (height - imageHeight)/2;
```

Порядок рендера (Screen.extractRenderStateWithTooltipAndSubtitles):
`nextStratum → extractBackground → [ScreenEvent.Render.Background] → nextStratum →
extractRenderState (contents: labels, slots, highlights) → [Render.Foreground] → carried item → tooltip`.

### RegisterMenuScreensEvent (мод-бас, только клиент)

`neoforge-src/net/neoforged/neoforge/client/event/RegisterMenuScreensEvent.java`:

```java
public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(
    MenuType<? extends M> menuType, MenuScreens.ScreenConstructor<M, U> screenConstructor)
// ScreenConstructor: (M menu, Inventory inv, Component title) -> U
// Дубликат = IllegalStateException

@SubscribeEvent
static void onRegisterScreens(RegisterMenuScreensEvent event) {
    event.register(MY_MENU.get(), MyScreen::new);
}
```

---

## Мини-чеклист для новой фичи «станок с меню + рецептом + пакетом»

1. `RecipeInput`-record → `Recipe<MyInput>` (matches/assemble/placementInfo/recipeBookCategory + CommonInfo).
2. `new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC)` → регистрация в `Registries.RECIPE_SERIALIZER`; `RecipeType.simple(id)` → `Registries.RECIPE_TYPE`.
3. Поиск: `RecipeManager.createCheck(TYPE)` в BlockEntity, `check.getRecipeFor(input, serverLevel)`.
4. Меню: `IMenuTypeExtension.create((id, inv, buf) -> new MyMenu(...))` → `Registries.MENU`; открытие `serverPlayer.openMenu(provider, buf -> ...)`.
5. Прогресс — `ContainerData` + `addDataSlots`; сложные данные — свой payload через `RegisterPayloadHandlersEvent`/`PayloadRegistrar.playToClient`.
6. Экран: `extends AbstractContainerScreen<MyMenu>`, `extractBackground` + `graphics.blit(RenderPipelines.GUI_TEXTURED, ...)`, регистрация в `RegisterMenuScreensEvent`.
