package dev.romankrukovsky.kubanhorizons.client.render;

import dev.romankrukovsky.kubanhorizons.entity.Manul;
import dev.romankrukovsky.kubanhorizons.entity.ManulCoat;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Рендерер манула: одна модель, четыре текстуры окраса.
 *
 * <p>Пути текстур собираются один раз в статическую карту, а не на каждом
 * кадре: {@code Identifier} в горячем пути рендера — это аллокация на кадр на
 * каждую особь.</p>
 *
 * <p>Здесь же снимаются признаки, которых нет у сущности: сезонная пушистость
 * и «мокнет под дождём». Оба вычисляются из мира на клиенте, а не хранятся в
 * сущности, потому что от них не зависит ни одно игровое правило — это чисто
 * визуальные величины, и синхронизировать их по сети было бы расточительно.</p>
 */
public class ManulRenderer extends MobRenderer<Manul, ManulRenderState, ManulModel> {
    private static final Map<ManulCoat, Identifier> TEXTURES = new EnumMap<>(ManulCoat.class);

    static {
        for (ManulCoat coat : ManulCoat.values()) {
            TEXTURES.put(coat, KHIds.of("textures/entity/manul_" + coat.key() + ".png"));
        }
    }

    public ManulRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, new ManulModel(context.bakeLayer(layer)), 0.4F);
    }

    @Override
    public ManulRenderState createRenderState() {
        return new ManulRenderState();
    }

    @Override
    public void extractRenderState(Manul entity, ManulRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.coat = entity.coat().key();
        state.hissing = entity.isHissing();
        state.frozen = entity.isFrozen();
        // Сидение приходит двумя путями: игрок усадил прирученного зверя или
        // сработал ManulLoafGoal. Для рендера это одна и та же поза «булкой».
        state.sitting = entity.isInSittingPose() || entity.isLoafing();
        state.sleeping = entity.isSleeping();
        // «В воздухе» вместо точного момента прыжка: клиент знает только позу,
        // но её достаточно — поза в полёте узнаётся и без разгонной фазы.
        state.airborne = !entity.onGround() && !entity.isInWater();
        state.inRain = entity.isInWaterOrRain() && !entity.isInWater()
                && entity.level().isRaining();
        state.fluff = seasonalFluff(entity);
        // Сдвиг фазы по id: без него звери в кадре дышали бы синхронно.
        // Модуль 1.0 — модели нужна дробная часть, а не абсолютное число.
        state.seed = (entity.getId() * 0.618F) % 1.0F;
    }

    /**
     * Сезонная пушистость 0..1 по климату в точке зверя.
     *
     * <p>В моде нет системы времён года, поэтому «зима» определяется тем же
     * признаком, которым игра решает, идти снегу или дождю: холодно в этой
     * точке — шуба густая. Это даёт нужный эффект (манул в горах и зимой
     * круглее, в августовской степи суше) без новой подсистемы.</p>
     *
     * <p>Снежный покров добавляет ещё немного: визуально зверь «отвечает» на
     * погоду, которую игрок видит вокруг.</p>
     */
    private static float seasonalFluff(Manul entity) {
        BlockPos pos = entity.blockPosition();
        var level = entity.level();
        var biome = level.getBiome(pos).value();
        // warmEnoughToRain == тепло; иначе холодно и шуба гуще.
        float base = biome.warmEnoughToRain(pos, level.getSeaLevel()) ? 0.35F : 0.9F;
        if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SNOW)) {
            base += 0.1F;
        }
        // Мокрая шерсть прилегает — под дождём зверь визуально «худеет».
        if (entity.isInWater()) {
            base -= 0.3F;
        }
        return Mth.clamp(base, 0.0F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(ManulRenderState state) {
        // Неизвестный окрас — базовый степной: испорченное сохранение не должно
        // давать отсутствующую текстуру.
        return TEXTURES.getOrDefault(ManulCoat.byKey(state.coat), TEXTURES.get(ManulCoat.STEPPE));
    }
}
