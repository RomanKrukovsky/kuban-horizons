package dev.romankrukovsky.kubanhorizons.trade;

import com.google.common.collect.ImmutableSet;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.registry.KHBlocks;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Региональные профессии поселенцев.
 *
 * <p>Каждая профессия привязана к <b>уже существующему</b> рабочему блоку
 * мода. Профессия без реального рабочего места недостижима в игре, поэтому
 * профессии из CONTENT_BIBLE §11, у которых нужного блока пока нет
 * (сыровар — чан, пчеловод — пасека, торговец — прилавок), сознательно не
 * заводятся: пустая регистрация выглядела бы готовой функцией и скрывала бы
 * отсутствие содержимого.</p>
 *
 * <p><b>Три условия достижимости.</b> Профессия работает только когда
 * выполнены все три, и каждое из них — отдельный способ всё сломать:</p>
 * <ol>
 *   <li>POI покрывает <i>все</i> состояния блока
 *       ({@code getPossibleStates()}). Взять только
 *       {@code defaultBlockState()} значит потерять повёрнутые варианты: блок,
 *       поставленный лицом на юг, перестал бы быть рабочим местом.</li>
 *   <li>POI лежит в теге {@code minecraft:acquirable_job_site} — см.
 *       {@code KHPoiTypeTagsProvider}. Вне тега житель POI не видит вовсе.</li>
 *   <li>Предикат {@code heldJobSite} указывает на этот же POI: по нему
 *       {@code AssignProfessionFromJobSite} находит профессию по занятому
 *       блоку. Ошибка здесь даёт жителя, который занял верстак и остался без
 *       работы.</li>
 * </ol>
 *
 * <p>Один блок принадлежит ровно одному POI: ванильный
 * {@code PoiTypes.registerBlockStates} падает с
 * {@code IllegalStateException} на состоянии, заявленном дважды. Поэтому
 * маслопресс остаётся за маслоделом, и новые профессии берут только
 * свободные блоки.</p>
 *
 * <p>Сделки — data-driven через datapack-реестры {@code villager_trade} и
 * {@code trade_set} (см. {@link KHTrades}).</p>
 */
public final class KHProfessions {
    private static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, KubanHorizons.MOD_ID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, KubanHorizons.MOD_ID);

    public static final ResourceKey<PoiType> OIL_PRESS_POI_KEY = poiKey("oil_press");
    public static final ResourceKey<PoiType> HAND_MILL_POI_KEY = poiKey("hand_mill");
    public static final ResourceKey<PoiType> DRYING_RACK_POI_KEY = poiKey("drying_rack");
    public static final ResourceKey<PoiType> GRAPE_TRELLIS_POI_KEY = poiKey("grape_trellis");
    public static final ResourceKey<PoiType> SMOKEHOUSE_POI_KEY = poiKey("smokehouse");

    /** Ключи наборов сделок маслодела (datapack-реестр trade_set). */
    public static final ResourceKey<TradeSet> OIL_PRESSER_TRADES_1 = tradeSetKey("oil_presser_level_1");
    public static final ResourceKey<TradeSet> OIL_PRESSER_TRADES_2 = tradeSetKey("oil_presser_level_2");

    /** Ключи наборов сделок мельника. */
    public static final ResourceKey<TradeSet> MILLER_TRADES_1 = tradeSetKey("miller_level_1");
    public static final ResourceKey<TradeSet> MILLER_TRADES_2 = tradeSetKey("miller_level_2");

    /** Ключи наборов сделок чайного мастера. */
    public static final ResourceKey<TradeSet> TEA_MASTER_TRADES_1 = tradeSetKey("tea_master_level_1");
    public static final ResourceKey<TradeSet> TEA_MASTER_TRADES_2 = tradeSetKey("tea_master_level_2");

    /** Ключи наборов сделок виноградаря. */
    public static final ResourceKey<TradeSet> VINTNER_TRADES_1 = tradeSetKey("vintner_level_1");
    public static final ResourceKey<TradeSet> VINTNER_TRADES_2 = tradeSetKey("vintner_level_2");

    /** Ключи наборов сделок рыбака. */
    public static final ResourceKey<TradeSet> FISHERMAN_TRADES_1 = tradeSetKey("fisherman_level_1");
    public static final ResourceKey<TradeSet> FISHERMAN_TRADES_2 = tradeSetKey("fisherman_level_2");

    /** POI маслодела: все состояния блока маслопресса. */
    public static final Holder<PoiType> OIL_PRESS_POI =
            jobSite("oil_press", () -> KHBlocks.OIL_PRESS.get());

    /** POI мельника: жёрнов ручной мельницы. */
    public static final Holder<PoiType> HAND_MILL_POI =
            jobSite("hand_mill", () -> KHBlocks.HAND_MILL.get());

    /** POI чайного мастера: сушильная рама (у неё 4 поворота — важно). */
    public static final Holder<PoiType> DRYING_RACK_POI =
            jobSite("drying_rack", () -> KHBlocks.DRYING_RACK.get());

    /** POI виноградаря: шпалера (все 5 стадий, включая пустую). */
    public static final Holder<PoiType> GRAPE_TRELLIS_POI =
            jobSite("grape_trellis", () -> KHBlocks.GRAPE_TRELLIS.get());

    /** POI рыбака: коптильня (4 поворота × 2 состояния огня). */
    public static final Holder<PoiType> SMOKEHOUSE_POI =
            jobSite("smokehouse", () -> KHBlocks.SMOKEHOUSE.get());

    /** Профессия «маслодел» — маслопресс. */
    public static final Holder<VillagerProfession> OIL_PRESSER =
            profession("oil_presser", OIL_PRESS_POI_KEY, SoundEvents.VILLAGER_WORK_FARMER,
                    OIL_PRESSER_TRADES_1, OIL_PRESSER_TRADES_2);

    /**
     * Профессия «мельник» — ручная мельница.
     *
     * <p>Голос взят у ванильного фермера: мельница — зерновое ремесло, и
     * отдельного звука для неё в игре нет.</p>
     */
    public static final Holder<VillagerProfession> MILLER =
            profession("miller", HAND_MILL_POI_KEY, SoundEvents.VILLAGER_WORK_FARMER,
                    MILLER_TRADES_1, MILLER_TRADES_2);

    /**
     * Профессия «чайный мастер» — сушильная рама.
     *
     * <p>Рама сушит и лист, и фрукты, поэтому мастер торгует обеими ветками
     * сушки, а не только чаем.</p>
     */
    public static final Holder<VillagerProfession> TEA_MASTER =
            profession("tea_master", DRYING_RACK_POI_KEY, SoundEvents.VILLAGER_WORK_LIBRARIAN,
                    TEA_MASTER_TRADES_1, TEA_MASTER_TRADES_2);

    /**
     * Профессия «виноградарь» — шпалера.
     *
     * <p>Шпалера — определяющий блок виноградника, поэтому рабочее место
     * именно она. {@code maxTickets = 1} не даёт нескольким жителям занять
     * одну шпалеру, так что виноградник из десятков шпалер не превращается в
     * очередь.</p>
     */
    public static final Holder<VillagerProfession> VINTNER =
            profession("vintner", GRAPE_TRELLIS_POI_KEY, SoundEvents.VILLAGER_WORK_FARMER,
                    VINTNER_TRADES_1, VINTNER_TRADES_2);

    /**
     * Профессия «рыбак» — коптильня.
     *
     * <p>Рабочее место — не вода, а коптильня: рыбак ценен переработкой, и
     * именно она даёт ему товар. Ванильный {@code minecraft:fisherman} живёт
     * на бочке и не конфликтует с этим POI.</p>
     */
    public static final Holder<VillagerProfession> FISHERMAN =
            profession("fisherman", SMOKEHOUSE_POI_KEY, SoundEvents.VILLAGER_WORK_FISHERMAN,
                    FISHERMAN_TRADES_1, FISHERMAN_TRADES_2);

    private KHProfessions() {
    }

    private static ResourceKey<PoiType> poiKey(String name) {
        return ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, KHIds.of(name));
    }

    private static ResourceKey<TradeSet> tradeSetKey(String name) {
        return ResourceKey.create(Registries.TRADE_SET, KHIds.of(name));
    }

    /**
     * POI рабочего места по блоку.
     *
     * <p>{@code getPossibleStates()} — обязательно: иначе POI промахнётся по
     * повёрнутым и «горящим» вариантам блока. {@code maxTickets = 1} — один
     * работник на станок, {@code validRange = 1} — как у ванильных рабочих
     * мест.</p>
     */
    private static Holder<PoiType> jobSite(String name,
                                          Supplier<net.minecraft.world.level.block.Block> block) {
        return POI_TYPES.register(name,
                () -> new PoiType(
                        Set.copyOf(block.get().getStateDefinition().getPossibleStates()),
                        1, 1));
    }

    /**
     * Профессия с двумя уровнями сделок.
     *
     * <p>{@code heldJobSite} и {@code acquirableJobSite} указывают на один и
     * тот же POI: житель занимает станок и по нему же получает профессию.</p>
     */
    private static Holder<VillagerProfession> profession(String name,
                                                         ResourceKey<PoiType> poi,
                                                         net.minecraft.sounds.SoundEvent workSound,
                                                         ResourceKey<TradeSet> level1,
                                                         ResourceKey<TradeSet> level2) {
        return PROFESSIONS.register(name,
                () -> new VillagerProfession(
                        Component.translatable("entity.minecraft.villager." + KubanHorizons.MOD_ID
                                + "." + name),
                        holder -> holder.is(poi),
                        holder -> holder.is(poi),
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        workSound,
                        Int2ObjectMap.ofEntries(
                                Int2ObjectMap.entry(1, level1),
                                Int2ObjectMap.entry(2, level2))));
    }

    public static void register(IEventBus modEventBus) {
        POI_TYPES.register(modEventBus);
        PROFESSIONS.register(modEventBus);
    }
}
