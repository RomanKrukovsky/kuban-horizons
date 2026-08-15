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

    /** Возвращает среднее значение связи (для расчёта бюджета). */
    public int getAverageBond() {
        return (trust + respect + fear + affection + freedomDrive + power + corruption) / 7;
    }

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

    // ... остальные методы (observeWording, observeRescue и т.д.) оставлены без изменений
}
