package dev.romankrukovsky.kubanhorizons.client.render;

import com.geckolib.animation.AnimationState;
import com.geckolib.animation.object.DataTicket;
import com.geckolib.animation.object.PlayState;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.model.GeoModel;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.visual.GenieTailData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * GeckoLib модель кубанской джиннии.
 * Реализует деформацию хвоста (tail1..tail7) на основе DataTicket.
 */
public class KubanGenieModel extends GeoModel<KubanGenie> {

    private static final ResourceLocation MODEL = KubanHorizons.id("geckolib/models/kuban_genie.geo.json");
    private static final ResourceLocation TEXTURE = KubanHorizons.id("textures/entity/kuban_genie.png");
    private static final ResourceLocation ANIM = KubanHorizons.id("geckolib/animations/kuban_genie.animation.json");

    @Override
    public ResourceLocation getModelResource(KubanGenie animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KubanGenie animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(KubanGenie animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(KubanGenie animatable, long instanceId, AnimationState<KubanGenie> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Получаем кости хвоста
        GeoBone tail1 = this.getAnimationProcessor().getBone("tail1");
        GeoBone tail2 = this.getAnimationProcessor().getBone("tail2");
        GeoBone tail3 = this.getAnimationProcessor().getBone("tail3");
        GeoBone tail4 = this.getAnimationProcessor().getBone("tail4");
        GeoBone tail5 = this.getAnimationProcessor().getBone("tail5");
        GeoBone tail6 = this.getAnimationProcessor().getBone("tail6");
        GeoBone tail7 = this.getAnimationProcessor().getBone("tail7");

        if (tail1 == null) return;

        // Читаем значения напрямую из сущности (совместимо со старым MC + GeckoLib)
        float baseSway = animatable.getTailSway();
        float freq = animatable.getTailFrequency();

        // Применяем затухающую деформацию по длине хвоста
        float t = (animatable.tickCount % 200) / 200.0F;
        float phase = t * Mth.TWO_PI * freq;

        applyTailBone(tail1, baseSway, phase, 1.0F);
        applyTailBone(tail2, baseSway, phase, 0.85F);
        applyTailBone(tail3, baseSway, phase, 0.70F);
        applyTailBone(tail4, baseSway, phase, 0.55F);
        applyTailBone(tail5, baseSway, phase, 0.40F);
        applyTailBone(tail6, baseSway, phase, 0.25F);
        applyTailBone(tail7, baseSway, phase, 0.10F);
    }

    private void applyTailBone(GeoBone bone, float baseSway, float phase, float falloff) {
        if (bone == null) return;
        float angle = baseSway * falloff * (float) Math.sin(phase);
        bone.setRotX(angle);
    }
}
