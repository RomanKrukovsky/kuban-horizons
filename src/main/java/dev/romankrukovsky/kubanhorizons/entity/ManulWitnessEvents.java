package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Манул видит, как игрок обходится с живностью, и делает выводы.
 *
 * <p>Это вторая половина механики доверия. Подношения его растят, а этот
 * слушатель — отнимает: зверь, на глазах которого игрок бьёт скот, перестаёт
 * ему верить. Без этого «не вреди окрестным животным» было бы правилом,
 * которое ничего не проверяет, а {@code Manul.witnessHarm} — методом, который
 * никто не вызывает.</p>
 *
 * <p>Слушатель, а не проверка внутри манула: событие приходит по факту урона,
 * тогда как манулу пришлось бы каждый тик обходить окрестных животных и
 * сравнивать их здоровье с прошлым. Событие дешевле и точнее — оно знает
 * виновника.</p>
 *
 * <p>Реагируют только те особи, которые находятся рядом с местом
 * происшествия ({@link Manul#nearby}): манул судит по тому, что видел сам, а
 * не по глобальной статистике игрока. Поэтому убийство свиньи в другом конце
 * мира доверие не портит.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class ManulWitnessEvents {
    private ManulWitnessEvents() {
    }

    /**
     * Смерть животного рядом с манулом снижает доверие свидетелей.
     *
     * <p>Смерть, а не каждый удар: иначе один бой с кабаном обнулил бы
     * многодневное знакомство, и доверие стало бы недостижимым. Здесь цена
     * ощутима, но соразмерна — {@code witnessHarm} снимает 3 очка при шкале в
     * 24, то есть примерно один день подношений.</p>
     */
    @SubscribeEvent
    public static void onAnimalDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        // Только мирная живность: за убитого кабана-вредителя или волка манул
        // игрока не осуждает — это защита хозяйства, а не жестокость.
        if (!(dead instanceof Animal) || dead instanceof Manul || dead instanceof WildBoar) {
            return;
        }
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        for (Manul witness : Manul.nearby(level, dead.blockPosition())) {
            witness.witnessHarm(dead, killer);
        }
    }
}
