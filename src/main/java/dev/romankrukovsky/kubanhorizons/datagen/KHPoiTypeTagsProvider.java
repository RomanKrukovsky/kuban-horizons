package dev.romankrukovsky.kubanhorizons.datagen;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.trade.KHProfessions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.concurrent.CompletableFuture;

/**
 * Рабочие места мода в ванильных тегах POI.
 *
 * <p><b>Зачем этот провайдер существует.</b> Регистрация POI-типа и профессии
 * ещё не делает работу достижимой. Безработный житель ищет рабочее место
 * предикатом {@code VillagerProfession.ALL_ACQUIRABLE_JOBS}, а он устроен
 * ровно так: {@code holder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE)}. Тот же тег
 * фильтрует память {@code POTENTIAL_JOB_SITE} в {@code Villager.POI_MEMORIES}.
 * Поэтому POI вне тега {@code minecraft:acquirable_job_site} житель не видит
 * вообще: он не подойдёт к блоку, не займёт его и не получит профессию —
 * {@code AssignProfessionFromJobSite} просто никогда не сработает.</p>
 *
 * <p>До появления этого файла тег в моде не генерировался ни одной строкой,
 * поэтому маслодел был мёртвой профессией: блок ставился, сделки лежали в
 * реестре, перевод был на месте — и ни один житель не мог стать маслоделом.
 * Это тот же класс промаха, что разделочный стол без рецептов: регистрация
 * выглядела полной, а путь в игру отсутствовал.</p>
 *
 * <p>{@code VILLAGE} добавляется следом, потому что от него зависят вещи,
 * которые игрок считает частью поселения: границы деревни в
 * {@code PoiManager}, маршруты {@code MoveThroughVillageGoal} и выбор центра
 * рейда в {@code Raids}. Без него хутор из блоков мода не считался бы
 * деревней, хотя жители в нём работают.</p>
 */
final class KHPoiTypeTagsProvider extends TagsProvider<PoiType> {
    KHPoiTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.POINT_OF_INTEREST_TYPE, registries, KubanHorizons.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Список ведётся вместе с профессиями: новое рабочее место обязано
        // попасть сюда, иначе профессия недостижима (см. gametest
        // professions_are_reachable — он падает именно на этом).
        tag(PoiTypeTags.ACQUIRABLE_JOB_SITE)
                .add(KHProfessions.OIL_PRESS_POI_KEY)
                .add(KHProfessions.HAND_MILL_POI_KEY)
                .add(KHProfessions.DRYING_RACK_POI_KEY)
                .add(KHProfessions.GRAPE_TRELLIS_POI_KEY)
                .add(KHProfessions.SMOKEHOUSE_POI_KEY);
        tag(PoiTypeTags.VILLAGE)
                .add(KHProfessions.OIL_PRESS_POI_KEY)
                .add(KHProfessions.HAND_MILL_POI_KEY)
                .add(KHProfessions.DRYING_RACK_POI_KEY)
                .add(KHProfessions.GRAPE_TRELLIS_POI_KEY)
                .add(KHProfessions.SMOKEHOUSE_POI_KEY);
    }
}
