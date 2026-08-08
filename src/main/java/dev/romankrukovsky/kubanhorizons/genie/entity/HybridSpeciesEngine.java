package dev.romankrukovsky.kubanhorizons.genie.entity;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Синтез гибридных видов существ и эволюционный дрейф (Hybrid Speciation & Evolution Engine). */
public final class HybridSpeciesEngine {
    private HybridSpeciesEngine() {
    }

    public static LivingEntity synthesizeHybrid(ServerLevel level, Vec3 pos, String hybridType) {
        LivingEntity entity = EntityTypes.FOX.create(level, EntitySpawnReason.COMMAND);
        if (entity != null) {
            entity.snapTo(pos.x(), pos.y(), pos.z(), 0.0F, 0.0F);
            String normalized = hybridType == null ? "" : hybridType.toLowerCase(java.util.Locale.ROOT);
            boolean flying = normalized.contains("fly") || normalized.contains("лет");
            boolean glowing = normalized.contains("glow") || normalized.contains("свет");
            entity.setNoGravity(flying);
            entity.setGlowingTag(glowing);
            entity.getPersistentData().putBoolean("KubanHybrid", true);
            entity.getPersistentData().putString("KubanHybridTraits", normalized);
            var movement = entity.getAttribute(Attributes.FLYING_SPEED);
            if (flying && movement != null) {
                movement.setBaseValue(0.25D);
            }
            level.addFreshEntity(entity);
            MagicalSignature.cast(level, pos);
        }
        return entity;
    }
}
