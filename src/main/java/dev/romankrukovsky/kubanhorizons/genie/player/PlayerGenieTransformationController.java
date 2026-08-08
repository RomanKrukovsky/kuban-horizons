package dev.romankrukovsky.kubanhorizons.genie.player;

import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Контроллер 5-стадийной трансформации игрока в Джиннию.
 */
public final class PlayerGenieTransformationController {
    private PlayerGenieTransformationController() {
    }

    public static void startTransformation(ServerLevel level, ServerPlayer player, WishIntent.Target wishTarget) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (attachment.isGenie() && attachment.getStage() != PlayerGenieAttachment.Stage.HUMAN) {
            return;
        }
        attachment.setGenie(true);
        attachment.setStage(PlayerGenieAttachment.Stage.BODY_REWRITE);
        attachment.setWishProgressPercent(63); // Запуск с 63% выполнения Желания №1
        attachment.setNextTransformationTick(level.getGameTime() + 40L);

        // Стадия 1: Смертность удалена, переписывание тела
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.stage1_status"));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.mortality_removed"));
        MagicalSignature.cast(level, player.position());
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 1.0, 0.5, 0.05);

    }

    /** Продвигает кинематографичную сцену по сохранённому серверному времени. */
    public static void tickTransformation(ServerLevel level, ServerPlayer player) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie() || attachment.getStage() == PlayerGenieAttachment.Stage.HUMAN
                || attachment.getStage() == PlayerGenieAttachment.Stage.FULL_GENIE
                || level.getGameTime() < attachment.getNextTransformationTick()) {
            return;
        }
        switch (attachment.getStage()) {
            case BODY_REWRITE -> advanceToStage2(level, player, attachment);
            case TAIL_FORMATION -> advanceToStage3(level, player, attachment);
            case AVATAR_CUSTOMIZATION -> advanceToStage4(level, player, attachment);
            case INVULNERABILITY_TEST -> finalizeTransformation(level, player, attachment);
            case HUMAN, FULL_GENIE -> {
                // Эти состояния обработаны выше.
            }
        }
    }

    public static void advanceToStage2(ServerLevel level, ServerPlayer player, PlayerGenieAttachment attachment) {
        attachment.setStage(PlayerGenieAttachment.Stage.TAIL_FORMATION);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        if (player.connection != null) {
            player.onUpdateAbilities();
        }

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.stage2_flight"));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.stage2_anatomical"));
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(), player.getZ(), 80, 0.4, 0.6, 0.4, 0.02);
        attachment.setNextTransformationTick(level.getGameTime() + 40L);
    }

    private static void advanceToStage3(ServerLevel level, ServerPlayer player, PlayerGenieAttachment attachment) {
        attachment.setStage(PlayerGenieAttachment.Stage.AVATAR_CUSTOMIZATION);
        attachment.setAvatarStyle("KUBAN_DJINNIA_AVATAR");
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.stage3_form_dialogue"));
        attachment.setNextTransformationTick(level.getGameTime() + 60L);
    }

    private static void advanceToStage4(ServerLevel level, ServerPlayer player, PlayerGenieAttachment attachment) {
        attachment.setStage(PlayerGenieAttachment.Stage.INVULNERABILITY_TEST);

        // Стадия 4: Демонстрация неуязвимости (спавн скелета и стрела)
        BlockPos spawnPos = player.blockPosition().relative(player.getDirection(), 4);
        if (level.isEmptyBlock(spawnPos)) {
            LivingEntity testSkeleton = EntityTypes.SKELETON.create(level, EntitySpawnReason.EVENT);
            if (testSkeleton != null) {
                testSkeleton.snapTo(spawnPos.getX() + 0.5D, (double) spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
                testSkeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                level.addFreshEntity(testSkeleton);

                Projectile arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.EVENT);
                if (arrow != null) {
                    arrow.snapTo(testSkeleton.getX(), testSkeleton.getEyeY(), testSkeleton.getZ(), testSkeleton.getYRot(), testSkeleton.getXRot());
                    Vec3 dir = player.getEyePosition().subtract(testSkeleton.getEyePosition()).normalize().scale(1.2);
                    arrow.setDeltaMovement(dir);
                    level.addFreshEntity(arrow);
                }
            }
        }

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.stage4_damage_ignored"));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.stage4_genie_quote"));
        attachment.setNextTransformationTick(level.getGameTime() + 60L);
    }

    public static void finalizeTransformation(ServerLevel level, ServerPlayer player, PlayerGenieAttachment attachment) {
        attachment.setStage(PlayerGenieAttachment.Stage.FULL_GENIE);
        attachment.setNextTransformationTick(0L);

        if (!attachment.isVesselCreated()) {
            ItemStack playerLamp = new ItemStack(KHItems.PLAYER_GENIE_LAMP.get());
            playerLamp.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    Component.translatable("item.kubanhorizons.player_genie_lamp", player.getName().getString()));
            net.minecraft.nbt.CompoundTag lampData = new net.minecraft.nbt.CompoundTag();
            lampData.putString("GeniePlayer", player.getUUID().toString());
            net.minecraft.world.item.component.CustomData.set(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA, playerLamp, lampData);

            if (!player.getInventory().add(playerLamp)) {
                player.drop(playerLamp, false);
            }
            attachment.setVesselCreated(true);
        }

        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.vessel_created"));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.price_dialogue"));
        MagicalSignature.cast(level, player.position());
    }

    /**
     * Обработка любого входящего урона игрока-Джиннии: отмена и замена на визуальные эффекты.
     */
    public static boolean handleGenieDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (!attachment.isGenie()) {
            return false;
        }

        ServerLevel level = (ServerLevel) player.level();

        if (source.is(net.minecraft.world.damagesource.DamageTypes.LAVA) || source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            player.clearFire();
            return true;
        }

        if (source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION) || source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION)) {
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.6, 0.8, 0.6, 0.1);
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
            return true;
        }

        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            Vec3 pos = player.position();
            player.teleportTo(pos.x, pos.y + 0.1, pos.z);
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(), player.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            return true;
        }

        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            Vec3 pos = player.position();
            player.teleportTo(pos.x, pos.y + 20.0, pos.z);
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.transformation.void_rescue"));
            return true;
        }

        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.3, 0.6, 0.3, 0.05);
        return true; // Отмена всех остальных видов урона
    }
}
