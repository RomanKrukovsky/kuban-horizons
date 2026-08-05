# Блоки, культуры и BlockEntity — Minecraft 26.2 + NeoForge 26.2.0.48-beta

Источники: `mc-sources/26.2/mc-src` (декомпилированные, с Neo-патчами) и `neoforge-src`.
Напоминание: `ResourceLocation` → `net.minecraft.resources.Identifier`.

---

## 1. CropBlock (`net.minecraft.world.level.block.CropBlock`)

Иерархия: `CropBlock extends VegetationBlock implements BonemealableBlock`.

Ключевые константы:

```java
public static final MapCodec<CropBlock> CODEC = simpleCodec(CropBlock::new);
public static final int MAX_AGE = 7;
public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
private static final VoxelShape[] SHAPES = Block.boxes(7, age -> Block.column(16.0, 0.0, 2 + age * 2));
```

### Методы (точные сигнатуры)

```java
public CropBlock(BlockBehaviour.Properties properties)               // registerDefaultState(... AGE=0)
@Override public MapCodec<? extends CropBlock> codec()

@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
@Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos)
    // return state.is(BlockTags.SUPPORTS_CROPS);  ← ваниль сажает на тег, не на инстанс FarmlandBlock!

protected IntegerProperty getAgeProperty()           // переопредели, если своя AGE
public int getMaxAge()                                // 7; переопределяемый
public int getAge(BlockState state)
public BlockState getStateForAge(int age)
public final boolean isMaxAge(BlockState state)

@Override protected boolean isRandomlyTicking(BlockState state)      // !isMaxAge(state)

@Override
protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
// Логика:
//   if (!level.isAreaLoaded(pos, 1)) return;                        // Neo-патч: не грузить чанки
//   if (level.getRawBrightness(pos, 0) >= 9) {
//       float f = getGrowthSpeed(state, level, pos);
//       // Neo: рост обёрнут в хуки CommonHooks.canCropGrow(...) / fireCropGrowPost(...)
//       if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int)(25.0F / f) + 1) == 0)) {
//           level.setBlock(pos, this.getStateForAge(age + 1), 2);
//           CommonHooks.fireCropGrowPost(level, pos, state);
//       }
//   }

public void growCrops(Level level, BlockPos pos, BlockState state)   // bonemeal: age + [2..5], clamp по maxAge
protected int getBonemealAgeIncrease(Level level)                    // Mth.nextInt(level.getRandom(), 2, 5)

protected static float getGrowthSpeed(BlockState cropBlockState, BlockGetter level, BlockPos pos)
// База 1.0; для каждого из 9 блоков под культурой (3x3 на уровне pos.below()):
//   Neo: soilDecision = blockState.canSustainPlant(level, posBelow, Direction.UP, blockState)
//   если (isDefault ? blockState.is(BlockTags.GROWS_CROPS) : isTrue) → +1.0 (или +3.0 если blockState.isFertile(...) — увлажнённая земля)
//   не-центральные клетки делятся на 4.
// Штраф /2, если такая же культура растёт и по горизонтали, и по вертикали (или по диагонали).

@Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
// Neo: сперва canSustainPlant у блока снизу; если default →
//   hasSufficientLight(level, pos) && super.canSurvive(...)
public static boolean hasSufficientLight(LevelReader level, BlockPos pos)  // getRawBrightness(pos,0) >= 8

@Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
        InsideBlockEffectApplier effectApplier, boolean isPrecise)          // Ravager топчет (через EventHooks.canEntityGrief)

protected ItemLike getBaseSeedId()                    // Items.WHEAT_SEEDS; ГЛАВНЫЙ метод для своих семян
@Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData)

// BonemealableBlock:
@Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)  // !isMaxAge
@Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) // true
@Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) // growCrops

@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) // builder.add(AGE)
```

### VoxelShape API (статические хелперы в `Block`)

```java
public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
public static VoxelShape[] boxes(int endInclusive, IntFunction<VoxelShape> voxelShapeFactory) // массив 0..endInclusive
public static VoxelShape cube(double size)
public static VoxelShape cube(double sizeX, double sizeY, double sizeZ)
public static VoxelShape column(double sizeXZ, double minY, double maxY)   // центрированная колонна, размеры в пикселях (0..16)
public static VoxelShape column(double sizeX, double sizeZ, double minY, double maxY)
public static VoxelShape boxZ(double sizeXY, double minZ, double maxZ)     // + перегрузки
```

Пример из CropBlock: `Block.boxes(7, age -> Block.column(16.0, 0.0, 2 + age * 2))` — по фигуре на каждый age.
Есть также `BlockBehaviour#getShapeForEachState(Function<BlockState, VoxelShape>)` → `Function<BlockState, VoxelShape>` (используется в PitcherCropBlock).

---

## 2. Двухблочная культура — PitcherCropBlock (эталон)

`PitcherCropBlock extends DoublePlantBlock implements BonemealableBlock`

```java
public static final int MAX_AGE = 4;
public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF; // = BlockStateProperties.DOUBLE_BLOCK_HALF
private static final int DOUBLE_PLANT_AGE_INTERSECTION = 3;  // с age>=3 занимает 2 блока
private final Function<BlockState, VoxelShape> shapes = this.makeShapes(); // через getShapeForEachState
```

### Принцип работы

1. **Пока age < 3 — блок ОДИНАРНЫЙ** (существует только LOWER-половина).
   `isDouble(int age) { return age >= 3; }`
2. **Ставится как одиночный**: `getStateForPlacement` возвращает просто `defaultBlockState()`, а `setPlacedBy` переопределён ПУСТЫМ — верхняя половина при посадке НЕ ставится (в отличие от DoublePlantBlock, где setPlacedBy ставит UPPER).
3. **Тикает только низ**:
   ```java
   @Override public boolean isRandomlyTicking(BlockState state) {
       return state.getValue(HALF) == DoubleBlockHalf.LOWER && !this.isMaxAge(state);
   }
   @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
       float growthSpeed = CropBlock.getGrowthSpeed(state, level, pos);   // переиспользует статику CropBlock!
       if (random.nextInt((int)(25.0F / growthSpeed) + 1) == 0) this.grow(level, state, pos, 1);
   }
   ```
4. **Рост** — верхняя половина появляется в момент пересечения порога:
   ```java
   private void grow(ServerLevel level, BlockState lowerState, BlockPos lowerPos, int increase) {
       int updatedAge = Math.min(lowerState.getValue(AGE) + increase, 4);
       if (this.canGrow(level, lowerPos, lowerState, updatedAge)) {
           BlockState newLowerState = lowerState.setValue(AGE, updatedAge);
           level.setBlock(lowerPos, newLowerState, 2);
           if (isDouble(updatedAge)) {
               level.setBlock(lowerPos.above(), newLowerState.setValue(HALF, DoubleBlockHalf.UPPER), 3);
           }
       }
   }
   private boolean canGrow(LevelReader level, BlockPos lowerPos, BlockState lowerState, int newAge) {
       return !this.isMaxAge(lowerState) && sufficientLight(level, lowerPos)
           && level.isInsideBuildHeight(lowerPos.above())
           && (!isDouble(newAge) || canGrowInto(level, lowerPos.above()));  // сверху air или сама культура
   }
   ```
5. **updateShape**: если double — делегирует `DoublePlantBlock.updateShape` (взаимное разрушение половин); если single — просто `canSurvive ? state : AIR`:
   ```java
   @Override public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
           BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random)
   ```
6. **PosAndState** — приватный record для нормализации «кликнули по любой половине → найти нижнюю»:
   ```java
   private record PosAndState(BlockPos pos, BlockState state) {}

   private PitcherCropBlock.@Nullable PosAndState getLowerHalf(LevelReader level, BlockPos pos, BlockState state) {
       if (isLower(state)) return new PosAndState(pos, state);
       BlockPos lowerPos = pos.below();
       BlockState lowerState = level.getBlockState(lowerPos);
       return isLower(lowerState) ? new PosAndState(lowerPos, lowerState) : null;
   }
   private static boolean isLower(BlockState s) { return s.is(Blocks.PITCHER_CROP) && s.getValue(HALF) == DoubleBlockHalf.LOWER; }
   ```
7. **Bonemeal через нижнюю половину** (кликать можно по любой):
   ```java
   @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
       PosAndState lowerHalf = this.getLowerHalf(level, pos, state);
       return lowerHalf != null && this.canGrow(level, lowerHalf.pos, lowerHalf.state, lowerHalf.state.getValue(AGE) + 1);
   }
   @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
       PosAndState lowerHalf = this.getLowerHalf(level, pos, state);
       if (lowerHalf != null) this.grow(level, lowerHalf.state, lowerHalf.pos, 1);  // BONEMEAL_INCREASE = 1
   }
   ```
   Примечание: у CropBlock метод называется `growCrops(...)`, у PitcherCropBlock — приватный `grow(...)`; общего `growCrops` в двухблочном варианте нет.
8. **Формы**: `makeShapes()` строит `Function<BlockState, VoxelShape>` через `getShapeForEachState`; LOWER идёт от y=-1 (чуть врастает в землю), UPPER — остаток высоты. `getCollisionShape` у UPPER — `Shapes.empty()`.
9. **createBlockStateDefinition**: `builder.add(AGE); super.createBlockStateDefinition(builder);` (super добавляет HALF).
10. `canSurvive`: Neo-версия сначала спрашивает `canSustainPlant` у почвы; `mayPlaceOn` → `BlockTags.SUPPORTS_CROPS`. `canBeReplaced` → false.

### DoublePlantBlock — полезное для своих двухблочников

```java
public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
// updateShape: если сосед по оси Y перестал быть парной половиной → AIR
// canSurvive у UPPER: блок снизу == this && HALF == LOWER
public static void placeAt(LevelAccessor level, BlockState state, BlockPos lowerPos, @Block.UpdateFlags int updateType)
// setPlacedBy: ставит UPPER над LOWER (PitcherCrop это отключает)
```

`DoubleBlockHalf` — enum `UPPER / LOWER` в `net.minecraft.world.level.block.state.properties`.

---

## 3. VegetationBlock / BonemealableBlock / FarmlandBlock / SweetBerryBushBlock

### VegetationBlock (бывший BushBlock)

`public abstract class VegetationBlock extends Block`

```java
protected abstract MapCodec<? extends VegetationBlock> codec();
protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos)  // BlockTags.SUPPORTS_VEGETATION
@Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
        BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random)
        // !canSurvive → AIR
@Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
        // Neo: belowState.canSustainPlant(level, below, Direction.UP, state); если default → mayPlaceOn(belowState, ...)
@Override protected boolean propagatesSkylightDown(BlockState state)
@Override protected boolean isPathfindable(BlockState state, PathComputationType type)
```

Обрати внимание: `updateShape` теперь принимает `LevelReader + ScheduledTickAccess + RandomSource` (не LevelAccessor).

### BonemealableBlock (интерфейс)

```java
boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state);
boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state);
void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state);

static boolean hasSpreadableNeighbourPos(LevelReader level, BlockPos pos, BlockState blockToPlace)
static Optional<BlockPos> findSpreadableNeighbourPos(Level level, BlockPos pos, BlockState blockToPlace)

default BlockPos getParticlePos(BlockPos blockPos)   // above() для NEIGHBOR_SPREADER
default BonemealableBlock.Type getType()             // enum Type { NEIGHBOR_SPREADER, GROWER } — default GROWER
```

### FarmlandBlock (moisture API) — класс называется `FarmlandBlock`, не FarmBlock

```java
public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE;  // IntegerProperty.create("moisture", 0, 7)
public static final int MAX_MOISTURE = 7;
private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 15.0);

@Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
// нет воды и дождя: moisture-- ; при 0 и без растения (тег MAINTAINS_FARMLAND) → turnToDirt
// вода/дождь рядом: moisture = 7
@Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) // !canSurvive → dirt
@Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
// !aboveState.isSolid() || shouldMaintainFarmland (above().is(BlockTags.MAINTAINS_FARMLAND))
@Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance)
// Neo: CommonHooks.onFarmlandTrample(...) → turnToDirt (вытаптывание)
public static void turnToDirt(@Nullable Entity sourceEntity, BlockState state, Level level, BlockPos pos)
private static boolean isNearWater(LevelReader level, BlockPos pos)
// скан BlockPos.betweenClosed(pos.offset(-4,0,-4), pos.offset(4,1,4)) через state.canBeHydrated(...)
// + NeoForge: FarmlandWaterManager.hasBlockWaterTicket(level, pos)
```

Neo-хук плодородия: `blockState.isFertile(level, pos)` — увлажнённая farmland даёт x3 в `getGrowthSpeed`.

### SweetBerryBushBlock (многосборный куст)

`extends VegetationBlock implements BonemealableBlock`

```java
public static final int MAX_AGE = 3;
public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
// Формы: age 0 → column(10,0,8); age 3 → Shapes.block(); иначе column(14,0,16)

@Override protected void randomTick(...)  // age<3, свет над блоком >=9, шанс 1/5, через CommonHooks.canCropGrow

// Сбор без разрушения — паттерн многоразового урожая:
@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
// if (AGE > 1):
//   Block.dropFromBlockInteractLootTable(serverLevel, BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH,
//       state, level.getBlockEntity(pos), null, player, (lvl, stack) -> Block.popResource(lvl, pos, stack));
//   звук + setBlock(pos, state.setValue(AGE, 1), 2) + gameEvent(BLOCK_CHANGE)
//   return InteractionResult.SUCCESS;

@Override protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hitResult)
// не max age + костная мука → PASS (пропустить к обработке предмета), иначе super

@Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
        InsideBlockEffectApplier effectApplier, boolean isPrecise)
// makeStuckInBlock(state, new Vec3(0.8F, 0.75, 0.8F)); урон при движении >= 0.003 (кроме FOX/BEE)
```

Важно: дроп при сборе идёт через **loot table взаимодействия** (`BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH` + `Block.dropFromBlockInteractLootTable`), а не хардкодом количества.

---

## 4. BaseEntityBlock + BlockEntity — СЕРИАЛИЗАЦИЯ СМЕНИЛАСЬ

### ⚠️ Главное: `saveAdditional`/`loadAdditional` теперь берут `ValueOutput`/`ValueInput`, НЕ `CompoundTag`

`net.minecraft.world.level.storage.ValueInput` / `ValueOutput` — абстракция над NBT (реализации `TagValueInput` / `TagValueOutput`), с `ProblemReporter` для логирования ошибок и без прямого доступа к тегам.

```java
// BlockEntity:
protected void loadAdditional(ValueInput input)      // переопределяем; super читает NeoForgeData + attachments
protected void saveAdditional(ValueOutput output)    // переопределяем; super пишет NeoForgeData + attachments

public final void loadWithComponents(ValueInput input)   // loadAdditional + data components
public final void loadCustomOnly(ValueInput input)

public void saveWithoutMetadata(ValueOutput output)       // saveAdditional + components
public final CompoundTag saveWithoutMetadata(HolderLookup.Provider registries)  // мост в CompoundTag через TagValueOutput
public void saveWithFullMetadata(ValueOutput output)      // + id + x/y/z
public final CompoundTag saveWithFullMetadata(HolderLookup.Provider registries)
public void saveWithId(ValueOutput output)
public void saveCustomOnly(ValueOutput output) / saveCustomOnly(HolderLookup.Provider)

public static @Nullable BlockEntity loadStatic(BlockPos pos, BlockState state, CompoundTag tag, HolderLookup.Provider registries)
public static @Nullable Component parseCustomNameSafe(ValueInput input, String name)
public void setChanged()
```

### API ValueOutput / ValueInput

```java
// ValueOutput:
<T> void store(String name, Codec<T> codec, T value);
<T> void storeNullable(String name, Codec<T> codec, @Nullable T value);
void putBoolean/putByte/putShort/putInt/putLong/putFloat/putDouble/putString/putIntArray(String name, ...);
ValueOutput child(String name);
ValueOutput.ValueOutputList childrenList(String name);
<T> ValueOutput.TypedOutputList<T> list(String name, Codec<T> codec);
void discard(String name);
boolean isEmpty();

// ValueInput:
<T> Optional<T> read(String name, Codec<T> codec);
Optional<ValueInput> child(String name);            ValueInput childOrEmpty(String name);
Optional<ValueInputList> childrenList(String name); ValueInputList childrenListOrEmpty(String name);
<T> Optional<TypedInputList<T>> list(String name, Codec<T> codec);  <T> TypedInputList<T> listOrEmpty(...);
boolean getBooleanOr(String name, boolean def);  int getIntOr(String name, int def);  Optional<Integer> getInt(String name);
long getLongOr / Optional<Long> getLong;  float getFloatOr;  double getDoubleOr;
Optional<String> getString(String name);  String getStringOr(String name, String def);  Optional<int[]> getIntArray(String name);
@Deprecated HolderLookup.Provider lookup();
```

Т.е. чтение — всегда с default'ом или Optional; сложные типы — через Codec (`ItemStack.CODEC`, `DataComponentMap.CODEC` и т.п.).

### Синхронизация на клиент — по-прежнему CompoundTag

```java
public @Nullable Packet<ClientGamePacketListener> getUpdatePacket()   // обычно ClientboundBlockEntityDataPacket.create(this)
public CompoundTag getUpdateTag(HolderLookup.Provider registries)     // default: new CompoundTag(); обычно saveWithoutMetadata(registries)

// ClientboundBlockEntityDataPacket:
public static ClientboundBlockEntityDataPacket create(BlockEntity blockEntity)
public static ClientboundBlockEntityDataPacket create(BlockEntity blockEntity, BiFunction<BlockEntity, RegistryAccess, CompoundTag> updateTagSaver)
```

### Дроп содержимого — теперь через preRemoveSideEffects

`onRemove` в блоке для дропа инвентаря больше не обязателен: BlockEntity сам делает это, если реализует `Container`:

```java
// BlockEntity:
public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    if (this instanceof Container container && this.level != null) {
        Containers.dropContents(this.level, pos, container);
    }
}

// Containers:
public static void dropContents(Level level, BlockPos pos, Container container)
public static void dropContents(Level level, Entity entity, Container container)
public static void dropContents(Level level, BlockPos pos, NonNullList<ItemStack> list)
public static void dropItemStack(Level level, double x, double y, double z, ItemStack itemStack)
```

### BaseEntityBlock

```java
public abstract class BaseEntityBlock extends Block implements EntityBlock {
    protected abstract MapCodec<? extends BaseEntityBlock> codec();
    @Override protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int b0, int b1);
    @Override protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos);
    protected static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
        BlockEntityType<A> actual, BlockEntityType<E> expected, @Nullable BlockEntityTicker<? super E> ticker);
}

// EntityBlock:
@Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState);
default <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type);
default <T extends BlockEntity> @Nullable GameEventListener getListener(ServerLevel level, T blockEntity);
```

Neo-бонусы у BlockEntity: `getPersistentData()` (CompoundTag NeoForgeData), data attachments (`setData`/`syncData`), `invalidateCapabilities()` вызывается в `setRemoved`/`clearRemoved`.

---

## 5. BlockState properties

```java
// IntegerProperty:
public static IntegerProperty create(String name, int min, int max)

// EnumProperty (T extends Enum<T> & StringRepresentable):
public static <T> EnumProperty<T> create(String name, Class<T> clazz)
public static <T> EnumProperty<T> create(String name, Class<T> clazz, Predicate<T> filter)
public static <T> EnumProperty<T> create(String name, Class<T> clazz, T... values)
public static <T> EnumProperty<T> create(String name, Class<T> clazz, List<T> values)
```

Готовые константы в `BlockStateProperties`:

```java
IntegerProperty AGE_1 / AGE_2 / AGE_3 / AGE_4 / AGE_5 / AGE_7 / AGE_15 / AGE_25   // "age", 0..N
IntegerProperty MOISTURE                                    // "moisture", 0..7
EnumProperty<DoubleBlockHalf> DOUBLE_BLOCK_HALF             // "half": upper/lower
EnumProperty<Half> HALF                                     // "half": top/bottom (плиты/лестницы)
// + константы MAX_AGE_1..MAX_AGE_25 (int)
```

Регистрация в блоке:

```java
@Override
protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(AGE, HALF, ...);
}
// В конструкторе: this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
```

---

## 6. Тикер BlockEntity

```java
@FunctionalInterface
public interface BlockEntityTicker<T extends BlockEntity> {
    void tick(final Level level, final BlockPos pos, final BlockState state, final T entity);
    default BlockEntityTicker<T> andThen(BlockEntityTicker<? super T> after);
}
```

Типовой паттерн в блоке:

```java
@Override
public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide()
        ? null
        : createTickerHelper(type, MyBlockEntities.MY_BE.get(), MyBlockEntity::serverTick);
    // serverTick: static void serverTick(Level level, BlockPos pos, BlockState state, MyBlockEntity be)
}
```

`createTickerHelper` — protected static в `BaseEntityBlock` (проверка `expected == actual` + небезопасный каст).

---

## Быстрые заметки/грабли

- `randomTick`/`getShape`/`canSurvive`/`updateShape`/`entityInside`/`useWithoutItem` в `BlockBehaviour` — `protected` (Pitcher переоткрыл часть как `public`, потому что переопределяет из DoublePlantBlock).
- `entityInside` получил новые параметры: `InsideBlockEffectApplier effectApplier, boolean isPrecise`.
- `updateShape` — новая сигнатура с `ScheduledTickAccess` и `RandomSource`.
- Рост культур на Neo всегда через `CommonHooks.canCropGrow` / `fireCropGrowPost` (события CropGrowEvent) — в своих культурах вызывать так же.
- Почва: не проверяй `instanceof FarmlandBlock` — используй теги `SUPPORTS_CROPS` / `GROWS_CROPS` / `MAINTAINS_FARMLAND` и Neo-хуки `canSustainPlant` / `isFertile` / `canBeHydrated`.
- `GameRules.MOB_GRIEFING` теперь читается как `level.getGameRules().get(GameRules.MOB_GRIEFING)`; сущности — `EntityTypes` (не EntityType) в свежем маппинге этого дампа.
