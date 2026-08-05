package dev.romankrukovsky.kubanhorizons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Клиентская конфигурация мода: визуальные эффекты, звук, подсказки.
 * Не влияет на игровую логику и не синхронизируется с сервером.
 */
public final class KHClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue PARTICLE_DENSITY = BUILDER
            .comment("Density multiplier for decorative particles (steppe wind, press drips…).",
                    "Множитель плотности декоративных частиц (степной ветер, капли пресса…).")
            .defineInRange("particles.density", 1.0D, 0.0D, 2.0D);

    private static final ModConfigSpec.DoubleValue AMBIENCE_VOLUME = BUILDER
            .comment("Volume multiplier for Kuban Horizons ambient sounds.",
                    "Множитель громкости атмосферных звуков мода.")
            .defineInRange("ambience.volume", 1.0D, 0.0D, 1.0D);

    private static final ModConfigSpec.BooleanValue DETAILED_TOOLTIPS = BUILDER
            .comment("Show extended gameplay tooltips on Kuban Horizons items.",
                    "Показывать расширенные игровые подсказки на предметах мода.")
            .define("tooltips.detailed", true);

    private static final ModConfigSpec.BooleanValue DEBUG_OVERLAY = BUILDER
            .comment("Developer debug overlay (fertility values, irrigation networks).",
                    "Отладочный оверлей разработчика (значения плодородия, сети орошения).")
            .define("debug.overlay", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private KHClientConfig() {
    }

    public static double particleDensity() {
        return PARTICLE_DENSITY.get();
    }

    public static double ambienceVolume() {
        return AMBIENCE_VOLUME.get();
    }

    public static boolean detailedTooltips() {
        return DETAILED_TOOLTIPS.get();
    }

    public static boolean debugOverlay() {
        return DEBUG_OVERLAY.get();
    }
}
