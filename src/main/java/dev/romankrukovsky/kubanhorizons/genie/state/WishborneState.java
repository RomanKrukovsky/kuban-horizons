package dev.romankrukovsky.kubanhorizons.genie.state;

import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class WishborneState {

    private ManifestationState currentState = ManifestationState.MANIFESTED;
    private float realityAnchoring = 0.0f; // 0.0 = fully anchored, 1.0 = about to be banished
    private int schemaVersion = 1;

    public ManifestationState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(ManifestationState state) {
        this.currentState = state;
    }

    public float getRealityAnchoring() {
        return realityAnchoring;
    }

    public void increaseAnchoring(float amount) {
        this.realityAnchoring = Math.min(1.0f, this.realityAnchoring + amount);
    }

    public void decreaseAnchoring(float amount) {
        this.realityAnchoring = Math.max(0.0f, this.realityAnchoring - amount);
    }

    public boolean isBanished() {
        return currentState == ManifestationState.BANISHED || realityAnchoring >= 1.0f;
    }

    public void serialize(ValueOutput output) {
        output.putInt("SchemaVersion", schemaVersion);
        output.putString("State", currentState.name());
        output.putFloat("Anchoring", realityAnchoring);
    }

    public void deserialize(ValueInput input) {
        int version = input.getIntOr("SchemaVersion", 0);
        if (version == schemaVersion) {
            String stateName = input.getStringOr("State", "MANIFESTED");
            this.currentState = ManifestationState.valueOf(stateName);
            this.realityAnchoring = input.getFloatOr("Anchoring", 0.0f);
        }
    }
}
