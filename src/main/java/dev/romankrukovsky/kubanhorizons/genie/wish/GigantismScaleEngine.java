package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.gigantism.GiantPieBuilder;
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
                return new WishExecutor.Result(false, "message.kubanhorizons.genie.wish.failed");
            }
            case BIG_PIE -> {
                if (GiantPieBuilder.buildGiantPie(level, targetPos)) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 2.5D, targetPos.getY() + 2.0D,
                            targetPos.getZ() + 2.5D, 40, 1.5D, 1.0D, 1.5D, 0.1D);
                    return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.big_pie");
                }
                return new WishExecutor.Result(false, "message.kubanhorizons.genie.wish.no_space");
            }
            case BIG_BED -> {
                if (GiantPieBuilder.buildGiantBed(level, targetPos)) {
                    level.sendParticles(ParticleTypes.ENCHANT, targetPos.getX() + 2.0D, targetPos.getY() + 1.0D,
                            targetPos.getZ() + 3.0D, 30, 1.2D, 0.6D, 1.2D, 0.1D);
                    return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.big_bed");
                }
                return new WishExecutor.Result(false, "message.kubanhorizons.genie.wish.no_space");
            }
            default -> {
                // Delegate unhandled gigantism targets to LLM for graceful handling
                return LLMWishExecutor.execute(level, player, intent.detailParam());
            }
        }
    }
}
