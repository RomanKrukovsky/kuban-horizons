package dev.romankrukovsky.kubanhorizons.genie.wish;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;

/** Исполнитель мета-желаний, изменяющих фундаментальные правила Minecraft. */
public final class MetaRuleEngine {
    private MetaRuleEngine() {
    }

    public static WishExecutor.Result execute(ServerLevel level, Player player, WishIntent intent) {
        switch (intent.target()) {
            case META_NO_CREEPER_DAMAGE -> {
                if (level.getServer() != null) {
                    level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
                    level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0D, player.getZ(),
                            50, 1.0D, 1.0D, 1.0D, 0.1D);
                    return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.meta_no_creeper_damage");
                }
            }
            case META_LONGER_NIGHT -> {
                if (level.getServer() != null) {
                    CommandSourceStack source = player instanceof ServerPlayer serverPlayer
                            ? serverPlayer.createCommandSourceStack()
                            : level.getServer().createCommandSourceStack();
                    level.getServer().getCommands().performPrefixedCommand(source, "time set 18000");
                    level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(),
                            60, 1.5D, 1.5D, 1.5D, 0.2D);
                    return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.meta_longer_night");
                }
            }
            case META_INSTANT_SMELT -> {
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
                        40, 0.8D, 0.8D, 0.8D, 0.1D);
                return new WishExecutor.Result(true, "message.kubanhorizons.genie.wish.meta_instant_smelt");
            }
            default -> {
            }
        }
        return new WishExecutor.Result(false, "message.kubanhorizons.genie.wish.unknown");
    }
}
