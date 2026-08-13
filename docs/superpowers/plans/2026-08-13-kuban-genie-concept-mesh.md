# Kuban Genie Concept Mesh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Создать через Blockbench MCP отдельную детальную mesh-модель кубанской джиннии по четырём концептам, сохранить редактируемый `.bbmodel`, GLB и три проверочных изображения.

**Architecture:** Геометрия создаётся воспроизводимым сценарием, который общается с локальным Blockbench MCP 1.6.1 по Streamable HTTP на `http://127.0.0.1:2228/bb-mcp`. Сценарий создаёт новый generic-model проект, строит отдельные полигональные детали и арматуру, формирует единый атлас, затем просит Blockbench сохранить проект, экспортировать GLB и снять ортографические виды.

**Tech Stack:** Blockbench 5.x, Blockbench MCP 1.6.1, JavaScript в Blockbench, Node.js MCP-клиент, формат `.bbmodel`, glTF/GLB, PNG 1024×1024.

## Global Constraints

- Исходные референсы: `KUBAN_CONCEPT_01.png`–`KUBAN_CONCEPT_04.png`.
- Текущие `kuban_genie.geo.json`, `kuban_genie.png`, glowmask и анимации не изменять.
- Создавать только отдельную mesh-модель; она не подменяет кубоидную GeckoLib-модель.
- Основной атлас: ровно 1024×1024, без фотореализма.
- Обязательные признаки: очки, тиара с сапфирами, серьги, коралловое ожерелье, вышитый белый верх, золотые браслеты, подсолнух, бордовый пояс, рушник, сине-фиолетовый S-образный хвост.
- Выходные файлы: `kuban_genie_concept_mesh.bbmodel`, `kuban_genie_concept_mesh.glb`, `kuban_genie_concept_mesh_texture.png`, `genie_mesh_checks/front.png`, `side.png`, `back.png`.
- Все операции создания и экспорта проходят через Blockbench MCP; локальный сценарий только отправляет MCP-вызовы и проверяет ответы.

---

## File Structure

- Create: `tools/blockbench/blockbench_mcp_client.mjs` — минимальный JSON-RPC клиент с инициализацией сессии и вызовом инструментов.
- Create: `tools/blockbench/build_kuban_genie_mesh.mjs` — описание сцены, материалы, геометрия, арматура, сохранение и рендеры.
- Create: `tools/blockbench/check_kuban_genie_mesh.mjs` — проверка проекта через MCP и проверка выходных файлов.
- Create: `kuban_genie_concept_mesh.bbmodel` — редактируемый исходник Blockbench.
- Create: `kuban_genie_concept_mesh.glb` — универсальная экспортная копия.
- Create: `kuban_genie_concept_mesh_texture.png` — единый атлас.
- Create: `genie_mesh_checks/front.png`, `side.png`, `back.png` — проверочные виды.

### Task 1: Надёжный клиент Blockbench MCP

**Files:**
- Create: `tools/blockbench/blockbench_mcp_client.mjs`
- Test: встроенная команда `node tools/blockbench/blockbench_mcp_client.mjs health`

**Interfaces:**
- Produces: `connectBlockbench(): Promise<BlockbenchClient>`.
- Produces: `BlockbenchClient.callTool(name: string, args: object): Promise<object>`.
- Produces: CLI-команда `health`, печатающая имя сервера, версию и `ready=true`.

- [ ] **Step 1: Реализовать инициализацию MCP-сессии**

Клиент отправляет `initialize` с `protocolVersion: "2025-03-26"`, сохраняет заголовок `mcp-session-id`, затем отправляет `notifications/initialized`. При HTTP-ошибке, JSON-RPC `error` или отсутствии session id процесс завершается с ненулевым кодом и понятным сообщением.

- [ ] **Step 2: Реализовать вызов инструмента**

`callTool` отправляет `tools/call` с `{name, arguments}` и возвращает `result`; поле `isError: true` считается ошибкой. Счётчик JSON-RPC id увеличивается на каждый запрос.

- [ ] **Step 3: Проверить подключение**

Run: `node tools/blockbench/blockbench_mcp_client.mjs health`

Expected: вывод содержит `Blockbench MCP 1.6.1` и `ready=true`.

- [ ] **Step 4: Зафиксировать клиент**

```bash
git add tools/blockbench/blockbench_mcp_client.mjs
git commit -m "build(blockbench): add MCP client"
```

### Task 2: Базовая сцена и силуэт

**Files:**
- Create: `tools/blockbench/build_kuban_genie_mesh.mjs`
- Create: `kuban_genie_concept_mesh.bbmodel`

**Interfaces:**
- Consumes: `connectBlockbench()` и `callTool()` из Task 1.
- Produces: проект `kuban_genie_concept_mesh` формата `free`/generic model.
- Produces: группы `body`, `head`, `hair`, `arm_l`, `arm_r`, `hips`, `tail_01`–`tail_10`.

- [ ] **Step 1: Создать новый проект и контрольную точку**

Вызвать `create_project({name: "kuban_genie_concept_mesh", format: "free"})`, затем `save_checkpoint({name: "empty genie mesh"})`. Проверить через `get_project_info`, что активен новый проект и в нём нет элементов.

- [ ] **Step 2: Построить основные формы**

Через `create_sphere`, `create_cylinder` и `place_mesh` создать голову, шею, грудь, талию, таз, плечи, предплечья и кисти. Базовая высота от макушки до начала хвоста — 48 условных пикселей; размах рук — 28; глубина головы — 8; талия заметно уже груди и таза.

- [ ] **Step 3: Построить S-образный хвост**

Создать десять сужающихся сегментов с центрами, образующими S-кривую во фронтальном и боковом видах. Соседние сегменты перекрываются на 8–12% длины, чтобы при поворотах не появлялись щели.

- [ ] **Step 4: Проверить и сохранить базовый силуэт**

Вызвать `list_outline({include_cubes: false, include_meshes: true})`; ожидаются группы головы, корпуса, обеих рук и десяти сегментов хвоста. Вызвать `export_model({codec_id: "project", path: "/Users/romanmolodyko/Documents/kuban-horizon/kuban_genie_concept_mesh.bbmodel", max_content_length: 0})`.

- [ ] **Step 5: Зафиксировать силуэт**

```bash
git add tools/blockbench/build_kuban_genie_mesh.mjs kuban_genie_concept_mesh.bbmodel
git commit -m "feat(genie): build concept mesh silhouette"
```

### Task 3: Лицо, волосы, одежда и украшения

**Files:**
- Modify: `tools/blockbench/build_kuban_genie_mesh.mjs`
- Modify: `kuban_genie_concept_mesh.bbmodel`

**Interfaces:**
- Consumes: группы и основные сетки Task 2.
- Produces: отдельные объекты `eyes`, `glasses`, `tiara`, `tiara_gems`, `earrings`, `necklace`, `blouse`, `sleeves`, `bangles`, `sunflower`, `belt`, `belt_gems`, `rushnyk`, `hair_cap`, `hair_back`, `hair_locks`.

- [ ] **Step 1: Добавить лицо и волосы**

Собрать крупные белки глаз, коричневые радужки, чёрные зрачки, тонкую улыбку и чёрную оправу. Волосы строятся отдельной шапкой, задней массой и боковыми прядями; ни одна сетка волос не проходит перед глазами.

- [ ] **Step 2: Добавить одежду и вышивку как геометрию-основу**

Создать белый лиф и рукава поверх тела с небольшим отступом от кожи. На передней части лифа и рукавах оставить отдельные поверхности для красно-чёрного орнамента, чтобы UV не смешивались с кожей.

- [ ] **Step 3: Добавить украшения и асимметрию**

Создать золотую тиару с пятью синими камнями, серьги, коралловое ожерелье, три браслета на каждом запястье, бордовый пояс с центральным сапфиром и цепями, подсолнух только на правом плече и рушник только на левом боку.

- [ ] **Step 4: Пересохранить проект и проверить структуру**

`list_outline` должен содержать все имена из интерфейса задачи. Повторно вызвать `export_model` с codec `project` и тем же путём.

- [ ] **Step 5: Зафиксировать детали**

```bash
git add tools/blockbench/build_kuban_genie_mesh.mjs kuban_genie_concept_mesh.bbmodel
git commit -m "feat(genie): add concept mesh details"
```

### Task 4: Атлас, UV и материалы

**Files:**
- Modify: `tools/blockbench/build_kuban_genie_mesh.mjs`
- Create: `kuban_genie_concept_mesh_texture.png`
- Modify: `kuban_genie_concept_mesh.bbmodel`

**Interfaces:**
- Produces: текстура `kuban_genie_concept_mesh_texture` размером 1024×1024.
- Produces: непересекающиеся UV-области для кожи, лица, волос, белой ткани, вышивки, золота, сапфиров, кораллов, бордового пояса, подсолнуха и десяти оттенков хвоста.

- [ ] **Step 1: Создать атлас через MCP**

Вызвать `create_texture` с размером 1024×1024. Через `risky_eval` использовать canvas активной текстуры для заливки зон палитры и прорисовки симметричных красно-чёрных мотивов вышивки; отключить сглаживание canvas.

- [ ] **Step 2: Разложить UV**

Применить текстуру ко всем объектам через `apply_texture`; через `set_mesh_uv` назначить отдельные прямоугольные зоны. Орнаменты лифа, рукавов и рушника используют собственные области, лицо не делит UV с волосами.

- [ ] **Step 3: Сохранить текстуру и проект**

Через Blockbench API внутри `risky_eval` сохранить PNG в `/Users/romanmolodyko/Documents/kuban-horizon/kuban_genie_concept_mesh_texture.png`; затем повторно экспортировать проект codec `project`.

- [ ] **Step 4: Проверить атлас**

Run: `sips -g pixelWidth -g pixelHeight kuban_genie_concept_mesh_texture.png`

Expected: `pixelWidth: 1024`, `pixelHeight: 1024`.

- [ ] **Step 5: Зафиксировать оформление**

```bash
git add tools/blockbench/build_kuban_genie_mesh.mjs kuban_genie_concept_mesh.bbmodel kuban_genie_concept_mesh_texture.png
git commit -m "feat(genie): texture concept mesh"
```

### Task 5: Арматура и веса

**Files:**
- Modify: `tools/blockbench/build_kuban_genie_mesh.mjs`
- Modify: `kuban_genie_concept_mesh.bbmodel`

**Interfaces:**
- Produces: арматуру `genie_rig`.
- Produces: кости `root`, `body`, `spine`, `chest`, `neck`, `head`, `hair`, `upper_arm_l/r`, `forearm_l/r`, `hand_l/r`, `hips`, `belt`, `rushnyk`, `tail_01`–`tail_10`.

- [ ] **Step 1: Создать арматуру и кости**

Вызвать `add_armature({name: "genie_rig", add_initial_bone: false})`, затем создать кости через `add_armature_bone` с явными origin, length и parent_id. Хвост — последовательная цепочка из десяти костей.

- [ ] **Step 2: Назначить веса**

Получить ключи вершин через `get_vertex_weights`; назначить жёсткие веса аксессуарам и плавные веса плечам, локтям, талии и хвосту через `set_vertex_weights_batch`. Сумма весов деформируемой вершины должна быть 1.0.

- [ ] **Step 3: Проверить тестовые изгибы**

Через `update_armature_bone` временно повернуть правое плечо на 25°, правый локоть на 35°, `tail_05` на 18°. Снять экран, убедиться в отсутствии разрывов, затем вернуть вращения к `[0,0,0]`.

- [ ] **Step 4: Пересохранить проект**

Экспортировать codec `project` в `kuban_genie_concept_mesh.bbmodel`.

- [ ] **Step 5: Зафиксировать риг**

```bash
git add tools/blockbench/build_kuban_genie_mesh.mjs kuban_genie_concept_mesh.bbmodel
git commit -m "feat(genie): rig concept mesh"
```

### Task 6: Экспорт, проверочные виды и автоматическая проверка

**Files:**
- Modify: `tools/blockbench/build_kuban_genie_mesh.mjs`
- Create: `tools/blockbench/check_kuban_genie_mesh.mjs`
- Create: `kuban_genie_concept_mesh.glb`
- Create: `genie_mesh_checks/front.png`
- Create: `genie_mesh_checks/side.png`
- Create: `genie_mesh_checks/back.png`

**Interfaces:**
- Consumes: завершённый активный проект и атлас.
- Produces: GLB и три ортографических изображения.
- Produces: команда проверки `node tools/blockbench/check_kuban_genie_mesh.mjs`.

- [ ] **Step 1: Экспортировать GLB**

Сначала вызвать `list_export_formats` и найти codec `gltf`; затем вызвать `export_model({codec_id: "gltf", options: {binary: true}, path: "/Users/romanmolodyko/Documents/kuban-horizon/kuban_genie_concept_mesh.glb", max_content_length: 0})`.

- [ ] **Step 2: Снять три ортографических вида**

Вызвать `set_camera_angle` с `projection: "orthographic"` для фронта, правого бока и спины. После каждого положения вызвать `capture_screenshot` и сохранить возвращённое PNG-содержимое в соответствующий файл `genie_mesh_checks`.

- [ ] **Step 3: Реализовать автоматическую проверку**

Проверяющий сценарий вызывает `get_project_info`, `list_outline`, `list_textures`, `list_armatures` и `list_armature_bones`; проверяет размер текстуры, наличие обязательных объектов и костей, десять сегментов хвоста и ненулевой размер всех шести выходных файлов.

- [ ] **Step 4: Запустить проверки**

Run: `node tools/blockbench/check_kuban_genie_mesh.mjs`

Expected: `PASS: project`, `PASS: texture 1024x1024`, `PASS: required parts`, `PASS: rig`, `PASS: exports`.

- [ ] **Step 5: Визуально сравнить виды**

Сравнить `front.png` с `KUBAN_CONCEPT_01.png`, `side.png` с `KUBAN_CONCEPT_02.png`, `back.png` с `KUBAN_CONCEPT_03.png`; исправить только заметные расхождения силуэта, перекрытия и отсутствующие детали. Повторить Task 6 Steps 1–4 после исправлений.

- [ ] **Step 6: Финальная проверка неизменности игровых ассетов**

Run: `git diff --exit-code -- src/main/resources/assets/kubanhorizons/geckolib/models/kuban_genie.geo.json src/main/resources/assets/kubanhorizons/geckolib/animations/kuban_genie.animation.json src/main/resources/assets/kubanhorizons/textures/entity/kuban_genie.png src/main/resources/assets/kubanhorizons/textures/entity/kuban_genie_glowmask.png`

Expected: команда завершается с кодом 0 относительно состояния на старте выполнения плана. Если эти файлы уже были изменены пользователем до начала, сравнить их сохранённые хэши до и после выполнения вместо сравнения с Git.

- [ ] **Step 7: Зафиксировать результат**

```bash
git add tools/blockbench/build_kuban_genie_mesh.mjs tools/blockbench/check_kuban_genie_mesh.mjs kuban_genie_concept_mesh.bbmodel kuban_genie_concept_mesh.glb kuban_genie_concept_mesh_texture.png genie_mesh_checks
git commit -m "feat(genie): add detailed concept mesh"
```
