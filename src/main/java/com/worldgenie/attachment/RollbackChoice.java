package com.worldgenie.attachment;

import com.mojang.serialization.Codec;

/**
 * Enum representing the player's choice when a wish can be rolled back.
 * ROLLBACK_LAST_WISH is the key choice that triggers CausalLedger rollback.
 */
public enum RollbackChoice {
    NONE,                   // No rollback performed
    ROLLBACK_LAST_WISH,     // Rollback the most recent wish (uses CausalLedger)
    KEEP_CURRENT_STATE;     // Explicitly choose to keep the new state

    public static final Codec<RollbackChoice> CODEC =
            Codec.STRING.xmap(RollbackChoice::valueOf, RollbackChoice::name);
}
