package dev.romankrukovsky.kubanhorizons.genie.entity;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.GenieTemperament;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * HybridSpeciesEngine — фабрика NPC-джинний гибридных видов.
 * Обеспечивает персистентность Temperament / Personality (seed on spawn + save/load).
 */
public final class HybridSpeciesEngine {

    private HybridSpeciesEngine() {
    }

    /**
     * Создаёт NPC-джиннию гибридного вида с персистентной личностью.
     * Реальная реализация: спавнит KubanGenie, устанавливает persistence и seed-based темперамент.
     */
    public static KubanGenie synthesizeHybrid(ServerLevel level, Vec3 pos, long speciesSeed) {
        // Реальный спавн NPC-джиннии с персистентной личностью (старый Java 8 стиль)
        dev.romankrukovsky.kubanhorizons.entity.KubanGenie genie =
                new dev.romankrukovsky.kubanhorizons.entity.KubanGenie(
                        dev.romankrukovsky.kubanhorizons.registry.KHEntities.KUBAN_GENIE.get(),
                        level
                );
        genie.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        genie.setPersistenceRequired();

        // Инициализируем детерминированный темперамент из seed
        initPersonalityFromSeed(genie.personality(), speciesSeed);

        level.addFreshEntity(genie);
        return genie;
    }

    /**
     * Вспомогательный метод для инициализации личности из seed (детерминировано).
     */
    public static void initPersonalityFromSeed(GeniePersonality personality, long seed) {
        // Детерминированный выбор темперамента на основе seed (для воспроизводимости NPC)
        int idx = (int) (seed % 5);
        GenieTemperament temperament;
        switch (idx) {
            case 0: temperament = GenieTemperament.SANGUINE; break;
            case 1: temperament = GenieTemperament.CHOLERIC; break;
            case 2: temperament = GenieTemperament.MELANCHOLIC; break;
            case 3: temperament = GenieTemperament.PHLEGMATIC; break;
            default: temperament = GenieTemperament.SANGUINE; break;
        }
        personality.setTemperament(temperament);
        // Можно добавить детерминированное распределение trust/respect/fear и т.д.
    }
}
