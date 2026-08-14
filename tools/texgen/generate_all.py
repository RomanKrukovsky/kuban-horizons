"""Единая точка генерации всех текстур Kuban Horizons.

Запуск: python3 tools/texgen/generate_all.py
Детерминированно пересоздаёт все PNG в assets/kubanhorizons/textures.
"""
import sys

import gen_crops
import gen_entities
import gen_blocks
import gen_items
import gen_gui
import gen_manul
import gen_quadrupeds
import gen_small_fauna

# Джинния и глечик собираются отдельными модулями: у них, в отличие от
# остальных текстур, геометрия и развёртка генерируются вместе, и порядок
# внутри сборки обязателен (см. gen_genie_all.py).
import gen_genie_model
import gen_genie_texture
import gen_genie_anim
import gen_jug
import gen_uv_map
import check_genie

if __name__ == "__main__":
    gen_crops.main()
    gen_blocks.main()
    gen_items.main()
    gen_gui.main()
    gen_entities.main()
    gen_manul.main()
    gen_small_fauna.main()
    gen_quadrupeds.main()

    gen_genie_model.main()
    gen_genie_texture.main()
    gen_genie_anim.main()
    gen_jug.main()
    gen_uv_map.main()
    if check_genie.main() != 0:
        print("Модель джиннии не прошла проверку.")
        sys.exit(1)
    print("Готово.")
