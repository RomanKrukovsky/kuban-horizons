package dev.romankrukovsky.kubanhorizons.genie.entity;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Движок изменения склонностей и характера NPC (NPC Personality Modifier Engine). */
public final class NPCPersonalityEngine {
    private NPCPersonalityEngine() {
    }

    public static void modifyPersonality(ServerLevel level, LivingEntity target, String trait) {
        String normalized = trait == null ? "" : trait.toLowerCase(java.util.Locale.ROOT);
        target.getPersistentData().putString("KubanGeniePersonality", normalized);
        var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (normalized.contains("active") || normalized.contains("деятель")) {
                speed.setBaseValue(Math.min(1.0D, speed.getBaseValue() * 1.25D));
            } else if (normalized.contains("calm") || normalized.contains("спокой")) {
                speed.setBaseValue(Math.max(0.01D, speed.getBaseValue() * 0.8D));
            }
        }
        var attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && (normalized.contains("peace") || normalized.contains("мир"))) {
            attack.setBaseValue(0.0D);
        }
        MagicalSignature.cast(level, target.position());
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.0D, target.getZ(),
                15, 0.4D, 0.5D, 0.4D, 0.05D);
    }
}
