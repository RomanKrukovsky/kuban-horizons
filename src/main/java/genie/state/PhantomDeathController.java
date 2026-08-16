package genie.state;

import genie.interfaces.WishborneState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Controller for phantom death states that replace traditional HP systems.
 * Manages state transitions and visual effects.
 */
public class PhantomDeathController implements WishborneState {

    private final Entity entity;
    private WishborneStateEnum state = WishborneStateEnum.MANIFESTED;
    private int anchorLevel = 100;
    private long lastStateChangeTime = 0;
    private static final long STATE_CHANGE_COOLDOWN = 2000; // 2 seconds

    public PhantomDeathController(Entity entity) {
        this.entity = entity;
    }

    @Override
    public WishborneStateEnum getState() {
        return this.state;
    }

    @Override
    public void setState(WishborneStateEnum state) {
        if (state == this.state) return;

        long currentTime = entity.level().getGameTime();
        if (currentTime - this.lastStateChangeTime < STATE_CHANGE_COOLDOWN && state != WishborneStateEnum.MANIFESTED) {
            return; // Prevent rapid state changes
        }

        this.state = state;
        this.lastStateChangeTime = currentTime;

        // Play sound and particles based on new state
        playStateTransitionEffects();
    }

    @Override
    public boolean isManifested() {
        return this.state == WishborneStateEnum.MANIFESTED;
    }

    @Override
    public boolean canTakeDamage() {
        return this.state == WishborneStateEnum.MANIFESTED || this.state == WishborneStateEnum.DISPERSED;
    }

    @Override
    public int getAnchorLevel() {
        return this.anchorLevel;
    }

    @Override
    public void setAnchorLevel(int level) {
        this.anchorLevel = Math.min(100, Math.max(0, level));
    }

    /**
     * Handles damage to the entity based on wishborne state
     * @param source The damage source
     * @param amount The damage amount
     * @return true if damage was applied, false if absorbed
     */
    public boolean handleDamage(DamageSource source, float amount) {
        if (!canTakeDamage()) {
            spawnAbsorptionParticles(amount * 2);
            return false; // Damage absorbed by state
        }

        if (this.state == WishborneStateEnum.MANIFESTED && this.anchorLevel > 0) {
            // Reduce damage based on anchor level
            float reducedAmount = amount * (this.anchorLevel / 100.0f);
            if (reducedAmount < 1.0f) reducedAmount = 1.0f;

            if (this.anchorLevel <= 25) {
                // Low anchor - take full damage but transition to DISPERSED
                this.state = WishborneStateEnum.DISPERSED;
                playStateTransitionEffects();
            } else {
                // Partial damage
                this.anchorLevel -= (int)(amount * 2);
                if (this.anchorLevel < 0) this.anchorLevel = 0;
            }
            return true;
        }

        return true; // Damage applied normally
    }

    /**
     * Handles entity death based on wishborne state
     * @param source The damage source that killed the entity
     */
    public void handleDeath(DamageSource source) {
        if (this.state == WishborneStateEnum.BANISHED) {
            // Already banished - no effect
            return;
        }

        if (this.state == WishborneStateEnum.SEALED) {
            // Sealed state - drop vessel instead
            dropVessel();
            return;
        }

        // Transition through states
        if (this.state == WishborneStateEnum.MANIFESTED) {
            this.state = WishborneStateEnum.DISPERSED;
            playStateTransitionEffects();
            this.anchorLevel = Math.max(0, this.anchorLevel - 50);
        } else if (this.state == WishborneStateEnum.DISPERSED) {
            this.state = WishborneStateEnum.SEALED;
            playStateTransitionEffects();
        } else if (this.state == WishborneStateEnum.SEALED) {
            this.state = WishborneStateEnum.BANISHED;
            playStateTransitionEffects();

            // Remove entity from world
            if (this.entity instanceof LivingEntity livingEntity) {
                livingEntity.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    /**
     * Attempts to recover from dispersed state
     * @return true if recovery was successful
     */
    public boolean attemptRecovery() {
        if (this.state == WishborneStateEnum.DISPERSED && this.anchorLevel >= 30) {
            this.state = WishborneStateEnum.MANIFESTED;
            this.anchorLevel = Math.min(100, this.anchorLevel + 20);
            playStateTransitionEffects();
            return true;
        }
        return false;
    }

    /**
     * Drops the vessel containing the genie
     */
    private void dropVessel() {
        if (this.entity instanceof Player player) {
            // TODO: Implement vessel dropping logic
            // This would drop the player's genie lamp or other vessel
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cYour genie vessel has been dropped!"));
        }
    }

    /**
     * Plays visual effects for state transitions
     */
    private void playStateTransitionEffects() {
        if (this.entity.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = this.entity.position();

            switch (this.state) {
                case MANIFESTED:
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.x, pos.y + 1, pos.z, 50, 0.5, 0.5, 0.5, 0.1);
                    serverLevel.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    break;

                case DISPERSED:
                    serverLevel.sendParticles(ParticleTypes.PORTAL,
                        pos.x, pos.y + 1, pos.z, 30, 0.5, 0.5, 0.5, 0.2);
                    serverLevel.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 1.5F, 0.8F);
                    break;

                case SEALED:
                    serverLevel.sendParticles(ParticleTypes.WITCH,
                        pos.x, pos.y + 1, pos.z, 20, 0.3, 0.3, 0.3, 0.1);
                    serverLevel.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.ARMOR_EQUIP_DIAMOND, SoundSource.NEUTRAL, 1.0F, 0.7F);
                    break;

                case BANISHED:
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        pos.x, pos.y + 1, pos.z, 40, 0.5, 0.5, 0.5, 0.15);
                    serverLevel.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.WITHER_DEATH, SoundSource.NEUTRAL, 2.0F, 0.5F);
                    break;
            }
        }
    }

    /**
     * Spawns absorption particles when damage is negated
     * @param amount The amount of damage being absorbed
     */
    private void spawnAbsorptionParticles(float amount) {
        if (this.entity.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = this.entity.position();

            for (int i = 0; i < amount * 2; i++) {
                double offsetX = (Math.random() - 0.5) * 2.0;
                double offsetY = Math.random() * 1.5;
                double offsetZ = (Math.random() - 0.5) * 2.0;

                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    pos.x + offsetX, pos.y + 1 + offsetY, pos.z + offsetZ,
                    1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Updates the controller every tick
     */
    public void tick() {
        if (this.state == WishborneStateEnum.DISPERSED && this.anchorLevel < 100) {
            // Gradually recover anchor level when dispersed
            this.anchorLevel = Math.min(100, this.anchorLevel + 1);
        }
    }
}