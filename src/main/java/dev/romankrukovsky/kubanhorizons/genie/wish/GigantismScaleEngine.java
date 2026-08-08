package dev.romankrukovsky.kubanhorizons.genie.wish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Движок исполнения желаний гигантизма (курицы высотой 30 блоков, гигантские животные). */
public final class GigantismScaleEngine {
    private GigantismScaleEngine() {
    }

    public static WishExecutor.Result execute(ServerLevel level, Player player, WishIntent intent) {
        BlockPos targetPos = player.blockPosition().relative(player.getDirection(), 4);

        switch (intent.target()) {
            case BIG_CHICKEN -> {
                LivingEntity chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
                if (chicken != null) {
                    chicken.snapTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, player.getYRot(), 0.0F);
                    var scaleAttribute = chicken.getAttribute(Attributes.SCALE);
                    if (scaleAttribute != null) {
                        scaleAttribute.setBaseValue(15.0D);
                    }
                    level.addFreshEntity(chicken);
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetPos.getX() + 0.5D, targetPos.getY() + 2.0D,
                            targetPos.getZ() + 0.5D, 3, 0.0D, 0.0D, 0.0D, 0.0D);
                    return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.big_chicken");
                }
            }
            default -> {
            }
        }
        return new WishExecutor.Result(false, "message.kubanhorizons.genie.wish.unknown");
    }
}
