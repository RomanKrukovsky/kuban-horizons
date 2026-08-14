package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Движок живых картин-порталов и зеркального мира (Living Paintings & Mirror World Engine). */
public final class LivingPaintingEngine {
    private static final Map<UUID, ReturnLocation> RETURN_LOCATIONS = new ConcurrentHashMap<>();

    private record ReturnLocation(ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot) {
    }

    private LivingPaintingEngine() {
    }

    public static boolean enterDimension(ServerLevel original, BlockPos pos, ServerPlayer player,
                                         ResourceKey<Level> targetDimension, Vec3 targetPos) {
        var server = original.getServer();
        ServerLevel destination = server.getLevel(targetDimension);
        if (destination == null) {
            return false;
        }
        RETURN_LOCATIONS.put(player.getUUID(),
                new ReturnLocation(original.dimension(), player.position(), player.getYRot(), player.getXRot()));
        MagicalSignature.cast(original, Vec3.atCenterOf(pos));
        player.teleport(new TeleportTransition(destination, targetPos, Vec3.ZERO,
                player.getYRot(), player.getXRot(), Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.living_painting"));
        return true;
    }

    public static boolean leave(ServerPlayer player) {
        ReturnLocation location = RETURN_LOCATIONS.remove(player.getUUID());
        if (location == null) {
            return false;
        }
        var server = player.level().getServer();
        if (server == null) {
            return false;
        }
        ServerLevel destination = server.getLevel(location.dimension());
        if (destination == null) {
            return false;
        }
        player.teleport(new TeleportTransition(destination, location.position(), Vec3.ZERO,
                location.yRot(), location.xRot(), Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        return true;
    }

    public static boolean enterPaintingDimension(ServerLevel level, BlockPos pos, Player player) {
        MagicalSignature.cast(level, Vec3.atCenterOf(pos));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.living_painting"));
        return true;
    }

    public static boolean enterMirrorWorld(ServerLevel level, BlockPos pos, Player player) {
        MagicalSignature.cast(level, Vec3.atCenterOf(pos));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mirror_world"));
        return true;
    }
}
