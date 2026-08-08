package dev.romankrukovsky.kubanhorizons.genie.defense;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Локальный контроллер иллюзии «Ложная смерть» («Нет, я не умерла»). */
public final class PhantomDeathController {
    private static final Map<UUID, PhantomState> ACTIVE_PHANTOMS = new ConcurrentHashMap<>();

    private PhantomDeathController() {
    }

    public static void triggerPhantomDeath(KubanGenie genie, ServerLevel level, Entity attacker) {
        UUID genieId = genie.getUUID();
        if (ACTIVE_PHANTOMS.containsKey(genieId)) {
            return;
        }

        level.sendParticles(ParticleTypes.LARGE_SMOKE, genie.getX(), genie.getY() + 1.2D, genie.getZ(),
                45, 0.5D, 0.8D, 0.5D, 0.05D);
        level.sendParticles(ParticleTypes.SQUID_INK, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                30, 0.4D, 0.6D, 0.4D, 0.08D);
        level.sendParticles(ParticleTypes.PORTAL, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                35, 0.6D, 1.0D, 0.6D, 0.1D);
        genie.playDespawn();

        genie.setInvisible(true);
        genie.setInvulnerable(true);

        UUID targetId = attacker != null ? attacker.getUUID() : (genie.getOwner() != null ? genie.getOwner().getUUID() : null);
        ACTIVE_PHANTOMS.put(genieId, new PhantomState(genieId, targetId, level.getGameTime() + 40L));
    }

    public static void tickServer(ServerLevel level) {
        long now = level.getGameTime();
        ACTIVE_PHANTOMS.entrySet().removeIf(entry -> {
            PhantomState state = entry.getValue();
            if (now < state.reappearAt()) {
                return false;
            }
            if (!(level.getEntity(state.genieId()) instanceof KubanGenie genie) || !genie.isAlive()) {
                return true;
            }

            Entity target = state.targetId() != null ? level.getEntity(state.targetId()) : genie.getOwner();
            if (target != null) {
                Vec3 behind = target.position().subtract(target.getLookAngle().scale(1.8D)).add(0.0D, 0.5D, 0.0D);
                genie.teleportTo(behind.x(), behind.y(), behind.z());
            }

            genie.setInvisible(false);
            genie.setInvulnerable(false);
            genie.playSpawn();

            level.sendParticles(ParticleTypes.PORTAL, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                    40, 0.5D, 0.8D, 0.5D, 0.1D);

            if (target instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.phantom_death.finished"));
            } else if (genie.getOwner() instanceof ServerPlayer owner) {
                owner.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.phantom_death.finished"));
            }
            return true;
        });
    }

    private record PhantomState(UUID genieId, UUID targetId, long reappearAt) {
    }
}
