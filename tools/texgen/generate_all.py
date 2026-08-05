"""Единая точка генерации всех текстур Kuban Horizons.

Запуск: python3 tools/texgen/generate_all.py
Детерминированно пересоздаёт все PNG в assets/kubanhorizons/textures.
"""
import gen_crops
import gen_blocks
import gen_items
import gen_gui

if __name__ == "__main__":
    gen_crops.main()
    gen_blocks.main()
    gen_items.main()
    gen_gui.main()
    print("Готово.")
