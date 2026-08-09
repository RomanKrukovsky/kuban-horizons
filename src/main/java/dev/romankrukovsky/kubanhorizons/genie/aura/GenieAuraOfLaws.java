package dev.romankrukovsky.kubanhorizons.genie.aura;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Личная «Аура Законов» Кубанской Джиннии.
 *
 * <p>Снаряды в ауре сначала замирают, а через мгновение рассыпаются. Просто
 * обнулять скорость нельзя: снаряд с нулевой скоростью не считается
 * упавшим и висит в воздухе вечно, а файербол гаста и голова иссушителя
 * продолжают каждый тик рисовать шлейф дыма. Пауза перед исчезновением
 * нужна, чтобы игрок увидел, что выстрел остановили, а не просто пропал.</p>
 */
public final class GenieAuraOfLaws {
    private static final double AURA_RADIUS = 16.0D;
    private static final double AURA_RADIUS_SQUARED = AURA_RADIUS * AURA_RADIUS;

    /** Сколько снаряд висит остановленным, прежде чем рассыпаться. */
    private static final long HOLD_TICKS = 10L;

    /** Момент остановки каждого снаряда: ключ — UUID снаряда. */
    private static final Map<UUID, Long> HELD_PROJECTILES = new HashMap<>();

    private GenieAuraOfLaws() {
    }

    public static void tickAuraOfLaws(KubanGenie genie, ServerLevel level) {
        AABB box = genie.getBoundingBox().inflate(AURA_RADIUS);
        long now = level.getGameTime();

        // 1. Снаряды останавливаются и берутся на учёт для последующего роспуска
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box)) {
            hold(genie, level, projectile, now);
        }
        // Только что добавленный снаряд может ещё не попасть в section-index.
        // Отдельно проверяем все отслеживаемые сущности уровня.
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Projectile projectile
                    && projectile.distanceToSqr(genie) <= AURA_RADIUS_SQUARED) {
                hold(genie, level, projectile, now);
            }
        }

        // 2. Огонь автоматически гаснет
        BlockPos center = genie.blockPosition();
        if (level.getRandom().nextInt(5) == 0) {
            int rx = level.getRandom().nextInt(11) - 5;
            int ry = level.getRandom().nextInt(5) - 2;
            int rz = level.getRandom().nextInt(11) - 5;
            BlockPos pos = center.offset(rx, ry, rz);
            if (level.getBlockState(pos).is(Blocks.FIRE) || level.getBlockState(pos).is(Blocks.SOUL_FIRE)) {
                level.removeBlock(pos, false);
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        10, 0.2D, 0.2D, 0.2D, 0.02D);
            }
        }

        // 3. Плавное падение хозяина при высоком приземлении
        var owner = genie.getOwner();
        if (owner != null && owner.distanceToSqr(genie) <= AURA_RADIUS_SQUARED) {
            if (owner.fallDistance > 3.0F) {
                owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, owner.getX(), owner.getY() + 0.2D, owner.getZ(),
                        5, 0.2D, 0.2D, 0.2D, 0.01D);
            }
        }
    }

    /** Останавливает враждебный снаряд и запоминает момент остановки. */
    private static void hold(KubanGenie genie, ServerLevel level, Projectile projectile, long now) {
        if (!affects(genie, projectile)) {
            return;
        }
        projectile.setDeltaMovement(Vec3.ZERO);
        // Файербол и голова иссушителя иначе продолжают гореть на месте.
        projectile.clearFire();
        if (HELD_PROJECTILES.putIfAbsent(projectile.getUUID(), now) == null
                && level.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, projectile.getX(), projectile.getY(), projectile.getZ(),
                    3, 0.1D, 0.1D, 0.1D, 0.01D);
        }
    }

    /**
     * Распускает остановленные снаряды, чей срок висения истёк.
     *
     * <p>Вызывается каждый тик, а не вместе с самой аурой: интервал ауры
     * длиннее задержки, и снаряды исчезали бы с заметным запозданием.</p>
     */
    public static void tickHeldProjectiles(ServerLevel level) {
        if (HELD_PROJECTILES.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        HELD_PROJECTILES.entrySet().removeIf(entry -> {
            // Снаряд ищется по всем измерениям: он мог быть остановлен в
            // Нижнем мире, а этот тик пришёл из оверворлда, и запись нельзя
            // выбрасывать только потому, что здесь его нет.
            Entity entity = level.getEntityInAnyDimension(entry.getKey());
            if (!(entity instanceof Projectile projectile) || !projectile.isAlive()) {
                return true;
            }
            if (now - entry.getValue() < HOLD_TICKS) {
                // Пока висит — держим на месте: инерция и гравитация иначе
                // потащат снаряд дальше и он всё-таки попадёт в цель.
                projectile.setDeltaMovement(Vec3.ZERO);
                return false;
            }
            if (projectile.level() instanceof ServerLevel projectileLevel) {
                dissolve(projectileLevel, projectile);
            }
            return true;
        });
    }

    /** Рассыпает снаряд, не нанося урона. */
    private static void dissolve(ServerLevel level, Projectile projectile) {
        level.sendParticles(ParticleTypes.PORTAL, projectile.getX(), projectile.getY(), projectile.getZ(),
                12, 0.2D, 0.2D, 0.2D, 0.06D);
        level.sendParticles(ParticleTypes.SMOKE, projectile.getX(), projectile.getY(), projectile.getZ(),
                6, 0.15D, 0.15D, 0.15D, 0.01D);
        projectile.discard();
    }

    /**
     * Подпадает ли снаряд под действие ауры.
     *
     * <p>Свои снаряды и снаряды хозяина не трогаются, иначе аура мешала бы
     * стрелять. Рыболовный крючок и фейерверк на элитрах — не атака, и их
     * роспуск выглядел бы поломкой, а не защитой.</p>
     */
    private static boolean affects(KubanGenie genie, Projectile projectile) {
        if (projectile instanceof FishingHook) {
            return false;
        }
        Entity owner = projectile.getOwner();
        if (projectile instanceof FireworkRocketEntity
                && owner instanceof net.minecraft.world.entity.LivingEntity living
                && living.isFallFlying()) {
            // Ракета, толкающая летящего на элитрах, — транспорт, а не атака.
            return false;
        }
        return owner != genie && (owner == null || owner != genie.getOwner());
    }

    /** Сбрасывает учёт остановленных снарядов; нужно тестам между сценариями. */
    public static void clearHeldForTesting() {
        HELD_PROJECTILES.clear();
    }

    /** Сколько снарядов сейчас удерживается аурой. */
    public static int heldCountForTesting() {
        return HELD_PROJECTILES.size();
    }
}
