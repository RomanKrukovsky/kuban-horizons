package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

/**
 * Ночные вылазки манула: рыба у торговцев и любопытство к пасеке.
 *
 * <h2>Кража рыбы на рынке</h2>
 *
 * <p>Манул ночью подходит к торговцу и уносит рыбу у него из тюка. Это не
 * наказание игроку: рыба исчезает у <em>жителя</em>, а зверь получает
 * доверие — так «вор» превращается в способ познакомиться, ради которого
 * стоит завести торговца рядом с двором.</p>
 *
 * <p>Реализовано событием уровня, а не {@code Goal}: цели регистрируются в
 * {@code Manul.registerGoals}, а этот файл принадлежит другому слою.
 * Событие даёт тот же наблюдаемый результат без правок чужого класса.</p>
 *
 * <h2>Почему только ночью и только рядом</h2>
 *
 * <p>Ночь — {@code isDarkOutside}: манул сумеречный зверь, и днём такая
 * вылазка выглядела бы наглостью, а не повадкой. Радиус
 * {@link #STEAL_RADIUS} мал, поэтому кража требует, чтобы зверь и торговец
 * действительно оказались рядом: событие остаётся сценой, которую игрок
 * может увидеть, а не фоновой утечкой товара.</p>
 *
 * <h2>Пасека</h2>
 *
 * <p>Механического эффекта у пасеки нет и не задумано: мёд манул не крадёт и
 * пчёл не трогает — это было бы вредительство, которого у него по замыслу
 * нет, и оно дублировало бы роль нутрии и кабана. Улей остаётся местом, где
 * зверя стоит искать, и это проверяемо через
 * {@link #hasApiaryNearby}; сидеть на самом улье
 * {@link ManulLoafGoal} не даёт (улья нет в его списке поверхностей), и
 * добавлять его туда — правка чужого файла, не входящая в этот слой.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class ManulNightRaids {
    /** Как часто проверяются ночные вылазки (в тиках). */
    private static final int CHECK_INTERVAL = 200;

    /** Радиус, в котором манул может дотянуться до тюка торговца. */
    private static final double STEAL_RADIUS = 3.0D;

    /**
     * Шанс кражи за проверку (1 к N).
     *
     * <p>Редкость обязательна: при проверке раз в 10 секунд без неё зверь
     * вычистил бы тюк торговца за минуту, и сцена превратилась бы в баг.</p>
     */
    private static final int STEAL_CHANCE = 5;

    /** Сколько доверия даёт удачная кража. */
    private static final int TRUST_GAIN = 1;

    private ManulNightRaids() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL != 0L) {
            return;
        }
        // Днём вылазок нет: манул сумеречный.
        if (!level.isDarkOutside()) {
            return;
        }
        // Проход только по загруженным манулам рядом с игроками: пустой мир
        // не должен стоить ни одного тика.
        for (var player : level.players()) {
            AABB area = player.getBoundingBox().inflate(32.0D);
            for (Manul manul : level.getEntitiesOfClass(Manul.class, area, Manul::isAlive)) {
                tryStealFish(level, manul);
            }
        }
    }

    /**
     * Пробует украсть рыбу у ближайшего торговца.
     *
     * <p>Крадёт ровно одну рыбу за раз и только из тюка: тюк торговца —
     * ванильный {@code SimpleContainer}, и убыль оттуда видна игроку в
     * предложениях сделки.</p>
     */
    private static void tryStealFish(ServerLevel level, Manul manul) {
        if (level.getRandom().nextInt(STEAL_CHANCE) != 0) {
            return;
        }
        stealFishNow(level, manul);
    }

    /**
     * Одна попытка кражи без проверки шанса и времени суток.
     *
     * <p>Существует для GameTest: шанс {@link #STEAL_CHANCE} и ночь делают
     * событие редким, и тест, ждущий совпадения обоих условий, был бы
     * нестабильным. Проверять нужно сам путь «зверь → тюк → доверие», а
     * редкость задаётся в {@link #tryStealFish} и проверке ночи выше.</p>
     */
    public static boolean stealFishNow(ServerLevel level, Manul manul) {
        AABB reach = manul.getBoundingBox().inflate(STEAL_RADIUS);
        List<AbstractVillager> traders = level.getEntitiesOfClass(AbstractVillager.class, reach,
                trader -> trader.isAlive() && !trader.isBaby());
        for (AbstractVillager trader : traders) {
            SimpleContainer bag = trader.getInventory();
            for (int slot = 0; slot < bag.getContainerSize(); slot++) {
                ItemStack stack = bag.getItem(slot);
                if (stack.isEmpty() || !stack.is(ItemTags.FISHES)) {
                    continue;
                }
                bag.removeItem(slot, 1);
                // Зверь доволен: доверие растёт, даже если игрок не кормил его
                // сам. Это и делает торговца рядом с двором осмысленным.
                manul.adjustTrust(TRUST_GAIN);
                level.playSound(null, manul.blockPosition(), SoundEvents.ITEM_PICKUP,
                        SoundSource.NEUTRAL, 0.6F, 1.4F);
                // Торговец замечает потерю — сцена читается со стороны.
                trader.setUnhappyCounter(40);
                return true;
            }
        }
        return false;
    }

    /**
     * Есть ли улей рядом — место, где манула стоит искать.
     *
     * <p>Используется описанием мест обитания и тестом: пасека объявлена
     * частью его мира, и проверка должна быть выполнимой, а не только
     * упомянутой в тексте.</p>
     */
    public static boolean hasApiaryNearby(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(BlockTags.BEEHIVES)) {
                return true;
            }
        }
        return false;
    }
}
