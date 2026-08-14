package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHDimensions;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Безопасный визит хозяина в дворец внутри лампы с точным возвратом. */
public final class VesselTravelService {
    private static final Vec3 PALACE_SPAWN =
            new Vec3(0.5D, KHDimensions.PALACE_FLOOR_Y + 2.0D, 0.5D);

    private VesselTravelService() {
    }

    public static boolean isVisitingPalace(ServerPlayer player) {
        return player.level().dimension().equals(KHDimensions.ETERNAL_KUBAN);
    }

    public static boolean enterPalace(ServerPlayer player, UUID genieId) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (attachment.isGenie()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.law.use_forfeit"));
            return false;
        }
        KubanGenie genie = GenieLampItem.findGenie(player.level().getServer(), genieId);
        if (genie == null || !genie.isOwnedBy(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.lamp.unavailable"));
            return false;
        }
        ServerLevel target = player.level().getServer().getLevel(KHDimensions.ETERNAL_KUBAN);
        if (target == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.vessel.dimension_missing"));
            return false;
        }

        attachment.setBoundVesselEntry(player.position(), player.getYRot());
        attachment.setBoundVesselDimension(player.level().dimension());
        ServerLevel from = (ServerLevel) player.level();
        MagicalSignature.cast(from, player.position());
        target.getChunk(0, 0);
        player.teleport(new TeleportTransition(target, PALACE_SPAWN, Vec3.ZERO,
                player.getYRot(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.vessel.palace_entered"));
        return true;
    }

    public static boolean leavePalace(ServerPlayer player) {
        if (!isVisitingPalace(player)) {
            return false;
        }
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (attachment.isGenie()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.law.use_forfeit"));
            return false;
        }
        Optional<Vec3> entry = attachment.getBoundVesselEntry();
        ServerLevel target = attachment.getBoundVesselDimension()
                .map(key -> player.level().getServer().getLevel(key))
                .orElse(null);
        if (entry.isEmpty() || target == null) {
            player.teleport(player.findRespawnPositionAndUseSpawnBlock(
                    false, TeleportTransition.DO_NOTHING));
        } else {
            Vec3 destination = entry.get();
            player.teleport(new TeleportTransition(target, destination, Vec3.ZERO,
                    attachment.getBoundVesselYaw(), 0.0F, Set.<Relative>of(),
                    TeleportTransition.DO_NOTHING));
            target.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0D,
                    destination.z, 60, 0.5D, 0.8D, 0.5D, 0.1D);
            MagicalSignature.cast(target, destination);
        }
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.vessel.palace_left"));
        return true;
    }
}
