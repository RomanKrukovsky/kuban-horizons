#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор звуков маслопресса для Kuban Horizons.

Синтезирует три звука полностью программно (numpy, без сторонних сэмплов):
  * creak.ogg  — скрип деревянного винтового пресса (~1.0 с)
  * work.ogg   — рабочий цикл: скрип + постукивание + журчание масла (~1.8 с, зацикливается)
  * finish.ogg — завершение отжима: деревянный «клак» + капли (~0.6 с)

Требования (ART_BIBLE.md §6, TECH_SPEC.md §9):
  OGG Vorbis, моно, 44.1 кГц; тёплые «деревянные» тембры без резкой синтетики.

Кодирование: WAV (float32, во временный файл) -> OGG Vorbis.
Порядок попыток: 1) ffmpeg + libvorbis; 2) python-модуль `soundfile`
(libsndfile, format='OGG', subtype='VORBIS').
На машине сборки: ffmpeg 8.1.2 (Homebrew) собран БЕЗ libvorbis, а его
встроенный кодировщик `vorbis` не поддерживает моно — поэтому фактически
использован `soundfile` 0.14.0 (установлен через
`pip3 install --user --break-system-packages soundfile`; флаг понадобился
из-за PEP 668 в Homebrew-питоне).

Скрипт детерминирован: фиксированный SEED, один запуск пересоздаёт все файлы.

Запуск:  python3 tools/soundgen/generate_press_sounds.py
"""

import os
import shutil
import struct
import subprocess
import sys
import tempfile

import numpy as np

# ----------------------------------------------------------------------------
# Константы
# ----------------------------------------------------------------------------

SEED = 20260805          # детерминированность: дата создания ассетов
SR = 44100               # частота дискретизации, Гц
PEAK_DBFS = -3.0         # целевой пик, dBFS
FADE_MS = 12.0           # fade-in/fade-out на краях (>= 10 мс, без щелчков)

# Каталог ресурсов мода (относительно корня репозитория)
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_DIR = os.path.join(
    ROOT, "src", "main", "resources", "assets", "kubanhorizons",
    "sounds", "block", "oil_press",
)

rng = np.random.default_rng(SEED)


# ----------------------------------------------------------------------------
# Вспомогательные DSP-функции
# ----------------------------------------------------------------------------

def t_axis(dur: float) -> np.ndarray:
    """Ось времени длительностью dur секунд."""
    n = int(round(dur * SR))
    return np.arange(n) / SR


def fft_bandpass(x: np.ndarray, lo: float, hi: float, roll: float = 0.5) -> np.ndarray:
    """Полосовой фильтр через БПФ с мягкими (косинусными) краями полосы.

    roll — относительная ширина скатов (0.5 => скат в пол-октавы).
    Линейная фаза, без звона IIR — достаточно для шумовых компонент.
    """
    n = len(x)
    spec = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(n, 1.0 / SR)
    # Мягкие края: косинусный переход от 0 к 1 вокруг lo и от 1 к 0 вокруг hi
    lo0, lo1 = lo / (1 + roll), lo * (1 + roll)
    hi0, hi1 = hi / (1 + roll), hi * (1 + roll)
    win = np.zeros_like(freqs)
    rise = (freqs >= lo0) & (freqs < lo1)
    win[rise] = 0.5 - 0.5 * np.cos(np.pi * (freqs[rise] - lo0) / (lo1 - lo0))
    win[(freqs >= lo1) & (freqs <= hi0)] = 1.0
    fall = (freqs > hi0) & (freqs <= hi1)
    win[fall] = 0.5 + 0.5 * np.cos(np.pi * (freqs[fall] - hi0) / (hi1 - hi0))
    return np.fft.irfft(spec * win, n)


def decaying_sine(dur: float, freq: float, decay: float, phase: float = 0.0) -> np.ndarray:
    """Затухающая синусоида — «деревянный» резонансный удар."""
    t = t_axis(dur)
    return np.sin(2 * np.pi * freq * t + phase) * np.exp(-t / decay)


def place(dest: np.ndarray, src: np.ndarray, at_s: float, gain: float = 1.0) -> None:
    """Вклеить сигнал src в dest начиная с момента at_s (с обрезкой по краю)."""
    i0 = int(round(at_s * SR))
    if i0 >= len(dest):
        return
    n = min(len(src), len(dest) - i0)
    dest[i0:i0 + n] += gain * src[:n]


def apply_fades(x: np.ndarray, fade_ms: float = FADE_MS) -> np.ndarray:
    """Косинусные fade-in/fade-out на краях — гарантия отсутствия щелчков."""
    n = int(SR * fade_ms / 1000.0)
    n = min(n, len(x) // 2)
    ramp = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, n))  # 0 -> 1
    x = x.copy()
    x[:n] *= ramp
    x[-n:] *= ramp[::-1]
    return x


def normalize(x: np.ndarray, peak_dbfs: float = PEAK_DBFS) -> np.ndarray:
    """Нормализация пика к peak_dbfs (без клиппинга)."""
    peak = np.max(np.abs(x))
    if peak <= 0:
        return x
    return x * (10 ** (peak_dbfs / 20.0) / peak)


def loopify(x: np.ndarray, xfade_s: float = 0.12) -> np.ndarray:
    """Сделать сигнал бесшовно зацикливаемым: хвост кроссфейдится в начало."""
    n = int(xfade_s * SR)
    fade_out = 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, n))  # 1 -> 0
    body = x[:-n].copy()
    body[:n] = body[:n] * (1 - fade_out) + x[-n:] * fade_out
    return body


# ----------------------------------------------------------------------------
# Строительные блоки тембров
# ----------------------------------------------------------------------------

def wood_creak(dur: float, f_lo: float = 80.0, f_hi: float = 200.0,
               slow_attack: bool = True) -> np.ndarray:
    """Скрип дерева: ЧМ-пила + шум + «зерно» stick-slip.

    1) Пилообразный тон, частота которого медленно «ползёт» f_lo -> f_hi
       с дрожанием (медленный случайный вибрато) — трение по резьбе.
    2) Полосовой шум 300–2500 Гц — шероховатость поверхности.
    3) Stick-slip: серия коротких затухающих импульсов с нарастающей
       частотой следования — характерное «зернистое» проскальзывание.
    """
    t = t_axis(dur)
    n = len(t)

    # --- ЧМ-пила: скольжение частоты + медленное случайное дрожание ---
    glide = f_lo + (f_hi - f_lo) * (t / dur) ** 1.4
    wobble_src = rng.standard_normal(n)
    wobble = fft_bandpass(wobble_src, 2.0, 9.0)             # медленные колебания
    wobble *= 18.0 / (np.max(np.abs(wobble)) + 1e-12)       # +-18 Гц дрожания
    inst_freq = np.clip(glide + wobble, 40.0, 400.0)
    phase = 2 * np.pi * np.cumsum(inst_freq) / SR
    # Пила через сумму гармоник (band-limited, чтобы не было алиасинга)
    saw = np.zeros(n)
    for k in range(1, 12):
        saw += ((-1) ** (k + 1)) * np.sin(k * phase) / k
    saw *= 2 / np.pi
    # Лёгкая амплитудная «шершавость» пилы тем же дрожанием
    saw *= 0.75 + 0.25 * np.tanh(wobble / 12.0)

    # --- Шумовая компонента (трение) ---
    noise = fft_bandpass(rng.standard_normal(n), 300.0, 2500.0)
    noise *= 0.9 / (np.max(np.abs(noise)) + 1e-12)

    # --- Stick-slip зерно: импульсы с нарастающей частотой следования ---
    grain = np.zeros(n)
    rate0, rate1 = 14.0, 55.0          # Гц: следование импульсов растёт
    pos = 0.04
    while pos < dur - 0.02:
        frac = pos / dur
        # каждый импульс — короткий затухающий резонанс дерева
        f_res = rng.uniform(500.0, 1400.0)
        imp = decaying_sine(0.02, f_res, 0.004) * rng.uniform(0.5, 1.0)
        place(grain, imp, pos)
        rate = rate0 + (rate1 - rate0) * frac
        pos += 1.0 / rate * rng.uniform(0.85, 1.15)

    sig = saw + 0.35 * noise * (0.3 + 0.7 * np.abs(saw)) + 0.8 * grain

    # --- Огибающая: медленная атака, спад в конце ---
    if slow_attack:
        atk = 1 - np.exp(-t / (dur * 0.22))                 # медленная атака
    else:
        atk = 1 - np.exp(-t / 0.02)
    rel = 1 - np.exp(-(dur - t) / (dur * 0.10))             # плавный спад
    sig *= atk * rel

    # Тёплый тембр: слегка приглушить всё выше ~3.5 кГц
    return fft_bandpass(sig, 45.0, 3500.0)


def wood_knock(freq: float, dur: float = 0.35, decay: float = 0.06) -> np.ndarray:
    """Глухой деревянный удар: пара негармоничных затухающих мод + тук-транзиент."""
    body = decaying_sine(dur, freq, decay)
    body += 0.45 * decaying_sine(dur, freq * 2.76, decay * 0.55)   # негармоничная мода
    body += 0.20 * decaying_sine(dur, freq * 5.40, decay * 0.30)
    # Короткий шумовой транзиент контакта
    click = fft_bandpass(rng.standard_normal(int(0.02 * SR)), 800.0, 4000.0)
    click *= np.exp(-t_axis(0.02) / 0.004)
    out = body.copy()
    place(out, 0.4 * click / (np.max(np.abs(click)) + 1e-12), 0.0)
    return out


def oil_gurgle(dur: float) -> np.ndarray:
    """«Журчание» масла: низкополосный шум с АМ 8–14 Гц и мягкими бульками."""
    n = int(round(dur * SR))
    t = t_axis(dur)
    noise = fft_bandpass(rng.standard_normal(n), 250.0, 1200.0)
    noise /= np.max(np.abs(noise)) + 1e-12
    # Амплитудная модуляция с медленно гуляющей частотой 8–14 Гц
    am_freq = 8.0 + 6.0 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.37 * t))
    am_phase = 2 * np.pi * np.cumsum(am_freq) / SR
    am = 0.55 + 0.45 * np.sin(am_phase)
    return noise * am


def drop_chirp(f0: float = 1200.0, f1: float = 400.0, dur: float = 0.09) -> np.ndarray:
    """Капля: синус-чирп вниз f0 -> f1 с резонансным «хвостом»."""
    t = t_axis(dur)
    # Экспоненциальное скольжение частоты вниз
    freq = f0 * (f1 / f0) ** (t / dur)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    env = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 0.7 * np.exp(-t / (dur * 0.6))
    chirp = np.sin(phase) * env
    # Резонанс «плюха» — короткая затухающая синусоида около конечной частоты
    tail = decaying_sine(dur, f1 * 1.02, dur * 0.35) * 0.5
    return chirp + tail


# ----------------------------------------------------------------------------
# Сами звуки
# ----------------------------------------------------------------------------

def make_creak() -> np.ndarray:
    """oil_press_creak (~1.0 с): скрип деревянного винтового пресса."""
    dur = 1.05
    sig = wood_creak(dur, 80.0, 200.0, slow_attack=True)
    return apply_fades(normalize(sig))


def make_work() -> np.ndarray:
    """oil_press_work (~1.8 с): рабочий цикл, бесшовно зацикливается."""
    dur = 1.92                      # с запасом под кроссфейд зацикливания
    n = int(round(dur * SR))
    sig = np.zeros(n)

    # Ритм цикла: 2 «полуоборота» винта в секунду
    beat = 0.48                     # шаг ритма, с

    # 1) Ритмичное поскрипывание: короткие скрипы на каждом шаге
    for i, at in enumerate(np.arange(0.0, dur, beat)):
        c = wood_creak(0.34, 90.0 + 10 * (i % 2), 180.0, slow_attack=False)
        place(sig, c, at + 0.02, gain=0.55 * (0.85 + 0.3 * ((i + 1) % 2)))

    # 2) Глухое деревянное постукивание (60–120 Гц) в конце каждого шага
    knock_freqs = [62.0, 95.0, 78.0, 112.0]
    for i, at in enumerate(np.arange(0.0, dur, beat)):
        k = wood_knock(knock_freqs[i % 4], dur=0.30, decay=0.055)
        place(sig, k, at + 0.30, gain=0.8)

    # 3) Тихое журчание масла — непрерывный фон
    sig += 0.16 * oil_gurgle(dur)

    sig = loopify(sig, xfade_s=0.12)           # бесшовный цикл (~1.8 с)
    return apply_fades(normalize(sig))


def make_finish() -> np.ndarray:
    """oil_press_finish (~0.6 с): деревянный «клак» + 3 капли."""
    dur = 0.62
    n = int(round(dur * SR))
    sig = np.zeros(n)

    # Двойной «клак»: два затухающих удара, второй чуть тише и выше
    place(sig, wood_knock(110.0, dur=0.25, decay=0.045), 0.005, gain=1.0)
    place(sig, wood_knock(150.0, dur=0.20, decay=0.035), 0.085, gain=0.7)

    # Капель: 3 чирпа вниз 1200 -> 400 Гц с лёгким разбросом
    for at, g in [(0.24, 0.55), (0.36, 0.45), (0.50, 0.38)]:
        f0 = 1200.0 * rng.uniform(0.92, 1.08)
        d = drop_chirp(f0, 400.0, dur=rng.uniform(0.07, 0.10))
        place(sig, d, at, gain=g)

    return apply_fades(normalize(sig))


# ----------------------------------------------------------------------------
# Запись WAV и кодирование в OGG Vorbis
# ----------------------------------------------------------------------------

def write_wav(path: str, x: np.ndarray) -> None:
    """Записать mono float32 WAV без сторонних зависимостей."""
    data = np.clip(x, -1.0, 1.0).astype("<f4").tobytes()
    with open(path, "wb") as f:
        byte_rate = SR * 4
        f.write(b"RIFF" + struct.pack("<I", 4 + 26 + 12 + len(data)) + b"WAVE")
        # fmt-чанк: IEEE float (3), mono, 44100 Гц, 32 бита
        f.write(b"fmt " + struct.pack("<IHHIIHH", 18, 3, 1, SR, byte_rate, 4, 32))
        f.write(struct.pack("<H", 0))                    # cbSize = 0
        f.write(b"fact" + struct.pack("<II", 4, len(x)))
        f.write(b"data" + struct.pack("<I", len(data)) + data)


def encode_ogg(wav_path: str, ogg_path: str) -> str:
    """Кодировать WAV -> OGG Vorbis. Возвращает имя использованного кодировщика.

    Приоритет: 1) ffmpeg (libvorbis); 2) python-модуль soundfile (libsndfile).
    Встроенный ffmpeg-кодировщик `vorbis` не используется: он экспериментальный
    и не поддерживает моно.
    """
    if shutil.which("ffmpeg"):
        r = subprocess.run(
            ["ffmpeg", "-y", "-v", "error", "-i", wav_path,
             "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis", "-qscale:a", "5",
             ogg_path],
            capture_output=True,
        )
        if r.returncode == 0:
            return "ffmpeg (libvorbis)"
        # ffmpeg есть, но без libvorbis — падаем на soundfile ниже
    try:
        import soundfile as sf  # fallback: libsndfile умеет OGG/Vorbis
        data, sr = sf.read(wav_path)
        sf.write(ogg_path, data, sr, format="OGG", subtype="VORBIS")
        return "soundfile (libsndfile)"
    except ImportError:
        sys.exit("Нет ни ffmpeg с libvorbis, ни python-модуля soundfile — "
                 "кодирование невозможно.")


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    sounds = {
        "creak.ogg": make_creak(),
        "work.ogg": make_work(),
        "finish.ogg": make_finish(),
    }
    with tempfile.TemporaryDirectory() as tmp:
        for name, sig in sounds.items():
            wav = os.path.join(tmp, name.replace(".ogg", ".wav"))
            ogg = os.path.join(OUT_DIR, name)
            write_wav(wav, sig)
            enc = encode_ogg(wav, ogg)
            peak_db = 20 * np.log10(np.max(np.abs(sig)))
            print(f"{name}: {len(sig) / SR:.3f} c, пик {peak_db:+.2f} dBFS, "
                  f"кодировщик: {enc} -> {ogg}")


if __name__ == "__main__":
    main()
