#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор голосов степной фауны Kuban Horizons (фазан и перепел).

Синтезирует восемь звуков полностью программно, без сторонних сэмплов:
  фазан  — ambient (двойной хриплый крик), hurt, death, flush (взлёт)
  перепел — ambient (трёхсложный посвист «подь-полоть»), hurt, death, flush

Гейт `capture_audio.sh` нашёл эти восемь событий зарегистрированными в
KHSounds без единого .ogg: птицы были в мире и молчали. Голос — половина
присутствия существа, и на dedicated server именно он сообщает игроку, что
рядом кто-то есть, когда самой птицы ещё не видно.

Тембры разведены осознанно, чтобы виды не путались на слух:
  * фазан крупный, голос низкий (280–900 Гц), резкий, с хрипом и двумя слогами;
  * перепел мелкий, голос высокий (1300–2600 Гц), чистый свистовой, три слога.

Оба flush — «взрывной» короткий взлёт: плотный шумовой удар крыльев с быстрым
затуханием, характерный для куриных птиц, поднятых с земли.

Требования (ART_BIBLE.md §6, TECH_SPEC.md §9): OGG Vorbis, моно, 44.1 кГц,
без резкой синтетики. Скрипт детерминирован: фиксированный SEED.

Запуск:  python3 tools/soundgen/generate_bird_sounds.py
"""

import os
import sys
import tempfile

import numpy as np

# DSP-примитивы и кодирование переиспользуются из генератора пресса —
# один набор соглашений на все звуки мода (частота, пик, fade, кодировщик).
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_press_sounds import (  # noqa: E402
    SR, apply_fades, encode_ogg, fft_bandpass, normalize, place, t_axis,
    write_wav,
)

SEED = 20260807
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_DIR = os.path.join(
    ROOT, "src", "main", "resources", "assets", "kubanhorizons",
    "sounds", "entity",
)

rng = np.random.default_rng(SEED)


# ----------------------------------------------------------------------------
# Строительные блоки птичьего голоса
# ----------------------------------------------------------------------------

def syrinx(dur: float, f0: float, f1: float, harmonics: int = 5,
           rasp: float = 0.0, vibrato: float = 0.0) -> np.ndarray:
    """Голосовой источник птицы: скользящий тон с гармониками.

    Птичий голос — не чистая синусоида: у него есть обертоны, а у куриных
    ещё и хрип (модуляция шумом). f0->f1 — глиссандо основного тона.
    """
    t = t_axis(dur)
    if len(t) == 0:
        return t
    # Логарифмическое скольжение частоты звучит естественнее линейного.
    f = f0 * (f1 / f0) ** (t / max(t[-1], 1e-6))
    if vibrato > 0:
        f = f * (1.0 + vibrato * np.sin(2 * np.pi * 18.0 * t))
    phase = 2 * np.pi * np.cumsum(f) / SR
    out = np.zeros_like(t)
    for h in range(1, harmonics + 1):
        # Обертоны спадают ~1/h^1.4 — тембр яркий, но не пилообразный.
        out += np.sin(h * phase) / (h ** 1.4)
    if rasp > 0:
        # Хрип: амплитудная модуляция узкополосным шумом.
        noise = fft_bandpass(rng.standard_normal(len(t)), 60.0, 400.0)
        noise = noise / (np.max(np.abs(noise)) + 1e-9)
        out *= (1.0 - rasp) + rasp * (0.5 + 0.5 * noise)
    return out


def env_ad(dur: float, attack: float, decay_pow: float = 2.0) -> np.ndarray:
    """Огибающая «быстрая атака — плавный спад»: один слог крика."""
    t = t_axis(dur)
    if len(t) == 0:
        return t
    a = int(max(1, attack * SR))
    env = np.ones_like(t)
    env[:a] = np.linspace(0.0, 1.0, a) ** 0.6
    tail = np.linspace(0.0, 1.0, len(t) - a) if len(t) > a else np.zeros(0)
    env[a:] = (1.0 - tail) ** decay_pow
    return env


def syllable(dur: float, f0: float, f1: float, rasp: float = 0.0,
             attack: float = 0.008, harmonics: int = 5,
             vibrato: float = 0.0) -> np.ndarray:
    """Один слог: источник + огибающая."""
    return syrinx(dur, f0, f1, harmonics, rasp, vibrato) * env_ad(dur, attack)


def wingbeat(dur: float = 0.42, beats: int = 7) -> np.ndarray:
    """Взлёт куриной птицы: серия плотных шумовых ударов крыльев.

    Частота ударов падает по ходу взлёта, амплитуда — тоже: птица уходит.
    """
    out = np.zeros(int(dur * SR))
    tpos = 0.0
    for i in range(beats):
        frac = i / max(beats - 1, 1)
        beat_dur = 0.055
        n = int(beat_dur * SR)
        noise = fft_bandpass(rng.standard_normal(n), 120.0, 1500.0)
        env = np.exp(-np.linspace(0, 1, n) * 7.0)
        place(out, noise * env, tpos, gain=1.0 - 0.55 * frac)
        tpos += 0.052 + 0.028 * frac
        if tpos >= dur:
            break
    return out


# ----------------------------------------------------------------------------
# Фазан: крупная птица, низкий резкий двусложный крик
# ----------------------------------------------------------------------------

def pheasant_ambient() -> np.ndarray:
    out = np.zeros(int(1.05 * SR))
    # Характерный «кок-кок»: два резких слога, второй ниже и короче.
    place(out, syllable(0.30, 780.0, 300.0, rasp=0.45, attack=0.005), 0.02)
    place(out, syllable(0.22, 620.0, 260.0, rasp=0.5, attack=0.004), 0.36, 0.85)
    return normalize(apply_fades(out))


def pheasant_hurt() -> np.ndarray:
    out = np.zeros(int(0.42 * SR))
    # Боль: резче, выше стартовая частота, больше хрипа.
    place(out, syllable(0.26, 900.0, 340.0, rasp=0.6, attack=0.003,
                        vibrato=0.05), 0.01)
    return normalize(apply_fades(out))


def pheasant_death() -> np.ndarray:
    out = np.zeros(int(0.85 * SR))
    # Смерть: тот же тембр, но тон уходит вниз и обрывается.
    place(out, syllable(0.52, 820.0, 180.0, rasp=0.65, attack=0.004,
                        vibrato=0.04), 0.01)
    place(out, syllable(0.16, 420.0, 150.0, rasp=0.7, attack=0.006), 0.50, 0.55)
    return normalize(apply_fades(out))


def pheasant_flush() -> np.ndarray:
    out = np.zeros(int(0.72 * SR))
    # Взлёт: испуганный вскрик поверх ударов крыльев.
    place(out, wingbeat(0.62, beats=8), 0.03, 0.9)
    place(out, syllable(0.20, 880.0, 380.0, rasp=0.55, attack=0.003), 0.0, 0.75)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Перепел: мелкая птица, высокий чистый трёхсложный посвист
# ----------------------------------------------------------------------------

def quail_ambient() -> np.ndarray:
    out = np.zeros(int(0.95 * SR))
    # «Подь-по-лоть»: три слога, средний короче, третий выше и с подъёмом.
    place(out, syllable(0.13, 1450.0, 1750.0, harmonics=3, attack=0.006), 0.04)
    place(out, syllable(0.09, 1300.0, 1500.0, harmonics=3, attack=0.005), 0.22, 0.8)
    place(out, syllable(0.17, 1700.0, 2350.0, harmonics=3, attack=0.006,
                        vibrato=0.03), 0.36)
    return normalize(apply_fades(out))


def quail_hurt() -> np.ndarray:
    out = np.zeros(int(0.30 * SR))
    place(out, syllable(0.16, 2100.0, 1250.0, harmonics=4, attack=0.003,
                        vibrato=0.06), 0.01)
    return normalize(apply_fades(out))


def quail_death() -> np.ndarray:
    out = np.zeros(int(0.60 * SR))
    place(out, syllable(0.34, 1950.0, 700.0, harmonics=4, attack=0.004,
                        vibrato=0.05), 0.01)
    place(out, syllable(0.11, 1100.0, 600.0, harmonics=3, attack=0.005), 0.34, 0.5)
    return normalize(apply_fades(out))


def quail_flush() -> np.ndarray:
    out = np.zeros(int(0.55 * SR))
    # Перепел мельче: удары чаще и тише, вскрик выше.
    place(out, wingbeat(0.46, beats=9), 0.02, 0.7)
    place(out, syllable(0.12, 2200.0, 1500.0, harmonics=3, attack=0.003), 0.0, 0.7)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Сборка
# ----------------------------------------------------------------------------

def main() -> None:
    sounds = {
        "pheasant/ambient.ogg": pheasant_ambient(),
        "pheasant/hurt.ogg": pheasant_hurt(),
        "pheasant/death.ogg": pheasant_death(),
        "pheasant/flush.ogg": pheasant_flush(),
        "quail/ambient.ogg": quail_ambient(),
        "quail/hurt.ogg": quail_hurt(),
        "quail/death.ogg": quail_death(),
        "quail/flush.ogg": quail_flush(),
    }
    with tempfile.TemporaryDirectory() as tmp:
        for name, sig in sounds.items():
            ogg = os.path.join(OUT_DIR, name)
            os.makedirs(os.path.dirname(ogg), exist_ok=True)
            wav = os.path.join(tmp, name.replace("/", "_").replace(".ogg", ".wav"))
            write_wav(wav, sig)
            enc = encode_ogg(wav, ogg)
            peak_db = 20 * np.log10(np.max(np.abs(sig)) + 1e-12)
            print(f"{name}: {len(sig) / SR:.3f} c, пик {peak_db:+.2f} dBFS, "
                  f"кодировщик: {enc}")


if __name__ == "__main__":
    main()
