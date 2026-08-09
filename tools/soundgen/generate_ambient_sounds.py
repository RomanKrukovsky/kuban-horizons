#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор атмосферы четырёх биомов Kuban Horizons.

Четыре подписных биома мода — степь, пойма, плавни и лиман — генерировались
с блоком effects, в котором были только цвета: ни петли, ни настроения, ни
редких вкраплений. На слух они не отличались от ванильных равнин. Мод при
этом уже нёс синтезированный суховей и тридцать четыре звуковых события,
то есть тишина в собственных биомах была не нехваткой инструментов, а
незакрытым пробелом.

Синтезирует восемь звуков полностью программно, без сторонних сэмплов:
  степь   — loop (сухой ветер по ковылю), additions (далёкий посвист)
  пойма   — loop (листва и вода), additions (плеск у берега)
  плавни  — loop (шелест тростника), additions (хлопок крыльев в камыше)
  лиман   — loop (широкий ветер над водой), additions (крик над отмелью)

Четыре разных места — четыре разных тембра, разведённых так, чтобы биом
угадывался с закрытыми глазами:
  * степь  — сухо и высоко: шум 300–3000 Гц, треск сухостоя, ни капли воды;
  * пойма  — влажно и мягко: шелест листвы 700–5000 Гц плюс тихий низкий
             журчащий слой, темп спокойнее степного;
  * плавни — тесно и глухо: узкий свист стеблей 700–1900 Гц над стоячей
             водой, без степной яркости в верхах;
  * лиман  — простор: низкий гул 40–500 Гц открытой воды и редкие всплески,
             самый тёмный тембр из четырёх.

Разведённость проверяется не на слух, а числом: спектральный центроид
четырёх петель обязан отличаться заметно. Первая версия плавней совпала со
степной с точностью до 4 Гц — два разных места звучали одинаково, — поэтому
у тростника убран верхний слой и усилен стоячий низ.

ПЕТЛИ. loop-звуки играются бесконечно, поэтому шов между концом и началом
слышен как щелчок или «дыхание». Обычные fade-in/fade-out тут не годятся:
они дают провал громкости на стыке. Здесь швы убраны иначе:
  1) все модуляции — синусы с целым числом периодов на длину файла,
     поэтому огибающая на стыке непрерывна;
  2) шумовая основа склеена сама с собой кросс-фейдом (loop_seam):
     последние секунды подмешиваются к первым по косинусу, и точка склейки
     перестаёт существовать.
Функция apply_fades к петлям намеренно НЕ применяется — она бы вернула
щелчок, который мы только что убрали.

Требования (ART_BIBLE.md §6, TECH_SPEC.md §9): OGG Vorbis, моно, 44.1 кГц.
Скрипт детерминирован: фиксированный SEED.

Запуск:  python3 tools/soundgen/generate_ambient_sounds.py
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

# Длина петель. Достаточно долгая, чтобы повтор не считывался ухом как
# «трек по кругу», и достаточно короткая, чтобы файл остался лёгким.
LOOP_DUR = 12.0
# Длина кросс-фейда шва петли.
SEAM = 2.0

rng = np.random.default_rng(SEED)


# ----------------------------------------------------------------------------
# Строительные блоки петель
# ----------------------------------------------------------------------------

def loop_seam(x: np.ndarray, seam: float = SEAM) -> np.ndarray:
    """Сшивает сигнал в бесшовную петлю кросс-фейдом «хвост в начало».

    Берёт последние seam секунд и подмешивает их к первым по косинусу,
    после чего хвост отбрасывается. Результат короче входа на seam, зато
    его конец переходит в начало без разрыва: значение и наклон совпадают.
    """
    n = int(round(seam * SR))
    if n <= 0 or len(x) <= 2 * n:
        return x
    head, tail = x[:n].copy(), x[-n:]
    ramp = 0.5 - 0.5 * np.cos(np.linspace(0.0, np.pi, n))  # 0 -> 1
    # Хвост угасает, начало разгорается: сумма мощностей постоянна.
    head = head * ramp + tail * ramp[::-1]
    out = x[:-n].copy()
    out[:n] = head
    return out


def cyc(t: np.ndarray, cycles: int, dur: float, phase: float = 0.0) -> np.ndarray:
    """Синус с ЦЕЛЫМ числом периодов на длину петли.

    Целое число периодов — обязательное условие бесшовности: любая
    модуляция с дробным числом периодов даёт на стыке скачок громкости,
    который слышен как щелчок каждые dur секунд.
    """
    return np.sin(2 * np.pi * cycles * t / dur + phase)


def breathing_noise(dur: float, lo: float, hi: float,
                    cycles: int, depth: float) -> np.ndarray:
    """Отфильтрованный шум с медленным «дыханием» громкости.

    Основа любой из четырёх петель: ветер и вода — это шум, а узнаваемым
    его делает полоса частот и темп колебания громкости.
    """
    t = t_axis(dur)
    base = fft_bandpass(rng.standard_normal(len(t)), lo, hi)
    env = 1.0 - depth + depth * (0.5 + 0.5 * cyc(t, cycles, dur))
    return base * env


def rustle(dest: np.ndarray, dur: float, lo: float, hi: float,
           density: float, gain: float, decay: float = 20.0) -> None:
    """Рассыпает по сигналу короткие сухие трески — шелест стеблей и листа.

    Треск не ставится в последние 0.3 с: он попал бы в зону сшивки петли
    и прозвучал бы дважды подряд на стыке.
    """
    pos = 0.05
    limit = dur - 0.3
    while pos < limit:
        n = int(0.030 * SR)
        tt = np.linspace(0.0, 0.030, n)
        burst = fft_bandpass(rng.standard_normal(n), lo, hi) * np.exp(-tt * decay)
        place(dest, burst, pos, gain * (0.6 + 0.8 * rng.random()))
        pos += density * (0.5 + rng.random())


# ----------------------------------------------------------------------------
# Степь: сухой ветер по ковылю
# ----------------------------------------------------------------------------

def steppe_loop() -> np.ndarray:
    """Открытая сухая степь: ровный высокий ветер и треск сухостоя.

    Ни одного низкого «водяного» слоя: степь звучит тонко и сухо, и именно
    отсутствие низа отличает её от лимана, где низ — основа.
    """
    dur = LOOP_DUR
    t = t_axis(dur)
    # Тело ветра: средняя полоса, две наложенных волны дыхания.
    sig = breathing_noise(dur, 300.0, 3000.0, cycles=3, depth=0.45)
    sig += 0.5 * breathing_noise(dur, 700.0, 5200.0, cycles=5, depth=0.55)
    # Посвист в верхах: медленно проплывает, создаёт ощущение простора.
    whistle = fft_bandpass(rng.standard_normal(len(t)), 2200.0, 6000.0)
    sig += 0.22 * whistle * (0.5 + 0.5 * cyc(t, 2, dur, phase=-0.7))
    # Сухая трава: частый мелкий треск.
    rustle(sig, dur, 2600.0, 8000.0, density=0.13, gain=0.16)
    return normalize(loop_seam(sig), peak_dbfs=-9.0)


def steppe_additions() -> np.ndarray:
    """Далёкий одиночный посвист ветра в бурьяне — редкое вкрапление."""
    dur = 2.60
    t = t_axis(dur)
    band = fft_bandpass(rng.standard_normal(len(t)), 1400.0, 4200.0)
    # Одна волна: звук приходит и уходит, а не включается.
    env = np.sin(np.pi * t / dur) ** 2
    sig = band * env
    rustle(sig, dur, 3000.0, 8000.0, density=0.30, gain=0.10)
    return normalize(apply_fades(sig), peak_dbfs=-12.0)


# ----------------------------------------------------------------------------
# Пойма: листва и вода
# ----------------------------------------------------------------------------

def floodplain_loop() -> np.ndarray:
    """Заливной луг: мягкий шелест листвы и тихий низ текущей воды.

    Темп дыхания вдвое спокойнее степного: в пойме нет открытого ветра,
    воздух вязкий от влаги.
    """
    dur = LOOP_DUR
    t = t_axis(dur)
    # Листва: мягкая широкая полоса.
    sig = breathing_noise(dur, 700.0, 5000.0, cycles=2, depth=0.40)
    # Вода: низкий ровный слой, почти без модуляции — река не дышит.
    sig += 0.42 * breathing_noise(dur, 90.0, 900.0, cycles=1, depth=0.18)
    # Мокрая листва звучит глуше сухой: треск реже и мягче степного.
    rustle(sig, dur, 1400.0, 5200.0, density=0.22, gain=0.13, decay=26.0)
    # Тихое журчание: узкая полоса с мелкой частой рябью.
    ripple = fft_bandpass(rng.standard_normal(len(t)), 1100.0, 2600.0)
    sig += 0.14 * ripple * (0.5 + 0.5 * cyc(t, 11, dur))
    return normalize(loop_seam(sig), peak_dbfs=-9.0)


def floodplain_additions() -> np.ndarray:
    """Плеск у берега: короткий всплеск воды с мягким спадом."""
    out = np.zeros(int(1.60 * SR))
    for at, g in ((0.05, 1.0), (0.34, 0.55), (0.62, 0.30)):
        n = int(0.34 * SR)
        tt = np.linspace(0.0, 0.34, n)
        splash = fft_bandpass(rng.standard_normal(n), 500.0, 4200.0) * np.exp(-tt * 9.0)
        place(out, splash, at, g)
    return normalize(apply_fades(out), peak_dbfs=-12.0)


# ----------------------------------------------------------------------------
# Плавни: стена тростника
# ----------------------------------------------------------------------------

def plavni_loop() -> np.ndarray:
    """Тростниковые плавни: узкий свист стеблей, стоячая вода, теснота.

    Полоса намеренно узкая (900–2600 Гц) и модуляция частая: в отличие от
    открытой степи здесь звук не уходит вдаль, а дребезжит рядом, в стене
    камыша. Низа почти нет — вода стоячая и не шумит.
    """
    dur = LOOP_DUR
    t = t_axis(dur)
    # Свист стеблей: одна узкая полоса. Верхнего слоя, как в степи, здесь
    # нет намеренно — с ним центроид плавней совпадал со степным с точностью
    # до 4 Гц, и на слух два биома были неотличимы. Тростник должен звучать
    # ниже и теснее сухой травы, а не так же ярко.
    sig = breathing_noise(dur, 700.0, 1900.0, cycles=7, depth=0.50)
    sig += 0.40 * breathing_noise(dur, 1100.0, 2400.0, cycles=13, depth=0.60)
    # Сухие стебли бьются друг о друга: плотный, но глухой стук — полоса
    # вдвое ниже степного треска, иначе он тянет тембр обратно в степь.
    rustle(sig, dur, 900.0, 3200.0, density=0.075, gain=0.22, decay=24.0)
    # Стоячая вода под камышом: заметный глухой низ, болото, а не река.
    sig += 0.45 * breathing_noise(dur, 60.0, 320.0, cycles=2, depth=0.30)
    return normalize(loop_seam(sig), peak_dbfs=-9.0)


def plavni_additions() -> np.ndarray:
    """Хлопок крыльев в камыше: кто-то поднялся из тростника рядом."""
    out = np.zeros(int(1.80 * SR))
    # Четыре взмаха с замедлением: птица уходит вверх.
    at = 0.06
    gap = 0.15
    for i in range(4):
        n = int(0.09 * SR)
        tt = np.linspace(0.0, 0.09, n)
        flap = fft_bandpass(rng.standard_normal(n), 180.0, 1500.0) * np.exp(-tt * 16.0)
        place(out, flap, at, 0.95 - 0.16 * i)
        at += gap
        gap *= 1.22
    # Следом — качнувшийся тростник.
    rustle(out, 1.80, 2200.0, 6500.0, density=0.16, gain=0.12)
    return normalize(apply_fades(out), peak_dbfs=-12.0)


# ----------------------------------------------------------------------------
# Лиман: простор солёной воды
# ----------------------------------------------------------------------------

def liman_loop() -> np.ndarray:
    """Лиман: широкий низкий гул открытой воды и далёкая отмель.

    Самый тёмный тембр из четырёх: основа — 40–500 Гц. Верх подрезан
    намеренно, чтобы лиман не путался со степью; ощущение простора даёт
    не свист, а именно масса низа и очень медленное дыхание.
    """
    dur = LOOP_DUR
    t = t_axis(dur)
    # Масса открытой воды.
    sig = breathing_noise(dur, 40.0, 500.0, cycles=1, depth=0.35)
    sig += 0.55 * breathing_noise(dur, 200.0, 1400.0, cycles=2, depth=0.45)
    # Ветер над водой: приглушённая середина, без степного посвиста.
    sig += 0.30 * breathing_noise(dur, 800.0, 2600.0, cycles=3, depth=0.50)
    # Мелкая волна на отмели: редкие мягкие всплески.
    pos = 0.4
    while pos < dur - 0.4:
        n = int(0.42 * SR)
        tt = np.linspace(0.0, 0.42, n)
        wash = fft_bandpass(rng.standard_normal(n), 300.0, 3000.0) * np.exp(-tt * 6.0)
        place(sig, wash, pos, 0.16 + 0.10 * rng.random())
        pos += 1.1 + 1.4 * rng.random()
    return normalize(loop_seam(sig), peak_dbfs=-9.0)


def liman_additions() -> np.ndarray:
    """Далёкий крик над отмелью: одинокий голос в пустом просторе."""
    dur = 1.90
    out = np.zeros(int(dur * SR))
    # Два хриплых слога с падающим тоном — птица, но неразборчиво далёкая.
    for at, f0, f1, g in ((0.05, 1250.0, 780.0, 1.0), (0.52, 1080.0, 700.0, 0.62)):
        n = int(0.30 * SR)
        tt = np.linspace(0.0, 0.30, n)
        sweep = np.linspace(f0, f1, n)
        tone = np.sin(2 * np.pi * np.cumsum(sweep) / SR)
        rasp = fft_bandpass(rng.standard_normal(n), 600.0, 3600.0)
        env = np.minimum(1.0, tt / 0.02) * np.exp(-tt * 5.0)
        place(out, (0.65 * tone + 0.5 * rasp) * env, at, g)
    return normalize(apply_fades(out), peak_dbfs=-13.0)


# ----------------------------------------------------------------------------
# Сборка
# ----------------------------------------------------------------------------

def main() -> None:
    sounds = {
        "ambient/steppe/loop.ogg": steppe_loop(),
        "ambient/steppe/additions.ogg": steppe_additions(),
        "ambient/floodplain/loop.ogg": floodplain_loop(),
        "ambient/floodplain/additions.ogg": floodplain_additions(),
        "ambient/plavni/loop.ogg": plavni_loop(),
        "ambient/plavni/additions.ogg": plavni_additions(),
        "ambient/liman/loop.ogg": liman_loop(),
        "ambient/liman/additions.ogg": liman_additions(),
    }
    with tempfile.TemporaryDirectory() as tmp:
        for name, sig in sounds.items():
            ogg = os.path.join(SOUNDS_DIR, name)
            os.makedirs(os.path.dirname(ogg), exist_ok=True)
            wav = os.path.join(tmp, name.replace("/", "_").replace(".ogg", ".wav"))
            write_wav(wav, sig)
            enc = encode_ogg(wav, ogg)
            peak_db = 20 * np.log10(np.max(np.abs(sig)) + 1e-12)
            # Для петель печатается ещё и разрыв на стыке: если сшивка
            # сломается, число вырастет, и это будет видно сразу.
            seam = ""
            if name.endswith("loop.ogg"):
                gap = abs(float(sig[0]) - float(sig[-1]))
                seam = f", шов {gap:.5f}"
            print(f"{name}: {len(sig) / SR:.3f} c, пик {peak_db:+.2f} dBFS{seam}, "
                  f"кодировщик: {enc}")


if __name__ == "__main__":
    main()
