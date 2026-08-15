package dev.romankrukovsky.kubanhorizons.genie.visual;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.state.ManifestationState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Visual effects tied to ManifestationState changes.
 */
public final class GenieManifestationEffects {

    private GenieManifestationEffects() {}

    public static void onStateChanged(KubanGenie genie, ManifestationState oldState, ManifestationState newState) {
        if (!(genie.level() instanceof ServerLevel level)) return;

        switch (newState) {
            case DISPERSED -> {
                // Smoke + portal particles for dispersion
                level.sendParticles(ParticleTypes.LARGE_SMOKE, genie.getX(), genie.getY() + 1.0, genie.getZ(),
                        40, 0.4, 0.8, 0.4, 0.02);
                level.sendParticles(ParticleTypes.PORTAL, genie.getX(), genie.getY() + 1.2, genie.getZ(),
                        30, 0.3, 0.6, 0.3, 0.05);
            }
            case SEALED -> {
                level.sendParticles(ParticleTypes.ENCHANT, genie.getX(), genie.getY() + 1.0, genie.getZ(),
                        60, 0.5, 1.0, 0.5, 0.1);
            }
            case BANISHED -> {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, genie.getX(), genie.getY() + 1.0, genie.getZ(),
                        3, 0.0, 0.0, 0.0, 0.0);
            }
            case MANIFESTED -> {
                // Re-materialization sparkle
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, genie.getX(), genie.getY() + 1.0, genie.getZ(),
                        25, 0.4, 0.6, 0.4, 0.05);
            }
        }
    }
}
