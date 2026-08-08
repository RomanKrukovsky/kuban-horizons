package dev.romankrukovsky.kubanhorizons.genie.entity;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Движок изменения склонностей и характера NPC (NPC Personality Modifier Engine). */
public final class NPCPersonalityEngine {
    private NPCPersonalityEngine() {
    }

    public static void modifyPersonality(ServerLevel level, LivingEntity target, String trait) {
        MagicalSignature.cast(level, target.position());
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.0D, target.getZ(),
                15, 0.4D, 0.5D, 0.4D, 0.05D);
    }
}
