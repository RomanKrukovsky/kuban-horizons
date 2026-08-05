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

/**
 * Региональные профессии поселенцев.
 *
 * <p>Первая профессия — <b>маслодел</b> ({@code oil_presser}): рабочее
 * место — маслопресс. Сделки — data-driven через datapack-реестр
 * {@code trade_set} (см. {@code KHTradeSets} в datagen).</p>
 */
public final class KHProfessions {
    private static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, KubanHorizons.MOD_ID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, KubanHorizons.MOD_ID);

    public static final ResourceKey<PoiType> OIL_PRESS_POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, KHIds.of("oil_press"));

    /** Ключи наборов сделок маслодела (datapack-реестр trade_set). */
    public static final ResourceKey<TradeSet> OIL_PRESSER_TRADES_1 =
            ResourceKey.create(Registries.TRADE_SET, KHIds.of("oil_presser_level_1"));
    public static final ResourceKey<TradeSet> OIL_PRESSER_TRADES_2 =
            ResourceKey.create(Registries.TRADE_SET, KHIds.of("oil_presser_level_2"));

    /** POI маслодела: все состояния блока маслопресса. */
    public static final Holder<PoiType> OIL_PRESS_POI =
            POI_TYPES.register("oil_press",
                    () -> new PoiType(
                            Set.copyOf(KHBlocks.OIL_PRESS.get().getStateDefinition().getPossibleStates()),
                            1, 1));

    /** Профессия «маслодел». */
    public static final Holder<VillagerProfession> OIL_PRESSER =
            PROFESSIONS.register("oil_presser",
                    () -> new VillagerProfession(
                            Component.translatable("entity.minecraft.villager.kubanhorizons.oil_presser"),
                            holder -> holder.is(OIL_PRESS_POI_KEY),
                            holder -> holder.is(OIL_PRESS_POI_KEY),
                            ImmutableSet.of(),
                            ImmutableSet.of(),
                            SoundEvents.VILLAGER_WORK_FARMER,
                            Int2ObjectMap.ofEntries(
                                    Int2ObjectMap.entry(1, OIL_PRESSER_TRADES_1),
                                    Int2ObjectMap.entry(2, OIL_PRESSER_TRADES_2))));

    private KHProfessions() {
    }

    public static void register(IEventBus modEventBus) {
        POI_TYPES.register(modEventBus);
        PROFESSIONS.register(modEventBus);
    }
}
