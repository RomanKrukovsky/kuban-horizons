package dev.romankrukovsky.kubanhorizons.genie.entity;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Синтез гибридных видов существ и эволюционный дрейф (Hybrid Speciation & Evolution Engine). */
public final class HybridSpeciesEngine {
    private HybridSpeciesEngine() {
    }

    public static LivingEntity synthesizeHybrid(ServerLevel level, Vec3 pos, String hybridType) {
        LivingEntity entity = EntityTypes.FOX.create(level, EntitySpawnReason.COMMAND);
        if (entity != null) {
            entity.snapTo(pos.x(), pos.y(), pos.z(), 0.0F, 0.0F);
            level.addFreshEntity(entity);
            MagicalSignature.cast(level, pos);
        }
        return entity;
    }
}
