"""Полная сборка ассетов Кубанской джиннии.

Порядок здесь принципиален: упаковщик атласа раскладывает UV заново при
любом изменении набора кубоидов, поэтому текстуру нельзя рисовать раньше
геометрии — узор уедет на чужие грани. Этот сценарий фиксирует порядок,
чтобы его нельзя было нарушить по забывчивости.

Запуск: ``python3 tools/texgen/gen_genie_all.py``
"""
import subprocess
import sys
import os

HERE = os.path.dirname(os.path.abspath(__file__))
# 1) геометрия задаёт UV → 2) текстура рисует по этим UV → 3) анимации
# сверяются с именами костей → 4) проверка всего вместе.
STEPS = [
    ("геометрия", "gen_genie_model.py"),
    ("текстура", "gen_genie_texture.py"),
    ("анимации", "gen_genie_anim.py"),
    ("проверка", "check_genie.py"),
]


def main():
    for label, script in STEPS:
        print("== %s (%s)" % (label, script))
        res = subprocess.run([sys.executable, os.path.join(HERE, script)],
                             cwd=HERE)
        if res.returncode != 0:
            print("\nсборка остановлена на шаге «%s»" % label)
            return res.returncode
        print()
    print("джинния собрана без ошибок")
    return 0


if __name__ == "__main__":
    sys.exit(main())
