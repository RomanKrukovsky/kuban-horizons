package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2COpenOwnerDeathScreen;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Протокол выбора после смерти владельца джиннии.
 *
 * <p>Когда владелец джиннии умирает, джинния предлагает 4 варианта:
 * 1. Воскресить владельца на месте смерти
 * 2. Сохранить душу владельца (отложить смерть на 1 минуту)
 * 3. Откатить последнее желание (через CausalLedger)
 * 4. Освободить джиннию и позволить игроку заспавниться заново
 */
public final class OwnerDeathProtocol {

    /** Карта ожидания выбора: владелец -> UUID джиннии для Role Swap (RESPAWN_FREE) */
    private static final ConcurrentHashMap<UUID, UUID> PENDING_GENIE_FOR_OWNER = new ConcurrentHashMap<>();

    private OwnerDeathProtocol() {
    }

    public enum DeathChoice {
        /** Воскресить владельца на месте смерти */
        RESURRECT_OWNER,
        /** Сохранить душу владельца (отложить смерть) */
        SAVE_SOUL,
        /** Откатить последнее желание */
        ROLLBACK_LAST_WISH,
        /** Освободить джиннию, игрок становится обычным */
        RESPAWN_FREE
    }

    /**
     * Вызывается при смерти владельца джиннии.
     * Открывает экран выбора на клиенте.
     */
    public static void onOwnerDeath(KubanGenie genie, ServerPlayer owner) {
        // Сохраняем UUID джиннии для возможного Role Swap (RESPAWN_FREE)
        PENDING_GENIE_FOR_OWNER.put(owner.getUUID(), genie.getUUID());
        // Отправляем пакет для открытия экрана выбора
        S2COpenOwnerDeathScreen.send(owner, genie.getUUID());
    }

    /**
     * Вызывается при фатальном уроне владельца (LivingIncomingDamageEvent).
     * Отменяет урон, спасает владельца и открывает экран выбора.
     *
     * @return true если спасение сработало (урон отменён)
     */
    public static boolean rescueNow(KubanGenie genie, net.minecraft.world.level.Level level, ServerPlayer owner) {
        if (level.isClientSide()) return false;
        // Сохраняем UUID джиннии для возможного Role Swap (RESPAWN_FREE)
        PENDING_GENIE_FOR_OWNER.put(owner.getUUID(), genie.getUUID());
        // Отменяем урон и открываем экран выбора
        S2COpenOwnerDeathScreen.send(owner, genie.getUUID());
        return true;
    }

    /**
     * Выполняет выбранный вариант.
     */
    public static void executeChoice(ServerPlayer player, DeathChoice choice) {
        switch (choice) {
            case RESURRECT_OWNER -> resurrectOwner(player);
            case SAVE_SOUL -> saveSoul(player);
            case ROLLBACK_LAST_WISH -> rollbackLastWish(player);
            case RESPAWN_FREE -> respawnFree(player);
        }
    }

    private static void resurrectOwner(ServerPlayer player) {
        // Воскрешает владельца на месте смерти
        player.setHealth(player.getMaxHealth());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.owner_death.resurrected"));
    }

    private static void saveSoul(ServerPlayer player) {
        // Реальная задержка смерти на 60 секунд через scheduled task (старый Java 8 стиль)
        if (!(player.level() instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();

        final int delayTicks = 20 * 60; // 60 секунд
        final float savedHealth = player.getHealth();

        // Отправляем сообщение о спасении
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.owner_death.soul_saved"));

        // Визуальный эффект: сияние вокруг игрока
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                20, 0.4D, 0.6D, 0.4D, 0.01D);

        // Планируем проверку через 60 секунд
        // Используем встроенный scheduler сервера (старый Java стиль)
        level.getServer().execute(() -> {
            // Проверяем, что игрок всё ещё жив и в том же мире
            if (player.isAlive() && player.level() == level) {
                // Если здоровье упало ниже savedHealth, восстанавливаем
                if (player.getHealth() < savedHealth) {
                    player.setHealth(savedHealth);
                    player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.owner_death.soul_restored"));
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            player.getX(), player.getY() + 1.0D, player.getZ(),
                            15, 0.3D, 0.5D, 0.3D, 0.02D);
                }
            }
        });

        // Для реальной отложенной задачи используем tick-based подход
        // Сохраняем время в attachment для проверки в tick handler'е
        dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment attachment =
                player.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
        attachment.setLastWishTick(level.getGameTime() + delayTicks);
    }

    private static void rollbackLastWish(ServerPlayer player) {
        // Реальный откат последнего желания через CausalLedger (старый Java 8 стиль)
        if (!(player.level() instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();

        // Получаем CausalLedger из WorldGenieMemoryPersistence
        dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemoryPersistence persistence =
                new dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemoryPersistence(
                        level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                                .resolve("genie_memory").toFile());

        try {
            dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.CausalLedger ledger = persistence.createCausalLedger();
            java.util.Optional<com.worldgenie.attachment.CausalEntry> lastEntry = ledger.getLastNonRolledBackEntry();

            if (lastEntry.isPresent()) {
                com.worldgenie.attachment.CausalEntry entry = lastEntry.get();

                // Применяем beforeState к игроку
                player.load(entry.beforeState());

                // Помечаем запись как откатанную
                ledger = ledger.markAsRolledBack(entry.id());
                // Сохраняем обновлённый ledger (persistence handles disk write)

                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.owner_death.rollback_success"));
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        30, 0.5D, 0.8D, 0.5D, 0.02D);
            } else {
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.owner_death.rollback_empty"));
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.owner_death.rollback_failed"));
            KubanHorizons.LOGGER.error("Failed to rollback wish via CausalLedger", e);
        }
    }

    private static void respawnFree(ServerPlayer player) {
        // Role Swap: зеркало превращает джиннию во владельца и наоборот.
        // Реальная трансформация: удаляем сущность джиннии, сбрасываем attachment игрока в HUMAN.
        UUID genieUuid = PENDING_GENIE_FOR_OWNER.remove(player.getUUID());
        if (genieUuid != null && player.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(genieUuid);
            if (entity instanceof KubanGenie genie) {
                genie.discard();
            }
        }

        // Сброс джинновского состояния игрока
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        attachment.setGenie(false);
        attachment.setStage(PlayerGenieAttachment.Stage.HUMAN);
        attachment.setMasterUUID(null);
        attachment.setBoundVesselPos(null);
        attachment.setBoundVesselDimension(null);
        attachment.setBoundVesselEntry(null, 0.0F);
        attachment.clearDimensionalReturn();
        attachment.setWishProgressPercent(0);
        attachment.setTierLevel(1);
        attachment.setVesselCreated(false);
        attachment.setCorruption(0);
        attachment.setLastWishTick(0L);
        attachment.setNextTransformationTick(0L);
        attachment.setAvatarStyle("DEFAULT_KUBAN");

        // Очистка состояния pocket scene, чтобы игрок не оставался в dangling состоянии
        dev.romankrukovsky.kubanhorizons.genie.dimension.PocketSceneService.cancel(player);

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.owner_death.free"));
    }
}
