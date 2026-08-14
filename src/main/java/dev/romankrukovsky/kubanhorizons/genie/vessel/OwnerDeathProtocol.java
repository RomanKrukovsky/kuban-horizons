package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Протокол выбора реагирования при гибели хозяина (Owner Death Choice Protocol). */
public final class OwnerDeathProtocol {
    private OwnerDeathProtocol() {
    }

    public static void handleOwnerDeath(KubanGenie genie, ServerLevel level, Player player) {
        if (!genie.isOwnedBy(player)) {
            return;
        }
        MagicalSignature.cast(level, player.position());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.death_choice"));
    }

    public enum DeathChoice {
        RESURRECT_OWNER,
        SAVE_SOUL,
        ROLLBACK_LAST_WISH,
        RESPAWN_FREE
    }

    public static void executeChoice(ServerPlayer player, DeathChoice choice) {
        if (player == null) return;
        ServerLevel level = (ServerLevel) player.level();
        switch (choice) {
            case RESURRECT_OWNER -> {
                player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.5F));
                player.clearFire();
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));
                MagicalSignature.cast(level, player.position());
                dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory.get(level)
                        .recordRescue(player.blockPosition(), level.getGameTime());
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.rescue_success"));
            }
            case SAVE_SOUL -> {
                MagicalSignature.cast(level, player.position());
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.soul_saved"));
            }
            case ROLLBACK_LAST_WISH -> {
                MagicalSignature.cast(level, player.position());
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.rollback_success"));
            }
            case RESPAWN_FREE -> {
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.respawn_free"));
            }
        }
    }

    /** Безопасный первый вариант протокола: отменить смертельный исход и стабилизировать хозяина. */
    public static boolean rescueNow(KubanGenie genie, ServerLevel level, ServerPlayer player) {
        if (!genie.isOwnedBy(player)) {
            return false;
        }
        player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.5F));
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));
        MagicalSignature.cast(level, player.position());
        dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory.get(level)
                .recordRescue(player.blockPosition(), level.getGameTime());
        return true;
    }
}
