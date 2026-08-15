package dev.romankrukovsky.kubanhorizons.client.screen;

/**
 * Клиентское состояние трансформации игрока для HUD и экрана.
 *
 * <p>Сервер шлёт {@link
 * dev.romankrukovsky.kubanhorizons.network.packet.s2c.S2CTransformationSync};
 * обработчик кладёт данные сюда. HUD и экран читают отсюда, а не из
 * {@code Minecraft.getInstance().player}, чтобы не зависеть от порядка
 * загрузки клиента и иметь одно место для значения прогресса, которого нет
 * в синхронизируемом кодеке attachment'а.</p>
 *
 * <p>Стадия — упрощённая клиентская 0..3: HUMAN, AWAKENING, HALF_GENIE, GENIE.
 * Прогресс — 0..100. {@code active} отдельно от стадии: HUMAN с
 * {@code active=false} — это просто игрок без трансформации, а HUMAN с
 * {@code active=true} — старт, когда прогресс только начал идти.</p>
 */
public final class TransformationClientState {
    private TransformationClientState() {
    }

    private static boolean active = false;
    private static int stageIndex = 0;
    private static float progress = 0.0f;

    public static void update(int stageIndex, float progress) {
        TransformationClientState.active = true;
        TransformationClientState.stageIndex = Math.max(0, Math.min(3, stageIndex));
        TransformationClientState.progress = Math.max(0.0f, Math.min(100.0f, progress));
    }

    public static void clear() {
        TransformationClientState.active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static int stageIndex() {
        return stageIndex;
    }

    public static float progress() {
        return progress;
    }
}