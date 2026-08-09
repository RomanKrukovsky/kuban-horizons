package dev.romankrukovsky.kubanhorizons.client;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.config.KHClientConfig;
import dev.romankrukovsky.kubanhorizons.irrigation.IrrigationChannelBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Отладочный оверлей разработчика.
 *
 * <p>Настройка {@code debug.overlay} существовала как мёртвая галочка: её
 * читал только сам класс конфигурации. Здесь она включает то, что обещает —
 * показ невидимых чисел систем мода для блока под прицелом.</p>
 *
 * <p>ЧТО ИМЕННО ПОКАЗЫВАЕТСЯ И ПОЧЕМУ НЕ ПЛОДОРОДИЕ. Описание настройки
 * упоминало «значения плодородия», но плодородие живёт в attachment на
 * чанке и НЕ синхронизируется на клиент: {@code KHAttachments.CHUNK_FERTILITY}
 * заведён без {@code .sync()}, а {@link
 * dev.romankrukovsky.kubanhorizons.soil.SoilFertility} принимает только
 * {@code ServerLevel}. Клиент этого числа физически не знает, и оверлей,
 * рисующий «плодородие», показывал бы выдумку. Поэтому здесь показано то,
 * что клиенту действительно известно из состояний блоков:</p>
 * <ul>
 *   <li>влажность грядки ({@code MOISTURE 0..7}) — по ней видно, доходит ли
 *       вода; именно этим управляет система орошения;</li>
 *   <li>удалённость желоба от водозабора ({@code DISTANCE}) — сеть орошения
 *       целиком читается по этому числу, включая место обрыва;</li>
 *   <li>биом под ногами — четыре биома мода похожи на глаз, а атмосфера и
 *       спавн у них разные.</li>
 * </ul>
 * <p>Настоящее плодородие остаётся у почвенного щупа: он серверный и
 * отвечает точным числом. Дублировать его догадкой в HUD — хуже, чем не
 * показывать вовсе.</p>
 *
 * <p>Оверлей рисуется в {@code Post} фазе HUD: в {@code Pre} его перекрыл
 * бы сам HUD. Выключенный по умолчанию — это инструмент разработчика, а не
 * украшение.</p>
 */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID, value = Dist.CLIENT)
public final class KHDebugOverlay {
    /** Отступ от края экрана, чтобы строки не липли к рамке. */
    private static final int MARGIN = 4;

    /** Цвет строк: белый, непрозрачный. */
    private static final int COLOR = 0xFFFFFFFF;

    private KHDebugOverlay() {
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        if (!KHClientConfig.debugOverlay()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        // Отладочный экран F3 уже занимает левый верх — не спорим с ним.
        if (client.level == null || client.player == null || client.getDebugOverlay().showDebugScreen()) {
            return;
        }
        List<Component> lines = lines(client.level, client.player.blockPosition(), client.hitResult);
        if (lines.isEmpty()) {
            return;
        }
        int y = MARGIN;
        for (Component line : lines) {
            event.getGuiGraphics().text(client.font, line, MARGIN, y, COLOR);
            y += client.font.lineHeight + 1;
        }
    }

    /**
     * Строки оверлея для текущего прицела.
     *
     * <p>Вынесено отдельным методом без обращения к {@code Minecraft},
     * чтобы содержимое оверлея можно было проверить тестом, а не только
     * глазами на скриншоте.</p>
     *
     * @param level    мир (клиентский или серверный — читаются только
     *                 состояния блоков и биом)
     * @param playerAt позиция наблюдателя (для строки биома)
     * @param hit      результат прицеливания, может быть {@code null}
     */
    public static List<Component> lines(Level level, BlockPos playerAt, HitResult hit) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("debug.kubanhorizons.biome",
                level.getBiome(playerAt).getRegisteredName()));
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return lines;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FarmlandBlock) {
            lines.add(Component.translatable("debug.kubanhorizons.moisture",
                    state.getValue(FarmlandBlock.MOISTURE), FarmlandBlock.MAX_MOISTURE));
        }
        // Каменный желоб — наследник деревянного, поэтому одна проверка
        // покрывает оба: отдельная ветка была бы недостижимым кодом.
        if (state.getBlock() instanceof IrrigationChannelBlock) {
            lines.add(channelLine(state.getValue(IrrigationChannelBlock.DISTANCE)));
        }
        return lines;
    }

    /**
     * Строка про желоб: ноль означает «воды нет», а не «расстояние 0».
     *
     * <p>Разделение важно для отладки: сухой желоб и желоб вплотную к
     * водозабору — разные состояния, и путать их значит не видеть обрыв
     * сети.</p>
     */
    private static Component channelLine(int distance) {
        return distance <= 0
                ? Component.translatable("debug.kubanhorizons.channel.dry")
                : Component.translatable("debug.kubanhorizons.channel.distance", distance);
    }
}
