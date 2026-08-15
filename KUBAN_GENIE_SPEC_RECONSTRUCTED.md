# KUBAN_GENIE_SPEC.md — Полная техническая спецификация модели Кубанской джиннии

**Версия:** 2.0 (реконструированная из GENIE_VISION.md, GENIE_CONCEPT.md и предыдущих итераций)  
**Цель:** Blockbench + Geckolib 4.x (Fabric/NeoForge)  
**Размер текстуры:** 128×128 px, PNG, без сглаживания  
**Все размеры в Minecraft-пикселях (1 блок = 16 px)**  
**Оси:** X вправо, Y вверх, Z вперёд. Начало координат — центр между «ступнями» (Y=0 = уровень парения).

---

## 1. Габариты и физика

| Параметр | Значение |
|----------|----------|
| Высота модели (макушка → кончик хвоста) | 53 px по вертикали (~45 px после изгиба) |
| Верх головы (`head`) | Y = 38 |
| Верх модели (`hair_cap`, `tiara_gem_c`) | Y = 39 |
| Кончик хвоста (покой) | Y = -14 |
| Hitbox | 0.8 × 2.4 |
| Eye height | 2.05 |
| Смещение модели | кончик хвоста висит ~0.3 блока над землёй |
| `noGravity` | true |
| Навигация | `FlyingPathNavigation` |

**Лор-обоснование (из GENIE_CONCEPT.md):**  
Тело джиннии — **магический аватар**, а не биологическое существо. Ноги в истинной форме **физически отсутствуют**. Хвост — это **истинная форма** (anchor point). Верхняя часть тела — проекция воли и дыма. При входе в лампу тело втягивается именно через хвост.

---

## 2. Иерархия костей (полная)

```
root
└── body                      ← общая волна парения (idle bob)
    ├── torso
    │   ├── head              ← вращается за игроком (ваниль)
    │   │   ├── hair_cap
    │   │   ├── tiara
    │   │   │   ├── tiara_gem_c (emissive)
    │   │   │   ├── tiara_gem_l (emissive)
    │   │   │   └── tiara_gem_r (emissive)
    │   │   ├── glasses
    │   │   ├── earring_l     ← своя кость, качание
    │   │   └── earring_r     ← своя кость, качание
    │   ├── hair_back         ← ВАЖНО: родитель = torso, НЕ head
    │   │   └── hair_tips     ← затухающее качание (60% амплитуды)
    │   ├── bodice
    │   ├── necklace
    │   ├── arm_l
    │   │   ├── armband_l
    │   │   └── bangles_l
    │   └── arm_r
    │       ├── armband_r
    │       ├── sunflower_r   ← АСИММЕТРИЯ (только справа)
    │       └── bangles_r
    └── hips
        ├── belt
        │   ├── belt_gem_c
        │   ├── belt_gem_l
        │   ├── belt_gem_r
        │   ├── pendant_l     ← своя кость + затухающее качание
        │   ├── pendant_c
        │   ├── pendant_r
        │   └── rushnyk
        │       └── rushnyk_tip
        └── tail1
            └── tail2
                └── tail3
                    └── tail4
                        └── tail5
                            └── tail6 (dither 25%)
                                └── tail7 (dither 50%, emissive)
```

**Ключевое решение:** `hair_back` висит на `torso`, а не на `head`. Иначе при повороте головы задняя масса волос (глубина ~8 px) прошьёт спину насквозь. Стык маскируется золотой лентой на затылке.

---

## 3. Кубоиды — точные размеры и позиции

### 3.1 Голова и украшения

| Кость | Размер (W×H×D) | Позиция (X, Y, Z) | Пивот | Примечание |
|-------|----------------|-------------------|-------|------------|
| head | 8×8×8 | -4, 30, -4 | 0, 30, 0 | база |
| hair_cap | 10×9×10 | -5, 30, -5 | 0, 30, 0 | inflate 0 |
| tiara | 9×2×1 | -4.5, 36, -5.5 | 0, 30, 0 | emissive |
| tiara_gem_c | 2×2×1 | -1, 37, -6 | — | emissive (glowmask) |
| tiara_gem_l | 1×1×1 | 3, 36.5, -6 | — | emissive |
| tiara_gem_r | 1×1×1 | -4, 36.5, -6 | — | emissive |
| glasses | 8×3×1 | -4, 33, -4.6 | — | alpha, вынос 0.6 px |
| earring_l | 1×3×1 | 4, 31, -1 | 4.5, 33, -1 | своя кость, качание |
| earring_r | 1×3×1 | -5, 31, -1 | -4.5, 33, -1 | своя кость, качание |

### 3.2 Волосы (задняя масса)

| Кость | Размер | Позиция | Пивот |
|-------|--------|---------|-------|
| hair_back | 10×20×8 | -5, 12, 1 | 0, 31, 2 |
| hair_tips | 8×8×6 | -4, 5, 2 | 0, 13, 2 |

Затухающее качание: `hair_tips` получает **60% амплитуды** `hair_back` со сдвигом фазы **4 тика**.

### 3.3 Торс и руки

| Кость | Размер | Позиция | Пивот | Прим. |
|-------|--------|---------|-------|-------|
| torso | 8×12×4 | -4, 18, -2 | 0, 30, 0 | |
| bodice | 9×5×5 | -4.5, 24, -2.5 | 0, 30, 0 | inflate 0.5 |
| necklace | 6×3×1 | -3, 28, -2.6 | — | коралловые бусы |
| arm_l | 4×12×4 | 4, 18, -2 | 5, 29, 0 | |
| arm_r | 4×12×4 | -8, 18, -2 | -5, 29, 0 | |
| armband_l/r | 5×4×5 | ±4/-8.5, 24, -2.5 | — | inflate 0.5, вышивка |
| sunflower_r | 3×3×2 | -9, 27, -1 | — | **асимметрия** (только справа) |
| bangles_l/r | 5×3×5 | ±4/-8.5, 19, -2.5 | — | inflate 0.5 |

**Кисти** — сплошной кубоид (без прорези между пальцами). При 4×4 px прорезь = 1 пиксель и читается как дыра.

### 3.4 Бёдра и пояс

| Кость | Размер | Позиция | Пивот |
|-------|--------|---------|-------|
| hips | 8×5×4 | -4, 13, -2 | 0, 18, 0 |
| belt | 9×4×5 | -4.5, 14, -2.5 | 0, 18, 0 |
| belt_gem_c | 2×2×1 | -1, 15, -3 | — |
| belt_gem_l | 2×2×1 | 2, 15, -3 | — |
| belt_gem_r | 2×2×1 | -4, 15, -3 | — |
| pendant_c | 1×4×1 | -0.5, 10, -3 | 0, 14, -3 |
| pendant_l | 1×3×1 | 3, 11, -3 | 3.5, 14, -3 |
| pendant_r | 1×3×1 | -4, 11, -3 | -3.5, 14, -3 |
| rushnyk | 3×10×1 | 3, 4, -2.6 | 4, 14, -2.6 |
| rushnyk_tip | 3×5×1 | 3, -1, -2.6 | 4, 4, -2.6 |

Подвески и рушник — на **собственных костях** с затухающим качанием. Со спины подвески **не моделируются** (только в текстуре).

### 3.5 Хвост — 7 сегментов (критически важно)

**Правило:** Каждый сегмент заходит в родительский на **1 px по высоте** — иначе при повороте в анимации между ними откроются щели.

| Кость | Размер (W×H×D) | Позиция (X, Y, Z) | Пивот (X, Y, Z) | Цвет | Примечание |
|-------|----------------|-------------------|-----------------|------|------------|
| tail1 | 8×6×8 | -4, 8, -4 | 0, 13, 0 | `#2A3FA8` | base |
| tail2 | 7×6×7 | -3.5, 3, -3.5 | 0, 9, 0 | `#3550C4` | |
| tail3 | 6×5×6 | -3, -1, -3 | 0, 4, 0 | `#4A63D8` | |
| tail4 | 5×5×5 | -2.5, -5, -2.5 | **0, 0, 0** | `#5B58D4` | **законный пивот 0,0,0** |
| tail5 | 4×4×4 | -2, -8, -2 | 0, -4, 0 | `#6B5FD0` | |
| tail6 | 4×4×4 | -2, -11, -2 | 0, -7, 0 | `#8B5CF6` | **dither 25%** |
| tail7 | 3×4×3 | -1.5, -14, -1.5 | 0, -10, 0 | `#A87CF0` | **dither 50% + emissive** |

**Поза покоя (S-образная кривая) — точные углы:**

| Кость | rotX (наклон по X) | Примечание |
|-------|--------------------|------------|
| tail1 | +8° | начало подъёма |
| tail2 | +14° | максимальный подъём |
| tail3 | +12° | начало спуска |
| tail4 | **-6°** | **перегиб** (смена знака) |
| tail5 | -18° | |
| tail6 | -28° | |
| tail7 | -35° | крючок на конце |

**Смена знака между tail3 и tail4** даёт характерный перегиб S-кривой. Нарастание угла к концу — «крючок».

**Рендер хвоста:**
- Слой: `cutout_no_cull` (НЕ `translucent` — сортировка в Minecraft некорректна).
- tail6: 25% прозрачных пикселей (шахматка).
- tail7: 50% прозрачных пикселей (шахматка).
- tail6 + tail7: emissive (glowmask).

---

## 4. Текстура 128×128 — точная раскладка UV

**Размер:** 128×128, PNG, **без сглаживания**. На 64×64 вышивка превратится в кашу.

### 4.1 Зональная раскладка

| Область | Координаты (X, Y) | Содержимое | Примечание |
|---------|-------------------|------------|------------|
| Голова + украшения | 0,0 – 64,32 | head, hair_cap, tiara, glasses, earrings | |
| Торс + лиф | 0,32 – 48,64 | torso, bodice, necklace | |
| Правая рука (асимметрия) | 48,32 – 96,64 | arm_r + sunflower_r | **отдельная зона** |
| Левая рука | 0,64 – 48,96 | arm_l (можно зеркалить) | |
| Бёдра + пояс + рушник | 48,64 – 96,96 | hips, belt, pendants, rushnyk | |
| Задняя масса волос | 96,0 – 128,64 | hair_back + hair_tips | |
| 7 сегментов хвоста | 0,96 – 96,128 | tail1–tail7 | **основная зона** |
| Глечик (отдельная сущность) | 96,96 – 128,128 | jug | |

### 4.2 Палитра (максимум 20 цветов)

```
кожа          #C97F3C  #B06B2E  #8F5320 (тень)
волосы        #141414  #2A2A2A  #3D3D3D (пряди)
белое полотно #F2EDE4  #D6CFC2 (тень)
вышивка крас. #B01F24  #7E1418
вышивка чёрн. #1A1A1A
золото        #E8B33A  #C4902A  #8F6418
сапфир        #1E4FD9  #4A7BF5 (блик)
пояс бордо    #6E1B22
терракота     #B5561F  #8A3F16
дым хвоста    #2A3FA8 → #A87CF0 (градиент по сегментам)
```

**Красный обязательно с чёрным контуром** — чистый красный по белому на 16 px размывается в розовое.

### 4.3 Emissive glowmask (отдельный слой)

Светятся:
- Все 3 камня тиары (`tiara_gem_*`)
- 3 камня пояса (`belt_gem_*`)
- 3 подвески (`pendant_*`)
- 2 серьги (`earring_*`)
- `tail6` и `tail7` (частично)

---

## 5. Анимации (Geckolib JSON)

### 5.1 Список анимаций

| Имя | Длительность | Loop | Содержимое |
|-----|--------------|------|------------|
| `idle` | 60 тиков | да | `body` ±1 px по Y; волна по хвосту (фазовый сдвиг 3 тика на сегмент, амплитуда 4°); волосы и подвески качаются с затуханием |
| `move` | 40 тиков | да | то же, амплитуда волны ×2, лёгкий наклон `body` вперёд на 5° |
| `greet` | 30 тиков | нет | правая рука к груди, поклон головы 12° (казачий жест) |
| `wish` | 45 тиков | нет | обе руки вперёд-вверх, ладони раскрыты, вспышка emissive ×2, хвост подтягивается |
| `cast` | 50 тиков | нет | левая рука вверх, хвост закручивается в тугую спираль (все сегменты +25°), рушник разворачивается |
| `spawn` | 35 тиков | нет | хвост вытягивается из глечика, сегменты появляются снизу вверх с масштабом 0→1, партиклы `portal` |
| `despawn` | 30 тиков | нет | обратный порядок, схлопывание в глечик |
| `hurt` | 10 тиков | нет | тряска `body`, красный overlay |

### 5.2 Формула волны хвоста (в animation JSON)

```
rot_x[i] = base[i] + amplitude * sin(time * speed - i * phase_offset)
phase_offset = 0.45 рад
speed = 0.08 (idle) / 0.16 (move)
```

---

## 6. Java Entity + Renderer (Geckolib 4.x)

### 6.1 Entity

```java
public class KubanGenieEntity extends PathfinderMob implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public KubanGenieEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true; // летает
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        if (state.isMoving()) {
            state.setAnimation(RawAnimation.begin().thenLoop("move"));
        } else {
            state.setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
```

### 6.2 Renderer

```java
public class KubanGenieRenderer extends GeoEntityRenderer<KubanGenieEntity> {
    public KubanGenieRenderer(EntityRendererProvider.Context context) {
        super(context, new KubanGenieModel());
        this.shadowRadius = 0.6f;
    }

    @Override
    public RenderType getRenderType(KubanGenieEntity animatable, ResourceLocation texture,
                                    PoseStack poseStack, @Nullable MultiBufferSource bufferSource,
                                    float partialTick, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha) {
        return RenderType.entityCutoutNoCull(texture); // важно для dithering хвоста
    }
}
```

### 6.3 Model (Geckolib)

```java
public class KubanGenieModel extends GeoModel<KubanGenieEntity> {
    @Override
    public ResourceLocation getModelResource(KubanGenieEntity animatable) {
        return new ResourceLocation("kubangeniemod", "geo/kuban_genie.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KubanGenieEntity animatable) {
        return new ResourceLocation("kubangeniemod", "textures/entity/kuban_genie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KubanGenieEntity animatable) {
        return new ResourceLocation("kubangeniemod", "animations/kuban_genie.animation.json");
    }
}
```

---

## 7. Глечик (отдельная сущность)

Одна кость `jug`, пивот `0,0,0`. Y=0 — дно. Общая высота 12 px.

| Кубоид | Размер | Позиция (X, Y, Z) | Прим. |
|--------|--------|-------------------|-------|
| jug_body | 8×8×8 | -4, 0, -4 | терракота, Y 0–8 |
| jug_belly | 10×5×10 | -5, 2, -5 | широкая часть, Y 2–7 |
| jug_neck | 5×3×5 | -2.5, 8, -2.5 | Y 8–11 |
| jug_rim | 6×1×6 | -3, 11, -3 | Y 11–12 |
| jug_inside | 4×1×4 | -2, 10.5, -2 | тёмный `#1A1008` |
| jug_handle | 1×5×2 | 4, 4, -1 | сбоку, Y 4–9 |

Красно-чёрная полоса-орнамент на брюшке. Медленное вращение вокруг Y (0.5°/тик). При `spawn` джиннии — партиклы из горла.

---

## 8. Открытые вопросы дизайна (из исходного SPEC)

Решенные в этой версии:
- [x] **Асимметрия подсолнуха** — только на правой руке (текстура рисуется отдельно).
- [x] **Основание хвоста** — tail1 8 px, пояс перекрывает стык.
- [x] **Талия** — расширена на 1–2 px для предотвращения щелей.
- [x] **Рушник** — удлинён до 10 px (значения в §3 увеличены).
- [x] **Задняя масса волос** — добавлены 2–3 вертикальные пряди на 1 тон светлее (`#2A2A2A`).
- [x] **Вырез лифа** — сплошная линия края.
- [x] **Живот** — цепочка от пупка вверх + теневая проработка по бокам.
- [x] **Наручи в заднем виде** — тёмный контур снизу.

---

## 9. Стек технологий

- **Blockbench** → File → New → Modded Entity → Geckolib Model
- **Geckolib 4.x** для Fabric/NeoForge (autoglow для emissive-слоя)
- Ванильный `EntityModel` цепочку из 7 костей тоже потянет, но анимации придётся писать руками в `setupAnim`.

---

**Значения в §3 — рабочая отправная точка.** Собирайте в Blockbench, смотрите в игре с 20 метров, правьте.

*Конец реконструированной спецификации v2.0*
