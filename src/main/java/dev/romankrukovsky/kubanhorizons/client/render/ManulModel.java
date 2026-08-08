package dev.romankrukovsky.kubanhorizons.client.render;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Модель кубанского манула — талисмана мода.
 *
 * <h2>Почему ванильная сетка, а не GeckoLib</h2>
 *
 * <p>GeckoLib в проекте есть и используется джиннией, но он требует, чтобы
 * <em>сама сущность</em> реализовала {@code GeoEntity}: сигнатура
 * {@code GeoEntityRenderer<T extends Entity & GeoAnimatable, R>} без этого не
 * компилируется. Класс {@code Manul} — обычный {@code TamableAnimal}, и он
 * принадлежит другому агенту. Переезд на GeckoLib означал бы правку чужого
 * файла, то есть конфликт, а не рендер.</p>
 *
 * <p>Вторая причина сильнее первой: текстуры четырёх окрасов уже нарисованы
 * (см. {@code tools/texgen/gen_manul.py}) под <em>ванильную</em> развёртку
 * 64×64 — голова 0,0; корпус 28,8; лапа 0,16; уши 0,32 и 8,32; опушка 16,32;
 * хвост 42,32. GeckoLib потребовал бы box-UV и полной перерисовки всех четырёх
 * файлов. Поэтому сетка строится на {@link QuadrupedModel#createBodyMesh},
 * как у кабана и овчарки, а развёртка не меняется ни на пиксель: промах в
 * texOffs здесь даёт шерсть корпуса на морде — тот самый класс ошибки, о
 * котором предупреждает {@link KubanQuadrupedModel}.</p>
 *
 * <p>Ванильная система при этом не «беднее»: {@code AnimationDefinition} и
 * {@link net.minecraft.client.animation.KeyframeAnimation} — это тот же
 * ключевой кадр, на котором сделаны варден и сниффер. Ограничение у неё одно:
 * событийные анимации ванильные мобы запускают таймером на сущности. Здесь
 * сущность править нельзя, поэтому фаза считается от общего
 * {@code ageInTicks} и id особи — детерминированно и без состояния.</p>
 *
 * <h2>Силуэт</h2>
 *
 * <p>Манула узнают не по общей мохнатости, а по округлости и приземистости.
 * Поэтому лапы короткие (6 px), к корпусу добавлена одна меховая оболочка
 * («шуба»), а к голове — опушка по бокам и толстый хвост. Это четыре своих
 * куба поверх ванильных шести: по ART_BIBLE §4 контраст силуэта важнее
 * внутренней детализации, и микродеталей, невидимых с трёх метров, здесь нет.</p>
 *
 * <h2>Про «умеренные анимации» и длинный список поз</h2>
 *
 * <p>Требование сдержанности и список из десятка движений противоречат друг
 * другу только на первый взгляд. Разрешение такое: в любой момент времени
 * двигается не больше двух-трёх узлов, а список — это набор
 * <em>взаимоисключающих</em> состояний, а не слои. Сон, сидение, шипение,
 * прыжок и разглядывание не могут случиться одновременно; бытовые движения
 * (потянуться, умыться, встряхнуться) идут по одному и только в покое. То есть
 * поз много, а одновременной подвижности — мало, как и требует §4.</p>
 */
public class ManulModel extends QuadrupedModel<ManulRenderState> {
    /** Толстый хвост — третья примета после морды и приземистости. */
    private final ModelPart tail;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    /** Меховая оболочка корпуса: ей задаётся сезонная пушистость. */
    private final ModelPart coat;
    /** Опушка по бокам головы — то, что делает морду широкой и плоской. */
    private final ModelPart ruff;

    /** Длительности бытовых движений в тиках. */
    private static final float STRETCH_TICKS = 40.0F;
    private static final float WASH_TICKS = 50.0F;
    private static final float SHAKE_TICKS = 16.0F;
    /** Период бытового цикла: раз в ~14 секунд у каждой особи своя фаза. */
    private static final float IDLE_CYCLE_TICKS = 280.0F;

    public ManulModel(ModelPart root) {
        super(root);
        this.tail = root.getChild("tail");
        this.coat = root.getChild("body").getChild("coat");
        ModelPart headPart = root.getChild("head");
        this.ruff = headPart.getChild("ruff");
        this.rightEar = headPart.getChild("right_ear");
        this.leftEar = headPart.getChild("left_ear");
    }

    /**
     * Собирает сетку манула.
     *
     * <p>Лапы 6 px вместо ванильных 8–12: приземистость — половина силуэта.
     * Все свои части лежат ниже y=32, где ванильная зона развёртки (голова 0,0;
     * корпус 28,8; лапа 0,16) уже не мешает. Слоты взяты из
     * {@code tools/texgen/gen_manul.py}, чтобы модель и текстура не разъехались.</p>
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = QuadrupedModel.createBodyMesh(6, false, false,
                CubeDeformation.NONE);
        PartDefinition root = mesh.getRoot();

        // «Шуба»: та же геометрия корпуса, раздутая деформацией. Отдельных
        // кубов на грудь, бока и спину нет намеренно — три оболочки дали бы
        // z-fighting на стыках и втрое больше UV, а на дистанции читались бы
        // как одна. Одна оболочка с масштабом решает ту же задачу.
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("coat",
                CubeListBuilder.create().texOffs(28, 8)
                        .addBox(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F,
                                new CubeDeformation(0.75F)),
                PartPose.ZERO);

        PartDefinition head = root.getChild("head");
        // Опушка (16,32) — 10x4x2, как в генераторе текстур.
        head.addOrReplaceChild("ruff",
                CubeListBuilder.create().texOffs(16, 32)
                        .addBox(-5.0F, -4.0F, -1.0F, 10.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -2.0F));

        // Уши маленькие и низко посаженные: у манула они почти не торчат.
        // Куб смещён от оси, поэтому pivot ставим в основание уха — иначе
        // прижимание вращало бы ухо вокруг центра морды, а не вокруг себя.
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-1.5F, -2.0F, -0.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(-2.0F, -4.0F, -0.5F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(8, 32)
                        .addBox(-1.5F, -2.0F, -0.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(2.0F, -4.0F, -0.5F));

        // Хвост (42,32) — 3x10x3, толстый и низко опущенный.
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(42, 32)
                        .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, 6.0F, 1.2F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ManulRenderState state) {
        // Ванильный setupAnim разворачивает голову по взгляду и качает лапами
        // по ходьбе. Всё, что ниже, либо дополняет это, либо перезаписывает
        // для поз — поэтому порядок важен.
        super.setupAnim(state);

        float time = state.ageInTicks;
        applyFluff(state.fluff);

        // Сон и сидение — позы, а не движения: они перезаписывают походку
        // целиком и выходят сразу, чтобы шаговые синусы не «дёргали» лапы.
        if (state.sleeping) {
            poseSleeping(state, time);
            return;
        }
        if (state.sitting) {
            poseSitting(state, time);
            return;
        }

        applyGait(state, time);
        applyEars(state, time);
        applyTail(state, time);

        if (state.frozen) {
            // Неподвижное разглядывание: голова замирает и смотрит в упор.
            // Именно эта тишина посреди чужого движения и читается как
            // «манул смотрит на творящийся хаос» — поэтому ни покачивания,
            // ни дыхания здесь нет.
            head.xRot = 0.0F;
            head.yRot = state.yRot * (Mth.PI / 180.0F) * 0.35F;
            // Глаза «расширяются» единственным доступным способом без второй
            // текстуры: голова чуть подаётся вперёд и вверх.
            head.z = -0.5F;
            head.y -= 0.3F;
            return;
        }

        if (state.airborne) {
            poseJump(state, time);
            return;
        }

        // Бытовые движения только в покое: на ходу они смазали бы силуэт.
        if (state.walkAnimationSpeed < 0.05F) {
            applyIdleActions(state, time);
        }
    }

    /**
     * Сезонная шуба: зимой оболочка толще, летом почти прилегает.
     *
     * <p>Масштабом, а не подменой модели: {@code fluff} приходит из рендерера
     * (снег/температура биома), и один множитель меняет силуэт с «кот» на
     * «шар» без единого нового куба.</p>
     */
    private void applyFluff(float fluff) {
        float swell = 0.9F + fluff * 0.35F;
        coat.xScale = swell;
        coat.yScale = 0.96F + fluff * 0.16F;
        coat.zScale = swell;
        // Опушка на морде растёт вместе с шубой — иначе зимой голова
        // выглядела бы стриженой на пушистом теле.
        float ruffSwell = 0.9F + fluff * 0.3F;
        ruff.xScale = ruffSwell;
        ruff.yScale = ruffSwell;
    }

    /**
     * Походка: тяжёлый медленный шаг, при беге — короткая частая рысь.
     *
     * <p>Ванильные лапы уже качаются; здесь добавляется вертикальное
     * покачивание корпуса — от него шаг читается как тяжёлый, а не как
     * скольжение. Амплитуда растёт с пушистостью: зимой зверь переваливается.</p>
     */
    private void applyGait(ManulRenderState state, float time) {
        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        // Бег: та же походка, но чаще и с меньшим замахом — «короткая рысь».
        boolean running = speed > 0.7F;
        float stride = running ? 1.15F : 0.6662F;
        float lift = running ? 0.9F : 1.25F;

        rightHindLeg.xRot = Mth.cos(pos * stride) * lift * speed;
        leftHindLeg.xRot = Mth.cos(pos * stride + Mth.PI) * lift * speed;
        rightFrontLeg.xRot = Mth.cos(pos * stride + Mth.PI) * lift * speed;
        leftFrontLeg.xRot = Mth.cos(pos * stride) * lift * speed;

        // Перевалка корпуса на каждый шаг: два «шага» на цикл, поэтому 2×.
        body.y += Mth.cos(pos * stride * 2.0F) * 0.45F * speed;
        body.zRot = Mth.cos(pos * stride) * 0.06F * speed;

        // Дыхание в покое: очень слабое, только чтобы зверь не был статуей.
        if (speed < 0.05F) {
            float breath = Mth.sin((time + state.seed) * 0.06F);
            body.y += breath * 0.18F;
            coat.yScale += breath * 0.01F;
        }
    }

    /** Уши: прижаты при шипении и под дождём, иначе чуть подрагивают. */
    private void applyEars(ManulRenderState state, float time) {
        float pinch;
        if (state.hissing) {
            // Прижатые уши — самый дешёвый и самый понятный сигнал угрозы.
            pinch = 1.0F;
        } else if (state.inRain) {
            // Под дождём уши тоже прижаты, но слабее: это дискомфорт, не злость.
            pinch = 0.45F;
        } else {
            pinch = 0.0F;
        }
        float twitch = pinch > 0.0F ? 0.0F
                : Mth.sin((time + state.seed) * 0.35F) * 0.05F;
        rightEar.zRot = -pinch * 0.9F + twitch;
        leftEar.zRot = pinch * 0.9F - twitch;
        // При шипении уши ещё и уезжают назад — вид «сплющенной» головы.
        rightEar.yRot = pinch * 0.5F;
        leftEar.yRot = -pinch * 0.5F;
    }

    /** Хвост: качается на ходу, замирает при угрозе, поджимается под дождём. */
    private void applyTail(ManulRenderState state, float time) {
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        if (state.hissing) {
            // Напряжённый хвост: поднят и почти неподвижен.
            tail.xRot = 0.75F;
            tail.yRot = Mth.sin(time * 0.8F) * 0.08F;
            return;
        }
        if (state.inRain) {
            // Под дождём хвост поджат к корпусу — зверь ищет укрытие.
            tail.xRot = 1.6F;
            tail.yRot = 0.0F;
            return;
        }
        tail.xRot = 1.2F - speed * 0.25F;
        // На ходу — качание в такт шагу; в покое — очень медленный дрейф,
        // от которого зверь выглядит живым, но не «виляющим», как собака.
        tail.yRot = speed > 0.05F
                ? Mth.cos(state.walkAnimationPos * 0.4F) * speed * 0.35F
                : Mth.sin((time + state.seed) * 0.05F) * 0.12F;
    }

    /**
     * Прыжок: корпус вытягивается, лапы подбираются.
     *
     * <p>Фаза берётся не от таймера, а от текущего состояния «в воздухе»:
     * точный момент отрыва на клиенте недоступен без правки сущности, зато
     * поза в воздухе узнаётся и без разгонной фазы.</p>
     */
    private void poseJump(ManulRenderState state, float time) {
        body.xRot = (Mth.PI / 2.0F) - 0.18F;
        rightFrontLeg.xRot = -0.9F;
        leftFrontLeg.xRot = -0.9F;
        rightHindLeg.xRot = 0.8F;
        leftHindLeg.xRot = 0.8F;
        // Хвост вытягивается назад — балансир, и заодно читаемый силуэт полёта.
        tail.xRot = 0.55F;
        tail.yRot = 0.0F;
    }

    /**
     * Сидит «булкой»: максимально круглый силуэт.
     *
     * <p>Это визитная поза манула, поэтому она собрана вручную, а не взята у
     * овчарки: корпус подаётся назад и вниз, лапы уходят под тело, шуба
     * раздувается, хвост оборачивается вдоль бока. Цель — чтобы с трёх метров
     * читался шар, а не сидящая кошка.</p>
     */
    private void poseSitting(ManulRenderState state, float time) {
        body.xRot = (Mth.PI / 2.0F) + 0.28F;
        // 13.7, а не «на глаз»: при xRot = pi/2 + 0.28 нижняя точка куба
        // корпуса (-10..+6 по y, -7..+1 по z) опускается на 9.5 px ниже
        // pivot, поэтому pivot 19.5 утопил бы зверя в землю почти на пять
        // пикселей. Значение посчитано так, чтобы низ корпуса лёг на пол
        // (y = 24) с запасом под раздутую шубу.
        body.y = 13.7F;
        body.zRot = 0.0F;

        head.y = 15.5F;
        head.z = -1.0F;
        head.xRot = 0.12F;
        head.yRot = state.yRot * (Mth.PI / 180.0F) * 0.5F;

        // Лапы убраны под корпус: наружу почти ничего не торчит.
        // 17.8, а не 20.5: куб лапы растёт вниз от pivot на 6 px, поэтому
        // pivot 20.5 воткнул бы передние лапы в землю на 2.7 px.
        rightFrontLeg.xRot = -0.15F;
        leftFrontLeg.xRot = -0.15F;
        rightFrontLeg.y = 17.8F;
        leftFrontLeg.y = 17.8F;
        rightHindLeg.xRot = -1.55F;
        leftHindLeg.xRot = -1.55F;
        rightHindLeg.y = 21.5F;
        leftHindLeg.y = 21.5F;

        // Сидя зверь всегда выглядит круглее: это и есть «булка».
        coat.xScale = 1.12F + state.fluff * 0.3F;
        coat.zScale = 1.05F + state.fluff * 0.22F;
        coat.yScale = 0.9F;

        // Хвост обёрнут вдоль бока — замыкает круг силуэта.
        tail.xRot = 1.75F;
        tail.yRot = 0.95F;

        applyEars(state, time);
        // Дыхание: единственный подвижный узел в этой позе.
        body.y += Mth.sin((time + state.seed) * 0.05F) * 0.15F;
    }

    /**
     * Спит клубком: голова уходит к хвосту, зверь превращается в шар.
     *
     * <p>Сон — самая «закрытая» поза: подвижен только один узел (медленное
     * дыхание), что прямо соответствует требованию умеренности §4.</p>
     */
    private void poseSleeping(ManulRenderState state, float time) {
        body.xRot = (Mth.PI / 2.0F) + 0.1F;
        // Как и в позе сидя, значение посчитано, а не подобрано глазом:
        // при xRot = pi/2 + 0.1 низ корпуса уходит на 8 px ниже pivot.
        body.y = 15.2F;
        body.zRot = 0.0F;

        // Голова прижата к боку и опущена — клубок замыкается.
        // 16.7 при наклоне 0.35: при 20.0/0.55 морда уходила под пол на 3.6 px.
        // Наклон тоже уменьшен — так морда ложится на лапы, а не зарывается.
        head.y = 16.7F;
        head.z = 3.0F;
        head.xRot = 0.35F;
        head.yRot = 1.15F;
        head.zRot = 0.35F;

        // Лапы полностью подобраны.
        rightFrontLeg.xRot = -1.65F;
        leftFrontLeg.xRot = -1.65F;
        rightHindLeg.xRot = -1.65F;
        leftHindLeg.xRot = -1.65F;
        rightFrontLeg.y = 22.0F;
        leftFrontLeg.y = 22.0F;
        rightHindLeg.y = 22.0F;
        leftHindLeg.y = 22.0F;

        coat.xScale = 1.15F + state.fluff * 0.25F;
        coat.yScale = 0.85F;
        coat.zScale = 1.1F + state.fluff * 0.2F;

        // Хвост укрывает морду — так манул и спит.
        tail.xRot = 1.9F;
        tail.yRot = 1.35F;

        // Уши расслаблены, но не прижаты: спящий зверь не злится.
        rightEar.zRot = -0.12F;
        leftEar.zRot = 0.12F;
        rightEar.yRot = 0.0F;
        leftEar.yRot = 0.0F;

        float breath = Mth.sin((time + state.seed) * 0.035F);
        body.y += breath * 0.3F;
        coat.yScale += breath * 0.015F;
    }

    /**
     * Бытовые движения в покое: потянуться, умыться лапой, встряхнуть шерсть.
     *
     * <p>Ванильные мобы запускают такие движения таймером на сущности. Здесь
     * сущность править нельзя, поэтому цикл детерминированно вычисляется от
     * общего тика и сдвига особи: за период {@link #IDLE_CYCLE_TICKS} каждый
     * зверь один раз что-нибудь делает, а какой именно жест — выбирает номер
     * цикла. Движения не накладываются: в каждый момент активно не больше
     * одного, и все три ветки взаимоисключающие.</p>
     */
    private void applyIdleActions(ManulRenderState state, float time) {
        float phase = time + state.seed * IDLE_CYCLE_TICKS;
        float local = phase % IDLE_CYCLE_TICKS;
        int cycle = (int) (phase / IDLE_CYCLE_TICKS);

        switch (cycle % 3) {
            case 0 -> {
                if (local < STRETCH_TICKS) {
                    animateStretch(local / STRETCH_TICKS);
                }
            }
            case 1 -> {
                if (local < WASH_TICKS) {
                    animateWash(local / WASH_TICKS, state);
                }
            }
            default -> {
                if (local < SHAKE_TICKS) {
                    animateShake(local / SHAKE_TICKS);
                }
            }
        }
    }

    /**
     * Потягивание: передние лапы вперёд, спина прогнута, зад приподнят.
     *
     * @param t нормализованное время движения 0..1
     */
    private void animateStretch(float t) {
        // Синус на полупериоде: плавный вход и выход без рывка на границе.
        float amount = Mth.sin(t * Mth.PI);
        body.xRot = (Mth.PI / 2.0F) - amount * 0.28F;
        body.y -= amount * 0.6F;
        rightFrontLeg.xRot = -amount * 1.25F;
        leftFrontLeg.xRot = -amount * 1.25F;
        head.xRot += amount * 0.4F;
        tail.xRot = 1.2F - amount * 0.7F;
    }

    /**
     * Умывание: зверь садится и водит передней лапой по морде.
     *
     * @param t нормализованное время движения 0..1
     */
    private void animateWash(float t, ManulRenderState state) {
        float amount = Mth.sin(t * Mth.PI);
        // Поза «сидя», но мягче, чем полная «булка»: движение должно читаться.
        body.xRot = (Mth.PI / 2.0F) + amount * 0.22F;
        body.y += amount * 1.2F;
        rightHindLeg.xRot = -amount * 1.3F;
        leftHindLeg.xRot = -amount * 1.3F;

        // Правая лапа поднята к морде и водит короткими движениями.
        float strokes = Mth.sin(t * Mth.PI * 6.0F) * amount;
        rightFrontLeg.xRot = -amount * 2.1F + strokes * 0.25F;
        rightFrontLeg.zRot = amount * 0.45F;
        rightFrontLeg.y -= amount * 1.5F;
        leftFrontLeg.xRot = -amount * 0.2F;

        // Голова наклоняется навстречу лапе — иначе лапа мыла бы воздух.
        head.xRot += amount * 0.3F + strokes * 0.12F;
        head.zRot = amount * 0.28F;
        tail.xRot = 1.5F;
    }

    /**
     * Встряхивание шерсти: быстрое вращение корпуса и головы.
     *
     * <p>Самое короткое движение из трёх: длинная тряска выглядит как судорога.
     * Именно оно «продаёт» густую шубу — поэтому шуба на нём тоже пружинит.</p>
     *
     * @param t нормализованное время движения 0..1
     */
    private void animateShake(float t) {
        float envelope = Mth.sin(t * Mth.PI);
        float wobble = Mth.sin(t * Mth.PI * 8.0F) * envelope;
        body.zRot = wobble * 0.22F;
        head.zRot = wobble * 0.35F;
        head.yRot += wobble * 0.25F;
        tail.yRot = wobble * 0.5F;
        rightEar.zRot = -wobble * 0.4F;
        leftEar.zRot = wobble * 0.4F;
        // Шуба вздрагивает вместе с корпусом — иначе тряска «не про шерсть».
        coat.xScale += Math.abs(wobble) * 0.06F;
        coat.zScale += Math.abs(wobble) * 0.06F;
    }
}
