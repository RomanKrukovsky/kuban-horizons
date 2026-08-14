"""Генерация UV_MAP.md из фактической модели.

Таблицы UV, набитые руками, разъезжаются с моделью после первой же правки:
упаковщик атласа перекладывает развёртки при любом изменении набора
кубоидов. Поэтому документ собирается из того же источника, что и
``.geo.json``, и не может ему противоречить.

Запуск: ``python3 tools/texgen/gen_uv_map.py``
"""
import os

import genie_parts as gp
import gen_genie_model
import gen_jug

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "UV_MAP.md")

HEADER = """# UV_MAP — Кубанская джинния

<!-- ФАЙЛ СГЕНЕРИРОВАН. Не правьте руками:
     источник — tools/texgen/genie_parts.py,
     пересборка — python3 tools/texgen/gen_uv_map.py -->

Атлас персонажа: **{atlas}×{atlas} px**, PNG, nearest-neighbour, без сглаживания.
Глечик выведен на отдельную текстуру **{jug}×{jug} px** (отдельная сущность).

Режим: **box UV** (Blockbench: Project → UV Mode → Box UV).
В таблицах указан **левый верхний угол** box-UV области — именно это значение
попадает в поле `uv` кубоида в `.geo.json`.

Footprint кубоида `W×H×D`: `ширина = 2·⌈D⌉ + 2·⌈W⌉`, `высота = ⌈D⌉ + ⌈H⌉`.
Округление **поосевое, а не по сумме**: грани рисуются целыми пикселями,
поэтому кубоид 4.6×4×3.8 занимает 18 px по ширине, а не 17.

Раскладка граней внутри области (проверено на атласе, а не по документации):

```
(D, 0)      up      W×D
(D+W, 0)    down    W×D
(0, D)      east    D×H     ← экранно левая сторона
(D, D)      north   W×H     ← лицо, смотрит в −Z
(D+W, D)    west    D×H
(2D+W, D)   south   W×H     ← спина
```
"""


def num(v):
    return int(v) if float(v).is_integer() else round(float(v), 2)


def table(bones, title):
    rows = ["", "### %s" % title, "",
            "| Кость | Кубоид | Позиция (X, Y, Z) | Размер (W×H×D) | "
            "Footprint | UV origin |", "|---|---|---|---|---|---|"]
    for bone in bones:
        for cube in bone.cubes:
            w, h = cube.footprint()
            rows.append("| `%s` | `%s` | %s | %s | %d×%d | %d, %d |" % (
                bone.name, cube.name,
                ", ".join(str(num(v)) for v in cube.pos),
                "×".join(str(num(v)) for v in cube.size),
                w, h, cube.uv[0], cube.uv[1]))
    return rows


def main():
    bones = gp.build()
    used = gen_genie_model.pack(bones)
    jug_bones = gen_jug.build()
    gen_genie_model.pack(jug_bones, atlas=gen_jug.ATLAS)

    lines = [HEADER.format(atlas=gen_genie_model.ATLAS, jug=gen_jug.ATLAS)]

    groups = [
        ("Голова и волосы", ("head", "hair_mass", "hair_tips",
                             "earring_l", "earring_r")),
        ("Торс", ("torso",)),
        ("Руки", ("arm_l", "arm_r")),
        ("Бёдра, пояс, рушник", ("hips", "belt", "pendant_l", "pendant_c",
                                 "pendant_r", "rushnyk", "rushnyk_mid",
                                 "rushnyk_tip")),
        ("Хвост", tuple("tail%d" % i for i in range(1, 8))),
    ]
    by = gp.bones_by_name(bones)
    for title, names in groups:
        lines += table([by[n] for n in names if n in by], title)

    fill = 100.0 * sum(used) / (gen_genie_model.ATLAS ** 2)
    lines += ["", "Занято атласа: **%.1f%%** (%d кубоидов)." % (
        fill, sum(len(b.cubes) for b in bones))]

    lines += ["", "---", "", "## Глечик (%d×%d)" % (gen_jug.ATLAS,
                                                    gen_jug.ATLAS)]
    lines += table(jug_bones, "Кубоиды глечика")
    lines += ["",
              "Горло открыто на **%.1f px** в глубину: стенки тулова, плеч, "
              "горла и венчика собраны кольцами, поэтому луч сверху доходит "
              "до дна шахты. Сплошной кубоид на любом из этих ярусов делает "
              "горло визуально закрытым." % (gen_jug.Y_RIM - 1.0
                                             - gen_jug.Y_BORE_FLOOR),
              ""]

    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    print("UV_MAP.md обновлён: %d кубоидов персонажа, %d глечика"
          % (sum(len(b.cubes) for b in bones),
             sum(len(b.cubes) for b in jug_bones)))


if __name__ == "__main__":
    main()
