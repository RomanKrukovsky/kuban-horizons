package dev.romankrukovsky.kubanhorizons.genie.social;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Система слухов, процедурных легенд и ежегодных фестивалей Джиннии (Genie Myth & Festival System). */
public final class GenieMythSystem {
    private GenieMythSystem() {
    }

    public static void startGenieFestival(ServerLevel level, BlockPos villageCenter, Player player) {
        MagicalSignature.cast(level, net.minecraft.world.phys.Vec3.atCenterOf(villageCenter));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.festival_start"));
    }
}
