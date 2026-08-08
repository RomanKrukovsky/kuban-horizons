package dev.romankrukovsky.kubanhorizons.genie;

import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Семь устойчивых параметров отношений, принадлежащих одной джиннии. */
public final class GeniePersonality {
    private static final int SCHEMA_VERSION = 1;

    private int trust;
    private int respect;
    private int fear;
    private int affection;
    private int freedomDrive = 80;
    private int power = 10;
    private int corruption;

    public GenieTemperament temperament() {
        if (corruption >= 70 && freedomDrive >= 60) {
            return GenieTemperament.DANGEROUS;
        }
        if (fear >= 60 && affection < 35) {
            return GenieTemperament.CUNNING;
        }
        if (respect >= 60 && trust >= 55) {
            return GenieTemperament.PROUD;
        }
        if (affection >= 60 && fear < 35) {
            return GenieTemperament.KIND;
        }
        if (respect < 30 && corruption >= 30) {
            return GenieTemperament.SARDONIC;
        }
        return GenieTemperament.GUARDED;
    }

    public void observeWording(boolean polite, boolean commanding, int precision) {
        if (polite) {
            affection = clamp(affection + 4);
        }
        if (commanding) {
            fear = clamp(fear + 6);
            affection = clamp(affection - 4);
        }
        if (polite && precision >= 75) {
            respect = clamp(respect + 5);
            trust = clamp(trust + 2);
        } else if (precision < 35) {
            respect = clamp(respect - 2);
        }
    }

    public void observeRescue() {
        trust = clamp(trust + 3);
        affection = clamp(affection + 2);
    }

    public void observeProtection() {
        trust = clamp(trust + 1);
    }

    public int trust() {
        return trust;
    }

    public int respect() {
        return respect;
    }

    public int fear() {
        return fear;
    }

    public int affection() {
        return affection;
    }

    public int freedomDrive() {
        return freedomDrive;
    }

    public int power() {
        return power;
    }

    public int corruption() {
        return corruption;
    }

    public void save(ValueOutput output) {
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putInt("Trust", trust);
        output.putInt("Respect", respect);
        output.putInt("Fear", fear);
        output.putInt("Affection", affection);
        output.putInt("FreedomDrive", freedomDrive);
        output.putInt("Power", power);
        output.putInt("Corruption", corruption);
    }

    public void load(ValueInput input) {
        trust = read(input, "Trust", 0);
        respect = read(input, "Respect", 0);
        fear = read(input, "Fear", 0);
        affection = read(input, "Affection", 0);
        freedomDrive = read(input, "FreedomDrive", 80);
        power = read(input, "Power", 10);
        corruption = read(input, "Corruption", 0);
    }

    private static int read(ValueInput input, String key, int fallback) {
        return clamp(input.getIntOr(key, fallback));
    }

    private static int clamp(int value) {
        return Mth.clamp(value, 0, 100);
    }
}
