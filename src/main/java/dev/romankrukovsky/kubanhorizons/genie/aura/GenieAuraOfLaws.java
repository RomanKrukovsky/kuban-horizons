package dev.romankrukovsky.kubanhorizons.genie.aura;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Личная «Аура Законов» Кубанской Джиннии (зависание снарядов, гашение огня, плавное падение). */
public final class GenieAuraOfLaws {
    private static final double AURA_RADIUS = 16.0D;

    private GenieAuraOfLaws() {
    }

    public static void tickAuraOfLaws(KubanGenie genie, ServerLevel level) {
        AABB box = genie.getBoundingBox().inflate(AURA_RADIUS);

        // 1. Стрелы и снаряды зависают в воздухе
        var projectiles = level.getEntitiesOfClass(Projectile.class, box);
        for (Projectile p : projectiles) {
            boolean isFriendly = p.getOwner() != null && (p.getOwner() == genie || p.getOwner() == genie.getOwner());
            if (!isFriendly) {
                p.setDeltaMovement(Vec3.ZERO);
                if (level.getRandom().nextInt(3) == 0) {
                    level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY(), p.getZ(),
                            3, 0.1D, 0.1D, 0.1D, 0.01D);
                }
            }
        }
        // Только что добавленный снаряд может ещё не попасть в section-index.
        // Отдельно проверяем все отслеживаемые сущности уровня.
        for (var entity : level.getAllEntities()) {
            if (entity instanceof Projectile projectile && !projectiles.contains(projectile)
                    && projectile.distanceToSqr(genie) <= AURA_RADIUS * AURA_RADIUS) {
                boolean friendly = projectile.getOwner() != null
                        && (projectile.getOwner() == genie || projectile.getOwner() == genie.getOwner());
                if (!friendly) projectile.setDeltaMovement(Vec3.ZERO);
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
        if (owner != null && owner.distanceToSqr(genie) <= AURA_RADIUS * AURA_RADIUS) {
            if (owner.fallDistance > 3.0F) {
                owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, owner.getX(), owner.getY() + 0.2D, owner.getZ(),
                        5, 0.2D, 0.2D, 0.2D, 0.01D);
            }
        }
    }
}
