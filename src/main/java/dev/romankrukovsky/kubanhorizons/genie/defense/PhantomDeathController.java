package dev.romankrukovsky.kubanhorizons.genie.defense;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.state.ManifestationState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Handles the "phantom death" visual effect when the genie is hit hard.
 *
 * <p>Goal: Make it feel like dispersion / temporary retreat, NOT death.
 * The original implementation scaled the model to 0 which looked like dying.
 */
public final class PhantomDeathController {

    private PhantomDeathController() {}

    public static void triggerPhantomDispersion(KubanGenie genie, ServerLevel level, Player attacker) {
        // Change state to DISPERSED instead of fake death
        genie.getWishborneState().setCurrentState(ManifestationState.DISPERSED);

        // Play a dispersion effect (smoke/particles) instead of scale-to-zero animation
        // TODO: Trigger GeckoLib animation "disperse" instead of "despawn"

        // Teleport behind attacker (keep the fun part)
        if (attacker != null) {
            genie.teleportTo(attacker.getX() - 1.5, attacker.getY(), attacker.getZ() - 1.5);
        }

        // After a short time, allow reformation (this would be handled by a tick task)
        // For now, just set state back after some time or on command
    }
}
