# Genie concept audit — full map

Read-only audit of every mechanic in `GENIE_CONCEPT.md` (77 KB) against the
actual code. No production code was written for this audit.

**Baseline, measured:** branch `gauntlet/full-production`, `compileJava` SUCCESS.
276 Java files / 30 614 LOC total. Genie subsystem 118 files / 9 261 LOC.
108 GameTest registrations, 11 unit-test files / 49 `@Test`.

**Method.** Verdicts come from reading implementations, counting lines, grepping
for callers, and inspecting asset JSON — never from comments, class names, or
existing docs. Doc claims were treated as hypotheses to disprove.

---

## 1. Three structural facts that decide most verdicts

**(a) There is no custom dimension.** No `dimension/` or `dimension_type/`
datapack directory exists anywhere; `src/main/resources/data/kubanhorizons/`
contains only `structure/`, and generated data contains only `worldgen/` biome
material. The only `ResourceKey<Level>` uses are reads of `level.dimension()`
(`GenieAnchor.java:50,76,103`). Therefore every "pocket world / lamp interior /
palace / mirror world / painting dimension" mechanic is at best an in-world
illusion today. The `genie/dimension/` package is named for dimensions but
registers none.

**(b) There is no genie client UI.** Exactly one `Screen` subclass exists in the
whole mod — `client/screen/OilPressScreen.java`, unrelated to the genie. Zero
`KeyMapping` registrations. Therefore the radial menu, dialogue screen,
appearance editor, HUD state, and master-choice GUI do not exist in any form.
Note `AGENTS.md` forbids client-only classes in common/server code, so this work
must live under `client/`.

**(c) The model has no legs, and the tail is not animated.**
`kuban_genie.geo.json` has **25 bones**, of which 7 are tail segments
(`tail1`–`tail7`), and **zero** leg/foot/shin/knee bones — the concept's "no legs
in true form" requirement is genuinely satisfied in geometry.
`kuban_genie.animation.json` defines **8** animations: `idle, move, greet, wish,
cast, spawn, despawn, hurt` — not the "dozens" the concept asks for.
`KubanGenieModel.java` is 29 lines and contains **zero** occurrences of "tail".
`GenieTailEngine` computes a `GenieTailState` server-side
(`GenieTailEngine.java:22-41`) that is **never synced** — no `EntityDataAccessor`,
no `SynchedEntityData`. So tail state cannot affect rendering at all.

---

## 2. Dead code: six engines with zero callers

Verified by grep across `src/main/java`, excluding each file's own definition:

| Class | LOC | Callers | Tests |
|---|---|---|---|
| `GenieMythSystem` | 18 | **0** | 0 |
| `GenieDreamEngine` | 15 | **0** | 0 |
| `LivingPaintingEngine` | 25 | **0** | 0 |
| `VisualReenactmentEngine` | 26 | **0** | 0 |
| `ItemMemoryReader` | 23 | **0** | 0 |
| `BlockWhispersEngine` | 33 | **0** | 0 |
| `NPCPersonalityEngine` | 33 | **0** | 0 |

Seven classes, 173 lines total, unreachable in any gameplay path. Two verified in
full: `GenieMythSystem.startGenieFestival` casts a particle signature and sends
one chat line; `GenieDreamEngine.enterDreamState` sends one chat line. These are
`Component.translatable` calls wearing epic names.

---

## 3. The wiring defect (highest value, lowest cost)

`PolicyService` (173 lines) already implements the concept's requirement for
**versioned, reversible** global rules: three policies (`MOB_GRIEFING`,
`WEATHER`, `OVERWORLD_CLOCK_RATE`) with preview → commit → rollback and durable
manifests (`PolicyService.java:20-22,141-165`).

It is referenced only at `WishRuntime.java:25,77,108` — constructed, never
invoked by any gameplay path.

The path players reach is `WishExecutor.java:27 → MetaRuleEngine.execute`:
- sets `MOB_GRIEFING` **directly, irreversibly** (`MetaRuleEngine.java:17-24`)
- runs `time set 18000` — one-shot, not a rule (`:26-37`)
- fakes instant-smelt with **particles + chat only** (`:38-41`)

Both a correct and an unsafe implementation exist; the unsafe one wins. This is a
wiring defect, not a missing feature.

---

## 4. Per-cluster verdicts

Legend: REAL = complete wired loop with meaningful test · PARTIAL = works but
narrow/hardcoded · FACADE = name exists, no loop (particles/chat only) ·
ABSENT = nothing · INFEASIBLE = not possible as literally written.

| Cluster | Items | REAL | PARTIAL | FACADE | ABSENT | INFEASIBLE |
|---|---|---|---|---|---|---|
| A Immortality & defense | 13 | 6 | 5 | 1 | 1 | 0 |
| B Wish language | 16 | 1 | 10 | 1 | 4 | 0 |
| C Rules & meta-magic | 15 | 0 | 4 | 3 | 7 | 1 |
| D Memory & causality | 13 | 0 | 3 | 4 | 4 | 2 |
| E Space & structures | 13 | 2 | 4 | 3 | 2 | 2 |
| F Dimensions & architecture | 15 | 0 | 2 | 4 | 7 | 2 |
| G Society & creatures | 17 | 0 | 3 | 3 | 9 | 2 |
| H Vessels & interface | 13 | 0 | 3 | 2 | 8 | 0 |
| I Player transformation | 13 | 1 | 5 | 2 | 5 | 0 |
| J Companion & visual | 25 | 2 | 6 | 6 | 11 | 0 |
| **Total** | **153** | **12** | **45** | **29** | **58** | **9** |

**8% of the concept is real. 19% is facade. 38% is absent.**

### The strongest asset
The **Safe Strong-Wish Runtime** is genuinely production-grade and is the one
thing worth building everything else on: closed capability registry, immutable
snapshots, preview → one-shot confirmation → transaction → journal → verify →
rollback, 24 h retained undo. Verified limits: 128 K blocks, 256 chunks, players
and Wishborne excluded, block entities + scheduled ticks + biome quart palettes
preserved. Rotation does not exist. 49 unit tests concentrate here.

### Engine size distribution (the facade tell)
`FlyingStructureEngine` 184 · `PlayerGenieTransformationController` 182 ·
`MiniaturizationEngine` 138 · `WorldGenieMemory` 245 — these are real work.
Against: `GenieDreamEngine` 15 · `VesselKind` 15 · `GenieMythSystem` 18 ·
`ItemMemoryReader` 23 · `LivingPaintingEngine` 25 ·
`VisualReenactmentEngine` 26 · `ConditionalWishEngine` 29 ·
`BlockWhispersEngine` 33 · `NPCPersonalityEngine` 33 ·
`HybridSpeciesEngine` 36 · `GenieRoleSwap` 36 · `GigantismScaleEngine` 40 ·
`CartoonAnatomyEngine` 44.

### Test-quality caveat
Some genie GameTests cannot fail for the reason they claim. Example,
`KHGameTests.java:579-586`: calls `tickTail` and `triggerFlatten` (both `void`),
then asserts only `genie.isAlive()`. It would pass if both methods were empty.

---

## 5. The nine infeasible-as-written items

Each needs a deliberate reframing decision, not silent approximation.

1. **Raise max world height** — build height is fixed per `DimensionType` at
   registration. → separate dimension with taller type, entered by portal.
2. **Non-euclidean corridors (5 outside / 500 inside)** — impossible in
   Minecraft's chunk/coordinate model. → teleport seams between disjoint regions.
3. **True spatial recursion (vessel inside its own palace)** — infinite regress.
   → per-instance copies with a hard depth cap.
4. **Rewriting causality / history** — no historical world state is stored.
   → journal-replay: re-apply recorded deltas, then dress the surroundings.
5. **Real alternate timelines ("А что если?")** — would need a full second world
   simulation per branch. → bounded snapshot instance, read-mostly, expiring.
6. **Genuinely new registered species at runtime** — registries freeze after
   load. → data-driven variants of existing entity types with inheritable traits.
7. **Conceptual bosses (Ошибка Реальности etc.)** — "exists only while known"
   has no engine analogue. → bespoke per-boss puzzle state machines.
8. **Her scale from a few pixels to hundreds of blocks** — collision, pathing and
   attribute scaling break at both extremes. → clamp to a tested band; treat the
   extremes as cutscene-only.
9. **Thought materialization** — cannot infer intent server-side from look
   direction. → explicit client-side intent signal.

---

## 6. Recommended slice order

Ranked by value ÷ cost, given the runtime is already solid.

1. **Route meta-rules through `PolicyService`; delete `MetaRuleEngine`'s direct
   game-rule writes.** Smallest change, removes an irreversible-world-edit path,
   activates 173 lines of finished code.
2. **Rule Engine (epic 3).** Replace `ConditionalWishEngine`'s 29 lines with real
   persistent event-condition-action rules: trigger registry, condition/action
   vocabulary, per-rule limits, save migration. Unlocks C1–C5 and is the
   backbone for delayed/conditional/chained wishes.
3. **Delete or implement the seven zero-caller classes.** Currently they violate
   `AGENTS.md`'s no-stubs rule and inflate apparent completeness.
4. **Client UI foundation.** One `Screen` + `KeyMapping` + payload plumbing;
   prerequisite for the radial menu, dialogue, and all HUD-dependent items.
5. **Tail state sync + GeckoLib controllers.** Sync `GenieTailState`, add
   controllers; converts a large slice of cluster J from facade to real.
6. **Pocket Space (epic 6).** Register the first real dimension. Large, and
   gates F1–F15, D6, H11.

Items 1–3 are cleanup that makes the codebase honest. 4–6 are the real features.
