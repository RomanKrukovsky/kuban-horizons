package dev.romankrukovsky.kubanhorizons.genie.defense;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.registry.KHItems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Иронические реакции на физические и магические атаки против Кубанской Джиннии. */
public final class WishborneDefenseHandler {
    private static final Map<UUID, SpoonSwap> ACTIVE_SWAPS = new ConcurrentHashMap<>();

    private WishborneDefenseHandler() {
    }

    public static boolean handleHurt(KubanGenie genie, ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        Entity directEntity = source.getDirectEntity();

        // 1. Атака мечом/топором в ближнем бою: превращение оружия в деревянную ложку на 5 секунд
        if (directEntity instanceof LivingEntity livingAttacker) {
            ItemStack held = livingAttacker.getMainHandItem();
            if (held.is(ItemTags.SWORDS) || held.is(ItemTags.AXES)) {
                swapWithSpoon(level, livingAttacker, held);
                tellAttacker(attacker, "message.kubanhorizons.genie.irony.spoon");
                genie.playHurt();
                return false;
            }
        }

        // 2. Стрелы и снаряды: перехват двух пальцев и превращение в предмет
        if (directEntity instanceof Projectile projectile) {
            level.sendParticles(ParticleTypes.PORTAL, projectile.getX(), projectile.getY(), projectile.getZ(),
                    25, 0.2D, 0.2D, 0.2D, 0.08D);
            ItemStack itemStack = projectile.getWeaponItem();
            if (itemStack.isEmpty()) {
                itemStack = new ItemStack(Items.ARROW);
            }
            ItemEntity dropped = new ItemEntity(level, genie.getX(), genie.getY() + 1.0D, genie.getZ(), itemStack);
            level.addFreshEntity(dropped);
            projectile.discard();
            tellAttacker(attacker, "message.kubanhorizons.genie.irony.arrow");
            genie.playCast();
            return false;
        }

        // 3. Звуковая волна Вардена: материализация звукового вала в предмет
        if (source.is(DamageTypes.SONIC_BOOM)) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, genie.getX(), genie.getY() + 1.2D, genie.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            ItemEntity sonicItem = new ItemEntity(level, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                    KHItems.SONIC_BOOM_ITEM.get().getDefaultInstance());
            level.addFreshEntity(sonicItem);
            tellAttacker(attacker, "message.kubanhorizons.genie.irony.sonic");
            genie.playCast();
            return false;
        }

        // 4. Взрыв / TNT: выживание без урона со слегка взъерошенной причёской
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, genie.getX(), genie.getY() + 2.0D, genie.getZ(),
                    30, 0.3D, 0.4D, 0.3D, 0.05D);
            level.sendParticles(ParticleTypes.WITCH, genie.getX(), genie.getY() + 2.2D, genie.getZ(),
                    15, 0.2D, 0.3D, 0.2D, 0.02D);
            tellAttacker(attacker, "message.kubanhorizons.genie.irony.tnt");
            genie.playHurt();
            return false;
        }

        // 5. Попытка административного /kill или падения в Безъязыкую Бездну
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            level.sendParticles(ParticleTypes.WITCH, genie.getX(), genie.getY() + 1.2D, genie.getZ(),
                    50, 0.6D, 0.8D, 0.6D, 0.1D);
            level.sendParticles(ParticleTypes.PORTAL, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                    40, 0.5D, 0.5D, 0.5D, 0.08D);
            KubanHorizons.LOGGER.info("Entity cannot be permanently destroyed.");
            tellAttacker(attacker, "message.kubanhorizons.genie.irony.kill");
            var owner = genie.getOwner();
            if (owner != null) {
                genie.snapTo(owner.getX() + 1.0D, owner.getY() + 1.0D, owner.getZ() + 1.0D, owner.getYRot(), 0.0F);
            }
            return false;
        }

        // 5. Сильная атака или любая другая авантюра: активация Phantom Death («Нет, я не умерла»)
        if (amount >= 5.0F || attacker != null) {
            PhantomDeathController.triggerPhantomDeath(genie, level, attacker);
            return false;
        }

        genie.playHurt();
        level.sendParticles(ParticleTypes.PORTAL, genie.getX(), genie.getY() + 1.0D, genie.getZ(),
                20, 0.4D, 0.8D, 0.4D, 0.05D);
        return false;
    }

    private static void swapWithSpoon(ServerLevel level, LivingEntity attacker, ItemStack originalItem) {
        ItemStack spoon = KHItems.WOODEN_SPOON.get().getDefaultInstance();
        ItemStack copy = originalItem.copy();
        attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spoon);
        ACTIVE_SWAPS.put(attacker.getUUID(), new SpoonSwap(copy, level.getGameTime() + 100L));
    }

    public static void tickServer(ServerLevel level) {
        long now = level.getGameTime();
        ACTIVE_SWAPS.entrySet().removeIf(entry -> {
            if (now >= entry.getValue().restoreAt()) {
                LivingEntity entity = level.getEntity(entry.getKey()) instanceof LivingEntity living ? living : null;
                if (entity != null) {
                    ItemStack current = entity.getMainHandItem();
                    if (current.is(KHItems.WOODEN_SPOON.get())) {
                        entity.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, entry.getValue().original());
                    } else {
                        entity.spawnAtLocation(level, entry.getValue().original());
                    }
                }
                return true;
            }
            return false;
        });
    }

    private static void tellAttacker(Entity attacker, String key) {
        if (attacker instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable(key));
        }
    }

    private record SpoonSwap(ItemStack original, long restoreAt) {
    }
}
