package dev.romankrukovsky.kubanhorizons.genie.visual;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.WishborneState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Визуальные эффекты нефизического состояния Wishborne-сущности.
 *
 * <p>Пока аватар рассеян, запечатан или изгнан, вокруг него идут характерные
 * частицы — так игрок видит состояние без вкладки и команд. Вызывается из
 * серверного тика джиннии.</p>
 */
public final class GenieManifestationEffects {

    private GenieManifestationEffects() {
    }

    /** Периодическая визуализация состояния; дёшево, раз в 20 тиков. */
    public static void tickStateEffects(KubanGenie genie, ServerLevel level) {
        switch (genie.getWishborneState().presence()) {
            case DISPERSED -> {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                        4, 0.4D, 0.8D, 0.4D, 0.02D);
                level.sendParticles(ParticleTypes.PORTAL, genie.getX(), genie.getY() + 1.2D, genie.getZ(),
                        3, 0.3D, 0.6D, 0.3D, 0.05D);
            }
            case SEALED -> {
                level.sendParticles(ParticleTypes.ENCHANT, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                        6, 0.5D, 1.0D, 0.5D, 0.1D);
            }
            case BANISHED -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                        3, 0.3D, 0.5D, 0.3D, 0.05D);
            }
            case MANIFESTED -> {
            }
        }
    }

    /** Одноразовый всплеск при возврате в проявленное состояние. */
    public static void onRemanifest(KubanGenie genie, ServerLevel level) {
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                25, 0.4D, 0.6D, 0.4D, 0.05D);
    }
}