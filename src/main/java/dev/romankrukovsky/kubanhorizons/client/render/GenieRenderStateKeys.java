package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.util.context.ContextKey;

/**
 * Ключи, под которыми джинновское состояние кладётся в состояние рендера.
 *
 * <p>Слой рендера в 26.2 получает только {@code AvatarRenderState} и не видит
 * ни сущность, ни её attachment. Поэтому модификатор состояния кладёт сюда
 * стадию один раз за кадр, а слой её читает — это штатный путь NeoForge
 * ({@code RegisterRenderStateModifiersEvent}) вместо чтения серверных данных
 * из потока отрисовки.</p>
 */
public final class GenieRenderStateKeys {
    /** Стадия превращения игрока; {@code null}, если игрок не джинния. */
    public static final ContextKey<PlayerGenieAttachment.Stage> GENIE_STAGE =
            new ContextKey<>(KHIds.of("genie_stage"));

    private GenieRenderStateKeys() {
    }
}
