#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор голосов фауны давления Kuban Horizons и погодных звуков.

Синтезирует двадцать звуков полностью программно, без сторонних сэмплов:
  кабан    — ambient (низкое хрюканье), hurt, death
  нутрия   — ambient (писк грызуна), hurt, death
  саранча  — ambient (сухая стридуляция), hurt
  пчела    — ambient (жужжание), hurt
  овчарка  — ambient (низкий лай), hurt, death
  осётр    — flop (шлепок мокрой рыбы)
  чайка    — ambient (резкий хохочущий крик), hurt
  цапля    — ambient (скрипучий грай), hurt
  суховей  — протяжный порыв сухого степного ветра
  джинн    — snap (магический щелчок)

Все двадцать событий были зарегистрированы в KHSounds без единого .ogg:
запущенная игра писала в лог «Missing sound for event» на каждое из них, то
есть вся новая живность была немой. Щелчок джинна хуже прочих: он реально
проигрывается из MagicalSignature, поэтому его отсутствие — слышимый сбой,
а не пропущенная деталь.

Тембры разведены по физическому размеру источника, чтобы виды не путались
на слух:
  * кабан — крупный зверь: 70–260 Гц, глубокое хрюканье с шумовой основой;
  * овчарка — крупная, но голос звонче кабана: 180–520 Гц, резкая атака;
  * нутрия — мелкий грызун: 900–2400 Гц, чистый писк с быстрым спадом;
  * саранча — не голос, а трение: импульсный треск 3–7 кГц без тона;
  * пчела — тональное жужжание 210 Гц с гармониками и биением крыльев;
  * чайка и цапля — птицы, но резче куриных: шумная хриплая подача.

Требования (ART_BIBLE.md §6, TECH_SPEC.md §9): OGG Vorbis, моно, 44.1 кГц.
Скрипт детерминирован: фиксированный SEED.

Запуск:  python3 tools/soundgen/generate_fauna_sounds.py
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

SEED = 20260809
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SOUNDS_DIR = os.path.join(
    ROOT, "src", "main", "resources", "assets", "kubanhorizons", "sounds",
)

rng = np.random.default_rng(SEED)


# ----------------------------------------------------------------------------
# Общие строительные блоки
# ----------------------------------------------------------------------------

def env_ad(dur: float, attack: float, decay_pow: float = 2.0) -> np.ndarray:
    """Огибающая «быстрая атака — плавный спад»: один выкрик."""
    t = t_axis(dur)
    if len(t) == 0:
        return t
    a = int(max(1, attack * SR))
    env = np.ones_like(t)
    env[:a] = np.linspace(0.0, 1.0, a) ** 0.6
    tail = np.linspace(0.0, 1.0, len(t) - a) if len(t) > a else np.zeros(0)
    env[a:] = (1.0 - tail) ** decay_pow
    return env


def voiced(dur: float, f0: float, f1: float, harmonics: int = 6,
           rasp: float = 0.0, vibrato: float = 0.0,
           rasp_lo: float = 60.0, rasp_hi: float = 400.0) -> np.ndarray:
    """Голосовой источник: скользящий тон с гармониками и опциональным хрипом.

    Логарифмическое скольжение f0->f1 звучит естественнее линейного, а хрип —
    это амплитудная модуляция узкополосным шумом (голосовые складки зверя,
    а не чистая синусоида генератора).
    """
    t = t_axis(dur)
    if len(t) == 0:
        return t
    f = f0 * (f1 / f0) ** (t / max(t[-1], 1e-6))
    if vibrato > 0:
        f = f * (1.0 + vibrato * np.sin(2 * np.pi * 16.0 * t))
    phase = 2 * np.pi * np.cumsum(f) / SR
    out = np.zeros_like(t)
    for h in range(1, harmonics + 1):
        out += np.sin(h * phase) / (h ** 1.35)
    if rasp > 0:
        noise = fft_bandpass(rng.standard_normal(len(t)), rasp_lo, rasp_hi)
        noise = noise / (np.max(np.abs(noise)) + 1e-9)
        out *= (1.0 - rasp) + rasp * (0.5 + 0.5 * noise)
    return out


def syllable(dur: float, f0: float, f1: float, rasp: float = 0.0,
             attack: float = 0.008, harmonics: int = 6,
             vibrato: float = 0.0, decay_pow: float = 2.0,
             rasp_lo: float = 60.0, rasp_hi: float = 400.0) -> np.ndarray:
    """Один слог: источник + огибающая."""
    return (voiced(dur, f0, f1, harmonics, rasp, vibrato, rasp_lo, rasp_hi)
            * env_ad(dur, attack, decay_pow))


def noise_burst(dur: float, lo: float, hi: float,
                decay: float = 7.0) -> np.ndarray:
    """Шумовой удар в полосе: основа шлепка, треска и порыва ветра."""
    n = int(max(1, dur * SR))
    x = fft_bandpass(rng.standard_normal(n), lo, hi)
    return x * np.exp(-np.linspace(0.0, 1.0, n) * decay)


# ----------------------------------------------------------------------------
# Дикий кабан: крупный зверь, низкое хрюканье с шумовой основой
# ----------------------------------------------------------------------------

def boar_ambient() -> np.ndarray:
    out = np.zeros(int(1.20 * SR))
    # Серия коротких хрюков вразнобой — кабан «разговаривает» очередями.
    for i, at in enumerate((0.02, 0.30, 0.55, 0.86)):
        f0 = 190.0 - 14.0 * i
        place(out, syllable(0.17, f0, 78.0, rasp=0.62, attack=0.006,
                            harmonics=8, rasp_lo=40.0, rasp_hi=260.0),
              at, 1.0 - 0.12 * i)
    # Носовое сипение поверх: воздух через пятачок.
    place(out, noise_burst(0.9, 220.0, 1400.0, decay=2.2), 0.05, 0.18)
    return normalize(apply_fades(out))


def boar_hurt() -> np.ndarray:
    out = np.zeros(int(0.52 * SR))
    # Боль: выше стартовый тон, резче атака, больше хрипа.
    place(out, syllable(0.30, 300.0, 110.0, rasp=0.72, attack=0.003,
                        harmonics=8, vibrato=0.05,
                        rasp_lo=50.0, rasp_hi=320.0), 0.01)
    place(out, noise_burst(0.18, 300.0, 2200.0, decay=9.0), 0.0, 0.32)
    return normalize(apply_fades(out))


def boar_death() -> np.ndarray:
    out = np.zeros(int(1.15 * SR))
    # Смерть: длинный визг, тон уходит вниз и обрывается сипением.
    place(out, syllable(0.62, 340.0, 90.0, rasp=0.7, attack=0.004,
                        harmonics=9, vibrato=0.04,
                        rasp_lo=45.0, rasp_hi=300.0), 0.01)
    place(out, syllable(0.22, 150.0, 70.0, rasp=0.78, attack=0.008,
                        harmonics=6, rasp_lo=35.0, rasp_hi=200.0), 0.66, 0.6)
    place(out, noise_burst(0.35, 180.0, 1200.0, decay=3.5), 0.60, 0.22)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Нутрия: мелкий грызун, высокий чистый писк
# ----------------------------------------------------------------------------

def nutria_ambient() -> np.ndarray:
    out = np.zeros(int(0.70 * SR))
    # Двойной короткий писк с подъёмом — «чирк-чирк» водяного грызуна.
    place(out, syllable(0.10, 980.0, 1550.0, harmonics=3, attack=0.004,
                        decay_pow=2.6), 0.03)
    place(out, syllable(0.08, 1150.0, 1750.0, harmonics=3, attack=0.004,
                        decay_pow=2.8), 0.22, 0.85)
    # Влажный призвук: нутрия живёт в воде.
    place(out, noise_burst(0.12, 1800.0, 5200.0, decay=11.0), 0.02, 0.14)
    return normalize(apply_fades(out))


def nutria_hurt() -> np.ndarray:
    out = np.zeros(int(0.34 * SR))
    place(out, syllable(0.18, 2100.0, 1050.0, harmonics=4, attack=0.002,
                        vibrato=0.07, decay_pow=2.2), 0.01)
    return normalize(apply_fades(out))


def nutria_death() -> np.ndarray:
    out = np.zeros(int(0.62 * SR))
    place(out, syllable(0.32, 2250.0, 620.0, harmonics=4, attack=0.003,
                        vibrato=0.06), 0.01)
    place(out, syllable(0.10, 900.0, 480.0, harmonics=3, attack=0.005), 0.34, 0.45)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Саранча: не голос, а трение — импульсный сухой треск
# ----------------------------------------------------------------------------

def locust_ambient() -> np.ndarray:
    """Стридуляция: зубчик по крылу. Тона нет вовсе, только частота импульсов."""
    dur = 1.30
    out = np.zeros(int(dur * SR))
    tpos = 0.06
    i = 0
    while tpos < dur - 0.05:
        # Импульсы идут группами: короткая серия, пауза, снова серия.
        in_burst = (i % 14) < 9
        click = noise_burst(0.010, 3200.0, 7400.0, decay=26.0)
        place(out, click, tpos, 1.0 if in_burst else 0.0)
        tpos += 0.017 if in_burst else 0.055
        i += 1
    # Тихий верхний «звон» крыла поверх треска.
    place(out, noise_burst(1.15, 5200.0, 9000.0, decay=1.4), 0.05, 0.10)
    return normalize(apply_fades(out))


def locust_hurt() -> np.ndarray:
    out = np.zeros(int(0.22 * SR))
    # Раздавленное насекомое: один сухой хруст.
    place(out, noise_burst(0.09, 900.0, 6200.0, decay=15.0), 0.01)
    place(out, syllable(0.05, 2600.0, 1200.0, harmonics=2, attack=0.002), 0.0, 0.35)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Кавказская пчела: тональное жужжание с биением крыльев
# ----------------------------------------------------------------------------

def bee_ambient() -> np.ndarray:
    """Жужжание: низкая гармоническая основа + амплитудное биение ~28 Гц."""
    dur = 1.40
    t = t_axis(dur)
    base = 212.0
    # Лёгкий дрейф частоты: живая пчела не держит тон идеально.
    f = base * (1.0 + 0.03 * np.sin(2 * np.pi * 1.7 * t)
                + 0.015 * np.sin(2 * np.pi * 4.3 * t))
    phase = 2 * np.pi * np.cumsum(f) / SR
    sig = np.zeros_like(t)
    for h in range(1, 8):
        sig += np.sin(h * phase) / (h ** 1.25)
    # Биение крыла: жужжание пульсирует, а не гудит ровно.
    sig *= 0.72 + 0.28 * np.sin(2 * np.pi * 28.0 * t)
    # Шумовая шероховатость крыла.
    sig += 0.12 * fft_bandpass(rng.standard_normal(len(t)), 1400.0, 4200.0)
    # Плавный вход-выход: пчела пролетает мимо.
    env = np.minimum(1.0, np.minimum(t / 0.18, (dur - t) / 0.30))
    return normalize(apply_fades(sig * env))


def bee_hurt() -> np.ndarray:
    out = np.zeros(int(0.26 * SR))
    # Резкий срыв жужжания вверх.
    place(out, syllable(0.16, 520.0, 240.0, harmonics=6, attack=0.002,
                        vibrato=0.10, rasp=0.3,
                        rasp_lo=200.0, rasp_hi=1200.0), 0.01)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Кавказская овчарка: крупная собака, низкий резкий лай
# ----------------------------------------------------------------------------

def shepherd_ambient() -> np.ndarray:
    out = np.zeros(int(1.05 * SR))
    # Два «вуф»: резкая атака, быстрый спад, второй тише.
    place(out, syllable(0.16, 480.0, 175.0, rasp=0.45, attack=0.002,
                        harmonics=9, decay_pow=2.4,
                        rasp_lo=90.0, rasp_hi=700.0), 0.03)
    place(out, syllable(0.14, 430.0, 165.0, rasp=0.48, attack=0.002,
                        harmonics=9, decay_pow=2.6,
                        rasp_lo=90.0, rasp_hi=700.0), 0.40, 0.8)
    # Грудной резонанс крупной собаки.
    place(out, noise_burst(0.22, 120.0, 900.0, decay=8.0), 0.02, 0.25)
    return normalize(apply_fades(out))


def shepherd_hurt() -> np.ndarray:
    out = np.zeros(int(0.46 * SR))
    # Взвизг: тон резко вверх, потом вниз.
    place(out, syllable(0.26, 700.0, 260.0, rasp=0.4, attack=0.002,
                        harmonics=7, vibrato=0.07,
                        rasp_lo=120.0, rasp_hi=800.0), 0.01)
    return normalize(apply_fades(out))


def shepherd_death() -> np.ndarray:
    out = np.zeros(int(0.95 * SR))
    place(out, syllable(0.46, 640.0, 150.0, rasp=0.5, attack=0.003,
                        harmonics=8, vibrato=0.05,
                        rasp_lo=100.0, rasp_hi=760.0), 0.01)
    place(out, syllable(0.18, 240.0, 110.0, rasp=0.6, attack=0.006,
                        harmonics=6, rasp_lo=70.0, rasp_hi=480.0), 0.50, 0.5)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Осётр: шлепок мокрой рыбы, тона нет
# ----------------------------------------------------------------------------

def sturgeon_flop() -> np.ndarray:
    out = np.zeros(int(0.55 * SR))
    # Удар тела о поверхность: плотный низкий шлепок.
    place(out, noise_burst(0.13, 90.0, 1900.0, decay=13.0), 0.02)
    # Брызги: короткий верхний рассып.
    place(out, noise_burst(0.20, 2600.0, 9500.0, decay=9.0), 0.05, 0.42)
    # Второй, слабый удар хвостом.
    place(out, noise_burst(0.09, 110.0, 1500.0, decay=16.0), 0.26, 0.5)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Чайка: резкий хохочущий крик
# ----------------------------------------------------------------------------

def gull_ambient() -> np.ndarray:
    out = np.zeros(int(1.35 * SR))
    # Долгий первый крик, затем серия коротких — «хохот» чайки.
    place(out, syllable(0.34, 1250.0, 880.0, rasp=0.4, attack=0.010,
                        harmonics=7, vibrato=0.05,
                        rasp_lo=300.0, rasp_hi=1800.0), 0.03)
    for i, at in enumerate((0.48, 0.68, 0.86, 1.02)):
        place(out, syllable(0.11, 1150.0 - 60.0 * i, 760.0, rasp=0.42,
                            attack=0.004, harmonics=6, decay_pow=2.4,
                            rasp_lo=300.0, rasp_hi=1800.0),
              at, 0.9 - 0.15 * i)
    return normalize(apply_fades(out))


def gull_hurt() -> np.ndarray:
    out = np.zeros(int(0.34 * SR))
    place(out, syllable(0.19, 1650.0, 900.0, rasp=0.5, attack=0.002,
                        harmonics=6, vibrato=0.08,
                        rasp_lo=350.0, rasp_hi=2100.0), 0.01)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Цапля: одиночный скрипучий грай
# ----------------------------------------------------------------------------

def heron_ambient() -> np.ndarray:
    out = np.zeros(int(0.80 * SR))
    # Один резкий «кра-а»: очень хрипло, почти шум с тональным ядром.
    place(out, syllable(0.42, 620.0, 380.0, rasp=0.78, attack=0.006,
                        harmonics=8, vibrato=0.03,
                        rasp_lo=140.0, rasp_hi=1500.0), 0.03)
    place(out, noise_burst(0.30, 700.0, 4200.0, decay=4.5), 0.02, 0.30)
    return normalize(apply_fades(out))


def heron_hurt() -> np.ndarray:
    out = np.zeros(int(0.40 * SR))
    place(out, syllable(0.24, 880.0, 420.0, rasp=0.8, attack=0.003,
                        harmonics=7, vibrato=0.06,
                        rasp_lo=160.0, rasp_hi=1700.0), 0.01)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Суховей: протяжный порыв сухого степного ветра
# ----------------------------------------------------------------------------

def dry_wind() -> np.ndarray:
    """Порыв: широкополосный шум с медленной огибающей и «шелестом» сухостоя.

    Длиннее звериных звуков (≈4 с), потому что играется как погодное событие
    над полем, а не как реплика существа.
    """
    dur = 4.20
    t = t_axis(dur)
    # Основа: низкий гул ветра.
    low = fft_bandpass(rng.standard_normal(len(t)), 60.0, 700.0)
    # Свист: верхняя полоса, медленно проплывающая по громкости.
    high = fft_bandpass(rng.standard_normal(len(t)), 1200.0, 5200.0)
    sweep = 0.5 + 0.5 * np.sin(2 * np.pi * 0.35 * t - 0.6)
    sig = low + 0.55 * high * sweep
    # Шелест сухой травы: редкие короткие трески.
    tpos = 0.15
    while tpos < dur - 0.2:
        place(sig, noise_burst(0.035, 2400.0, 8000.0, decay=18.0),
              tpos, 0.16 + 0.1 * rng.random())
        tpos += 0.09 + 0.22 * rng.random()
    # Два наложенных порыва: ветер не ровный.
    gust = (0.55
            + 0.45 * np.sin(2 * np.pi * 0.22 * t - 1.2) ** 2
            + 0.20 * np.sin(2 * np.pi * 0.61 * t) ** 2)
    env = np.minimum(1.0, np.minimum(t / 0.55, (dur - t) / 0.85))
    return normalize(apply_fades(sig * gust * env))


# ----------------------------------------------------------------------------
# Джинн: магический щелчок
# ----------------------------------------------------------------------------

def genie_snap() -> np.ndarray:
    """Щелчок пальцами + звон: играется из MagicalSignature при исполнении.

    Не звериный звук, поэтому тембр намеренно «нездешний»: сухой транзиент
    и следом чистые обертоны, которых нет ни у одного существа мода.
    """
    out = np.zeros(int(1.10 * SR))
    # Транзиент щелчка.
    place(out, noise_burst(0.035, 1800.0, 9000.0, decay=30.0), 0.01)
    # Звон: две чистые частоты в квинту, долгий спад.
    for f, g, dec in ((1180.0, 0.55, 2.6), (1770.0, 0.34, 3.0),
                      (2360.0, 0.18, 3.6)):
        n = int(0.95 * SR)
        tt = np.linspace(0.0, 0.95, n)
        tone = np.sin(2 * np.pi * f * tt) * np.exp(-tt * dec)
        place(out, tone, 0.015, g)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Сборка
# ----------------------------------------------------------------------------

def main() -> None:
    sounds = {
        "entity/wild_boar/ambient.ogg": boar_ambient(),
        "entity/wild_boar/hurt.ogg": boar_hurt(),
        "entity/wild_boar/death.ogg": boar_death(),
        "entity/nutria/ambient.ogg": nutria_ambient(),
        "entity/nutria/hurt.ogg": nutria_hurt(),
        "entity/nutria/death.ogg": nutria_death(),
        "entity/locust/ambient.ogg": locust_ambient(),
        "entity/locust/hurt.ogg": locust_hurt(),
        "entity/caucasian_bee/ambient.ogg": bee_ambient(),
        "entity/caucasian_bee/hurt.ogg": bee_hurt(),
        "entity/caucasian_shepherd/ambient.ogg": shepherd_ambient(),
        "entity/caucasian_shepherd/hurt.ogg": shepherd_hurt(),
        "entity/caucasian_shepherd/death.ogg": shepherd_death(),
        "entity/sturgeon/flop.ogg": sturgeon_flop(),
        "entity/gull/ambient.ogg": gull_ambient(),
        "entity/gull/hurt.ogg": gull_hurt(),
        "entity/heron/ambient.ogg": heron_ambient(),
        "entity/heron/hurt.ogg": heron_hurt(),
        "entity/genie/snap.ogg": genie_snap(),
        "weather/dry_wind.ogg": dry_wind(),
    }
    with tempfile.TemporaryDirectory() as tmp:
        for name, sig in sounds.items():
            ogg = os.path.join(SOUNDS_DIR, name)
            os.makedirs(os.path.dirname(ogg), exist_ok=True)
            wav = os.path.join(tmp, name.replace("/", "_").replace(".ogg", ".wav"))
            write_wav(wav, sig)
            enc = encode_ogg(wav, ogg)
            peak_db = 20 * np.log10(np.max(np.abs(sig)) + 1e-12)
            print(f"{name}: {len(sig) / SR:.3f} c, пик {peak_db:+.2f} dBFS, "
                  f"кодировщик: {enc}")


if __name__ == "__main__":
    main()
