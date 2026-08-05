"""GUI-текстура маслопресса (256×256, панель 176×166 в стиле ванильной печи)."""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src/main/resources/assets/kubanhorizons/textures/gui/container")

BG = (198, 198, 198, 255)
DARK = (55, 55, 55, 255)
LIGHT = (255, 255, 255, 255)
SLOT_BG = (139, 139, 139, 255)
BLACK_EDGE = (0, 0, 0, 255)
ARROW = (222, 176, 40, 255)
ARROW_HI = (240, 205, 90, 255)


def px(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), c)


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(im, x, y, c)


def panel(im, x0, y0, x1, y1):
    """Панель с ванильной 3-пиксельной рамкой (свет слева-сверху)."""
    rect(im, x0, y0, x1, y1, BG)
    # Внешняя чёрная скруглённая рамка
    for x in range(x0 + 1, x1):
        px(im, x, y0, BLACK_EDGE)
        px(im, x, y1, BLACK_EDGE)
    for y in range(y0 + 1, y1):
        px(im, x0, y, BLACK_EDGE)
        px(im, x1, y, BLACK_EDGE)
    # Светлая кромка (верх/лево), тёмная (низ/право)
    rect(im, x0 + 1, y0 + 1, x1 - 2, y0 + 2, LIGHT)
    rect(im, x0 + 1, y0 + 1, x0 + 2, y1 - 2, LIGHT)
    rect(im, x0 + 2, y1 - 2, x1 - 1, y1 - 1, (85, 85, 85, 255))
    rect(im, x1 - 2, y0 + 2, x1 - 1, y1 - 1, (85, 85, 85, 255))


def slot(im, x, y, size=18):
    """Слот 18×18: тёмный верх/лево, светлый низ/право (координата — угол рамки)."""
    rect(im, x, y, x + size - 1, y + size - 1, SLOT_BG)
    # Тёмная кромка сверху и слева
    rect(im, x, y, x + size - 2, y, DARK)
    rect(im, x, y, x, y + size - 2, DARK)
    # Светлая снизу и справа
    rect(im, x + 1, y + size - 1, x + size - 1, y + size - 1, LIGHT)
    rect(im, x + size - 1, y + 1, x + size - 1, y + size - 1, LIGHT)


def main():
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    # Панель 176×166
    panel(im, 0, 0, 175, 165)

    # Слоты пресса: вход (43,21), бутылка (43,51), результат (115,25), жмых (115,55)
    slot(im, 43, 21)
    slot(im, 43, 51)
    slot(im, 115, 25)
    slot(im, 115, 55)

    # Контур пустой стрелки между входом и результатом (x=76..97, y=35..50)
    rect(im, 76, 38, 91, 44, (160, 160, 160, 255))
    for i in range(4):
        rect(im, 92 + i, 38 + i + 1, 92 + i, 43 - i - 1 + 2, (160, 160, 160, 255))
    rect(im, 77, 39, 91, 43, (120, 120, 120, 255))

    # Инвентарь игрока: 9×3 с (7,83) + hotbar (7,141)
    for row in range(3):
        for col in range(9):
            slot(im, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(im, 7 + col * 18, 141)

    # Спрайт заполненной стрелки на (176,0), 22×16
    for i in range(16):
        rect(im, 176, 3 + 0, 191, 9, ARROW)
    rect(im, 176, 3, 191, 4, ARROW_HI)
    for i in range(4):
        rect(im, 192 + i, 4 + i, 192 + i, 8 - i + 2, ARROW)
    px(im, 192, 4, ARROW_HI)

    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, "oil_press.png"))
    print("gui/container/oil_press")


if __name__ == "__main__":
    main()
