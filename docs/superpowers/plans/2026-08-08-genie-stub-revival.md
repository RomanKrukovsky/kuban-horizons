# Genie Stub Revival Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace 18 facade "systems" in the `genie` package with real implementations that change world state, plus tests that fail when the implementation is removed.

**Architecture:** One shared `RegionSnapshot` module (built on vanilla `StructureTemplate`) is the single place the mod captures and restores regions of blocks. Five mechanics depend on it. Each subsequent group builds on that foundation and is committed separately.

**Tech Stack:** Minecraft 26.2, NeoForge 26.2.0.48-beta, Java 25 toolchain, Gradle 9.2.1 (ModDevGradle), registry-based GameTest.

## Global Constraints

- Build command: `JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home ./gradlew build`
- **In MC 26.2 `ResourceLocation` is named `Identifier`** (`net.minecraft.resources.Identifier`). Never import `ResourceLocation`.
- AD-004: code comments and Javadoc in **Russian**; git commit messages in **English**.
- AD-006: every serialized structure carries `SchemaVersion`.
- AD-005: all JSON (models, lang, loot) is produced by datagen, never hand-edited in `src/generated`.
- Registration uses `DeferredRegister` in `registry/KH*.java`, matching existing files.
- Server is the source of truth. No world mutation without a configured limit.
- Tests are registry-based: register in the `static {}` block of `KHGameTests` via `register(name, fn, maxTicks)`.
- **A test must fail if the implementation is deleted.** Assert world state, never a bare return value.

---

### Task 1: RegionSnapshot — capture and restore a region

**Files:**
- Create: `src/main/java/dev/romankrukovsky/kubanhorizons/genie/world/RegionSnapshot.java`
- Modify: `src/main/java/dev/romankrukovsky/kubanhorizons/config/KHServerConfig.java`
- Modify: `src/main/java/dev/romankrukovsky/kubanhorizons/gametest/KHGameTests.java`

**Interfaces:**
- Consumes: nothing (this is the foundation).
- Produces:
  - `RegionSnapshot.capture(ServerLevel level, BlockPos from, BlockPos to)` → `Optional<RegionSnapshot>`; empty when the volume exceeds the configured limit.
  - `RegionSnapshot.restore(ServerLevel level, BlockPos origin)` → `boolean`
  - `RegionSnapshot.clear(ServerLevel level, BlockPos origin)` → `void` (fills the captured volume with air)
  - `RegionSnapshot.size()` → `Vec3i`
  - `RegionSnapshot.toTag()` → `CompoundTag`
  - `RegionSnapshot.fromTag(ServerLevel level, CompoundTag tag)` → `RegionSnapshot`
  - `KHServerConfig.genieMaxRegionVolume()` → `int`

**Why this exists:** Pocket scenes, miniaturization, the reality theatre, gigantism and (later) wish rollback all need the same capability. Building it once keeps the write/rollback path in a single auditable place.

- [ ] **Step 1: Write the failing test**

Add this method to `KHGameTests`:

```java
    /** Снимок региона восстанавливает каждый блок после полной очистки. */
    private static void testRegionSnapshotRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos to = helper.absolutePos(new BlockPos(2, 3, 2));

        // Уникальный узор, который нельзя получить случайно.
        level.setBlock(from, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(from.offset(1, 0, 0), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        level.setBlock(from.offset(0, 1, 1), Blocks.EMERALD_BLOCK.defaultBlockState(), 3);

        var snapshot = dev.romankrukovsky.kubanhorizons.genie.world.RegionSnapshot
                .capture(level, from, to);
        helper.assertTrue(snapshot.isPresent(), "Снимок региона должен быть создан");

        // Стираем узор: если restore ничего не делает, тест обязан упасть.
        snapshot.get().clear(level, from);
        helper.assertTrue(level.getBlockState(from).isAir(),
                "После clear регион должен быть пустым");

        helper.assertTrue(snapshot.get().restore(level, from),
                "Восстановление должно сообщить об успехе");
        helper.assertTrue(level.getBlockState(from).is(Blocks.GOLD_BLOCK),
                "Золотой блок должен вернуться на место");
        helper.assertTrue(level.getBlockState(from.offset(1, 0, 0)).is(Blocks.DIAMOND_BLOCK),
                "Алмазный блок должен вернуться на место");
        helper.assertTrue(level.getBlockState(from.offset(0, 1, 1)).is(Blocks.EMERALD_BLOCK),
                "Изумрудный блок должен вернуться на место");
        helper.succeed();
    }

    /** Снимок отказывается захватывать регион больше лимита конфигурации. */
    private static void testRegionSnapshotRespectsLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = helper.absolutePos(new BlockPos(1, 2, 1));
        int limit = dev.romankrukovsky.kubanhorizons.config.KHServerConfig.genieMaxRegionVolume();
        // Сторона куба заведомо превышает лимит по объёму.
        int side = (int) Math.cbrt(limit) + 8;
        BlockPos tooBig = from.offset(side, side, side);

        helper.assertTrue(dev.romankrukovsky.kubanhorizons.genie.world.RegionSnapshot
                        .capture(level, from, tooBig).isEmpty(),
                "Регион больше лимита не должен захватываться");
        helper.succeed();
    }
```

Register both in the `static {}` block, after the last `player_genie_*` line:

```java
        register("region_snapshot_round_trip", KHGameTests::testRegionSnapshotRoundTrip, 200);
        register("region_snapshot_respects_limit", KHGameTests::testRegionSnapshotRespectsLimit, 100);
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home ./gradlew compileJava
```

Expected: FAIL — `package dev.romankrukovsky.kubanhorizons.genie.world does not exist` and `cannot find symbol: method genieMaxRegionVolume()`.

- [ ] **Step 3: Add the config limit**

In `KHServerConfig.java`, add a field next to the other sections:

```java
    // --- Джинния: изменения мира ---

    private static final ModConfigSpec.IntValue GENIE_MAX_REGION_VOLUME = BUILDER
            .comment("Maximum number of blocks the Genie may capture or move in one operation.",
                    "Максимальное число блоков, которое джинния может захватить или перенести за одну операцию.")
            .defineInRange("genie.maxRegionVolume", 32768, 64, 1048576);
```

And the getter, following the style of the existing getters in the file:

```java
    /** Лимит объёма региона для операций джиннии (Закон сохранности). */
    public static int genieMaxRegionVolume() {
        return GENIE_MAX_REGION_VOLUME.get();
    }
```

- [ ] **Step 4: Implement RegionSnapshot**

```java
package dev.romankrukovsky.kubanhorizons.genie.world;

import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Снимок региона блоков: единственная точка мода, где область мира
 * сохраняется и возвращается назад.
 *
 * <p>Используется карманными сценами, сжатием мира, театром прошлого и
 * гигантизмом. Объём ограничен конфигурацией — это Закон сохранности из
 * {@code GENIE_VISION.md}: ни одна операция не может незаметно повесить
 * сервер.</p>
 */
public final class RegionSnapshot {
    /** Версия схемы сериализации (AD-006). */
    public static final int SCHEMA_VERSION = 1;

    private static final String TAG_TEMPLATE = "Template";
    private static final String TAG_SCHEMA = "SchemaVersion";

    private final StructureTemplate template;

    private RegionSnapshot(StructureTemplate template) {
        this.template = template;
    }

    /**
     * Захватывает регион между двумя углами включительно.
     *
     * @return пустое значение, если объём превышает лимит конфигурации
     */
    public static Optional<RegionSnapshot> capture(ServerLevel level, BlockPos from, BlockPos to) {
        BlockPos min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()));
        BlockPos max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ()));

        Vec3i size = new Vec3i(
                max.getX() - min.getX() + 1,
                max.getY() - min.getY() + 1,
                max.getZ() - min.getZ() + 1);

        long volume = (long) size.getX() * size.getY() * size.getZ();
        if (volume > KHServerConfig.genieMaxRegionVolume()) {
            return Optional.empty();
        }

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, true, List.of());
        return Optional.of(new RegionSnapshot(template));
    }

    /** Возвращает блоки на место относительно указанного начала региона. */
    public boolean restore(ServerLevel level, BlockPos origin) {
        return template.placeInWorld(level, origin, origin,
                new StructurePlaceSettings(), level.getRandom(), 3);
    }

    /** Заполняет захваченный объём воздухом. */
    public void clear(ServerLevel level, BlockPos origin) {
        Vec3i size = template.getSize();
        for (BlockPos pos : BlockPos.betweenClosed(origin,
                origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Размер захваченного региона в блоках. */
    public Vec3i size() {
        return template.getSize();
    }

    /** Сериализация для хранения внутри предмета или SavedData. */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_SCHEMA, SCHEMA_VERSION);
        tag.put(TAG_TEMPLATE, template.save(new CompoundTag()));
        return tag;
    }

    /** Десериализация ранее сохранённого снимка. */
    public static RegionSnapshot fromTag(ServerLevel level, CompoundTag tag) {
        StructureTemplate template = new StructureTemplate();
        template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK),
                tag.getCompoundOrEmpty(TAG_TEMPLATE));
        return new RegionSnapshot(template);
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home ./gradlew build
```

Expected: BUILD SUCCESSFUL; `region_snapshot_round_trip` and `region_snapshot_respects_limit` pass.

If `getCompoundOrEmpty` does not resolve, check the accessor name in
`/tmp/mcsrc/net/minecraft/nbt/CompoundTag.java` and use the 26.2 equivalent.

- [ ] **Step 6: Prove the test is honest**

Temporarily replace the body of `restore` with `return true;`, rebuild, and confirm `region_snapshot_round_trip` **fails**. Then restore the real body.

This step is the entire point of the task: it is what the previous attempt skipped.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/romankrukovsky/kubanhorizons/genie/world/RegionSnapshot.java \
        src/main/java/dev/romankrukovsky/kubanhorizons/config/KHServerConfig.java \
        src/main/java/dev/romankrukovsky/kubanhorizons/gametest/KHGameTests.java
git commit -m "feat(genie): add region snapshot capture and restore

Single place where the mod captures and restores a region of blocks,
built on vanilla StructureTemplate and bounded by a server config limit.
Five wish mechanics depend on it."
```

---

### Task 2: Region payload component — store a region inside an item

**Files:**
- Create: `src/main/java/dev/romankrukovsky/kubanhorizons/registry/KHDataComponents.java`
- Modify: `src/main/java/dev/romankrukovsky/kubanhorizons/KubanHorizons.java`
- Modify: `src/main/java/dev/romankrukovsky/kubanhorizons/gametest/KHGameTests.java`

**Interfaces:**
- Consumes: `RegionSnapshot.toTag()` / `RegionSnapshot.fromTag(ServerLevel, CompoundTag)` from Task 1.
- Produces: `KHDataComponents.REGION_PAYLOAD` — a `DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>>` used with `ItemStack.set(...)` / `ItemStack.get(...)`. Task 3 (miniaturization) consumes it.

- [ ] **Step 1: Write the failing test**

Add to `KHGameTests`:

```java
    /** Предмет хранит захваченный регион и отдаёт его назад без потерь. */
    private static void testRegionPayloadRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(from, Blocks.GOLD_BLOCK.defaultBlockState(), 3);

        var snapshot = dev.romankrukovsky.kubanhorizons.genie.world.RegionSnapshot
                .capture(level, from, from);
        helper.assertTrue(snapshot.isPresent(), "Снимок должен быть создан");

        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(dev.romankrukovsky.kubanhorizons.registry.KHDataComponents.REGION_PAYLOAD.get(),
                snapshot.get().toTag());

        CompoundTag stored = stack.get(
                dev.romankrukovsky.kubanhorizons.registry.KHDataComponents.REGION_PAYLOAD.get());
        helper.assertTrue(stored != null, "Компонент региона должен читаться из предмета");

        // Стираем мир и восстанавливаем строго из предмета.
        level.setBlock(from, Blocks.AIR.defaultBlockState(), 3);
        dev.romankrukovsky.kubanhorizons.genie.world.RegionSnapshot
                .fromTag(level, stored)
                .restore(level, from);
        helper.assertTrue(level.getBlockState(from).is(Blocks.GOLD_BLOCK),
                "Регион должен восстановиться из данных предмета");
        helper.succeed();
    }
```

Register it:

```java
        register("region_payload_round_trip", KHGameTests::testRegionPayloadRoundTrip, 200);
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home ./gradlew compileJava
```

Expected: FAIL — `cannot find symbol: class KHDataComponents`.

- [ ] **Step 3: Create the component registry**

```java
package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация data components мода.
 */
public final class KHDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, KubanHorizons.MOD_ID);

    /**
     * Захваченный регион мира внутри предмета (сжатие мира джиннией).
     * Хранит результат {@code RegionSnapshot.toTag()}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> REGION_PAYLOAD =
            COMPONENTS.register("region_payload",
                    () -> DataComponentType.<CompoundTag>builder()
                            .persistent(CompoundTag.CODEC)
                            .build());

    private KHDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
```

- [ ] **Step 4: Wire registration into the mod entrypoint**

In `KubanHorizons.java`, next to the other `register(modEventBus)` calls (e.g. `KHItems.register(...)`), add:

```java
        KHDataComponents.register(modEventBus);
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home ./gradlew build
```

Expected: BUILD SUCCESSFUL; `region_payload_round_trip` passes.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/romankrukovsky/kubanhorizons/registry/KHDataComponents.java \
        src/main/java/dev/romankrukovsky/kubanhorizons/KubanHorizons.java \
        src/main/java/dev/romankrukovsky/kubanhorizons/gametest/KHGameTests.java
git commit -m "feat(genie): add region payload item component

Lets a single item carry a captured region of the world, which is what
turns miniaturization from a stub into a real mechanic."
```

---

### Task 3: Miniaturization — region becomes an item, and comes back

**Files:**
- Modify: `src/main/java/dev/romankrukovsky/kubanhorizons/genie/spatial/MiniaturizationEngine.java`
- Modify: `src/main/java/dev/romankrukovsky/kubanhorizons/gametest/KHGameTests.java`

**Interfaces:**
- Consumes: `RegionSnapshot` (Task 1), `KHDataComponents.REGION_PAYLOAD` (Task 2), existing `KHItems.MINIATURE_WORLD`, existing `WorldGenieMemory.recordEvent(BlockPos, String, String, long)`.
- Produces:
  - `MiniaturizationEngine.compressRegion(ServerLevel, BlockPos, int, Player)` → `ItemStack` (`ItemStack.EMPTY` only on genuine refusal, e.g. over the limit)
  - `MiniaturizationEngine.uncompressRegion(ServerLevel, BlockPos, ItemStack)` → `boolean`

**Current state:** both methods are stubs returning `ItemStack.EMPTY` / `false`.

- [ ] **Step 1: Write the failing test**

Add to `KHGameTests`:

```java
    /** Сжатие региона убирает блоки из мира и возвращает их из предмета. */
    private static void testMiniaturizationCompressAndRestore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos center = helper.absolutePos(new BlockPos(2, 2, 2));

        level.setBlock(center, Blocks.GOLD_BLOCK.defaultBlockState(), 3);

        ItemStack compressed = dev.romankrukovsky.kubanhorizons.genie.spatial.MiniaturizationEngine
                .compressRegion(level, center, 1, player);

        helper.assertTrue(!compressed.isEmpty(),
                "Сжатие должно вернуть предмет с регионом");
        helper.assertTrue(compressed.get(
                        dev.romankrukovsky.kubanhorizons.registry.KHDataComponents.REGION_PAYLOAD.get()) != null,
                "Предмет должен содержать компонент региона");
        helper.assertTrue(level.getBlockState(center).isAir(),
                "После сжатия блоки должны исчезнуть из мира");

        BlockPos target = helper.absolutePos(new BlockPos(6, 2, 6));
        helper.assertTrue(dev.romankrukovsky.kubanhorizons.genie.spatial.MiniaturizationEngine
                        .uncompressRegion(level, target, compressed),
                "Разворот региона должен сообщить об успехе");
        helper.assertTrue(level.getBlockState(target).is(Blocks.GOLD_BLOCK),
                "Золотой блок должен появиться в новом месте");
        helper.succeed();
    }
```

Register it:

```java
        register("genie_miniaturization_round_trip", KHGameTests::testMiniaturizationCompressAndRestore, 200);
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home ./gradlew build
```

Expected: FAIL — "Сжатие должно вернуть предмет с регионом", because the stub returns `ItemStack.EMPTY`.

- [ ] **Step 3: Replace the stub with a real implementation**

Full replacement file:

```java
package dev.romankrukovsky.kubanhorizons.genie.spatial;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import dev.romankrukovsky.kubanhorizons.genie.world.RegionSnapshot;
import dev.romankrukovsky.kubanhorizons.registry.KHDataComponents;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Сжатие области мира в предмет и обратное развёртывание.
 *
 * <p>Регион физически покидает мир: блоки заменяются воздухом, а их
 * состояние живёт внутри предмета. Развернуть можно в любом другом месте.</p>
 */
public final class MiniaturizationEngine {
    private MiniaturizationEngine() {
    }

    /**
     * Сжимает куб с центром {@code origin} и радиусом {@code radius}.
     *
     * @return предмет с регионом внутри либо {@link ItemStack#EMPTY},
     *         если объём превышает лимит конфигурации
     */
    public static ItemStack compressRegion(ServerLevel level, BlockPos origin, int radius, Player player) {
        BlockPos from = origin.offset(-radius, -radius, -radius);
        BlockPos to = origin.offset(radius, radius, radius);

        Optional<RegionSnapshot> snapshot = RegionSnapshot.capture(level, from, to);
        if (snapshot.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("message.kubanhorizons.genie.region_too_large"));
            return ItemStack.EMPTY;
        }

        // Регион покидает мир — именно это делает механику настоящей.
        snapshot.get().clear(level, from);

        ItemStack result = new ItemStack(KHItems.MINIATURE_WORLD.get());
        result.set(KHDataComponents.REGION_PAYLOAD.get(), snapshot.get().toTag());

        MagicalSignature.cast(level, Vec3.atCenterOf(origin));
        WorldGenieMemory.get(level).recordEvent(origin, "region_compressed",
                "message.kubanhorizons.genie.memory.region_compressed", level.getGameTime());
        return result;
    }

    /** Развёртывает сохранённый регион так, чтобы {@code target} стал его углом. */
    public static boolean uncompressRegion(ServerLevel level, BlockPos target, ItemStack stack) {
        CompoundTag payload = stack.get(KHDataComponents.REGION_PAYLOAD.get());
        if (payload == null) {
            return false;
        }

        boolean placed = RegionSnapshot.fromTag(level, payload).restore(level, target);
        if (placed) {
            MagicalSignature.cast(level, Vec3.atCenterOf(target));
            WorldGenieMemory.get(level).recordEvent(target, "region_restored",
                    "message.kubanhorizons.genie.memory.region_restored", level.getGameTime());
        }
        return placed;
    }
}
```

- [ ] **Step 4: Add the three translation keys**

In `datagen/KHTranslations.java`, next to the other `genie` entries, add both languages:

```java
        add("message.kubanhorizons.genie.region_too_large",
                "This region is too large even for me.",
                "Этот участок слишком велик даже для меня.");
        add("message.kubanhorizons.genie.memory.region_compressed",
                "A region of the world was folded into an item here.",
                "Здесь участок мира был свёрнут в предмет.");
        add("message.kubanhorizons.genie.memory.region_restored",
                "A folded region was unfolded here.",
                "Здесь свёрнутый участок был развёрнут.");
```

Match the exact helper name used by the surrounding lines in that file — if it is not `add(...)`, use whatever the neighbouring genie entries use.

- [ ] **Step 5: Regenerate data and run tests**

```bash
JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home ./gradlew runData
JAVA_HOME=~/jdks/jdk-21.0.12+8/Contents/Home ./gradlew build
```

Expected: BUILD SUCCESSFUL; `genie_miniaturization_round_trip` passes.

- [ ] **Step 6: Prove the test is honest**

Temporarily make `compressRegion` `return ItemStack.EMPTY;` again, rebuild, and confirm the test **fails**. Restore the real body.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/romankrukovsky/kubanhorizons/genie/spatial/MiniaturizationEngine.java \
        src/main/java/dev/romankrukovsky/kubanhorizons/datagen/KHTranslations.java \
        src/main/java/dev/romankrukovsky/kubanhorizons/gametest/KHGameTests.java \
        src/generated
git commit -m "feat(genie): make world miniaturization real

compressRegion now removes the blocks from the world and stores their
state in the item; uncompressRegion rebuilds them anywhere. Replaces a
stub that returned ItemStack.EMPTY."
```

---

### Remaining tasks

Tasks 1–3 establish the foundation and prove the pattern end to end: **write a
world-state test → watch it fail → implement → watch it pass → verify the test
fails without the implementation → commit.**

The remaining 15 facades and 2 partial systems follow the same five-step shape.
They are listed here in dependency order with their acceptance criterion; each
will be expanded into full step-by-step form (as in Tasks 1–3) before it is
executed, so that no task is implemented from a one-line description.

**Group 2 — real world changes (depends on Task 1)**

| Task | Mechanic | Acceptance criterion (asserted world state) |
|---|---|---|
| 4 | `PocketSceneEngine` | during the scene the blocks exist; after the timer every block equals the pre-scene snapshot |
| 5 | `WordMaterializer` | "золото" places gold blocks in a 5×5 letter shape; "дождь" starts real rain |
| 6 | `MagicDrawingHandler` | a polyline of player-marked points becomes a connected structure between them |
| 7 | `GigantismScaleEngine` | the spawned entity's scale attribute and bounding box are actually larger |
| 8 | `FlyingStructureEngine` | the structure's blocks move to new coordinates and stay mutually connected |

**Group 3 — memory and character**

| Task | Mechanic | Acceptance criterion |
|---|---|---|
| 9 | `ItemMemoryReader` | reported history matches the item's real components and `WorldGenieMemory` records |
| 10 | `BlockWhispersEngine` | a bell/portal/ancient block returns the event actually recorded at that position |
| 11 | `NPCPersonalityEngine` | the target's attribute values change and survive a save/load cycle |
| 12 | `WishContractEngine` | the written book contains a condition, a deadline and a loophole clause |
| 13 | `GenieDreamEngine` | sleeping produces a dream scene referencing a real recorded world event |

**Group 4 — living world**

| Task | Mechanic | Acceptance criterion |
|---|---|---|
| 14 | `HybridSpeciesEngine` | the hybrid actually flies and glows; offspring inherit the traits |
| 15 | `VisualReenactmentEngine` | ghost NPCs replay events stored for that location, not a fixed script |
| 16 | `GenieMythSystem` | rumours distort recorded facts over time; the festival changes village state |
| 17 | `OwnerDeathProtocol` | all four concept options produce four distinct, observable outcomes |

**Group 5 — hard (highest risk)**

| Task | Mechanic | Acceptance criterion |
|---|---|---|
| 18 | `LivingPaintingEngine` | the player ends up in a different `ServerLevel` and can return |
| 19 | `BiomeRewriterEngine` | `level.getBiome(pos)` returns the requested biome afterwards |
| 20 | `GenieRoleSwap` + `TrueOmnipotenceEnding` | player and genie exchange vessel state; the ending clears the HUD |

**Group 6 — transformation scene**

| Task | Mechanic | Acceptance criterion |
|---|---|---|
| 21 | `DistortedWishEngine` | stages advance across ticks, not in one; the current stage survives a rejoin |

**Task 22 — documentation truth pass.** Update `PROJECT_STATE.md` (`[x]` only
for what is proven), `GENIE_VISION.md` (works / partial / not started matrix),
`TEST_PLAN.md` (record the "test must fail without implementation" rule) and
`CHANGELOG.md`. Explicitly leave non-Euclidean geometry, alternate timelines
and the proposal/risk/confirmation pipeline marked **not done**.

**Task 23 — facade guard test.** A test that scans the `genie` package and
fails if a method returns a success value without touching world state,
so facades cannot silently return.

---

## Risk note

Tasks 18 and 19 are the two the spec flags as genuinely hard. `LevelStem`,
`FlatLevelSource` and cross-level `teleportTo` have been verified to exist in
26.2, so the approach is sound — but if a real dimension cannot be made to
work in this pass, the honest outcome is to **report it and leave the mechanic
marked not done**, never to restore a facade that returns `true`.
