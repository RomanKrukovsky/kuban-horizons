"""Единая точка генерации всех текстур Kuban Horizons.

Запуск: python3 tools/texgen/generate_all.py
Детерминированно пересоздаёт все PNG в assets/kubanhorizons/textures.
"""
import gen_crops
import gen_entities
import gen_blocks
import gen_items
import gen_gui
import gen_manul
import gen_quadrupeds
import gen_small_fauna

if __name__ == "__main__":
    gen_crops.main()
    gen_blocks.main()
    gen_items.main()
    gen_gui.main()
    gen_entities.main()
    gen_manul.main()
    # Мелкая фауна и четвероногие раньше сюда не попадали: их PNG оставались
    # от ручных прогонов модулей, и «пересоздать всё» их не касалось.
    gen_small_fauna.main()
    gen_quadrupeds.main()
    print("Готово.")
