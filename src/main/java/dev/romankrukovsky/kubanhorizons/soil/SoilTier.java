package dev.romankrukovsky.kubanhorizons.soil;

import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.worldgen.KHBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Три яруса почвы: обычная земля &lt; суглинок степи &lt; чернозём.
 *
 * <p>Ярусы объявлены в GAME_DESIGN.md §4, а числа — в TECH_SPEC.md §3:
 * «farmland 40, степной суглинок 60, чернозём 85». Здесь они и живут.</p>
 *
 * <h2>Почему у суглинка нет своего блока</h2>
 * <p>Средний ярус — свойство <b>места</b>, а не блока: суглинок это подпочва
 * кубанской степи, и грядка внутри степного биома стоит на нём по факту
 * географии. Отдельный блок «суглинок» пришлось бы искать, носить и класть,
 * то есть повторил бы роль чернозёма и обесценил бы её. Так у трёх ярусов
 * оказывается три разных источника: дом (40) — любая земля, степь (60) —
 * награда за переезд в регион, чернозём (85) — награда за поиск. Это ровно
 * второй шаг главного цикла, и он читается без единого лишнего блока.</p>
 *
 * <h2>Почему бонус именно такой</h2>
 * <p>85 против 40 даёт множитель роста ≈1.45 против 1.0 — заметно, но не
 * ломает игру: потолок шкалы (100 → 1.6) остался прежним и по-прежнему
 * берётся только вложениями (компост, ил). Чернозём не поднимает потолок, он
 * бесплатно подводит к нему близко. Ни одного нового штрафа при этом не
 * появилось: обычная грядка как была 40, так и осталась. Это и есть
 * «штрафы мягкие, бонусы заметные» (GAME_DESIGN.md §3).</p>
 *
 * <p>Истощение при этом общее и не ослаблено: −12 за повторную культуру
 * уводят чернозём с 85 ниже сорока за четыре одинаковых сбора, то есть
 * монокультура на чернозёме кончается <b>хуже</b> обычной грядки. Севооборот
 * на хорошей земле важнее, чем на плохой, а не наоборот.</p>
 */
public enum SoilTier {
    /** Обычная земля: базовая грядка где угодно. */
    PLAIN(40, "plain"),
    /** Суглинок степи: грядка внутри кубанской степи. */
    STEPPE_LOAM(60, "loam"),
    /** Чернозём: найденный и перенесённый блок. */
    CHERNOZEM(85, "chernozem");

    private final int baseFertility;
    private final String key;

    SoilTier(int baseFertility, String key) {
        this.baseFertility = baseFertility;
        this.key = key;
    }

    /** Базовое плодородие яруса (не хранится в данных, выводится из места). */
    public int baseFertility() {
        return baseFertility;
    }

    /** Суффикс ключа перевода для щупа. */
    public String translationKey() {
        return "message.kubanhorizons.soil_probe.tier." + key;
    }

    /**
     * Ярус грядки в конкретной точке мира.
     *
     * <p>Порядок проверок важен: блок сильнее биома. Принесённый в лес
     * чернозём обязан остаться чернозёмом, иначе перенос лишался бы смысла.</p>
     */
    public static SoilTier at(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(KHBlocks.CHERNOZEM_FARMLAND.get()) || state.is(KHBlocks.CHERNOZEM.get())) {
            return CHERNOZEM;
        }
        // Степной суглинок: подпочва биома. Проверяется по биому именно
        // позиции грядки, а не по игроку — грядка на краю степи получает
        // ярус того места, где лежит.
        if (level.getBiome(pos).is(KHBiomes.KUBAN_STEPPE)) {
            return STEPPE_LOAM;
        }
        return PLAIN;
    }
}
