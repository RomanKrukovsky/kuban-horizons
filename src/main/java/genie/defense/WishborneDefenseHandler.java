package genie.defense;

import genie.GenieStateSnapshot;
import genie.genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Handles wishborne defense mechanics including damage cancellation, weapon replacement,
 * projectile interception, and various defensive abilities.
 */
public class WishborneDefenseHandler {

    private final KubanGenie genie;
    private int invulnerableTicks = 0;
    private boolean isPhantom = false;

    public WishborneDefenseHandler(KubanGenie genie) {
        this.genie = genie;
    }

    /**
     * Handle incoming damage with wishborne defenses
     * @return true if damage was cancelled
     */
    public boolean handleDamage(DamageSource source, float amount) {
        // Check if already invulnerable
        if (invulnerableTicks > 0) {
            invulnerableTicks--;
            return true;
        }

        // Check for wishborne-specific damage cancellation
        if (canCancelDamage(source, amount)) {
            return true;
        }

        // Check for spoon weapon replacement
        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            if (replaceWeapon(attacker)) {
                return true;
            }
        }

        // Check for projectile interception
        if (interceptProjectile(source)) {
            return true;
        }

        // Check for sonic boom
        if (createSonicBoom(source)) {
            return true;
        }

        return false;
    }

    /**
     * Check if damage can be cancelled
     */
    private boolean canCancelDamage(DamageSource source, float amount) {
        // Never cancel void damage or creative damage
        if (source == DamageSource.OUT_OF_WORLD || source == DamageSource.GENERIC) {
            return false;
        }

        // Check emotional aura
        String aura = genie.getEmotionalAura();
        if (aura != null) {
            switch (aura) {
                case "pyro_aura":
                    // Fire damage is reduced
                    if (source.isFire() || source.isExplosion()) {
                        return true;
                    }
                    break;
                case "hydro_aura":
                    // Water damage is reduced
                    if (source == DamageSource.DROWN || source == DamageSource.LAVA) {
                        return true;
                    }
                    break;
                case "geo_aura":
                    // Fall damage is reduced
                    if (source == DamageSource.FALL) {
                        return true;
                    }
                    break;
                case "aero_aura":
                    // Wind/air damage is reduced
                    if (source == DamageSource.FLY_INTO_WALL) {
                        return true;
                    }
                    break;
                case "lumi_aura":
                    // Light damage is reduced
                    if (source == DamageSource.LIGHTNING_BOLT) {
                        return true;
                    }
                    break;
            }
        }

        return false;
    }

    /**
     * Replace attacker's weapon with a spoon
     */
    private boolean replaceWeapon(LivingEntity attacker) {
        ItemStack heldItem = attacker.getMainHandItem();
        if (!heldItem.isEmpty() && !attacker.level().isClientSide) {
            // Replace with spoon
            ItemStack spoon = new ItemStack(Items.WOODEN_SWORD);
            attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spoon);
            return true;
        }
        return false;
    }

    /**
     * Intercept incoming projectiles
     */
    private boolean interceptProjectile(DamageSource source) {
        Entity directEntity = source.getDirectEntity();
        Entity causingEntity = source.getEntity();

        if (directEntity != null && !directEntity.level().isClientSide) {
            // Check for arrows, tridents, snowballs, etc.
            if (directEntity instanceof net.minecraft.world.entity.projectile.AbstractArrow ||
                directEntity instanceof net.minecraft.world.entity.projectile.ThrownTrident) {

                // Create interception effect
                Vec3 pos = directEntity.position();
                directEntity.discard();

                // Spawn particles
                spawnDefenseParticles(pos, "intercept");
                return true;
            }
        }

        return false;
    }

    /**
     * Create sonic boom effect
     */
    private boolean createSonicBoom(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity != null && !entity.level().isClientSide) {
            // Create explosion effect
            Level level = entity.level();
            BlockPos pos = entity.blockPosition();

            level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 2.0F,
                Level.ExplosionInteraction.NONE);

            spawnDefenseParticles(pos.getCenter(), "sonic_boom");
            return true;
        }
        return false;
    }

    /**
     * Handle TNT explosions
     */
    public boolean handleTNTExplosion(Explosion explosion) {
        // Cancel TNT explosions near genie
        if (genie.distanceToSqr(explosion.getPosition()) < 64) {
            return true; // Cancel explosion
        }
        return false;
    }

    /**
     * Handle void damage
     */
    public boolean handleVoidDamage(Player player) {
        // Teleport player to safety
        if (player.level().isClientSide) return false;

        BlockPos safePos = findSafePosition(player.blockPosition(), 8);
        if (safePos != null) {
            player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
            return true;
        }
        return false;
    }

    /**
     * Find a safe position near the player
     */
    @Nullable
    private BlockPos findSafePosition(BlockPos center, int radius) {
        Level level = genie.level();

        for (int y = center.getY() + radius; y > center.getY() - radius; y--) {
            for (int x = center.getX() - radius; x < center.getX() + radius; x++) {
                for (int z = center.getZ() - radius; z < center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Spawn defense particles
     */
    private void spawnDefenseParticles(Vec3 position, String type) {
        // Implementation would spawn particles
        // For now, just log
        System.out.println("Wishborne defense: " + type + " at " + position);
    }

    /**
     * Set invulnerability ticks
     */
    public void setInvulnerableTicks(int ticks) {
        this.invulnerableTicks = ticks;
    }

    /**
     * Check if genie is in phantom state
     */
    public boolean isPhantom() {
        return isPhantom;
    }

    /**
     * Set phantom state
     */
    public void setPhantom(boolean phantom) {
        isPhantom = phantom;
    }

    /**
     * Get current defense state
     */
    public String getDefenseState() {
        if (invulnerableTicks > 0) {
            return "INVULNERABLE";
        }
        if (isPhantom) {
            return "PHANTOM";
        }
        return "NORMAL";
    }
}
