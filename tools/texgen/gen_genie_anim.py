"""Анимации Кубанской джиннии для GeckoLib.

Ключевые кадры считаются формулой, а не набиваются руками: волна по хвосту —
это синус с фазовым сдвигом на сегмент, и любая правка амплитуды вручную
неизбежно разъезжается между семью костями.

Формула волны (совпадает с §6 KUBAN_GENIE_SPEC.md):

    angle(i, t) = -A · sin(2π·t/T − i·φ)

где ``i`` — индекс сегмента, ``φ`` — сдвиг фазы на сегмент, ``A`` — амплитуда.
Затухающие подвесы (волосы, рушник, серьги) используют ту же волну с
множителем амплитуды и задержкой по времени.

Имена костей берутся из ``genie_parts``, поэтому переименование кости ломает
генератор здесь, а не молча даёт мёртвый канал в игре.
"""
import json
import math
import os

import genie_parts as gp

OUT = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "..",
    "src/main/resources/assets/kubanhorizons/geckolib/animations"
    "/kuban_genie.animation.json"))

TAILS = ["tail%d" % i for i in range(1, 8)]
# Подвесы: (кость, доля амплитуды хвоста, задержка в секундах).
# Затухание — 0.6 на звено, задержка 0.2 с: так кончик всегда идёт за основанием.
DANGLES = [
    ("hair_mass", 0.75, 0.0),
    ("hair_tips", 0.45, 0.2),
    ("rushnyk", 0.62, 0.0),
    ("rushnyk_mid", 0.40, 0.2),
    ("rushnyk_tip", 0.26, 0.4),
]


def _r(v):
    """Скругление до 9 знаков: убирает -0.0 и мусор двоичной дроби в JSON."""
    v = round(float(v), 9)
    return 0 if v == 0 else v


def _times(period, step):
    n = int(round(period / step))
    return [round(i * step, 3) for i in range(n + 1)]


def wave(period, amp, phase, step=0.25, lag=0.0, axis=0):
    """Синусоидальный канал вращения вокруг заданной оси."""
    frames = {}
    for t in _times(period, step):
        a = -amp * math.sin(2 * math.pi * (t - lag) / period - phase)
        rot = [0, 0, 0]
        rot[axis] = _r(a)
        frames["%s" % t] = rot
    return {"rotation": frames}


def bob(period, amp, step=0.25):
    """Вертикальное покачивание корпуса: ±amp px по Y."""
    frames = {}
    for t in _times(period, step):
        frames["%s" % t] = [0, _r(amp * math.sin(2 * math.pi * t / period)), 0]
    return {"position": frames}


def hover(period, amp, tail_amp, phase_step, lean=None):
    """Общий блок парения: корпус, волна по хвосту, затухающие подвесы."""
    bones = {}
    body = bob(period, amp)
    if lean is not None:
        body["rotation"] = {"%s" % t: [lean, 0, 0]
                            for t in _times(period, 0.25)}
    bones["body"] = body

    for i, name in enumerate(TAILS):
        bones[name] = wave(period, tail_amp, i * phase_step)

    for name, factor, lag in DANGLES:
        bones[name] = wave(period, tail_amp * factor, 0.0, lag=lag)

    # Серьги качаются в противофазе друг другу — иначе выглядят приклеенными.
    bones["earring_l"] = wave(period, tail_amp, 0.0)
    bones["earring_r"] = wave(period, -tail_amp, 0.0)
    return bones


def tail_curl(delta, hold_a, hold_b, length):
    """Хвост подтягивается/закручивается: все сегменты доворачиваются на delta."""
    out = {}
    for i, name in enumerate(TAILS):
        # Ближе к кончику доворот сильнее: спираль, а не жёсткая палка.
        k = 0.6 + 0.4 * (i / (len(TAILS) - 1))
        a = _r(delta * k)
        out[name] = {"rotation": {
            "0.0": [0, 0, 0],
            "%s" % hold_a: [0, 0, a],
            "%s" % hold_b: [0, 0, a],
            "%s" % length: [0, 0, 0],
        }}
    return out


def build():
    anims = {}

    # --- idle: спокойное парение -------------------------------------------
    anims["animation.idle"] = {
        "loop": True,
        "animation_length": 3,
        "bones": hover(3.0, amp=1.0, tail_amp=4.0, phase_step=math.pi / 6),
    }

    # --- move: та же волна вдвое сильнее плюс наклон вперёд -----------------
    anims["animation.move"] = {
        "loop": True,
        "animation_length": 2,
        "bones": hover(2.0, amp=1.5, tail_amp=8.0,
                       phase_step=math.pi / 6, lean=-5),
    }

    # --- greet: казачий жест, правая рука к груди --------------------------
    anims["animation.greet"] = {
        "animation_length": 1.5,
        "bones": {
            "arm_r": {"rotation": {"0.0": [0, 0, 0], "0.4": [-55, 0, 28],
                                    "1.0": [-55, 0, 28], "1.5": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.5": [12, 0, 0],
                                   "1.0": [12, 0, 0], "1.5": [0, 0, 0]}},
            "body": {"rotation": {"0.0": [0, 0, 0], "0.5": [6, 0, 0],
                                   "1.0": [6, 0, 0], "1.5": [0, 0, 0]}},
        },
    }

    # --- wish: обе руки вперёд-вверх, хвост подтягивается ------------------
    wish = {
        "arm_l": {"rotation": {"0.0": [0, 0, 0], "0.6": [-65, 0, -18],
                                "1.6": [-65, 0, -18], "2.25": [0, 0, 0]}},
        "arm_r": {"rotation": {"0.0": [0, 0, 0], "0.6": [-65, 0, 18],
                                "1.6": [-65, 0, 18], "2.25": [0, 0, 0]}},
        "body": {"position": {"0.0": [0, 0, 0], "0.6": [0, 1.5, 0],
                               "1.6": [0, 1.5, 0], "2.25": [0, 0, 0]}},
    }
    wish.update(tail_curl(12, 0.6, 1.6, 2.25))
    anims["animation.wish"] = {"animation_length": 2.25, "bones": wish}

    # --- cast: левая рука вверх, хвост в тугую спираль, рушник разворачивается
    cast = {
        "arm_l": {"rotation": {"0.0": [0, 0, 0], "0.6": [-145, 0, -12],
                                "1.8": [-145, 0, -12], "2.5": [0, 0, 0]}},
        "rushnyk": {"rotation": {"0.0": [0, 0, 0], "0.8": [0, 75, 18],
                                  "1.8": [0, 75, 18], "2.5": [0, 0, 0]}},
        "rushnyk_mid": {"rotation": {"0.0": [0, 0, 0], "0.9": [0, 55, 22],
                                      "1.8": [0, 55, 22], "2.5": [0, 0, 0]}},
        "rushnyk_tip": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 35, 25],
                                      "1.8": [0, 35, 25], "2.5": [0, 0, 0]}},
        "body": {"rotation": {"0.0": [0, 0, 0], "0.8": [-8, 0, 0],
                               "1.8": [-8, 0, 0], "2.5": [0, 0, 0]}},
    }
    cast.update(tail_curl(25, 0.8, 1.8, 2.5))
    anims["animation.cast"] = {"animation_length": 2.5, "bones": cast}

    # --- spawn: хвост вытягивается из глечика снизу вверх ------------------
    spawn = {"body": {"position": {"0.0": [0, -6, 0], "1.0": [0, 0.5, 0],
                                    "1.75": [0, 0, 0]},
                       "scale": {"0.0": [0.2, 0.2, 0.2], "1.0": [1, 1, 1],
                                  "1.75": [1, 1, 1]}}}
    for i, name in enumerate(TAILS):
        t0 = round(0.1 * i, 3)
        t1 = round(t0 + 0.5, 3)
        spawn[name] = {"scale": {
            "0.0": [0, 0, 0],
            "%s" % t0: [0, 0, 0],
            "%s" % t1: [1, 1, 1],
        }}
    anims["animation.spawn"] = {"animation_length": 1.75, "bones": spawn}

    # --- despawn: обратный порядок, схлопывание в глечик -------------------
    despawn = {"body": {"position": {"0.0": [0, 0, 0], "1.5": [0, -6, 0]},
                         "scale": {"0.0": [1, 1, 1], "1.5": [0.2, 0.2, 0.2]}}}
    for i, name in enumerate(TAILS):
        # Кончик исчезает первым: индекс считается с конца.
        t0 = round(0.1 * (len(TAILS) - 1 - i), 3)
        t1 = round(t0 + 0.5, 3)
        despawn[name] = {"scale": {
            "0.0": [1, 1, 1],
            "%s" % t0: [1, 1, 1],
            "%s" % t1: [0, 0, 0],
        }}
    anims["animation.despawn"] = {"animation_length": 1.5, "bones": despawn}

    # --- hurt: короткая тряска (урона нет, но реакция нужна) ---------------
    anims["animation.hurt"] = {
        "animation_length": 0.5,
        "bones": {"body": {"position": {
            "0.0": [0, 0, 0], "0.1": [0.4, 0, 0], "0.2": [-0.4, 0, 0],
            "0.3": [0.3, 0, 0], "0.4": [-0.2, 0, 0], "0.5": [0, 0, 0]}}},
    }

    return {"format_version": "1.8.0", "animations": anims}


def main():
    data = build()
    known = {b.name for b in gp.build()}
    for name, anim in data["animations"].items():
        for bone in anim["bones"]:
            if bone not in known:
                raise SystemExit(
                    "%s ссылается на несуществующую кость %s" % (name, bone))
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2, ensure_ascii=False)
        fh.write("\n")
    root = os.path.join(os.path.dirname(__file__), "..", "..")
    print("анимации: %s" % os.path.relpath(OUT, root))
    print("  %d анимаций, каналов на кости: %d"
          % (len(data["animations"]),
             sum(len(a["bones"]) for a in data["animations"].values())))


if __name__ == "__main__":
    main()
