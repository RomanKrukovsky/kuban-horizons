package genie.defense;

import genie.GenieStateSnapshot;
import genie.genie.KubanGenie;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Controls phantom death states instead of traditional HP system.
 * States: MANIFESTED, DISPERSED, SEALED, BANISHED
 */
public class PhantomDeathController {

    /**
     * Death states for wishborne
     */
    public enum PhantomState {
        MANIFESTED,   // Fully physical and vulnerable
        DISPERSED,    // Intangible, can reform
        SEALED,       // Sealed in vessel, cannot reform
        BANISHED      // Permanently removed
    }

    private final KubanGenie genie;
    private PhantomState currentState = PhantomState.MANIFESTED;
    private int reformCooldown = 0;
    private boolean isWild = false;

    public PhantomDeathController(KubanGenie genie) {
        this.genie = genie;
    }

    /**
     * Handle damage based on current state
     * @return true if damage was handled
     */
    public boolean handleDamage(float amount, @Nullable DamageSource source) {
        switch (currentState) {
            case MANIFESTED:
                // Normal damage handling
                return false;
            case DISPERSED:
                // Intangible - damage is reduced
                if (amount > 1.0F) {
                    genie.spawnReformParticles();
                }
                return true;
            case SEALED:
                // Sealed in vessel - no damage
                return true;
            case BANISHED:
                // Already banished - no effect
                return true;
        }
        return false;
    }

    /**
     * Change state
     */
    public void changeState(PhantomState newState) {
        if (newState != currentState) {
            this.currentState = newState;

            switch (newState) {
                case MANIFESTED:
                    genie.setEmotionalAura("manifested");
                    break;
                case DISPERSED:
                    genie.setEmotionalAura("dispersed");
                    break;
                case SEALED:
                    genie.setEmotionalAura("sealed");
                    break;
                case BANISHED:
                    genie.setEmotionalAura("banished");
                    break;
            }
        }
    }

    /**
     * Attempt to reform from dispersed state
     */
    public boolean attemptReform() {
        if (currentState == PhantomState.DISPERSED && reformCooldown <= 0) {
            changeState(PhantomState.MANIFESTED);
            reformCooldown = 20 * 60; // 1 minute cooldown
            return true;
        }
        return false;
    }

    /**
     * Update cooldowns
     */
    public void update() {
        if (reformCooldown > 0) {
            reformCooldown--;
        }
    }

    /**
     * Check if genie can be damaged
     */
    public boolean canTakeDamage() {
        return currentState == PhantomState.MANIFESTED;
    }

    /**
     * Check if genie is alive
     */
    public boolean isAlive() {
        return currentState != PhantomState.BANISHED && !isWild;
    }

    /**
     * Set wild state
     */
    public void setWild(boolean wild) {
        isWild = wild;
        if (wild) {
            changeState(PhantomState.BANISHED);
        }
    }

    /**
     * Save state to NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("phantom_state", currentState.name());
        tag.putInt("reform_cooldown", reformCooldown);
        tag.putBoolean("is_wild", isWild);
        return tag;
    }

    /**
     * Load state from NBT
     */
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("phantom_state")) {
            currentState = PhantomState.valueOf(tag.getString("phantom_state"));
        }
        reformCooldown = tag.getInt("reform_cooldown");
        isWild = tag.getBoolean("is_wild");
    }

    // Getters
    public PhantomState getCurrentState() {
        return currentState;
    }

    public int getReformCooldown() {
        return reformCooldown;
    }

    public boolean isWild() {
        return isWild;
    }

    /**
     * Get state display name
     */
    public Component getStateDisplay() {
        return switch (currentState) {
            case MANIFESTED -> Component.translatable("phantom.state.manifested");
            case DISPERSED -> Component.translatable("phantom.state.dispersed");
            case SEALED -> Component.translatable("phantom.state.sealed");
            case BANISHED -> Component.translatable("phantom.state.banished");
        };
    }
}
