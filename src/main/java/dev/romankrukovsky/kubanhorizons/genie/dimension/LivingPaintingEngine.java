package dev.romankrukovsky.kubanhorizons.genie.dimension;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHDimensions;
import dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHMagicDimensions;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Безопасный вход в живую картину и зазеркалье с точным возвратом. */
public final class LivingPaintingEngine {
    private static final Vec3 PAINTING_ENTRY =
            new Vec3(0.5D, KHMagicDimensions.FLOOR_Y + 2.0D, 0.5D);
    private static final Vec3 MIRROR_ENTRY =
            new Vec3(32.5D, KHMagicDimensions.FLOOR_Y + 2.0D, 0.5D);

    private LivingPaintingEngine() {
    }

    public static boolean enterPaintingDimension(ServerLevel level, BlockPos pos, Player player) {
        return enter(level, pos, player, KHMagicDimensions.PAINTING_WORLD,
                PAINTING_ENTRY, "message.kubanhorizons.genie.living_painting");
    }

    /** Живая картина может вести в любой существующий мир, сохраняя обратный путь. */
    public static boolean enterDimension(ServerLevel level, BlockPos pos, Player player,
                                         ResourceKey<Level> destinationKey, Vec3 destination) {
        return enter(level, pos, player, destinationKey, destination,
                "message.kubanhorizons.genie.living_painting");
    }

    public static boolean enterMirrorWorld(ServerLevel level, BlockPos pos, Player player) {
        return enter(level, pos, player, KHMagicDimensions.MIRROR_WORLD,
                MIRROR_ENTRY, "message.kubanhorizons.genie.mirror_world");
    }

    public static boolean isInside(Player player) {
        return KHMagicDimensions.isPocketDimension(player.level().dimension());
    }

    public static boolean leave(ServerPlayer player) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!isInside(player) && !attachment.hasDimensionalReturn()) {
            return false;
        }
        Optional<Vec3> savedPosition = attachment.getDimensionalReturnPosition();
        ServerLevel target = attachment.getDimensionalReturnDimension()
                .map(key -> player.level().getServer().getLevel(key))
                .orElse(null);
        if (savedPosition.isEmpty() || target == null) {
            player.teleport(player.findRespawnPositionAndUseSpawnBlock(
                    false, TeleportTransition.DO_NOTHING));
        } else {
            Vec3 destination = savedPosition.orElseThrow();
            target.getChunk(BlockPos.containing(destination));
            player.teleport(new TeleportTransition(target, destination, Vec3.ZERO,
                    attachment.getDimensionalReturnYaw(), attachment.getDimensionalReturnPitch(),
                    Set.<Relative>of(), TeleportTransition.DO_NOTHING));
            target.sendParticles(ParticleTypes.PORTAL,
                    destination.x, destination.y + 1.0D, destination.z,
                    60, 0.5D, 0.8D, 0.5D, 0.1D);
            MagicalSignature.cast(target, destination);
        }
        attachment.clearDimensionalReturn();
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.magic_realm.left"));
        return true;
    }

    private static boolean enter(ServerLevel level, BlockPos pos, Player player,
                                 ResourceKey<Level> destinationKey, Vec3 destination,
                                 String messageKey) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level() != level) {
            return false;
        }
        if (KHMagicDimensions.isPocketDimension(level.dimension())
                || level.dimension().equals(KHDimensions.ETERNAL_KUBAN)) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.magic_realm.recursive"));
            return false;
        }
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (attachment.hasDimensionalReturn()) {
            // Игрок уже не в карманном мире: запись осталась после внешнего
            // телепорта администратора. Она устарела и не должна запирать вход.
            attachment.clearDimensionalReturn();
        }
        ServerLevel target = level.getServer().getLevel(destinationKey);
        if (target == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.magic_realm.missing"));
            return false;
        }

        attachment.setDimensionalReturn(level.dimension(), player.position(),
                player.getYRot(), player.getXRot());
        MagicalSignature.cast(level, Vec3.atCenterOf(pos));
        target.getChunk(BlockPos.containing(destination));
        serverPlayer.teleport(new TeleportTransition(target, destination, Vec3.ZERO,
                player.getYRot(), player.getXRot(), Set.<Relative>of(),
                TeleportTransition.DO_NOTHING));
        if (serverPlayer.level() != target) {
            attachment.clearDimensionalReturn();
            return false;
        }
        MagicalSignature.cast(target, destination);
        serverPlayer.sendSystemMessage(Component.translatable(messageKey));
        return true;
    }
}
