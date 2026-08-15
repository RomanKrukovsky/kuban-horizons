package dev.romankrukovsky.kubanhorizons.genie;

import dev.romankrukovsky.kubanhorizons.genie.state.ManifestationState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Нефизическое состояние Wishborne-сущности. Урон не меняет это состояние:
 * воздействовать на него могут только печати, договоры и законы реальности.
 */
public final class WishborneState {
    private static final int SCHEMA_VERSION = 1;

    public enum Presence {
        MANIFESTED,
        DISPERSED,
        SEALED,
        BANISHED
    }

    private Presence presence = Presence.MANIFESTED;
    private int anchoring;

    public Presence presence() {
        return presence;
    }

    public int anchoring() {
        return anchoring;
    }

    public boolean canAct() {
        return presence == Presence.MANIFESTED;
    }

    public boolean isBanished() {
        return presence == Presence.BANISHED;
    }

    public void setCurrentState(ManifestationState state) {
        if (state == null) {
            return;
        }
        this.presence = switch (state) {
            case MANIFESTED -> Presence.MANIFESTED;
            case DISPERSED -> Presence.DISPERSED;
            case SEALED -> Presence.SEALED;
            case BANISHED -> Presence.BANISHED;
        };
    }

    public void increaseAnchoring(float amount) {
        if (presence == Presence.BANISHED || amount <= 0.0f) {
            return;
        }
        this.anchoring = Mth.clamp(this.anchoring + (int) Math.ceil(amount), 0, 100);
        if (this.anchoring >= 100) {
            this.presence = Presence.SEALED;
        }
    }

    public boolean applyAnchoring(int strength) {
        if (presence == Presence.BANISHED || strength <= 0) {
            return false;
        }
        anchoring = Mth.clamp(anchoring + strength, 0, 100);
        if (anchoring == 100) {
            presence = Presence.SEALED;
            return true;
        }
        return false;
    }

    public void weakenAnchoring(int strength) {
        if (strength <= 0 || presence == Presence.BANISHED) {
            return;
        }
        anchoring = Mth.clamp(anchoring - strength, 0, 100);
        if (presence == Presence.SEALED && anchoring == 0) {
            presence = Presence.MANIFESTED;
        }
    }

    public void disperseAvatar() {
        if (presence == Presence.MANIFESTED) {
            presence = Presence.DISPERSED;
        }
    }

    public void restoreAvatar() {
        if (presence == Presence.DISPERSED) {
            presence = Presence.MANIFESTED;
        }
    }

    public void banish() {
        presence = Presence.BANISHED;
        anchoring = 0;
    }

    public void save(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putString("Presence", presence.name());
        output.putInt("Anchoring", anchoring);
    }

    public void load(ValueInput input) {
        try {
            presence = Presence.valueOf(input.getStringOr("Presence", Presence.MANIFESTED.name()));
        } catch (IllegalArgumentException ignored) {
            presence = Presence.MANIFESTED;
        }
        anchoring = Mth.clamp(input.getIntOr("Anchoring", 0), 0, 100);
        if (presence == Presence.SEALED && anchoring < 100) {
            anchoring = 100;
        }
    }
}
