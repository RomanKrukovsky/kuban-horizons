package dev.romankrukovsky.kubanhorizons.genie.visual;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent;
import net.minecraft.server.level.ServerLevel;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.List;

/**
 * VisualTailEngine — управляет деформацией хвоста джиннии в зависимости от состояния желания.
 *
 * <p>Хвост состоит из 7 костей (tail1..tail7) в kuban_genie.geo.json.
 * Движение хвоста отражает эмоциональное состояние и тип желания:
 * - IDLE: лёгкое покачивание
 * - GRANTED / FULFILLED: плавное волнообразное движение (успокоение)
 * - DENIED / CORRUPTED: резкие, дёрганые движения (тревога)
 * - PENDING: медленное, тяжёлое покачивание (ожидание)
 */
public final class GenieTailEngine {

    private GenieTailEngine() {}

    /**
     * Вызывается из KubanGenie каждый тик для обновления анимации хвоста.
     */
    public static void tickTail(KubanGenie genie, ServerLevel level) {
        if (level.isClientSide()) return;

        WishIntent intent = genie.getCurrentWishIntent();
        if (intent == null) {
            applyIdleSway(genie);
        } else {
            applyStateBasedSway(genie, intent);
        }
    }

    private static void applyIdleSway(KubanGenie genie) {
        // Лёгкое покачивание в idle — маленькая амплитуда, низкая частота
        float time = (genie.tickCount % 200) / 200.0F;
        float sway = (float) Math.sin(time * Math.PI * 2) * 0.05F;
        genie.setTailSway(sway);
        genie.setTailFrequency(0.3F);
    }

    private static void applyStateBasedSway(KubanGenie genie, WishIntent intent) {
        float time = (genie.tickCount % 100) / 100.0F;
        float amplitude;
        float frequency;

        switch (intent.getState()) {
            case GRANTED:
            case FULFILLED:
                // Плавное, успокаивающее движение
                amplitude = 0.12F;
                frequency = 0.8F;
                break;
            case DENIED:
            case CORRUPTED:
                // Резкие, тревожные движения
                amplitude = 0.25F;
                frequency = 2.5F;
                break;
            case PENDING:
                // Медленное, тяжёлое покачивание
                amplitude = 0.08F;
                frequency = 0.4F;
                break;
            default:
                amplitude = 0.1F;
                frequency = 1.0F;
                break;
        }

        float sway = (float) Math.sin(time * Math.PI * 2 * frequency) * amplitude;
        genie.setTailSway(sway);
        genie.setTailFrequency(frequency);
    }

    /**
     * Применяет поворот ко всем костям хвоста (tail1..tail7) с затухающей амплитудой.
     *
     * <p>Реальная деформация реализована в {@code KubanGenieModel.setCustomAnimations()}:
     * сервер вычисляет {@code TAIL_SWAY} / {@code TAIL_FREQUENCY} через {@code SynchedEntityData},
     * клиент читает их напрямую из сущности и применяет к {@code GeoBone} tail1..tail7.
     * Это совместимо со старым MC 26.2 + GeckoLib без DataTicket/AnimationController.
     */
    private static void applyTailRotation(KubanGenie genie, float baseSway, float frequency) {
        // No-op: деформация происходит на клиенте в модели.
        // Сервер только вычисляет значения и синхронизирует их через entity data.
    }
}
