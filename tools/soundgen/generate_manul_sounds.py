#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор голоса кубанского манула — талисмана мода.

Пять звуков, полностью программный синтез, без сторонних сэмплов:
  ambient — низкое недовольное бурчание (НЕ милое «мяу»)
  hiss    — предупреждение, когда игрок подошёл слишком резко
  hurt    — короткий злой вскрик
  death   — угасающий стон
  purr    — мурлыканье, награда за высокое доверие

Про манула главное, что он не домашняя кошка: голос у него низкий, сиплый и
ворчливый, диапазон уже кошачьего, а вверх он уходит только в испуге. Поэтому
ambient построен как серия коротких брюзжащих слогов на 150–260 Гц с сильным
хрипом, а не как двухсложное «мя-у» с подъёмом тона: подъём мгновенно делает
зверя ласковым, и весь характер пропадает.

Hiss — центральный звук всей связки, потому что на нём держится взаимодействие:
это единственная реакция, которую игрок получает за неправильное поведение
(подошёл быстро — получил предупреждение). Он сделан как настоящее шипение:
широкополосная турбулентность 1.4–9 кГц БЕЗ тонального ядра. Тон здесь был бы
ошибкой — у кошачьего шипения нет высоты, это воздух через зубы, и любая
синусоида превращает его в свист чайника. Огибающая держит плато, а не сразу
падает: угроза длится, пока зверь смотрит на игрока.

Purr — противоположный полюс: награда за дни терпения, поэтому он должен
звучать тепло и «дорого». Мурлыканье физически это не тон, а амплитудная
модуляция низкого шума частотой 22–28 Гц (частота работы гортани кошки).
Собран именно так, а не как гудящая синусоида: пульсация на слух читается как
живое дыхание, и её слышно даже сквозь музыку.

Требования (ART_BIBLE.md §6, TECH_SPEC.md §9): OGG Vorbis, моно, 44.1 кГц.
Скрипт детерминирован: фиксированный SEED, один запуск даёт те же файлы.

Запуск:  python3 tools/soundgen/generate_manul_sounds.py
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

SEED = 20260810
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_DIR = os.path.join(
    ROOT, "src", "main", "resources", "assets", "kubanhorizons",
    "sounds", "entity", "manul",
)

rng = np.random.default_rng(SEED)


# ----------------------------------------------------------------------------
# Канонизация контейнера OGG
# ----------------------------------------------------------------------------

# Полином CRC-32 потока Ogg: 0x04C11DB7, без инверсий и без reflect —
# это не тот CRC32, что в zlib, поэтому таблица считается здесь.
_OGG_CRC = []
for _i in range(256):
    _r = _i << 24
    for _ in range(8):
        _r = ((_r << 1) ^ 0x04C11DB7) & 0xFFFFFFFF if _r & 0x80000000 \
            else (_r << 1) & 0xFFFFFFFF
    _OGG_CRC.append(_r)


def _ogg_crc(page: bytes) -> int:
    """CRC32 страницы Ogg (поле CRC при расчёте считается нулевым)."""
    crc = 0
    for byte in page:
        crc = ((crc << 8) & 0xFFFFFFFF) ^ _OGG_CRC[((crc >> 24) & 0xFF) ^ byte]
    return crc


def canonicalize_ogg(path: str, serial: int = 0x4B48414D) -> int:
    """Переписать serial number всех страниц Ogg на фиксированный.

    libvorbis выбирает serial потока случайно при каждом кодировании, поэтому
    два прогона генератора дают файлы с разными байтами при полностью
    идентичном звуке. Для репозитория это означает шумный diff на бинарниках
    и невозможность проверить детерминизм через контрольные суммы — а
    детерминизм здесь заявлен как требование.

    Сэмплы при этом не затрагиваются: меняется только 4 байта заголовка на
    странице (смещение 14) и следом пересчитывается CRC самой страницы
    (смещение 22). Возвращает число исправленных страниц.

    Serial 0x4B48414D — это ASCII «MAHK» (Manul, Kuban Horizons): любое
    фиксированное значение подошло бы, но осмысленное проще узнать в дампе.
    """
    with open(path, "rb") as fh:
        data = bytearray(fh.read())

    pages = 0
    pos = 0
    while True:
        pos = data.find(b"OggS", pos)
        if pos < 0:
            break
        # Длина страницы: 27 байт заголовка + таблица сегментов + сами сегменты.
        n_seg = data[pos + 26]
        seg_table = data[pos + 27:pos + 27 + n_seg]
        page_len = 27 + n_seg + sum(seg_table)
        page = data[pos:pos + page_len]
        # Ставим фиксированный serial и обнуляем CRC перед расчётом.
        page[14:18] = serial.to_bytes(4, "little")
        page[22:26] = b"\x00\x00\x00\x00"
        page[22:26] = _ogg_crc(bytes(page)).to_bytes(4, "little")
        data[pos:pos + page_len] = page
        pos += page_len
        pages += 1

    with open(path, "wb") as fh:
        fh.write(bytes(data))
    return pages


# ----------------------------------------------------------------------------
# Строительные блоки
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


def env_plateau(dur: float, attack: float, release: float) -> np.ndarray:
    """Огибающая с плато: атака, ровная середина, спад.

    Нужна шипению и мурлыканью — звукам, которые длятся, а не «выстреливают».
    Спад по косинусу, чтобы не было щелчка на срезе.
    """
    n = int(max(2, dur * SR))
    a = int(max(1, attack * SR))
    r = int(max(1, release * SR))
    a = min(a, n // 2)
    r = min(r, n - a)
    env = np.ones(n)
    env[:a] = 0.5 - 0.5 * np.cos(np.linspace(0.0, np.pi, a))
    env[n - r:] = 0.5 + 0.5 * np.cos(np.linspace(0.0, np.pi, r))
    return env


def growl(dur: float, f0: float, f1: float, harmonics: int = 8,
          rasp: float = 0.7, rasp_lo: float = 40.0, rasp_hi: float = 320.0,
          jitter: float = 0.0) -> np.ndarray:
    """Ворчащий голосовой источник: низкий тон с хрипом и дрожью.

    Хрип — амплитудная модуляция узкополосным шумом (голосовые складки зверя).
    Jitter — медленная случайная девиация частоты: без неё низкий тон звучит
    как гудение генератора, а не как недовольное животное.
    """
    t = t_axis(dur)
    if len(t) == 0:
        return t
    f = f0 * (f1 / f0) ** (t / max(t[-1], 1e-6))
    if jitter > 0:
        # Плавная дрожь: шум, отфильтрованный до единиц герц.
        wob = fft_bandpass(rng.standard_normal(len(t)), 2.0, 11.0)
        wob = wob / (np.max(np.abs(wob)) + 1e-9)
        f = f * (1.0 + jitter * wob)
    phase = 2 * np.pi * np.cumsum(f) / SR
    out = np.zeros_like(t)
    for h in range(1, harmonics + 1):
        out += np.sin(h * phase) / (h ** 1.25)
    if rasp > 0:
        noise = fft_bandpass(rng.standard_normal(len(t)), rasp_lo, rasp_hi)
        noise = noise / (np.max(np.abs(noise)) + 1e-9)
        out *= (1.0 - rasp) + rasp * (0.5 + 0.5 * noise)
    return out


def mutter(dur: float, f0: float, f1: float, rasp: float = 0.7,
           attack: float = 0.012, harmonics: int = 8,
           decay_pow: float = 1.8, jitter: float = 0.05,
           rasp_lo: float = 40.0, rasp_hi: float = 320.0) -> np.ndarray:
    """Один брюзжащий слог: ворчание под огибающей."""
    return (growl(dur, f0, f1, harmonics, rasp, rasp_lo, rasp_hi, jitter)
            * env_ad(dur, attack, decay_pow))


def breath(dur: float, lo: float, hi: float, decay: float = 6.0) -> np.ndarray:
    """Шумовой выдох в полосе: воздух, а не голос."""
    n = int(max(1, dur * SR))
    x = fft_bandpass(rng.standard_normal(n), lo, hi)
    return x * np.exp(-np.linspace(0.0, 1.0, n) * decay)


# ----------------------------------------------------------------------------
# Ambient: низкое недовольное бурчание
# ----------------------------------------------------------------------------

def manul_ambient() -> np.ndarray:
    """Три-четыре брюзжащих слога вразнобой: зверь бормочет себе под нос.

    Тон каждого следующего слога ниже предыдущего — фраза «оседает», как
    у ворчащего вслух. Ровный тон читался бы как мурлыканье, а восходящий —
    как просьба; манул не делает ни того, ни другого.
    """
    out = np.zeros(int(1.35 * SR))
    for i, at in enumerate((0.03, 0.34, 0.62, 0.92)):
        f0 = 235.0 - 22.0 * i
        place(out, mutter(0.22, f0, f0 * 0.62, rasp=0.66, attack=0.014,
                          harmonics=9, jitter=0.06),
              at, 0.95 - 0.13 * i)
    # Носовой призвук поверх: воздух через приплюснутую морду.
    place(out, breath(1.05, 260.0, 1500.0, decay=2.0), 0.04, 0.14)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Hiss: предупреждение — ядро всего взаимодействия
# ----------------------------------------------------------------------------

def manul_hiss() -> np.ndarray:
    """Шипение: широкополосная турбулентность без тона.

    Ни одной синусоиды: у кошачьего шипения нет высоты. Вместо тона —
    две шумовые полосы, верхняя чуть «плывёт» по громкости (воздух проходит
    неровно), плюс короткий плевковый транзиент в начале, который и делает
    звук пугающим на близкой дистанции.
    """
    dur = 0.78
    n = int(dur * SR)
    t = t_axis(dur)

    # Основа шипения: широкая полоса «воздух через зубы».
    core = fft_bandpass(rng.standard_normal(n), 1400.0, 8800.0)
    # Нижняя полоса добавляет массу — зверь крупный, шипение не тонкое.
    body = fft_bandpass(rng.standard_normal(n), 500.0, 1800.0)
    # Неровность потока: медленная модуляция верхней полосы.
    flow = 0.72 + 0.28 * np.sin(2 * np.pi * 5.5 * t + 0.7) ** 2

    sig = core * flow + 0.45 * body
    # Плато: угроза держится, пока зверь смотрит, и только потом спадает.
    sig *= env_plateau(dur, attack=0.030, release=0.26)

    # Плевок в начале: резкий транзиент поверх шипения.
    place(sig, breath(0.05, 900.0, 9500.0, decay=22.0), 0.005, 0.85)
    return normalize(apply_fades(sig))


# ----------------------------------------------------------------------------
# Hurt / death
# ----------------------------------------------------------------------------

def manul_hurt() -> np.ndarray:
    """Короткий злой вскрик: тон резко вверх и сразу вниз, много хрипа.

    Выше ambient, но не писк: манул в боли злится, а не жалуется.
    """
    out = np.zeros(int(0.44 * SR))
    place(out, mutter(0.24, 620.0, 240.0, rasp=0.62, attack=0.003,
                      harmonics=8, decay_pow=2.3, jitter=0.09,
                      rasp_lo=90.0, rasp_hi=700.0), 0.01)
    # Придыхание на атаке — звук «рвётся» из зверя.
    place(out, breath(0.10, 700.0, 5200.0, decay=13.0), 0.0, 0.30)
    return normalize(apply_fades(out))


def manul_death() -> np.ndarray:
    """Угасание: длинный стон вниз и короткий выдох вслед.

    Финальный выдох тише и ниже — фраза не обрывается, а затихает.
    """
    out = np.zeros(int(1.05 * SR))
    place(out, mutter(0.52, 560.0, 150.0, rasp=0.68, attack=0.004,
                      harmonics=9, decay_pow=1.7, jitter=0.07,
                      rasp_lo=70.0, rasp_hi=620.0), 0.01)
    place(out, mutter(0.20, 210.0, 110.0, rasp=0.74, attack=0.010,
                      harmonics=6, jitter=0.05,
                      rasp_lo=45.0, rasp_hi=280.0), 0.56, 0.55)
    place(out, breath(0.30, 220.0, 1600.0, decay=3.6), 0.52, 0.20)
    return normalize(apply_fades(out))


# ----------------------------------------------------------------------------
# Purr: награда за доверие
# ----------------------------------------------------------------------------

def manul_purr() -> np.ndarray:
    """Мурлыканье: низкий шум, промодулированный частотой работы гортани.

    Физика важнее красоты тембра. Мурлыканье — это ~25 Гц пульсация, а не
    гудящая нота: гортань кошки открывается и закрывается, и слышно именно
    ритм. Поэтому основа — узкополосный низкий шум, а огибающая пульсирует.
    Частота слегка уходит вверх к середине и обратно: живое дыхание, а не
    метроном.

    Длиннее остальных реплик (≈2.2 с), потому что играется как награда за дни
    терпения — короткий пшик тут прозвучал бы разочарованием.
    """
    dur = 2.20
    n = int(dur * SR)
    t = t_axis(dur)

    # Грудной резонанс: низкая полоса + чуть более высокая «шерстяная».
    low = fft_bandpass(rng.standard_normal(n), 55.0, 420.0)
    mid = fft_bandpass(rng.standard_normal(n), 420.0, 1250.0)
    sig = low + 0.28 * mid

    # Пульсация гортани: 23 -> 27 -> 23 Гц по ходу фразы.
    rate = 23.0 + 4.0 * np.sin(np.pi * t / dur)
    pulse = 0.5 + 0.5 * np.sin(2 * np.pi * np.cumsum(rate) / SR)
    # Возведение в степень заостряет импульс: слышен «рокот», а не тремоло.
    sig *= 0.32 + 0.68 * pulse ** 1.6

    # Тёплый низкий тон под шумом — тело зверя, очень тихо.
    place(sig, growl(dur, 82.0, 78.0, harmonics=3, rasp=0.25,
                     rasp_lo=30.0, rasp_hi=160.0, jitter=0.02), 0.0, 0.22)

    sig *= env_plateau(dur, attack=0.22, release=0.40)
    return normalize(apply_fades(sig))


# ----------------------------------------------------------------------------
# Сборка
# ----------------------------------------------------------------------------

def main() -> None:
    sounds = {
        "ambient.ogg": manul_ambient(),
        "hiss.ogg": manul_hiss(),
        "hurt.ogg": manul_hurt(),
        "death.ogg": manul_death(),
        "purr.ogg": manul_purr(),
    }
    os.makedirs(OUT_DIR, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for name, sig in sounds.items():
            ogg = os.path.join(OUT_DIR, name)
            wav = os.path.join(tmp, name.replace(".ogg", ".wav"))
            write_wav(wav, sig)
            enc = encode_ogg(wav, ogg)
            # Убираем случайный serial потока: без этого два прогона дают
            # разные байты при идентичном звуке.
            pages = canonicalize_ogg(ogg)
            peak_db = 20 * np.log10(np.max(np.abs(sig)) + 1e-12)
            print(f"entity/manul/{name}: {len(sig) / SR:.3f} c, "
                  f"пик {peak_db:+.2f} dBFS, кодировщик: {enc}, "
                  f"страниц Ogg канонизировано: {pages}")


if __name__ == "__main__":
    main()
