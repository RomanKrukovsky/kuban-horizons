"""Сборщик структурных заготовок Kuban Horizons.

Регион строит из собственных материалов (CONTENT_BIBLE §10): саман,
ракушечник, белёная штукатурка, черепица, плетень, резной наличник.
Структура из ванильного дуба — упущенная возможность: у мода есть
свой материальный набор, и постройки должны его показывать.

Палитра собирается автоматически: одинаковые состояния блока
переиспользуют один индекс, как в ванильных заготовках.
"""

import os

from nbtlib import write_nbt, read_nbt

# Версия данных мира MC 26.2 — совпадает с ванильными заготовками
# server-26.2.jar (data/minecraft/structure/**). Заниженный DataVersion
# заставил бы DataFixerUpper прогонять апгрейды на каждой загрузке.
DATA_VERSION = 4903


class Structure:
    """Сетка блоков с автоматической палитрой."""

    def __init__(self, size_x, size_y, size_z):
        self.size = (size_x, size_y, size_z)
        self._palette = []
        self._blocks = {}

    # --- палитра ---
    def _state_index(self, name, props=None):
        entry = {"Name": name}
        if props:
            # Свойства всегда строки: ванильный BlockState.CODEC
            # читает Properties как Map<String, String>.
            entry["Properties"] = {k: str(v) for k, v in props.items()}
        for i, existing in enumerate(self._palette):
            if existing == entry:
                return i
        self._palette.append(entry)
        return len(self._palette) - 1

    # --- запись блоков ---
    def set(self, x, y, z, name, props=None, nbt=None):
        """Ставит блок. Координаты вне размера — ошибка сборки, не тишина."""
        sx, sy, sz = self.size
        if not (0 <= x < sx and 0 <= y < sy and 0 <= z < sz):
            raise IndexError(
                "Блок %s вне размера структуры: (%d,%d,%d) при size=%s"
                % (name, x, y, z, self.size))
        block = {"pos": [x, y, z], "state": self._state_index(name, props)}
        if nbt:
            block["nbt"] = nbt
        self._blocks[(x, y, z)] = block

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def hollow_walls(self, x0, y0, z0, x1, y1, z1, name, props=None):
        """Только вертикальные стены по периметру, без пола и потолка."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    for y in range(y0, y1 + 1):
                        self.set(x, y, z, name, props)

    def chest(self, x, y, z, loot_table, facing="north"):
        """Сундук с таблицей добычи.

        Ключ ровно {@code LootTable} со строковым id — так пишет ванильный
        RandomizableContainer (см. RandomizableContainer.LOOT_TABLE_TAG).
        Сундук без этого тега сгенерируется пустым: именно так и выглядит
        «зарегистрировано, но бесполезно».
        """
        self.set(x, y, z, "minecraft:chest",
                 {"facing": facing, "type": "single", "waterlogged": "false"},
                 {"LootTable": loot_table, "id": "minecraft:chest"})

    # --- сохранение ---
    def block_count(self):
        return len(self._blocks)

    def to_nbt(self):
        # Порядок блоков — по Y, затем X, Z: так же сортирует ванильный
        # StructureTemplate.buildInfoList, и diff остаётся читаемым.
        ordered = [self._blocks[k] for k in
                   sorted(self._blocks, key=lambda p: (p[1], p[0], p[2]))]
        return {
            "DataVersion": DATA_VERSION,
            "size": list(self.size),
            "palette": self._palette,
            "blocks": ordered,
            "entities": [],
        }

    def save(self, path):
        write_nbt(path, self.to_nbt())
        # Немедленная перечитка: заготовка, которая не читается обратно,
        # обнаруживается здесь, а не в игре пустой коробкой.
        back = read_nbt(path)
        assert back["size"] == list(self.size), "size не сериализовался"
        assert len(back["blocks"]) == self.block_count(), \
            "потерялись блоки при записи"
        assert len(back["palette"]) == len(self._palette), \
            "потерялась палитра при записи"
        return len(back["blocks"]), len(back["palette"])


# --- материалы региона (CONTENT_BIBLE §10) --------------------------------

ADOBE = "kubanhorizons:adobe_bricks"
ADOBE_STAIRS = "kubanhorizons:adobe_brick_stairs"
ADOBE_SLAB = "kubanhorizons:adobe_brick_slab"
ADOBE_WALL = "kubanhorizons:adobe_brick_wall"
SHELL_ROCK = "kubanhorizons:shell_rock"
SHELL_ROCK_STAIRS = "kubanhorizons:shell_rock_stairs"
SHELL_ROCK_SLAB = "kubanhorizons:shell_rock_slab"
SHELL_ROCK_WALL = "kubanhorizons:shell_rock_wall"
PLASTER = "kubanhorizons:whitewashed_plaster"
PLASTER_STAIRS = "kubanhorizons:whitewashed_plaster_stairs"
PLASTER_SLAB = "kubanhorizons:whitewashed_plaster_slab"
ROOF_TILES = "kubanhorizons:roof_tiles"
ROOF_TILE_STAIRS = "kubanhorizons:roof_tile_stairs"
ROOF_TILE_SLAB = "kubanhorizons:roof_tile_slab"
CERAMIC = "kubanhorizons:decorative_ceramic"
CASING = "kubanhorizons:carved_window_casing"
WATTLE = "kubanhorizons:wattle"
WATTLE_GATE = "kubanhorizons:wattle_gate"
HAND_MILL = "kubanhorizons:hand_mill"
DRYING_RACK = "kubanhorizons:drying_rack"
TEA_BUSH = "kubanhorizons:tea_bush"

AIR = "minecraft:air"


def out_dir():
    root = os.path.dirname(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))))
    path = os.path.join(root, "src", "main", "resources", "data",
                        "kubanhorizons", "structure")
    os.makedirs(path, exist_ok=True)
    return path
