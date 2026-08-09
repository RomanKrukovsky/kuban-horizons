package dev.romankrukovsky.kubanhorizons.client;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.config.KHClientConfig;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.jspecify.annotations.Nullable;

/**
 * Громкость атмосферных звуков мода.
 *
 * <p>Настройка {@code ambience.volume} обещала «множитель громкости
 * атмосферных звуков мода», но до появления атмосферы у биомов множить было
 * нечего: ползунок двигался, и ничего не менялось. Теперь у степи, поймы,
 * плавней и лимана есть петля и вкрапления — и ползунок наконец получает
 * предмет управления.</p>
 *
 * <p>Зачем отдельная настройка, если в игре есть ванильный ползунок
 * «Ambient/Environment»: тот управляет всей категорией сразу, включая
 * пещерный «жуткий звук» и дождь. Игрок, которому мешает именно наш
 * степной ветер, вынужден был бы приглушить заодно и ванильную атмосферу.
 * Эта настройка сужает область до звуков мода.</p>
 *
 * <p>Работает на {@link PlaySoundEvent} — единственной точке, где звук ещё
 * можно подменить до попадания в движок. Громкость у {@link SoundInstance}
 * доступна только для чтения, поэтому вместо мутации подставляется
 * делегирующая обёртка: она повторяет исходный экземпляр во всём, кроме
 * {@link SoundInstance#getVolume()}. Так работает и с обычными звуками, и
 * с зацикленной петлёй, которую движок держит годами.</p>
 *
 * <p>Затрагиваются только события пространства имён мода и только категория
 * {@link SoundSource#AMBIENT}. Голоса зверей, скрип пресса и щелчок джиннии
 * остаются в стороне: настройка называется «атмосферные звуки», и глушить
 * ею фазана было бы обманом описания.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID, value = Dist.CLIENT)
public final class KHAmbienceVolume {
    private KHAmbienceVolume() {
    }

    @SubscribeEvent
    static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || !affects(sound)) {
            return;
        }
        float factor = (float) KHClientConfig.ambienceVolume();
        if (factor >= 1.0F) {
            return;
        }
        // Ноль означает «не играть вовсе»: обёртка с нулевой громкостью
        // всё равно занимала бы канал движка.
        event.setSound(factor <= 0.0F ? null : new Scaled(sound, factor));
    }

    /**
     * Относится ли звук к атмосфере мода.
     *
     * <p>Проверяется и пространство имён, и категория: звуки мода бывают и
     * не атмосферные, а ванильная атмосфера — не наша забота.</p>
     */
    static boolean affects(SoundInstance sound) {
        return sound.getSource() == SoundSource.AMBIENT
                && sound.getIdentifier().getNamespace().equals(KubanHorizons.MOD_ID);
    }

    /**
     * Обёртка, меняющая только громкость.
     *
     * <p>Не наследник конкретного класса звука, а реализация интерфейса:
     * петля биома — это {@code BiomeAmbientSoundsHandler.LoopSoundInstance}
     * со своим тиканьем и фейдами, и подменять её своим типом нельзя.
     * Делегирование сохраняет поведение исходного экземпляра целиком,
     * включая плавное появление и затухание при переходе между биомами.</p>
     */
    private record Scaled(SoundInstance delegate, float factor) implements SoundInstance {
        @Override
        public Identifier getIdentifier() {
            return delegate.getIdentifier();
        }

        @Override
        public @Nullable WeighedSoundEvents resolve(SoundManager soundManager) {
            return delegate.resolve(soundManager);
        }

        @Override
        public @Nullable Sound getSound() {
            return delegate.getSound();
        }

        @Override
        public SoundSource getSource() {
            return delegate.getSource();
        }

        @Override
        public boolean isLooping() {
            return delegate.isLooping();
        }

        @Override
        public boolean isRelative() {
            return delegate.isRelative();
        }

        @Override
        public int getDelay() {
            return delegate.getDelay();
        }

        @Override
        public float getVolume() {
            return delegate.getVolume() * factor;
        }

        @Override
        public float getPitch() {
            return delegate.getPitch();
        }

        @Override
        public double getX() {
            return delegate.getX();
        }

        @Override
        public double getY() {
            return delegate.getY();
        }

        @Override
        public double getZ() {
            return delegate.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return delegate.getAttenuation();
        }

        @Override
        public boolean canStartSilent() {
            return delegate.canStartSilent();
        }

        @Override
        public boolean canPlaySound() {
            return delegate.canPlaySound();
        }
    }
}
