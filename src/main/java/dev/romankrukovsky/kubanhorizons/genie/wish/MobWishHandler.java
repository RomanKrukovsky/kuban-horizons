package dev.romankrukovsky.kubanhorizons.genie.wish;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Диалоговый обработчик чтения и исполнения желаний мобов (корова, волк, голем, крипер). */
public final class MobWishHandler {
    private MobWishHandler() {
    }

    public static boolean handleMobWish(ServerLevel level, Player player, LivingEntity target) {
        if (target.getType() == EntityTypes.COW) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0D, target.getZ(),
                    40, 0.6D, 0.6D, 0.6D, 0.05D);
            level.setBlockAndUpdate(target.blockPosition(), Blocks.SNOW.defaultBlockState());
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.cow"));
            return true;
        }
        if (target.getType() == EntityTypes.WOLF && target instanceof TamableAnimal tamable) {
            tamable.tame(player);
            level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 0.8D, target.getZ(),
                    10, 0.3D, 0.3D, 0.3D, 0.0D);
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.wolf"));
            return true;
        }
        if (target.getType() == EntityTypes.IRON_GOLEM) {
            target.spawnAtLocation(level, new ItemStack(Items.POPPY));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 2.0D, target.getZ(),
                    20, 0.5D, 0.5D, 0.5D, 0.02D);
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.golem"));
            return true;
        }
        if (target.getType() == EntityTypes.CREEPER && target instanceof Creeper creeper) {
            creeper.setSwellDir(-1);
            level.sendParticles(ParticleTypes.FIREWORK, creeper.getX(), creeper.getY() + 1.2D, creeper.getZ(),
                    30, 0.4D, 0.5D, 0.4D, 0.1D);
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.creeper"));
            return true;
        }
        return false;
    }
}
