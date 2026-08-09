package dev.romankrukovsky.kubanhorizons.trade;

import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.Optional;

/**
 * Сделки профессии «маслодел» (datapack-реестры villager_trade + trade_set).
 *
 * <p>Экономика без циклов бесконечной выгоды: закупка сырья у игрока
 * дешевле, чем продажа переработанного продукта (см. GAME_DESIGN.md §9).</p>
 */
public final class KHTrades {
    // --- villager_trade ---
    public static final ResourceKey<VillagerTrade> OP1_SEEDS_TO_EMERALD =
            tradeKey("oil_presser/seeds_to_emerald");
    public static final ResourceKey<VillagerTrade> OP1_HEAD_TO_EMERALD =
            tradeKey("oil_presser/head_to_emerald");
    public static final ResourceKey<VillagerTrade> OP1_EMERALD_TO_ROASTED =
            tradeKey("oil_presser/emerald_to_roasted");
    public static final ResourceKey<VillagerTrade> OP2_EMERALD_TO_OIL =
            tradeKey("oil_presser/emerald_to_oil");
    public static final ResourceKey<VillagerTrade> OP2_OILCAKE_TO_EMERALD =
            tradeKey("oil_presser/oil_cake_to_emerald");
    /**
     * Щенок овчарки за эмеральды.
     *
     * <p>Единственный путь получить овчарку в выживании. Дикого спавна у неё
     * нет и быть не должно: это не фауна, а хозяйственная собака, и брать её
     * логично у людей. Без этой сделки контр-приём против кабана и нутрии был
     * недостижим — давление на ферму оставалось безответным.</p>
     */
    public static final ResourceKey<VillagerTrade> OP2_EMERALD_TO_SHEPHERD_EGG =
            tradeKey("oil_presser/emerald_to_shepherd_egg");

    private KHTrades() {
    }

    private static ResourceKey<VillagerTrade> tradeKey(String name) {
        return ResourceKey.create(Registries.VILLAGER_TRADE, KHIds.of(name));
    }

    public static void bootstrapTrades(BootstrapContext<VillagerTrade> context) {
        // Уровень 1: скупка сырья, продажа перекуса.
        context.register(OP1_SEEDS_TO_EMERALD, new VillagerTrade(
                new TradeCost(KHItems.SUNFLOWER_SEEDS.get(), 24),
                new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F,
                Optional.empty(), List.of()));
        context.register(OP1_HEAD_TO_EMERALD, new VillagerTrade(
                new TradeCost(KHItems.SUNFLOWER_HEAD.get(), 6),
                new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F,
                Optional.empty(), List.of()));
        context.register(OP1_EMERALD_TO_ROASTED, new VillagerTrade(
                new TradeCost(Items.EMERALD, 1),
                new ItemStackTemplate(KHItems.ROASTED_SUNFLOWER_SEEDS.get(), 6), 12, 1, 0.05F,
                Optional.empty(), List.of()));

        // Уровень 2: готовый продукт дороже эквивалента сырья.
        context.register(OP2_EMERALD_TO_OIL, new VillagerTrade(
                new TradeCost(Items.EMERALD, 3),
                new ItemStackTemplate(KHItems.SUNFLOWER_OIL.get()), 8, 5, 0.05F,
                Optional.empty(), List.of()));
        context.register(OP2_OILCAKE_TO_EMERALD, new VillagerTrade(
                new TradeCost(KHItems.OIL_CAKE.get(), 12),
                new ItemStackTemplate(Items.EMERALD), 12, 3, 0.05F,
                Optional.empty(), List.of()));

        // Щенок дорог намеренно: он открывает всю защиту хозяйства, поэтому
        // должен стоить как вложение, а не как перекус. maxUses = 2, чтобы
        // пара для разведения была достижима, но не бесконечна.
        context.register(OP2_EMERALD_TO_SHEPHERD_EGG, new VillagerTrade(
                new TradeCost(Items.EMERALD, 22),
                new ItemStackTemplate(KHItems.CAUCASIAN_SHEPHERD_SPAWN_EGG.get()), 2, 12, 0.05F,
                Optional.empty(), List.of()));
    }

    public static void bootstrapTradeSets(BootstrapContext<TradeSet> context) {
        var trades = context.lookup(Registries.VILLAGER_TRADE);
        context.register(KHProfessions.OIL_PRESSER_TRADES_1, new TradeSet(
                HolderSet.direct(
                        trades.getOrThrow(OP1_SEEDS_TO_EMERALD),
                        trades.getOrThrow(OP1_HEAD_TO_EMERALD),
                        trades.getOrThrow(OP1_EMERALD_TO_ROASTED)),
                ConstantValue.exactly(2.0F), false, Optional.empty()));
        context.register(KHProfessions.OIL_PRESSER_TRADES_2, new TradeSet(
                HolderSet.direct(
                        trades.getOrThrow(OP2_EMERALD_TO_OIL),
                        trades.getOrThrow(OP2_OILCAKE_TO_EMERALD),
                        trades.getOrThrow(OP2_EMERALD_TO_SHEPHERD_EGG)),
                ConstantValue.exactly(2.0F), false, Optional.empty()));
    }
}
