"""Минимальный писатель/читатель NBT для структурных заготовок Minecraft.

Зачем свой код, а не библиотека: заготовки структур мода собирались вручную
и в репозитории не было ни генератора, ни способа проверить, что лежит
внутри .nbt. Из-за этого обе существующие структуры остались пустыми
коробками из ванильного дуба — «визуальная посадка 🔨» в CONTENT_BIBLE §6.
Здесь ровно те теги, которые нужны формату structure: без внешних
зависимостей, чтобы генератор запускался на любой машине.

Формат structure (MC 26.2):
    root: TAG_Compound (без имени)
      DataVersion: TAG_Int
      size:     TAG_List<TAG_Int>[3]
      palette:  TAG_List<TAG_Compound{Name, Properties?}>
      blocks:   TAG_List<TAG_Compound{pos: List<Int>[3], state: Int, nbt?}>
      entities: TAG_List (пустой)
"""

import gzip
import struct

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10


# --- запись ---------------------------------------------------------------

def _w_str(out, s):
    b = s.encode("utf-8")
    out += struct.pack(">H", len(b))
    out += b


def _w_payload(out, value):
    """Пишет payload, тип определяется по Python-типу."""
    if isinstance(value, str):
        _w_str(out, value)
    elif isinstance(value, bool):
        out += struct.pack(">b", 1 if value else 0)
    elif isinstance(value, int):
        out += struct.pack(">i", value)
    elif isinstance(value, dict):
        for k, v in value.items():
            out += struct.pack(">B", _tag_of(v))
            _w_str(out, k)
            _w_payload(out, v)
        out += struct.pack(">B", TAG_END)
    elif isinstance(value, list):
        if not value:
            # Пустой список: тип элемента END, как пишет ванильный NbtIo.
            out += struct.pack(">B", TAG_END)
            out += struct.pack(">i", 0)
        else:
            et = _tag_of(value[0])
            out += struct.pack(">B", et)
            out += struct.pack(">i", len(value))
            for item in value:
                _w_payload(out, item)
    else:
        raise TypeError("Неподдерживаемый тип NBT: %r" % type(value))


def _tag_of(value):
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, dict):
        return TAG_COMPOUND
    if isinstance(value, list):
        return TAG_LIST
    raise TypeError("Неподдерживаемый тип NBT: %r" % type(value))


def write_nbt(path, root, root_name=""):
    """Пишет compound как gzip-NBT (ванильный NbtIo.writeCompressed)."""
    out = bytearray()
    out += struct.pack(">B", TAG_COMPOUND)
    _w_str(out, root_name)
    _w_payload(out, root)
    # mtime=0 — заготовки должны быть байт-в-байт воспроизводимыми,
    # иначе каждый запуск генератора шумит в git diff.
    with open(path, "wb") as fh:
        gz = gzip.GzipFile(filename="", mode="wb", fileobj=fh, mtime=0)
        gz.write(bytes(out))
        gz.close()


# --- чтение (для проверки собранного) -------------------------------------

class _R:
    def __init__(self, d):
        self.d = d
        self.i = 0

    def u1(self):
        v = self.d[self.i]
        self.i += 1
        return v

    def u2(self):
        v = struct.unpack_from(">H", self.d, self.i)[0]
        self.i += 2
        return v

    def i4(self):
        v = struct.unpack_from(">i", self.d, self.i)[0]
        self.i += 4
        return v

    def st(self):
        n = self.u2()
        v = self.d[self.i:self.i + n].decode("utf-8")
        self.i += n
        return v


def _r_payload(r, t):
    if t == TAG_END:
        return None
    if t == TAG_BYTE:
        return struct.unpack(">b", bytes([r.u1()]))[0]
    if t == TAG_SHORT:
        v = struct.unpack_from(">h", r.d, r.i)[0]
        r.i += 2
        return v
    if t == TAG_INT:
        return r.i4()
    if t == TAG_LONG:
        v = struct.unpack_from(">q", r.d, r.i)[0]
        r.i += 8
        return v
    if t == TAG_FLOAT:
        v = struct.unpack_from(">f", r.d, r.i)[0]
        r.i += 4
        return v
    if t == TAG_DOUBLE:
        v = struct.unpack_from(">d", r.d, r.i)[0]
        r.i += 8
        return v
    if t == TAG_BYTE_ARRAY:
        n = r.i4()
        v = list(r.d[r.i:r.i + n])
        r.i += n
        return v
    if t == TAG_STRING:
        return r.st()
    if t == TAG_LIST:
        et = r.u1()
        n = r.i4()
        return [_r_payload(r, et) for _ in range(n)]
    if t == TAG_COMPOUND:
        o = {}
        while True:
            tt = r.u1()
            if tt == TAG_END:
                break
            k = r.st()
            o[k] = _r_payload(r, tt)
        return o
    raise ValueError("Неизвестный тег NBT: %d" % t)


def read_nbt(path):
    with gzip.open(path, "rb") as fh:
        data = fh.read()
    r = _R(data)
    t = r.u1()
    r.st()
    return _r_payload(r, t)
